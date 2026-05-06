# OptiCare Assignment Algorithm: Detailed Flow and Component Interactions

This document explains how the assignment optimizer works, from workflow entry to final proposal approval, and how all `Algorithm` components collaborate.

## 1) Big Picture

The optimizer is a **two-stage search pipeline**:

1. **Feasibility gate**: quickly reject impossible scenarios using hard constraints.
2. **Optimization**:
   - Build a deterministic **greedy warm start**.
   - Improve it using **Simulated Annealing (SA)** over legal neighborhood moves.

The result is an `AssignmentProposal` containing:

- Baseline state and baseline cost (`baselineZ`)
- Best found state and cost (`proposedZ`)
- Iteration and stop metadata

Primary orchestrator: `Controller/DefaultAssignmentWorkflowService`.

---

## 2) End-to-End Runtime Flow

## Step A: Entry from workflow service

Method: `DefaultAssignmentWorkflowService.proposeAssignment(...)`

1. Resolve input baseline:
   - Use provided `currentState`, or create empty `AssignmentState`.
2. Run feasibility:
   - `FeasibilityChecker.check(department, patientById, baselineInput)`
3. If infeasible:
   - Return `AssignmentProposal(feasible=false, violations=...)`
   - No optimization run is started.
4. If feasible:
   - Build shared dependencies (`RiskMatrix`, `HardConstraints`, `CostCalculator`)
   - Run warm start
   - Run SA
   - Return feasible proposal with metrics and best state.

---

## Step B: Feasibility gate

Component: `Algorithm.feasibility.FeasibilityChecker`

It verifies before optimization:

1. **Capacity check**
   - `totalNeedingBeds = alreadyAssigned + eligibleWaiting`
   - Must be `<= totalBeds`
2. **Per-patient legal-bed existence**
   - For each eligible waiting patient, at least one free legal bed must exist.
3. **Current state validity**
   - Every existing assignment in the baseline must satisfy hard rules.

If any check fails, a list of human-readable violations is returned.

### Hard rule engine

Component: `Algorithm.feasibility.HardConstraints`

Used by both feasibility and neighborhood validation. Core predicates:

- `isBedClinicallyLegal(...)`
  - Bed not broken
  - Required bed type matches
  - Bariatric and ventilator requirements satisfied
  - Negative pressure room if required by risk policy
- `isCohortLegalInRoom(...)`
  - No forbidden risk pair in same room (from `RiskMatrix`)
- `isLegalAssignOrMoveToFreeBed(...)`
  - Combines free-bed check + clinical legality + cohort legality
- `isAssignmentStateGloballyValid(...)`
  - Full-state hard validation (used during neighbor validation)

---

## Step C: Deterministic warm start

Component: `Algorithm.greedy.GreedyWarmStart`

Purpose: create a good starting point for SA, deterministically.

Flow:

1. Copy baseline into a new mutable `AssignmentState`.
2. Build ordered waiting list:
   - Include only `WAITING` and not temporarily unavailable.
   - Sort via `WaitingListComparatorFactory.forGlobalQueue()`.
3. Build deterministic bed order:
   - By room id, then bed id.
4. For each waiting patient:
   - If currently unassigned, assign to the first legal free bed by `HardConstraints`.

Important behavior:

- It may leave patients unassigned if no legal bed exists.
- It is deterministic for same inputs (important for reproducibility).

---

## Step D: Objective construction and evaluation

Component: `Algorithm.CostCalculator`

`computeZ(...)` returns:

`Z_total = C_safety + C_clinical + C_policy + C_transfer + C_unassigned`

`CostCalculator` delegates each term to a strategy implementing `CostStrategy`.

### Cost terms and owners

1. **`C_safety`** (`SafetyCostStrategy`)
   - Cohorting risk penalties among patients sharing a room.
   - Uses `RiskMatrix`.

2. **`C_clinical`** (`ClinicalCostStrategy`)
   - Bed-feature fit and hard-incompatibility penalties (Big-M style).
   - Includes broken bed, required type, bariatric, ventilator, negative pressure.

3. **`C_policy`** (`PolicyCostStrategy`)
   - Distance from nurse station weighted by patient severity.

4. **`C_transfer`** (`TransferCostStrategy`)
   - Penalty for moves relative to initial baseline.
   - Optionally distance-scaled by topology graph.

5. **`C_unassigned`** (`UnassignedCostStrategy`)
   - Penalty for eligible waiting patients still unassigned.

---

## Step E: Simulated annealing improvement

Component: `Algorithm.sa.SimulatedAnnealingEngine`

Inputs:

- `warmStartState` (mutable working state)
- `baselineForTransfer` (fixed baseline reference for transfer term)
- `CostCalculator`, `AlgorithmConfig`, `HardConstraints`

Internal loop behavior:

1. Initialize seeded RNG (`config.randomSeed`) for reproducible runs.
2. Compute `zCurrent`, initialize `best` and `zBest`.
3. For each temperature level:
   - Repeat `iterationsPerTemperature`
   - Sample one legal neighbor move
   - Apply move in place
   - Recompute energy (`zNew`)
   - Accept if improved (`delta <= 0`) or probabilistically (`exp(-delta / T)`)
   - If rejected, undo move exactly
4. Cooling: `T = T * coolingRate`
5. Stop conditions:
   - Max iterations
   - Min temperature
   - Time limit
   - Thread interruption
   - Optional early cutoff:
     - Target energy reached
     - No-improvement steps threshold

Output: `SaResult(bestState, bestZ, iterations, finalTemperature, stoppedByTimeLimit)`

---

## 3) Neighborhood System (How SA explores)

### Move representation

Component: `Algorithm.neighborhood.NeighborMove`

Immutable descriptor containing move type and involved patient/bed IDs.

Move types:

- `ASSIGN`: unassigned patient -> free bed
- `MOVE`: assigned patient -> different free bed
- `SWAP`: two assigned patients exchange beds

### Move generation

Component: `RandomLegalNeighborSampler`

- Uses rejection sampling with attempt budget per iteration.
- Randomly picks one move family (`assign`, `move`, `swap`).
- Builds a candidate move from current state.
- Validates by:
  1. Applying move,
  2. Running `HardConstraints.isAssignmentStateGloballyValid(...)`,
  3. Undoing move.
- Returns first legal move or `null`.

### Move apply / undo

Component: `NeighborMoveExecutor`

- Applies move directly to shared working `AssignmentState`.
- Returns typed `UndoToken` (`AssignUndo`, `MoveUndo`, `SwapUndo`).
- Undo uses token to restore previous state deterministically.

This avoids full-state copy per proposal and keeps SA fast.

---

## 4) Shared Data and Contracts

### Assignment snapshot

Component: `AssignmentState`

Maintains two synchronized maps:

- `patientToBed`: patientId -> Bed
- `bedToPatient`: bedId -> patientId

Provides:

- `assign`, `unassign`, `isBedOccupied`
- `getBed(patientId)`, `getPatientIdInBed(bed)`
- Copy constructor for safe snapshots

Used across feasibility, warm start, SA, diff preview, and approval flow.

### Risk matrix source

Components: `RiskMatrixFactory`, `RiskMatrix`

- `RiskMatrixFactory` builds matrix from config (`bigM` source of truth).
- `RiskMatrix` defines pairwise cohort penalties.
- Forbidden pairs are represented by large penalties and used as hard constraints.

### Deterministic queue ordering

Component: `WaitingListComparatorFactory`

Global queue tuple:

1. Risk priority
2. Severity descending
3. Admission time ascending
4. Patient ID ascending

This ensures deterministic warm-start behavior.

---

## 5) Interaction Graph (Component-to-Component)

`DefaultAssignmentWorkflowService`
-> `FeasibilityChecker`
-> `HardConstraints`
-> (`RiskMatrixFactory` -> `RiskMatrix`)

`DefaultAssignmentWorkflowService`
-> `GreedyWarmStart`
-> `WaitingListComparatorFactory`
-> `HardConstraints`

`DefaultAssignmentWorkflowService`
-> `CostCalculator`
-> `SafetyCostStrategy` / `ClinicalCostStrategy` / `PolicyCostStrategy` / `TransferCostStrategy` / `UnassignedCostStrategy`

`DefaultAssignmentWorkflowService`
-> `SimulatedAnnealingEngine`
-> `RandomLegalNeighborSampler`
-> `NeighborMoveExecutor`
-> `HardConstraints` (global validation)
-> `CostCalculator` (energy comparison)

---

## 6) Proposal Lifecycle Outside the Core Optimizer

`DefaultAssignmentWorkflowService` also manages proposal state:

- `setPendingProposal(departmentId, proposal)`
- `getPendingProposal(departmentId)`
- `approvePendingProposal(...)` -> applies proposed state
- `rejectPendingProposal(...)` -> keeps current state

And operational helpers:

- `admitPatient(...)`
- `dischargePatient(...)`
- `buildWaitingQueueView(...)`

These methods connect optimization output to live workflow actions.

---

## 7) Practical Notes

- **Hard constraints** guard legality; SA should search mostly within legal space.
- **Big-M penalties** align objective with hard clinical safety requirements.
- **Warm start + SA** balances speed and solution quality.
- **Seeded RNG + deterministic comparators** improve reproducibility/debuggability.
- **Apply/undo neighborhood design** avoids expensive full-copy on each trial move.

