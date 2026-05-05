# OptiCare Project Plan (Deterministic)

This document is the final deterministic work plan for OptiCare. It is based on the theoretical background, the algorithmic problem definition, the chosen algorithm (Simulated Annealing), MVC architecture, and the current codebase. The intent is to remove ambiguity so the system is implemented consistently.

---

## 1. Problem summary and end goal

- **Goal:** Dynamic, optimal assignment of patients to departments/wards, rooms, and beds in a hospital.
- **Objective function:** Minimize  
  \( Z = \sum_{i} (C_{safety}^i + C_{clinical}^i + C_{policy}^i + C_{transfer}^i) + C_{unassigned} \)  
  with \( C_{unassigned} = W_{unassigned} \times \text{waitingCount} \) (eligible waiting patients not yet assigned).
- **Constraints:** Hard constraints (violation is forbidden / Big M) and soft constraints (finite penalties).
- **Core algorithm:** Simulated Annealing (SA) with three neighbor moves: **Assign**, **Move**, **Swap**.
- **Greedy initial state:** **Mandatory** warm start (does not reset the ward; it extends the current assignment).

**End product:** A **ward management application (UI/UX)** with **persistent storage** (default: JSON file-based; optional: MySQL at the final stage). The app maintains a persistent, always-available ward state, provides one-click assignment recommendations, shows feasibility + a clear quality score, and supports approve/reject. Optimization runs asynchronously (**cancel**, plus iteration / optional wall-clock limits from configuration) so the UI never freezes. There is no live intra-run SA callback stream in the current UI—it consumes the finished proposal only.

---

## 2. Architecture (MVC)

| Layer | Responsibility | Main components |
|------|----------------|-----------------|
| **Model** | Entities + data access | Patient, Room, Bed, Department, ClinicalData, PersonalDetails; topology graph; lookup tables |
| **Controller** | Workflow + optimization | Feasibility check, greedy warm start, SA engine, cost evaluation, waiting list policy, approve/reject |
| **View** | UI/UX | Ward map, status indicators, data entry, “Find assignment”, preview/diff, approve/reject, manual overrides |

---

## 3. Required data structures (per design)

| Structure | Use | Complexity |
|----------|-----|------------|
| **Directed weighted graph (rooms)** | Topology; walking adjacency; shortest paths to nurse station | Build: \(O(|V|+|E|)\), Dijkstra: \(O(E \log V)\) |
| **Assignment maps** | Current assignment and reverse lookup | \(O(1)\) lookup |
| **PriorityQueue (min-heap)** | Global waiting list with deterministic comparator | insert/extract \(O(\log n)\) |
| **FSM (PatientStatus)** | WAITING → ASSIGNED → DISCHARGED | \(O(1)\) |
| **2D RiskMatrix** | Cohorting penalty lookup between risk levels | \(O(1)\), space \(O(T^2)\) |

---

## 4. Cost function components (deterministic)

- **\(C_{safety}\)** (DECIDED): Cohorting within the same room. Pairwise sum using `RiskMatrix`.
  - Forbidden pairs ⇒ Big M (hard constraint).
  - Otherwise finite penalties (soft).
  - `RiskLevel.UNKNOWN` uses conservative **finite** matrix entries (not Big M by default).
- **\(C_{clinical}\)** (DECIDED): Bed fit (hard constraints): broken bed, required bed type mismatch, bariatric requirement, ventilator requirement; **explicit** `INFECTIOUS` requires negative-pressure room (null/UNKNOWN risk does not trigger that hard rule).
- **\(C_{policy}\)** (DECIDED): Nurse-station distance × severity. Uses `policyPenaltyWeight * distanceToNurseStation(room) * severityScore`.
- **\(C_{transfer}\)** (DECIDED): Transfer penalty relative to baseline assignment at SA start, scaled by spatial transport distance. Waiting-list assignments are not transfers.
  - Distance-aware form: \(C_{transfer} = W_{move} \times (1 + \alpha \cdot d_{shortest}(room_{baseline}, room_{current}))\) per moved patient.
  - Distances are looked up from precomputed shortest paths (no pathfinding inside SA iterations).
- **\(C_{unassigned}\)** (DECIDED): `unassignedPenaltyWeight *` count of `WAITING` patients on the department waiting list who are **not** `isTemporarilyUnavailable` and have **no** bed in the current `AssignmentState`.

---

## 5. Current project status (from the codebase)

### Implemented
- **Stage 3 (core optimizer):** `GreedyWarmStart`, `SimulatedAnnealingEngine`, `RandomLegalNeighborSampler`, `NeighborMoveExecutor`, `BedEquivalence`, `HardConstraints`, `SaResult`.
- **Model entities:** `Patient`, `PersonalDetails`, `ClinicalData`, `Bed`, `Room`, `Department` (includes `findRoomById`).
- **Enums:** `RiskLevel` (includes `UNKNOWN` + `waitingQueuePriority()` for future PQ), `Gender`, `BedType`, `PatientStatus` (`WAITING`, `ASSIGNED`, `DISCHARGED`).
- **Patient (pre–Stage 3):** `admittedAt` (`Instant`), `isTemporarilyUnavailable` (boolean).
- **Policy helper:** `PatientRiskPolicy` (null/missing risk → UNKNOWN for cohorting; isolation only if explicitly `INFECTIOUS`).
- **Assignment representation:** `AssignmentState` (patient→bed + bed→patient, deep copy).
- **Risk matrix:** `Algorithm.risk.RiskMatrix` (2D penalties with Big M rules + UNKNOWN column/row); **`Algorithm.risk.RiskMatrixFactory.fromConfig(AlgorithmConfig)`** is the only intended construction path.
- **Config:** `AlgorithmConfig` (Big M, transfer/policy/**unassigned** weights; SA parameters).
- **Cost function:** `CostCalculator` orchestrates **Strategy** implementations under `Algorithm.cost.*` (`Safety`, `Clinical`, `Policy`, `Transfer`, `Unassigned`); computes full \(Z\) including \(C_{unassigned}\) and baseline-aware transfer.
- **Feasibility:** `Algorithm.feasibility.FeasibilityChecker` + `Algorithm.feasibility.FeasibilityResult` (eligible waiting only: `WAITING`, not temporarily unavailable; capacity and legal-bed checks aligned with null/UNKNOWN clinical rules).
- **Topology graph:** `Algorithm.topology.RoomTopologyGraph` (room nodes).
- **Unit tests:** for the above components.

### Implemented (Stages 4–5 core — in-memory snapshot)
- **Controller/service:** `DefaultAssignmentWorkflowService` (and contract `AssignmentWorkflowService`) — propose, preview/diff, pending proposal, approve/reject, admit/discharge, waiting-queue view projection.
- **JavaFX Stage 5 shell:** drill-down dept/room/bed + waiting list, async optimize + cancel, preview/diff list, KPI + warnings + “why” panel, chart by optimize run ordinal, validated manual assignment override toward a free legal bed.

### Not implemented yet
- Optional MySQL persistence layer (repositories + schema + integration tests) — only if time allows, as a final stage.
- Audit trail (explicitly deferred past minimal Stage 7 RBAC).

---

## 6. Implementation stages (deterministic, stage-based)

All stages are **TDD-first**: write tests before implementation.

### Before Stage 3: locked decisions (no open interpretation)

These decisions must be treated as fixed requirements for implementation.

- **Control flow (DECIDED):** do not use `break` or `continue` **to control loops** (no early loop exit / skip-it). Prefer boolean flags, guarded `if`/`else`, early `return` from small helpers, or `Stream`/`Optional` when it stays readable. Prefer **`switch` expressions** (or arrow labels) instead of labeled fall-through `break` chains.
- **Canonical state (DECIDED):** baseline assignment at SA start + current candidate assignment.
  - Do not throw away the current ward state on new admissions.
- **Hard constraints (DECIDED):** never generate (or manually apply) moves that violate:
  - bed broken/occupied
  - missing required equipment (e.g., ventilator)
  - required bed type mismatch (including bariatric)
  - forbidden cohorting pair in the resulting room
- **Soft constraints (DECIDED):** nurse-station distance × severity; undesirable cohorting penalties.
- **Unassigned policy (DECIDED):**
  - Add `C_unassigned = W_unassigned * waitingCount` to the optimization objective.
  - `W_unassigned` must be very large (close to Big M, but finite), so the optimizer strongly prefers assignment when legal.
  - Optimization objective is:
    `Z_total = C_safety + C_clinical + C_policy + C_transfer + C_unassigned`.
- **Graph granularity (DECIDED):** room-level topology graph.
  - Special node `NURSE_STATION`.
  - Distances computed as shortest-path from nurse station.
  - `Room.distanceFromNurseStation` is a cache of that value (single source of truth: the graph).
- **Null policy (DECIDED):**
  - missing `clinicalData` or `requiredBedType`: no clinical penalty.
  - missing `riskLevel`: treat as `RiskLevel.UNKNOWN` (conservative penalties; not automatically forbidden).
  - missing gender: ignore gender policy term.
- **Temporarily unavailable patients (DECIDED):** boolean `Patient.isTemporarilyUnavailable` (persisted). Excluded from optimization:
  - WAITING + unavailable: not eligible for Assign.
  - ASSIGNED + unavailable: cannot Move/Swap.
- **Waiting list priority (DECIDED):** global department-level **PriorityQueue min-heap** maintained in Controller/Service.
  - DB/Model stores waiting list as a list; Controller builds/maintains PQ view.
  - Comparator tuple (smaller first): `(riskPriority, -severityScore, admittedAt, patientId)`.
  - Risk priority mapping: `IMMUNO_COMPROMISED=0`, `INFECTIOUS=1`, `RESPIRATORY=2`, `CLEAN=3`, `UNKNOWN=4`.
- **SA tuning (DECIDED):**
  - Geometric cooling: `T <- alpha * T` (alpha fixed).
  - Stop when any: max iterations, min temperature, no-improvement window, optional time limit.
  - Randomness must be seeded via config.
  - Neighbor generation: sample **one random legal move** per iteration.
- **Symmetry filter (DECIDED):**
  - Do not generate `Move`/`Swap` neighbors between equivalent beds in the same room.
  - Beds are equivalent if their assignment-relevant signature is identical (at minimum: `roomId`, `bedType`, `hasVentilator`, `isBroken=false`, and any future special-equipment flags).
  - This removes zero-information neighbors where `deltaZ = 0` and prevents iteration waste.
- **Greedy fallback policy (DECIDED):**
  - Greedy first tries legal-only placement for all waiting patients.
  - If legal-only greedy cannot place everyone, do **not** abort the run.
  - Start SA from the partial legal state with `C_unassigned` active.
  - Report infeasible only when feasibility checks prove no legal completion exists under hard constraints.
- **SA runtime model (DECIDED):**
  - Use a single working state with **delta/undo**.
  - Do not deep-copy assignment maps per neighbor.
  - Apply move in-place, compute `deltaZ`, accept/reject, undo immediately on rejection.
  - Deep-copy is allowed only when updating/publishing `bestState`.
- **Manual overrides (DECIDED):**
  - Hard constraint violation: block.
  - Soft penalty increase: allow with warning + `deltaZ`.
- **Preview/diff (DECIDED):** per-patient and per-bed diff + `Z`/`deltaZ` summary; approve/reject only (no partial apply).
- **Users and access control (DECIDED, minimal Stage 7):**
  - **Roles (fixed set):** `GUEST` (implicit, not stored), `NURSE`, `ADMIN`.
  - **GUEST:** ward drill-down view only (no optimization, no approve/reject, no manual override, no admit/discharge, no structure edits).
  - **NURSE:** everything the Stage 5–6 workflow already exposes, plus **admit** and **discharge** patient (in-memory + persisted ward JSON).
  - **ADMIN:** nurse capabilities plus **add department / room / bed** and **add user** (plaintext `users.json`; not production security).
  - **Enforcement:** UI disables/hides actions by role; handlers re-check role (minimal, not a separate security product). Stages 3–6 algorithm/controller code stays unchanged.
  - **Implementation timing:** minimal auth + UI gating is **Stage 7**; full audit trail / `AuthorizationService` / permission enum matrix remains optional backlog.

### Stage 1: Complete Model + core data structures **(done for pre–Stage 3 scope)**
- `RiskLevel.UNKNOWN`, `Patient.admittedAt`, `Patient.isTemporarilyUnavailable`, `Department.findRoomById`, `RiskLevel.waitingQueuePriority()` are implemented.
- Further model fields only if Stage 3+ discovers a gap.

### Stage 2: Cost function + feasibility **(done)**
- `RiskLevel.UNKNOWN`, null clinical / null risk policy, and `C_unassigned` are implemented in `CostCalculator` + strategies.
- `PatientRiskPolicy` centralizes cohorting vs isolation interpretation.
- **Strategy pattern:** `Algorithm.cost.CostStrategy` + `SafetyCostStrategy`, `ClinicalCostStrategy`, `PolicyCostStrategy`, `TransferCostStrategy`, `UnassignedCostStrategy`; `CostCalculator` sums them.
- **Factory pattern:** `Algorithm.risk.RiskMatrixFactory.fromConfig(AlgorithmConfig)` builds `Algorithm.risk.RiskMatrix`.
- `FeasibilityChecker` matches eligible-waiting and UNKNOWN/isolation rules above.

### Stage 3: Neighborhood + Simulated Annealing engine (core algorithm) **(done)**
- **Pre-Stage-3 preparation checkpoint (completed):**
  - Deterministic waiting comparator utility (`Algorithm.queue.WaitingListComparatorFactory`) with tuple order from locked decisions.
  - Canonical assignment hash utility (`Algorithm.determinism.AssignmentStateHasher`, SHA-256 over sorted `patientId=bedId` pairs).
  - Move contract types (`Algorithm.neighborhood.MoveType`, `Algorithm.neighborhood.NeighborMove`) prepared for delta/undo integration.
  - Reusable deterministic test fixtures (`Algorithm.fixtures.Stage3FixtureFactory`) and unit tests for comparator/hash/move contracts.
- Neighbor generation: `RandomLegalNeighborSampler` (Assign/Move/Swap, legal-only via `HardConstraints` + apply/validate/undo).
- Greedy warm start: `Algorithm.greedy.GreedyWarmStart` (deterministic queue order, partial placement allowed).
- SA engine: `Algorithm.sa.SimulatedAnnealingEngine` + `SaResult` (accept/reject, geometric cooling, seeded RNG, best state snapshot).
- In-place apply/undo: `Algorithm.neighborhood.NeighborMoveExecutor` (no per-neighbor map copies; best state deep-copied only on improvement).
- Symmetry: `Algorithm.neighborhood.BedEquivalence` filters zero-information Move/Swap in `RandomLegalNeighborSampler`.
- Shared hard rules: `Algorithm.feasibility.HardConstraints`; `FeasibilityChecker` delegates cohort + clinical checks.
- Config: `AlgorithmConfig` adds `maxTimeMillis`, `neighborSampleAttemptsPerIteration`.
- **Pre-Stage-4 extension (completed):**
  - `RoomTopologyGraph` computes all-pairs shortest paths once at startup (Floyd-Warshall), then serves O(1) distance lookups.
  - Startup requirement (mandatory): after loading department topology (rooms/edges) and before any call to `proposeAssignment` / SA optimization, call `precomputeAllPairsShortestPaths()` exactly once for the active topology snapshot.
  - If topology changes later (rooms/edges updated), recompute all-pairs shortest paths before the next optimization run.
  - Shortest-path algorithms must not run inside SA iteration loops.
  - `C_transfer` is distance-aware via graph shortest-path multiplier (same SA loop; no per-iteration pathfinding).

### Stage 4: Controller/service workflow (core system, in-memory persistence)
- Implement: admit/discharge, build waiting PQ view, propose assignment, preview/diff, approve/reject.
- Keep this stage focused on end-to-end workflow behavior in-memory; repository interface extraction can be deferred if needed.

### Stage 5: View + UI/UX (core workflow)
- **UI stack (DECIDED):** JavaFX (`javafx-controls`, `javafx-fxml`, `javafx-concurrent`, `javafx-charts`).
- **Navigation (implemented):** top-level lists with drill-down — **department → rooms → beds**; waiting-patient list tied to eligibility rules. Indicator-style labels (occupancy KPIs, occupancy on beds, feasibility text). This is **not** a graphical floor-plan viewer.
- Bed selection shows assigning patient id (from current `AssignmentState`); complementary “why” text for cohorting/equipment/transfer wording at high level when a occupied bed is selected.
- **“Find assignment”:** async (`javafx.concurrent.Task`) with **Cancel**; limits are **iteration count** and optionally **`AlgorithmConfig.maxTimeMillis`** inside the SA engine—not a distinct UI-driven progress channel. Result is consumed when the task **completes** (final `AssignmentProposal` / preview); the running SA working state is not surfaced to JavaFX controls mid-flight.
- **Preview/diff:** text summary plus per-row **patient assignment diff** (`PatientAssignmentDiff`): change type + from/to beds for non-unchanged patients.
- **Manual override (validated):** pick eligible **WAITING** patient + **free** target bed whose room satisfies hard rules; mutate in-memory assignment + statuses and waiting list consistency. Caller should discard any **stale pending** optimizer proposal whenever the ward state diverges outside Approve/Reject (e.g. after an override)—see Stage 5 optional polish.
- **Added for Stage 5 core (low effort / high impact):**
  - Top KPI bar: occupancy %, waiting count, unassigned estimate, **current/best Z** (last completed optimization for that pairing; rerun after manual ward edits if KPI Z must reflect the mutated graph).
  - Conflict/warning panel: hard feasibility violations / preview narrative.
  - “Why this assignment?” tooltip/title hint + textarea for qualitative breakdown when a patient is selected from a bed row.
- **Convergence chart (implemented):** `javafx-chart` adds a point per **successful** “Find assignment” completion (x = **run ordinal**, not SA iteration inside a run); y-values are **`baseline Z` vs `proposed/best Z`** from that outcome. Plotting is batch-style at run end—not streaming from intra-SA telemetry.
- **Follow-up Stage 5 backlog (optional after core UI is stable):**
  - **Graphical ward map:** spatial/topology canvas (beyond list drill-down); current product uses lists only.
  - Live per-iteration SA telemetry + chart updates (explicitly deferred; contrasts with convergence-by-run-number above).
  - Filter/search panel (risk level, room, patient id/name, status).
  - Legend + richer color coding for risk/isolation/bed-type states.
  - Session action history pane (optimize/approve/reject/manual override timeline).
  - Pending proposal / unsaved-change badge and related UX polish (today: clear stale pending proposal when overriding—must stay correct when persistence arrives).

### Pre-Stage-6 hardening checkpoint (deferred architectural cleanup)
- Extract explicit repository interfaces and add in-memory repository implementations behind them.
- Refactor controller/workflow services to depend on repository interfaces (not direct object mutation paths).
- Keep behavior unchanged; this is an architectural boundary step before file persistence work.

### Stage 6: Persistence layer (JSON file-based, required)
- Add JSON repositories + serialization/deserialization tests + integration tests.
- Swap in-memory repositories with JSON implementations without changing Controller/UI logic.
- Implement atomic write strategy for state-changing operations (approve assignment, manual override, discharge):
  - Write to temp file + fsync + atomic rename to avoid partial/corrupted files.
  - If persisted data changed since optimization started (snapshot/version token mismatch), reject commit and require re-run.
- Keep repository interfaces storage-agnostic so a DB backend can be added later without changing domain/controller code.

### Stage 7: Minimal RBAC + JavaFX auth (guest / nurse / admin)
- **Default session:** user starts as **GUEST** (no login required).
- **Login:** modal dialog (username + password only); credentials checked against `%USERPROFILE%\.opticare\users.json` (seed defaults `admin/admin`, `nurse/nurse` on first run; plaintext for coursework only).
- **Nurse:** all Stage 5–6 workflow actions + **Admit patient** (modal → waiting list + registry) + **Discharge patient** (selected waiting patient or occupant of selected bed).
- **Admin:** nurse capabilities + **Add department / room / bed** (modals; manual IDs) + **Add user** (modal → append to `users.json`).
- **Persistence:** ward snapshot remains `ward-state.json`; users remain separate `users.json`.
- **Tests:** users JSON round-trip + role capability helpers (minimal).

### Stage 8: Quality, performance, and tuning (formerly Stage 7)
- Unit tests for all cost components, SA behavior, and corner cases.
- Parameter tuning and profiling.
- Avoid recomputing expensive totals per iteration; prefer incremental updates where appropriate.
- **SOLID / Clean Code enforcement (DECIDED):**
  - Encapsulation: never expose mutable internal lists; use unmodifiable views or controlled methods.
  - JavaDoc: every public method in the SA engine, neighbor generation, and controller/workflow layer must have clear JavaDoc (purpose, inputs, outputs, and exceptions).
  - Logging: remove `System.out.println` usage; use SLF4J + Logback with levels (`INFO`, `DEBUG`, `ERROR`).

### Stage 8b (optional backlog): Audit trail + hardened RBAC
- Full `AuthorizationService`, permission enum matrix, password hashing, audit log persistence, and CI security gates — deferred until after core product + Stage 8 quality pass.

### Stage 9: CI/CD automation (GitHub Actions, DECIDED)
- CI/CD is implemented with **GitHub Actions** in `.github/workflows/`.
- Branch policy (applies from Stage 3 onward):
  - `main` and `dev` are protected branches.
  - Pull requests are required.
  - Only stage-appropriate required checks are enforced (defined below).

- **Stage-by-stage workflow rollout (single implementation path):**
  - **Stage 3 (algorithm stage):**
    - Introduce **`ci-core.yml`** and mark it required on PR/push to `dev`/`main`.
    - `ci-core.yml` runs `mvn -B clean test` and fails on any test failure.
    - Introduce **`determinism.yml`** and mark it required.
    - `determinism.yml` runs SA 3 times on the same fixed fixture with the same `randomSeed`, and must produce identical final canonical assignment (or identical canonical hash).
  - **Stage 6 (persistence stage):**
    - Introduce **`persistence-integration.yml`** and mark it required.
    - `persistence-integration.yml` runs JSON repository/integration tests (including atomic write and snapshot/version conflict cases).
  - **Stage 8 (quality/performance stage):**
    - Introduce **`quality.yml`** and mark it required.
    - `quality.yml` runs Checkstyle and SpotBugs; violations fail build.
    - Introduce **`security.yml`** and mark it required.
    - `security.yml` runs dependency vulnerability scanning; high/critical findings fail build.
    - Introduce **`performance.yml`**.
    - `performance.yml` runs deterministic heavy-fixture benchmarks with buffered threshold; starts as non-blocking for stabilization, then is promoted to required.
  - **Stage 9 (finalization stage):**
    - Introduce **`release.yml`** (manual trigger or tag-only).
    - `release.yml` builds runnable JAR only after required checks are green and uploads artifact to GitHub Releases.
    - Freeze the CI policy: all stage-introduced required workflows remain enforced.

---

## 7. Implementation notes

- **Performance:** per-iteration work must be small; avoid full-department scans when generating a neighbor.
- **Cohorting enforcement:** forbidden risk pairs are hard constraints; do not generate such neighbors.
- **Transfers:** transfer penalty is relative to baseline; it stabilizes assignments over time.
- **UI responsiveness:** SA runs in background; cancel + iteration/time caps are supported inside the SA engine; UI shows results when each async run completes.
- **Repository boundary:** keep Controller/UI storage-agnostic; persistence backend is swapped via repository implementations.
- **CI/CD policy:** GitHub Actions is mandatory for merge quality gates from Stage 3 onward, with required checks enabled progressively by stage (Stage 3/6/8) and finalized in Stage 9.

---

## 8. Summary: what is complete vs remaining

### Completed (high priority)
- Core entities/enums (including `UNKNOWN`, patient admission/unavailability), AssignmentState, RiskMatrix + factory, AlgorithmConfig (+ `W_unassigned`).
- CostCalculator (strategy-based Z + components including `C_unassigned`) and FeasibilityChecker (eligible waiting, UNKNOWN isolation rules).
- `PatientRiskPolicy`, `Department.findRoomById`.
- Room-level topology graph structure.
- **Stage 4 (in-memory):** `AssignmentWorkflowService` / `DefaultAssignmentWorkflowService` — propose, preview/diff, pending proposal, approve/reject, admit/discharge hooks, deterministic waiting-queue view comparator.
- **Stage 5 (core JavaFX):** `OptiCareApp` + `OptiCareViewFactory` — drill-down navigation, async optimize + cancel, preview/diff list, KPI bar, warnings/why panels, convergence chart by optimize **run ordinal**, validated manual assignment override (waiting → free legal bed).
- **Stage 6:** JSON ward snapshot persistence (`ward-state.json`), atomic writes, DTO/mapper + persistence tests.
- Unit tests including algorithm correctness smoke, brute-force micro optimum checks, scaled smoke, and Stage-4-style integration tests.

### Remaining (by stage)
- Stage 5: optional UI polish listed under **Follow-up Stage 5 backlog** (graphical ward map, search/filters, history, richer styling, optional live telemetry); core async workflow + preview + manual override + convergence-by-run chart are implemented in code.
- Pre-Stage-6 hardening: repository interfaces + in-memory repository adapters + controller wiring to interfaces.
- Stage 7: minimal RBAC + login + nurse admit/discharge + admin ward/user modals (`users.json` + UI gating).
- Stage 8: tuning + profiling + clean-code/logging hardening.
- Stage 8b (optional): audit + hardened RBAC (`AuthorizationService`, hashed passwords, audit log).
- Stage 9: CI/CD finalization and release workflow enforcement.
- Stage 10 (optional): MySQL backend as a drop-in persistence implementation (schema + JDBC + DB integration tests).
- Post-Stage-9 optimization backlog (optional): add a soft fragmentation/consolidation policy term to reduce unnecessary same-cohort room spread while preserving future isolation flexibility.
