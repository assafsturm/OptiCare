package View;

import Algorithm.AssignmentState;
import Algorithm.feasibility.HardConstraints;
import Algorithm.risk.RiskMatrixFactory;
import Config.AlgorithmConfig;
import Controller.AssignmentChangeType;
import Controller.AssignmentPreview;
import Controller.AssignmentProposal;
import Controller.DefaultAssignmentWorkflowService;
import Controller.PatientAssignmentDiff;
import Model.entety.Bed;
import Model.entety.ClinicalData;
import Model.entety.Department;
import Model.entety.Patient;
import Model.entety.Room;
import Model.enums.BedType;
import Model.enums.PatientStatus;
import Model.enums.RiskLevel;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stage 5 JavaFX shell: department/ward/room/bed drill-down + KPI + warnings + async optimize.
 */
public class OptiCareApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private final AlgorithmConfig config = new AlgorithmConfig();
    private final DefaultAssignmentWorkflowService workflowService = new DefaultAssignmentWorkflowService(config);

    private final List<Department> departments = new ArrayList<>();
    private final Map<String, Map<String, Patient>> patientByDepartmentId = new HashMap<>();
    private final Map<String, AssignmentState> currentStateByDepartmentId = new HashMap<>();
    private Task<AssignmentProposal> runningOptimizationTask;

    private final Label occupancyLabel = new Label();
    private final Label waitingLabel = new Label();
    private final Label unassignedLabel = new Label();
    private final Label currentZLabel = new Label("Current Z: -");
    private final Label bestZLabel = new Label("Best Z: -");
    private final Label selectedRoomLabel = new Label("Room: -");
    private final Label selectedBedLabel = new Label("Bed: -");
    private final Label selectedPatientLabel = new Label("Patient: -");
    private final Label departmentNameLabel = new Label("Department: -");
    private final Label departmentRoomsLabel = new Label("Rooms: -");
    private final Label departmentCapacityLabel = new Label("Capacity: -");
    private final TextArea warningsArea = new TextArea();
    private final TextArea whyArea = new TextArea();
    private final ListView<PatientAssignmentDiff> previewDiffList = new ListView<>();
    private final ListView<Department> departmentList = new ListView<>();
    private final ListView<Patient> waitingPatientList = new ListView<>();
    private final ListView<Room> roomList = new ListView<>();
    private final ListView<Bed> bedList = new ListView<>();
    private final XYChart.Series<Number, Number> bestZSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> currentZSeries = new XYChart.Series<>();

    private int optimizeRunIndex = 0;

    @Override
    public void start(Stage stage) {
        seedDemoData();
        configureListCellFactories();
        BorderPane root = new BorderPane();
        root.setTop(buildKpiBar());
        root.setLeft(buildDepartmentWardAndRoomPanel());
        root.setCenter(buildBedPanel());
        root.setRight(buildInsightsPanel());
        root.setBottom(buildActionsPanel());
        BorderPane.setMargin(root.getLeft(), new Insets(8));
        BorderPane.setMargin(root.getCenter(), new Insets(8));
        BorderPane.setMargin(root.getRight(), new Insets(8));
        BorderPane.setMargin(root.getBottom(), new Insets(8));

        configureListBindings();
        refreshKpis();
        refreshDepartmentOverview();

        Scene scene = new Scene(root, 1300, 760);
        stage.setTitle("OptiCare - Stage 5 UI");
        stage.setScene(scene);
        stage.show();
    }

    private HBox buildKpiBar() {
        return OptiCareViewFactory.buildKpiBar(occupancyLabel, waitingLabel, unassignedLabel, currentZLabel, bestZLabel);
    }

    private void configureListCellFactories() {
        departmentList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        departmentList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Department dept, boolean empty) {
                super.updateItem(dept, empty);
                if (empty || dept == null) {
                    setText(null);
                } else {
                    setText(dept.getName() + " (" + dept.getId() + ")");
                }
            }
        });

        roomList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        roomList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                if (empty || room == null) {
                    setText(null);
                } else {
                    setText(room.getId() + " (beds: " + room.getBeds().size() + ")");
                }
            }
        });
        waitingPatientList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        waitingPatientList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Patient p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                } else {
                    int severity = p.getClinicalData() == null ? 0 : p.getClinicalData().getSeverityScore();
                    RiskLevel risk = p.getClinicalData() == null || p.getClinicalData().getRiskLevel() == null
                            ? RiskLevel.UNKNOWN
                            : p.getClinicalData().getRiskLevel();
                    setText(p.getId() + " | " + risk + " | sev " + severity);
                }
            }
        });

        bedList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        bedList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Bed bed, boolean empty) {
                super.updateItem(bed, empty);
                if (empty || bed == null) {
                    setText(null);
                    setStyle("");
                    setTooltip(null);
                    return;
                }
                Department selectedDepartment = selectedDepartment();
                String pid = selectedDepartment == null
                        ? null
                        : currentStateFor(selectedDepartment).getPatientIdInBed(bed);
                if (pid == null) {
                    setText(bed.getId() + " | " + bed.getType() + " | EMPTY — available");
                    setStyle("-fx-background-color: #eaf6ea;");
                    setTooltip(new Tooltip("No patient assigned; valid target for manual assignment."));
                } else {
                    setText(bed.getId() + " | " + bed.getType() + " | OCCUPIED (" + pid + ")");
                    setStyle("-fx-background-color: #fff0f0;");
                    setTooltip(new Tooltip("Already assigned to " + pid + ". Manual override needs an EMPTY bed."));
                }
            }
        });

        previewDiffList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PatientAssignmentDiff d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : formatPatientDiffLine(d));
            }
        });
    }

    private static String formatPatientDiffLine(PatientAssignmentDiff d) {
        String from = d.fromBedId() != null ? d.fromBedId() : "\u2014";
        String to = d.toBedId() != null ? d.toBedId() : "\u2014";
        return d.patientId() + " | " + d.changeType() + " | " + from + " \u2192 " + to;
    }

    /** Lists only rows that differ from baseline (excludes UNCHANGED). */
    private static List<PatientAssignmentDiff> visiblePreviewDiffs(AssignmentPreview preview) {
        if (preview == null || preview.patientDiffs() == null) {
            return List.of();
        }
        return preview.patientDiffs().stream()
                .filter(d -> d.changeType() != AssignmentChangeType.UNCHANGED)
                .sorted(Comparator.comparing(PatientAssignmentDiff::patientId, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    private javafx.scene.layout.VBox buildDepartmentWardAndRoomPanel() {
        return OptiCareViewFactory.buildDepartmentWardAndRoomPanel(
                buildDepartmentOverviewCard(),
                departmentList,
                roomList,
                waitingPatientList,
                selectedRoomLabel
        );
    }

    private javafx.scene.layout.GridPane buildDepartmentOverviewCard() {
        return OptiCareViewFactory.buildDepartmentOverviewCard(departmentNameLabel, departmentRoomsLabel, departmentCapacityLabel);
    }

    private javafx.scene.layout.VBox buildBedPanel() {
        return OptiCareViewFactory.buildBedPanel(bedList, selectedBedLabel, selectedPatientLabel);
    }

    private javafx.scene.layout.VBox buildInsightsPanel() {
        return OptiCareViewFactory.buildInsightsPanel(warningsArea, previewDiffList, whyArea, bestZSeries, currentZSeries);
    }

    private HBox buildActionsPanel() {
        Button findAssignmentButton = new Button("Find Assignment");
        Button cancelButton = new Button("Cancel");
        Button approveButton = new Button("Approve");
        Button rejectButton = new Button("Reject");
        Button manualOverrideButton = new Button("Manual Override");
        findAssignmentButton.setOnAction(e -> runOptimizationAsync());
        cancelButton.setOnAction(e -> cancelOptimization());
        approveButton.setOnAction(e -> {
            Department selectedDepartment = selectedDepartment();
            if (selectedDepartment == null) return;
            AssignmentState current = currentStateFor(selectedDepartment);
            AssignmentState approved = workflowService.approvePendingProposal(selectedDepartment.getId(), current);
            currentStateByDepartmentId.put(selectedDepartment.getId(), approved);
            reconcileDepartmentAfterApproval(selectedDepartment, approved);
            warningsArea.setText("Approved pending proposal.");
            previewDiffList.getItems().clear();
            refreshKpis();
            refreshBeds();
            refreshWaitingPatients();
        });
        rejectButton.setOnAction(e -> {
            Department selectedDepartment = selectedDepartment();
            if (selectedDepartment == null) return;
            AssignmentState current = currentStateFor(selectedDepartment);
            AssignmentState rejected = workflowService.rejectPendingProposal(selectedDepartment.getId(), current);
            currentStateByDepartmentId.put(selectedDepartment.getId(), rejected);
            warningsArea.setText("Rejected pending proposal.");
            previewDiffList.getItems().clear();
            refreshKpis();
            refreshBeds();
            refreshWaitingPatients();
        });
        manualOverrideButton.setOnAction(e -> applyManualOverride());
        return OptiCareViewFactory.buildActionsPanel(
                findAssignmentButton, cancelButton, approveButton, rejectButton, manualOverrideButton
        );
    }

    private void configureListBindings() {
        departmentList.getItems().setAll(departments);
        if (!departments.isEmpty()) {
            departmentList.getSelectionModel().select(0);
        }
        departmentList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, dept) -> {
            roomList.getItems().setAll(dept == null ? List.of() : dept.getRooms());
            if (dept != null && !dept.getRooms().isEmpty()) {
                roomList.getSelectionModel().select(0);
            } else {
                bedList.getItems().clear();
            }
            refreshDepartmentOverview();
            refreshKpis();
            refreshBeds();
            refreshWaitingPatients();
            warningsArea.setText(dept == null ? "No department selected." : "Department switched to " + dept.getName() + ".");
            previewDiffList.getItems().clear();
            whyArea.clear();
            selectedRoomLabel.setText("Room: -");
            selectedBedLabel.setText("Bed: -");
            selectedPatientLabel.setText("Patient: -");
        });
        Department selected = selectedDepartment();
        if (selected != null) {
            roomList.getItems().setAll(selected.getRooms());
            if (!selected.getRooms().isEmpty()) {
                roomList.getSelectionModel().select(0);
            }
        }
        refreshWaitingPatients();
        roomList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, room) -> {
            selectedRoomLabel.setText("Room: " + (room == null ? "-" : room.getId()));
            refreshBeds();
        });
        bedList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, bed) -> {
            if (bed == null) {
                selectedBedLabel.setText("Bed: -");
                selectedPatientLabel.setText("Patient: -");
                whyArea.clear();
                return;
            }
            selectedBedLabel.setText("Bed: " + bed.getId() + " (" + bed.getType() + ")");
            Department selectedDepartment = selectedDepartment();
            String pid = selectedDepartment == null ? null : currentStateFor(selectedDepartment).getPatientIdInBed(bed);
            selectedPatientLabel.setText(pid == null ? "Status: EMPTY (bed available)" : "Status: OCCUPIED — " + pid);
            renderWhyPanel(pid);
        });
        refreshBeds();
    }

    private void refreshWaitingPatients() {
        Department selectedDepartment = selectedDepartment();
        waitingPatientList.getSelectionModel().clearSelection();
        waitingPatientList.getItems().clear();
        if (selectedDepartment != null) {
            waitingPatientList.getItems().addAll(workflowService.buildWaitingQueueView(selectedDepartment));
        }
        waitingPatientList.refresh();
        if (!waitingPatientList.getItems().isEmpty()) {
            waitingPatientList.getSelectionModel().select(0);
        }
    }

    private void refreshBeds() {
        Room selected = roomList.getSelectionModel().getSelectedItem();
        bedList.getSelectionModel().clearSelection();
        bedList.getItems().clear();
        if (selected != null) {
            bedList.getItems().addAll(selected.getBeds());
        }
        bedList.refresh();
        if (!bedList.getItems().isEmpty()) {
            bedList.getSelectionModel().select(0);
        }
    }

    private void refreshKpis() {
        Department selectedDepartment = selectedDepartment();
        if (selectedDepartment == null) {
            occupancyLabel.setText("Occupancy: -");
            waitingLabel.setText("Waiting: -");
            unassignedLabel.setText("Unassigned Estimate: -");
            return;
        }
        AssignmentState currentState = currentStateFor(selectedDepartment);
        int occupied = currentState.size();
        int capacity = selectedDepartment.getAllBeds().size();
        int waiting = workflowService.buildWaitingQueueView(selectedDepartment).size();
        int unassigned = Math.max(0, waiting - Math.max(0, capacity - occupied));
        double occupancyPct = capacity == 0 ? 0.0 : (100.0 * occupied / capacity);

        occupancyLabel.setText(String.format("Occupancy: %d/%d (%.1f%%)", occupied, capacity, occupancyPct));
        waitingLabel.setText("Waiting: " + waiting);
        unassignedLabel.setText("Unassigned Estimate: " + unassigned);
    }

    private void refreshDepartmentOverview() {
        Department selectedDepartment = selectedDepartment();
        if (selectedDepartment == null) {
            departmentNameLabel.setText("Department: -");
            departmentRoomsLabel.setText("Rooms: -");
            departmentCapacityLabel.setText("Capacity: -");
            return;
        }
        departmentNameLabel.setText("Department: " + selectedDepartment.getName() + " (" + selectedDepartment.getId() + ")");
        departmentRoomsLabel.setText("Rooms: " + selectedDepartment.getRooms().size());
        departmentCapacityLabel.setText("Capacity: " + selectedDepartment.getAllBeds().size() + " beds");
    }

    private void runOptimizationAsync() {
        if (runningOptimizationTask != null && runningOptimizationTask.isRunning()) {
            warningsArea.setText("Optimization is already running.");
            return;
        }
        Department selectedDepartment = selectedDepartment();
        if (selectedDepartment == null) {
            warningsArea.setText("Select a department before optimization.");
            return;
        }
        warningsArea.setText("Running optimization asynchronously...");
        Task<AssignmentProposal> task = new Task<>() {
            @Override
            protected AssignmentProposal call() {
                return workflowService.proposeAssignment(
                        selectedDepartment,
                        patientByDepartmentId.getOrDefault(selectedDepartment.getId(), Map.of()),
                        currentStateFor(selectedDepartment)
                );
            }
        };
        runningOptimizationTask = task;
        task.setOnSucceeded(evt -> {
            AssignmentProposal proposal = task.getValue();
            workflowService.setPendingProposal(selectedDepartment.getId(), proposal);
            AssignmentPreview preview = workflowService.buildPreview(proposal);
            optimizeRunIndex++;
            bestZSeries.getData().add(new XYChart.Data<>(optimizeRunIndex, proposal.proposedZ()));
            currentZSeries.getData().add(new XYChart.Data<>(optimizeRunIndex, proposal.baselineZ()));
            currentZLabel.setText(String.format("Current Z: %.2f", preview.baselineZ()));
            bestZLabel.setText(String.format("Best Z: %.2f", preview.proposedZ()));
            warningsArea.setText(buildWarningText(proposal, preview));
            previewDiffList.getItems().setAll(visiblePreviewDiffs(preview));
            runningOptimizationTask = null;
        });
        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            warningsArea.setText("Optimization failed: " + (ex == null ? "unknown error" : ex.getMessage()));
            previewDiffList.getItems().clear();
            runningOptimizationTask = null;
        });
        task.setOnCancelled(evt -> {
            warningsArea.setText("Optimization cancelled.");
            previewDiffList.getItems().clear();
            runningOptimizationTask = null;
        });
        Thread worker = new Thread(task, "opticare-optimizer");
        worker.setDaemon(true);
        worker.start();
    }

    private void cancelOptimization() {
        if (runningOptimizationTask != null && runningOptimizationTask.isRunning()) {
            runningOptimizationTask.cancel();
        } else {
            warningsArea.setText("No active optimization task to cancel.");
        }
    }

    private String buildWarningText(AssignmentProposal proposal, AssignmentPreview preview) {
        if (!proposal.feasible()) {
            return "Hard constraint violations:\n- " + String.join("\n- ", proposal.feasibilityViolations());
        }
        int listed = (int) preview.patientDiffs().stream()
                .filter(d -> d.changeType() != AssignmentChangeType.UNCHANGED)
                .count();
        return "Preview ready.\nChanged patients: " + preview.changedPatients()
                + "\nUnchanged patients: " + preview.unchangedPatients()
                + "\nDelta Z: " + String.format("%.2f", preview.deltaZ())
                + (listed == 0 && preview.changedPatients() == 0
                        ? "\n(No bed changes from baseline.)"
                        : "\nPer-patient moves: " + listed + " row(s) below (unchanged hidden).")
                + "\nUse Approve/Reject to apply.";
    }

    /**
     * Keeps patient lifecycle and waiting-list state consistent with approved assignments.
     * This prevents double counting in feasibility (assigned + still waiting).
     */
    private void reconcileDepartmentAfterApproval(Department department, AssignmentState approvedState) {
        if (department == null || approvedState == null) return;
        Map<String, Patient> patientById = patientByDepartmentId.getOrDefault(department.getId(), Map.of());
        for (Patient p : patientById.values()) {
            if (p != null && p.getId() != null) {
                boolean assigned = approvedState.getBed(p.getId()) != null;
                if (assigned) {
                    p.setStatus(PatientStatus.ASSIGNED);
                    department.getWaitingList().removeIf(w -> w != null && p.getId().equals(w.getId()));
                } else if (p.getStatus() != PatientStatus.DISCHARGED) {
                    p.setStatus(PatientStatus.WAITING);
                    boolean alreadyWaiting = department.getWaitingList().stream()
                            .anyMatch(w -> w != null && p.getId().equals(w.getId()));
                    if (!alreadyWaiting) {
                        department.getWaitingList().add(p);
                    }
                }
            }
        }
    }

    private void renderWhyPanel(String patientId) {
        if (patientId == null) {
            whyArea.setText("No patient selected. Select an occupied bed for a compact breakdown.");
            return;
        }
        Department selectedDepartment = selectedDepartment();
        if (selectedDepartment == null) {
            whyArea.setText("No department selected.");
            return;
        }
        Patient p = patientByDepartmentId.getOrDefault(selectedDepartment.getId(), Map.of()).get(patientId);
        if (p == null) {
            whyArea.setText("Patient metadata not found.");
            return;
        }
        whyArea.setText("Patient: " + patientId
                + "\nRisk: " + (p.getClinicalData() == null ? RiskLevel.UNKNOWN : p.getClinicalData().getRiskLevel())
                + "\nSeverity: " + (p.getClinicalData() == null ? 0 : p.getClinicalData().getSeverityScore())
                + "\nC_safety: room cohort compatibility"
                + "\nC_clinical: bed/equipment fit"
                + "\nC_policy: nurse distance x severity"
                + "\nC_transfer: baseline move distance");
    }

    private void applyManualOverride() {
        Department selectedDepartment = selectedDepartment();
        Patient selectedPatient = waitingPatientList.getSelectionModel().getSelectedItem();
        Bed selectedBed = bedList.getSelectionModel().getSelectedItem();
        if (selectedDepartment == null || selectedPatient == null || selectedBed == null) {
            warningsArea.setText("Manual override requires department + waiting patient + target bed selection.");
            return;
        }
        AssignmentState state = currentStateFor(selectedDepartment);
        if (state.isBedOccupied(selectedBed)) {
            String occ = state.getPatientIdInBed(selectedBed);
            warningsArea.setText("Manual override blocked: this bed is already OCCUPIED"
                    + (occ != null ? " (" + occ + ")." : ".")
                    + " In the bed list, choose a row that says EMPTY — available.");
            return;
        }
        HardConstraints hardConstraints = new HardConstraints(
                RiskMatrixFactory.fromConfig(config),
                selectedDepartment
        );
        Map<String, Patient> byId = patientByDepartmentId.getOrDefault(selectedDepartment.getId(), Map.of());
        if (!hardConstraints.isLegalAssignOrMoveToFreeBed(selectedPatient, selectedBed, state, byId)) {
            warningsArea.setText("Manual override blocked by hard constraints (clinical/cohort/isolation).");
            return;
        }
        state.assign(selectedPatient, selectedBed);
        selectedPatient.setStatus(PatientStatus.ASSIGNED);
        if (!selectedDepartment.getWaitingList().remove(selectedPatient)) {
            selectedDepartment.getWaitingList().removeIf(
                    p -> p != null && selectedPatient.getId() != null && selectedPatient.getId().equals(p.getId()));
        }
        workflowService.setPendingProposal(selectedDepartment.getId(), null);
        previewDiffList.getItems().clear();
        warningsArea.setText("Manual override applied for patient " + selectedPatient.getId() + " -> bed " + selectedBed.getId() + ". Pending optimizer proposal cleared—run Find Assignment again before Approve.");
        refreshKpis();
        refreshBeds();
        refreshWaitingPatients();
    }

    private void seedDemoData() {
        Room r1 = new Room("R1", "D1", 2, new java.util.ArrayList<>(), 5.0, true, true);
        Room r2 = new Room("R2", "D1", 2, new java.util.ArrayList<>(), 12.0, false, true);
        r1.getBeds().add(new Bed("B1", "R1", BedType.REGULAR, false, false));
        r1.getBeds().add(new Bed("B2", "R1", BedType.ICU, true, false));
        r2.getBeds().add(new Bed("B3", "R2", BedType.REGULAR, false, false));
        r2.getBeds().add(new Bed("B4", "R2", BedType.BARIATRIC, false, false));
        Department d1 = new Department("D1", "Internal", new java.util.ArrayList<>(List.of(r1, r2)), new java.util.ArrayList<>());

        Patient p1 = waiting("P1", RiskLevel.RESPIRATORY, 7, Instant.parse("2026-03-01T10:00:00Z"), false);
        Patient p2 = waiting("P2", RiskLevel.CLEAN, 3, Instant.parse("2026-03-01T11:00:00Z"), false);
        Patient p3 = waiting("P3", RiskLevel.IMMUNO_COMPROMISED, 8, Instant.parse("2026-03-01T09:00:00Z"), false);
        d1.getWaitingList().addAll(List.of(p1, p2, p3));
        Map<String, Patient> pByIdD1 = new HashMap<>();
        pByIdD1.put(p1.getId(), p1);
        pByIdD1.put(p2.getId(), p2);
        pByIdD1.put(p3.getId(), p3);
        patientByDepartmentId.put(d1.getId(), pByIdD1);
        AssignmentState stateD1 = new AssignmentState();

        Room r3 = new Room("R3", "D2", 2, new java.util.ArrayList<>(), 4.0, true, true);
        Room r4 = new Room("R4", "D2", 1, new java.util.ArrayList<>(), 9.0, false, true);
        r3.getBeds().add(new Bed("B5", "R3", BedType.ICU, true, false));
        r3.getBeds().add(new Bed("B6", "R3", BedType.REGULAR, false, false));
        r4.getBeds().add(new Bed("B7", "R4", BedType.REGULAR, false, false));
        Department d2 = new Department("D2", "Surgery", new java.util.ArrayList<>(List.of(r3, r4)), new java.util.ArrayList<>());
        Patient p4 = waiting("P4", RiskLevel.INFECTIOUS, 6, Instant.parse("2026-03-01T08:30:00Z"), false);
        Patient p5 = waiting("P5", RiskLevel.CLEAN, 2, Instant.parse("2026-03-01T08:40:00Z"), false);
        d2.getWaitingList().addAll(List.of(p4, p5));
        Map<String, Patient> pByIdD2 = new HashMap<>();
        pByIdD2.put(p4.getId(), p4);
        pByIdD2.put(p5.getId(), p5);
        patientByDepartmentId.put(d2.getId(), pByIdD2);
        AssignmentState stateD2 = new AssignmentState();

        departments.addAll(List.of(d1, d2));
        currentStateByDepartmentId.put(d1.getId(), stateD1);
        currentStateByDepartmentId.put(d2.getId(), stateD2);

        /* Apply seeded assignment before lists bind / paint so occupancy matches reality on first view.
           (Deferring via runLater mutated state without refreshing beds—cells showed EMPTY until selection changed.) */
        stateD1.assign(p2, r1.getBeds().get(0));
        p2.setStatus(PatientStatus.ASSIGNED);
        d1.getWaitingList().removeIf(p -> p != null && p2.getId().equals(p.getId()));
    }

    private Department selectedDepartment() {
        return departmentList.getSelectionModel().getSelectedItem();
    }

    private AssignmentState currentStateFor(Department department) {
        return currentStateByDepartmentId.computeIfAbsent(department.getId(), ignored -> new AssignmentState());
    }

    private Patient waiting(String id, RiskLevel risk, int severity, Instant admittedAt, boolean temporarilyUnavailable) {
        Patient p = new Patient(id, null, new ClinicalData(risk, severity, false, null), admittedAt, temporarilyUnavailable);
        p.setStatus(PatientStatus.WAITING);
        return p;
    }
}
