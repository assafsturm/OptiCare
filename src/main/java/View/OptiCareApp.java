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
import Auth.JsonFileUsersRepository;
import Auth.LoginResult;
import Auth.Role;
import Auth.RolePermissions;
import Auth.AppUser;
import Model.enums.BedType;
import Model.enums.PatientStatus;
import Model.enums.RiskLevel;
import Model.entety.PersonalDetails;
import Persistence.JsonFileWardStateRepository;
import Persistence.PersistencePaths;
import Persistence.WardStateMapper;
import Persistence.WardStateRepository;
import Persistence.dto.WardStateDocument;
import javafx.application.Application; // to use the javafx application framework
import javafx.concurrent.Task;// to run heavy work on a background thread so the javafx ui will not freeze
import javafx.geometry.Insets;// to set the padding of the ui elements
import javafx.scene.Scene;// to create the scene
import javafx.scene.chart.XYChart;// the charts 
import javafx.scene.control.Button;// the buttons
import javafx.scene.control.Label;// labels
import javafx.scene.control.ListCell; // for listview to show the correct text
import javafx.scene.control.ListView; // view for lists of items
import javafx.scene.control.SelectionMode; // to select multiple items or single item
import javafx.scene.control.TextArea; // text area for text input
import javafx.scene.control.Tooltip; // tooltips for hover
import javafx.scene.layout.BorderPane; // layout for the border pane
import javafx.scene.layout.HBox; // layout for the horizontal box
import javafx.scene.layout.VBox; // layout for the vertical box
import javafx.stage.Stage; // to create the stage

import java.io.IOException;
import java.time.Instant;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


// main application class, extends Application to use the javafx application framework
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

    private WardStateRepository wardStateRepository;
    private JsonFileUsersRepository usersRepository;
    private String bootstrapWarning;

    private Stage primaryStage;
    private Role currentRole = Role.GUEST;
    private String loggedInUsername = null;

    private Label roleLabel;
    private Button loginButton;
    private Button logoutButton;
    private Button findAssignmentButton;
    private Button cancelButton;
    private Button approveButton;
    private Button rejectButton;
    private Button manualOverrideButton;
    private Button admitPatientButton;
    private Button dischargePatientButton;
    private Button addDepartmentButton;
    private Button addRoomButton;
    private Button addBedButton;
    private Button addUserButton;

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

    private int optimizeRunIndex = 0;// index for the optimization run

    // startup -> load data -> layout -> buldings -> refresh -> show
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage; // main window
        usersRepository = new JsonFileUsersRepository(PersistencePaths.defaultUsersJsonPath());// create the users repository
        try {
            usersRepository.loadOrInitialize();// load or initialize the users file
        } catch (IOException e) {
            bootstrapWarning = "Could not initialize users file: " + e.getMessage();// 
        }

        loadOrSeedInitialState();// load or seed the initial state - load json -> hydrate -> applyHydration or seed demo data
        configureListCellFactories();// make sure viewList dosnet do T.toString()
        BorderPane root = new BorderPane();// create the root pane and plance sections in the correct order
        root.setTop(buildTopSection());// build method returns a subtree of controls (buttons, lists, charts)
        root.setLeft(buildDepartmentWardAndRoomPanel());
        root.setCenter(buildBedPanel());
        root.setRight(buildInsightsPanel());
        root.setBottom(buildBottomSection());
        BorderPane.setMargin(root.getLeft(), new Insets(8));// set the padding (spaces in bettwin sections)
        BorderPane.setMargin(root.getCenter(), new Insets(8));
        BorderPane.setMargin(root.getRight(), new Insets(8));
        BorderPane.setMargin(root.getBottom(), new Insets(8));

        configureListBindings();// for updates in view based on user actions
        refreshKpis();// refresh the KPIs
        refreshDepartmentOverview(); // refresh the department overview
        // refreshes in starte objects in memeory not ui. (acts liek a initialzion not refresh) (jsut here like that refresh normally after objects are being changed in memoey and need to be updatted in ui)
        applyRoleUi();// apply role base disabled buttons

        if (bootstrapWarning != null && !bootstrapWarning.isBlank()) {
            warningsArea.setText(bootstrapWarning);// set the warning setup if needed
        }
        // root for dynamic streching
        Scene scene = new Scene(root, 1300, 760);// create the scene (the stuff on the inside of the window)
        stage.setTitle("OptiCare");
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildTopSection() {// build the kpi and authentication section (the top)
        VBox top = new VBox(8); // create vertical box
        top.getChildren().add(OptiCareViewFactory.buildKpiBar(occupancyLabel, waitingLabel, unassignedLabel, currentZLabel, bestZLabel)); //build and add the kpi bar
        roleLabel = new Label("Role: Guest");
        loginButton = new Button("Log in");
        logoutButton = new Button("Log out");
        logoutButton.setDisable(true);// cant log out if not logged in
        loginButton.setOnAction(e -> onLoginClicked());
        logoutButton.setOnAction(e -> onLogoutClicked());
        HBox auth = new HBox(12, roleLabel, loginButton, logoutButton);// creat horizontal box for the authentication section
        auth.setPadding(new Insets(0, 10, 0, 10));// padding
        top.getChildren().add(auth);// add authentication
        return top;
    }

    private VBox buildBottomSection() {// build the actions section (the bottom)
        // create the buttons
        findAssignmentButton = new Button("Find Assignment");
        cancelButton = new Button("Cancel");
        approveButton = new Button("Approve");
        rejectButton = new Button("Reject");
        manualOverrideButton = new Button("Manual Override");
        admitPatientButton = new Button("Admit patient");
        dischargePatientButton = new Button("Discharge patient");
        addDepartmentButton = new Button("Add department");
        addRoomButton = new Button("Add room");
        addBedButton = new Button("Add bed");
        addUserButton = new Button("Add user");
        // set actions for the buttons
        findAssignmentButton.setOnAction(e -> runOptimizationAsync());// start optimization
        cancelButton.setOnAction(e -> cancelOptimization());// cancel optimization
        approveButton.setOnAction(e -> onApproveClicked());// approve proposal
        rejectButton.setOnAction(e -> onRejectClicked());// reject proposal
        manualOverrideButton.setOnAction(e -> applyManualOverride());// manual override
        admitPatientButton.setOnAction(e -> onAdmitPatientClicked());// admit patient
        dischargePatientButton.setOnAction(e -> onDischargePatientClicked());// discharge patient
        addDepartmentButton.setOnAction(e -> onAddDepartmentClicked());// add department
        addRoomButton.setOnAction(e -> onAddRoomClicked());// add room
        addBedButton.setOnAction(e -> onAddBedClicked());// add bed
        addUserButton.setOnAction(e -> onAddUserClicked());// add user
        // build horizontal boxes for the buttons
        HBox workflow = OptiCareViewFactory.buildActionsPanel(
                findAssignmentButton, cancelButton, approveButton, rejectButton, manualOverrideButton);
        HBox nurseRow = new HBox(10, admitPatientButton, dischargePatientButton);
        HBox adminRow = new HBox(10, addDepartmentButton, addRoomButton, addBedButton, addUserButton);
        VBox box = new VBox(10, workflow, nurseRow, adminRow);// creats  a grid like layout with the buttons
        return box;
    }

    private void onLoginClicked() {// login button action, maneges after someone presses Log in
        try {
            Optional<LoginResult> res = WardDialogs.showLoginDialog(primaryStage, usersRepository);// show the login dialog + handles authentication
            if (res.isPresent()) {
                currentRole = res.get().role();
                loggedInUsername = res.get().username();
                updateRoleLabel();// update the role label
                applyRoleUi();// apply the role ui (enable baesed on the role)
                warningsArea.setText("Logged in as " + loggedInUsername + " (" + currentRole + ").");
            }
        } catch (Exception ex) {
            warningsArea.setText("Login error: " + ex.getMessage());
        }
    }
    // logout button action 
    private void onLogoutClicked() {
        currentRole = Role.GUEST;// set the role to guest
        loggedInUsername = null;
        updateRoleLabel();// update the role label
        applyRoleUi();// apply the role ui dissabled buttons
        warningsArea.setText("Logged out - view only.");
    }

    private void updateRoleLabel() {// update the role label - setting the right text for the role label
        if (currentRole == Role.GUEST) {
            roleLabel.setText("Role: Guest ");
        } else {
            roleLabel.setText("Role: " + currentRole + (loggedInUsername == null ? "" : " — " + loggedInUsername));
        }
        logoutButton.setDisable(currentRole == Role.GUEST);// disable the logout button if the role is gust
    }
    // apply the role ui (enable/disable buttons baesed on the role)
    // called by onLoginClicked and onLogoutClicked
    private void applyRoleUi() {
        boolean nurseLike = RolePermissions.mayRunOptimization(currentRole);
        findAssignmentButton.setDisable(!nurseLike);
        cancelButton.setDisable(!nurseLike);
        approveButton.setDisable(!RolePermissions.mayApproveOrReject(currentRole));
        rejectButton.setDisable(!RolePermissions.mayApproveOrReject(currentRole));
        manualOverrideButton.setDisable(!RolePermissions.mayManualOverride(currentRole));
        admitPatientButton.setDisable(!RolePermissions.mayAdmitOrDischarge(currentRole));
        dischargePatientButton.setDisable(!RolePermissions.mayAdmitOrDischarge(currentRole));
        addDepartmentButton.setDisable(!RolePermissions.mayEditWardStructure(currentRole));
        addRoomButton.setDisable(!RolePermissions.mayEditWardStructure(currentRole));
        addBedButton.setDisable(!RolePermissions.mayEditWardStructure(currentRole));
        addUserButton.setDisable(!RolePermissions.mayManageUsers(currentRole));
    }
    // logic when new assignment is approved
    // checking permissions and getting the selected department and current state then approving the pending proposal and updating the state in memory and on disk
    private void onApproveClicked() {
        if (!RolePermissions.mayApproveOrReject(currentRole)) {// extra check (should be dissabled)
            warningsArea.setText("Log in as nurse or admin to approve.");
            return;
        }
        Department selectedDepartment = selectedDepartment();// get the selected department
        if (selectedDepartment == null) {
            return;
        }
        AssignmentState current = currentStateFor(selectedDepartment);// get the current state of the department
        AssignmentState approved = workflowService.approvePendingProposal(selectedDepartment.getId(), current);// approve the pending proposal (get the approved state from the proposal)
        currentStateByDepartmentId.put(selectedDepartment.getId(), approved); // put the approved state in the map by department id
        reconcileDepartmentAfterApproval(selectedDepartment, approved);// makes sure life sycle of patients is consistent
        warningsArea.setText("Approved pending proposal.");
        previewDiffList.getItems().clear();
        refreshKpis();// refresh the KPIs
        refreshBeds();// refresh the beds
        refreshWaitingPatients();// refresh the waiting patients
        persistWardSnapshot();// persist the ward snapshot (save the state to the json file)
    }
    // logic when new assignment is rejected
    // checking permissions and getting the selected department and current state then rejecting the pending proposal and updating the state in memory
    private void onRejectClicked() {
        if (!RolePermissions.mayApproveOrReject(currentRole)) {
            warningsArea.setText("Log in as nurse or admin to reject.");
            return;
        }
        Department selectedDepartment = selectedDepartment();
        if (selectedDepartment == null) {
            return;
        }
        AssignmentState current = currentStateFor(selectedDepartment);// get the current state of the department
        AssignmentState rejected = workflowService.rejectPendingProposal(selectedDepartment.getId(), current);// reject
        currentStateByDepartmentId.put(selectedDepartment.getId(), rejected); // put the rejected state in the map by department id (will be the same sate)
        warningsArea.setText("Rejected pending proposal.");
        previewDiffList.getItems().clear();
        refreshKpis();// refresh the KPIs
        refreshBeds();// refresh the beds
        refreshWaitingPatients();// refresh the waiting patients
    }

    private void onAdmitPatientClicked() {
        if (!RolePermissions.mayAdmitOrDischarge(currentRole)) {// extra check (should be dissabled)
            return;
        }
        Optional<WardDialogs.AdmitPatientInput> input = WardDialogs.showAdmitPatientDialog(primaryStage, departments);// open the admit patient dialog and hendle logic
        if (input.isEmpty()) {
            return;
        }
        WardDialogs.AdmitPatientInput x = input.get();
        // if the depatment is not in the map, create enrty and retuern the nested map, if it is return the nested map
        Map<String, Patient> reg = patientByDepartmentId.computeIfAbsent(x.department().getId(), k -> new HashMap<>()); // map method - give me the value for this key if non crate it store it and return
        if (reg.containsKey(x.patientId())) {// is the patient in depatment?
            warningsArea.setText("Patient id already exists in this department: " + x.patientId());
            return;
        }
        Patient p = buildPatientFromAdmit(x); // build the patient from the admit input
        workflowService.admitPatient(x.department(), p);// handle addmit logic
        reg.put(p.getId(), p); // put the patient in the map by id (this is not assigned just all patients in the department accros it all)
        refreshWaitingPatients();
        refreshKpis();
        persistWardSnapshot();// save the new "hospital" to the json file
        warningsArea.setText("Admitted patient " + p.getId() + " to waiting list.");
    }
    //called by onAdmitPatientClicked
    private static Patient buildPatientFromAdmit(WardDialogs.AdmitPatientInput x) {// build the patient from the admit input
        PersonalDetails pd = null;
        if (x.firstName() != null || x.lastName() != null || x.dateOfBirth() != null || x.gender() != null) {
            String fn = x.firstName() == null ? "" : x.firstName();
            String ln = x.lastName() == null ? "" : x.lastName();
            pd = new PersonalDetails(fn, ln, x.dateOfBirth(), x.gender());
        }
        ClinicalData cd = new ClinicalData(
                x.riskLevel(), x.severityScore(), x.needsVentilator(), x.requiredBedType(), x.weightKg());
        Patient p = new Patient(x.patientId(), pd, cd, x.admittedAt(), x.temporarilyUnavailable());
        p.setStatus(PatientStatus.WAITING); // set the patient status to waiting
        return p;
    }
    // handle discharge logic
    private void onDischargePatientClicked() {
        if (!RolePermissions.mayAdmitOrDischarge(currentRole)) {
            return;
        }
        Department d = selectedDepartment();// get the selected department
        if (d == null) {
            warningsArea.setText("Select a department first.");
            return;
        }
        String patientId = null;
        // start with waiting list
        Patient selWait = waitingPatientList.getSelectionModel().getSelectedItem();// what patient was cliked last
        if (selWait != null && selWait.getId() != null) {
            patientId = selWait.getId();
        } else {// if not waiting list, check beds
            Bed b = bedList.getSelectionModel().getSelectedItem();// what bed was cliked last
            if (b != null) {
                patientId = currentStateFor(d).getPatientIdInBed(b); // get the patient id in the bed
            }
        }
        if (patientId == null) { // if no patient id, show warning
            warningsArea.setText("Select a waiting patient or an occupied bed, then Discharge.");
            return;
        }
        AssignmentState next = workflowService.dischargePatient(d, patientId, currentStateFor(d));// disscarch patient from bed and return the new state
        currentStateByDepartmentId.put(d.getId(), next);// override the current state in memory with the new state
        refreshKpis();// refresh the KPIs
        refreshBeds();// refresh the beds
        refreshWaitingPatients();// refresh the waiting patients
        persistWardSnapshot();// save new state
        warningsArea.setText("Discharged patient " + patientId + ".");
    }
    // clear all pending proposals for all departments
    // caled by onAddDepartmentClicked, onAddRoomClicked, onAddBedClicked
    // if new somthing was added we need to clear the pending proposals to sync with the new structure
    private void clearPendingProposalsAllDepartments() {
        for (Department d : departments) {// clear all pending proposals for all departments
            workflowService.setPendingProposal(d.getId(), null);
        }
        previewDiffList.getItems().clear();
    }
     // create a new department and asve
    private void onAddDepartmentClicked() {
        if (!RolePermissions.mayEditWardStructure(currentRole)) {
            return;
        }
        Optional<WardDialogs.AddDepartmentInput> in = WardDialogs.showAddDepartmentDialog(primaryStage);// open the add department dialog and handle logic
        if (in.isEmpty()) {
            return;
        }
        String id = in.get().departmentId();
        if (departments.stream().anyMatch(dep -> id.equals(dep.getId()))) {// is the department id already in the list?
            warningsArea.setText("Department id already exists: " + id);
            return;
        }
        Department created = new Department(id, in.get().name(), new ArrayList<>(), new ArrayList<>());
        departments.add(created); // add the new department to the list
        patientByDepartmentId.put(id, new HashMap<>()); // create a new patient map for the department
        currentStateByDepartmentId.put(id, new AssignmentState()); // create a new state for the department
        departmentList.getItems().setAll(departments); // update the department list
        clearPendingProposalsAllDepartments(); // clear all pending proposals for all departments
        persistWardSnapshot(); // save the new state to the json file
        warningsArea.setText("Added department " + id + ".");
    }

    private void onAddRoomClicked() { // create a new room and save
        if (!RolePermissions.mayEditWardStructure(currentRole)) {
            return;
        }
        Optional<WardDialogs.AddRoomInput> in = WardDialogs.showAddRoomDialog(primaryStage, departments);// open the add room dialog and handle logic
        if (in.isEmpty()) {
            return;
        }
        WardDialogs.AddRoomInput x = in.get();
        if (x.department().getRooms().stream().anyMatch(r -> x.roomId().equals(r.getId()))) {// is the room id already in exsisting department?
            warningsArea.setText("Room id already exists in department: " + x.roomId());
            return;
        }
        Room room = new Room(x.roomId(), x.department().getId(), x.capacity(), new ArrayList<>(), // create a new room
                x.distanceFromNurseStation(), x.hasNegativePressure());
        x.department().getRooms().add(room); // add the new room to the department
        refreshRoomAndBedUiAfterStructureChange(x.department()); // refresh the room and bed ui 
        clearPendingProposalsAllDepartments();
        persistWardSnapshot(); // save the new state to the json file
        warningsArea.setText("Added room " + x.roomId() + " to " + x.department().getId() + ".");
    }

    private void onAddBedClicked() { // create a new bed and save
        if (!RolePermissions.mayEditWardStructure(currentRole)) {
            return;
        }
        Optional<WardDialogs.AddBedInput> in = WardDialogs.showAddBedDialog(primaryStage, departments);// open the add bed dialog and handle logic
        if (in.isEmpty()) {
            return;
        }
        WardDialogs.AddBedInput x = in.get();
        if (x.department().getAllBeds().stream().anyMatch(b -> x.bedId().equals(b.getId()))) {// is the bed id already in exsisting department?
            warningsArea.setText("Bed id already exists in department: " + x.bedId());
            return;
        }
        try {
            Bed bed = new Bed(x.bedId(), x.room().getId(), x.bedType(), x.hasVentilator(), x.broken()); // create a new bed
            x.room().addBed(bed); // add the new bed to the room
        } catch (RuntimeException ex) {
            warningsArea.setText("Could not add bed: " + ex.getMessage());
            return;
        }
        refreshRoomAndBedUiAfterStructureChange(x.department()); // refresh the room and bed ui
        clearPendingProposalsAllDepartments(); // clear all pending proposals for all departments
        persistWardSnapshot(); // save the new state to the json file
        warningsArea.setText("Added bed " + x.bedId() + " to room " + x.room().getId() + ".");
    }
    // called by onAddRoomClicked and onAddBedClicked
    private void refreshRoomAndBedUiAfterStructureChange(Department dept) { // refresh the room and bed ui after the structure change
        departmentList.getItems().setAll(departments); // update the department list
        departmentList.getSelectionModel().select(dept); // select the department
        roomList.getItems().setAll(dept.getRooms()); // update the room list
        if (!dept.getRooms().isEmpty()) { // if there are rooms, select the first one
            roomList.getSelectionModel().select(0);
        }
        refreshDepartmentOverview(); // refresh the department overview
        refreshKpis(); // refresh the KPIs
        refreshBeds(); // refresh the beds
        refreshWaitingPatients(); // refresh the waiting patients
    }
    // create a new user and save
    private void onAddUserClicked() {
        if (!RolePermissions.mayManageUsers(currentRole)) {
            return;
        }
        Optional<WardDialogs.AddUserInput> in = WardDialogs.showAddUserDialog(primaryStage);// open the add user dialog and handle logic
        if (in.isEmpty()) {
            return;
        }
        WardDialogs.AddUserInput x = in.get();
        try {
            usersRepository.addUser(new AppUser(x.username(), x.password(), x.role())); // create a new user and save
            warningsArea.setText("Saved user " + x.username() + " (" + x.role() + ").");
        } catch (IllegalArgumentException ex) {
            warningsArea.setText("Add user failed: " + ex.getMessage());
        } catch (IOException ex) {
            warningsArea.setText("Add user failed: " + ex.getMessage());
        }
    }
    // called by start
    private void loadOrSeedInitialState() { // load or seed the initial state - load json -> hydrate -> applyHydration or seed demo data
        Path persistencePath = PersistencePaths.defaultWardStateJsonPath();// get the path to the json file
        wardStateRepository = new JsonFileWardStateRepository(persistencePath); 
        try {
            var opt = wardStateRepository.loadIfPresent();// load the ward state if it exists
            if (opt.isEmpty()) {
                seedDemoData();// seed the demo data if file is not found
                return;
            }
            WardStateMapper.WardHydration h = WardStateMapper.hydrate(opt.get());// hydrate the ward state
            if (h.departments().isEmpty()) {
                seedDemoData();// seed the demo data if the file is empty
                bootstrapWarning = "Persistence file was empty; loaded demo data instead.\n" + persistencePath;
                return;
            }
            applyHydration(h);// apply the hydration to the state
        } catch (IllegalArgumentException ex) {
            seedDemoData();
            bootstrapWarning = "Could not load ward state (" + ex.getMessage() + "); using demo data.\n" + persistencePath;
        } catch (IOException ex) {
            seedDemoData();
            bootstrapWarning = "Could not read ward state file; using demo data.\n" + persistencePath + "\n" + ex.getMessage();
        }
    }
    // called by loadOrSeedInitialState
    private void applyHydration(WardStateMapper.WardHydration h) { // apply the hydration to the state
        departments.clear();// clear the departments
        departments.addAll(h.departments());// add the departments to the state
        patientByDepartmentId.clear();// clear the patient by department id
        for (var e : h.patientsByDepartmentId().entrySet()) {
            patientByDepartmentId.put(e.getKey(), new HashMap<>(e.getValue()));// add the patients to the state
        }
        currentStateByDepartmentId.clear();// clear the current state by department id
        for (var e : h.assignmentStates().entrySet()) {
            currentStateByDepartmentId.put(e.getKey(), e.getValue());// add the current state to the state
        }
        for (Department d : departments) {
            workflowService.setPendingProposal(d.getId(), null);// clear the pending proposals
        }
    }
    // called by onApproveClicked, onRejectClicked, onAdmitPatientClicked, onDischargePatientClicked, onAddDepartmentClicked, onAddRoomClicked, onAddBedClicked
    private void persistWardSnapshot() { // persist the ward snapshot - save the state to the json file
        if (wardStateRepository == null) {
            return;
        }
        try {
            WardStateDocument draft = WardStateMapper.captureDraft(// capture the draft of the state - java -> dto presist 
                    departments, patientByDepartmentId, currentStateByDepartmentId);
            long v = wardStateRepository.save(draft);// save the state to the json file
            String prior = warningsArea.getText();
            String note = "Saved ward state (version " + v + ") to " + wardStateRepository.getPersistencePath() + ".";
            warningsArea.setText(prior == null || prior.isBlank() ? note : prior + "\n" + note);
        } catch (IOException ex) {
            warningsArea.setText("Save failed: " + ex.getMessage());
        }
    }
    // override default behavior of listview to show the correct text
    // not doing T.toString
    private void configureListCellFactories() {
        departmentList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        departmentList.setCellFactory(list -> new ListCell<>() {// for depatment
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
        roomList.setCellFactory(list -> new ListCell<>() {// for room
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
        waitingPatientList.setCellFactory(list -> new ListCell<>() {// for waiting list
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
        bedList.setCellFactory(list -> new ListCell<>() {// for bed
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
                if (pid == null) { // if no patient in bed, show empty
                    setText(bed.getId() + " | " + bed.getType() + " | EMPTY — available");
                    setStyle("-fx-background-color: #eaf6ea;");// green background
                    setTooltip(new Tooltip("No patient assigned; valid target for manual assignment."));// tooltip on hover
                } else {
                    setText(bed.getId() + " | " + bed.getType() + " | OCCUPIED (" + pid + ")");
                    setStyle("-fx-background-color: #fff0f0;");// red background
                    setTooltip(new Tooltip("Already assigned to " + pid + ". Manual override needs an EMPTY bed."));// tooltip on hover
                }
            }
        });

        previewDiffList.setCellFactory(list -> new ListCell<>() {// for preview diff list
            @Override
            protected void updateItem(PatientAssignmentDiff d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : formatPatientDiffLine(d));
            }
        });
    }
    // format the patient diff line - patient id | change type | from bed id -> to bed id
    private static String formatPatientDiffLine(PatientAssignmentDiff d) {
        String from = d.fromBedId() != null ? d.fromBedId() : "\u2014";// uni code for dash
        String to = d.toBedId() != null ? d.toBedId() : "\u2014";// uni code for dash
        return d.patientId() + " | " + d.changeType() + " | " + from + " \u2192 " + to;// patient id | change type | from bed id -> to bed id
    }

    
    // called by buildInsightsPanel
    // filter out the unchanged diffs and sort the diffs by the patient id
    private static List<PatientAssignmentDiff> visiblePreviewDiffs(AssignmentPreview preview) {
        if (preview == null || preview.patientDiffs() == null) { // if no preview or no patient diffs, return empty list
            return List.of();
        }
        return preview.patientDiffs().stream()// stream the patient diffs and filter out the unchanged diffs and sort the diffs by the patient id
                .filter(d -> d.changeType() != AssignmentChangeType.UNCHANGED)
                .sorted(Comparator.comparing(PatientAssignmentDiff::patientId, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }
    // called by start
    // build the department ward and room panel
    private javafx.scene.layout.VBox buildDepartmentWardAndRoomPanel() {
        return OptiCareViewFactory.buildDepartmentWardAndRoomPanel(
                buildDepartmentOverviewCard(),
                departmentList,
                roomList,
                waitingPatientList,
                selectedRoomLabel
        );
    } // returns vbox with the department overview card, department list, room list, waiting patient list, and selected room label
// right
    private javafx.scene.layout.GridPane buildDepartmentOverviewCard() {// build the department overview card
        return OptiCareViewFactory.buildDepartmentOverviewCard(departmentNameLabel, departmentRoomsLabel, departmentCapacityLabel);
    } // returns gridpane with the department name, department rooms, and department capacity   
// center
    private javafx.scene.layout.VBox buildBedPanel() {// build the bed panel
        return OptiCareViewFactory.buildBedPanel(bedList, selectedBedLabel, selectedPatientLabel);
    } // returns vbox with the bed list, selected bed label, and selected patient label
// left
    private javafx.scene.layout.VBox buildInsightsPanel() {// build the insights panel  - returns vbox with the warnings area, preview diff list, why area, best z series, and current z series
        return OptiCareViewFactory.buildInsightsPanel(warningsArea, previewDiffList, whyArea, bestZSeries, currentZSeries);
    }
// wires the department -> room -> bed cascade and hooks selection listeners so the rest of the UI stays in sync.
    private void configureListBindings() {
        departmentList.getItems().setAll(departments); // fill the list with the departments in memory
        if (!departments.isEmpty()) { // if there are departments, select the first one
            departmentList.getSelectionModel().select(0);
        }
        
        // triggers and listeners for the department list
        // when the selected department changes, update the room list, bed list, department overview, KPIs, beds, waiting patients, warnings area, preview diff list, why area, selected room label, selected bed label, and selected patient label
        departmentList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, dept) -> {
            roomList.getItems().setAll(dept == null ? List.of() : dept.getRooms());// replace with new room list, if no rooms empty list
            if (dept != null && !dept.getRooms().isEmpty()) { // if there are rooms, select the first one
                roomList.getSelectionModel().select(0);
            } else { // if no rooms, clear the bed list
                bedList.getItems().clear();
            }
            refreshDepartmentOverview();// refresh the department overview for viewing the change
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
        // bec listeners dont rerun after the initial selection, we need to manually set the room list and select the first room
        Department selected = selectedDepartment();
        if (selected != null) { // if there is a selected department, set the room list and select the first room
            roomList.getItems().setAll(selected.getRooms());
            if (!selected.getRooms().isEmpty()) { // if there are rooms, select the first one
                roomList.getSelectionModel().select(0);
            }
        } 
        refreshWaitingPatients();// refresh the waiting patients to the current selected department
        // triggers and listeners for the room list
        // when the selected room changes, update the bed list and selected room label
        roomList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, room) -> {// when the selected room changes, update the bed list and selected room label
            selectedRoomLabel.setText("Room: " + (room == null ? "-" : room.getId()));
            refreshBeds();
        });
        // triggers and listeners for the bed list
        // when the selected bed changes, update the selected bed label, selected patient label, and why area
        // duplicate like dep
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
            renderWhyPanel(pid); // render the why panel for the selected bed
        });
        refreshBeds();// refresh the beds to the current selected room
    }
//refresh the waiting patients (sorted) 
    private void refreshWaitingPatients() {
        Department selectedDepartment = selectedDepartment();
        waitingPatientList.getSelectionModel().clearSelection(); // clear the selection
        waitingPatientList.getItems().clear();// clear the waiting patient list
        if (selectedDepartment != null) {
            waitingPatientList.getItems().addAll(workflowService.buildWaitingQueueView(selectedDepartment));// add the  waiting patients  to the list
        }
        waitingPatientList.refresh();// forces list cells to redraw
        if (!waitingPatientList.getItems().isEmpty()) { // if there are waiting patients, select the first one
            waitingPatientList.getSelectionModel().select(0); 
        }
    }
//refresh the beds 
    private void refreshBeds() {
        Room selected = roomList.getSelectionModel().getSelectedItem();// current selected room
        bedList.getSelectionModel().clearSelection();// clear the bed selection
        bedList.getItems().clear();// clear the bed list
        if (selected != null) {
            bedList.getItems().addAll(selected.getBeds());// add the beds to the list
        }
        bedList.refresh();// forces list cells to redraw
        if (!bedList.getItems().isEmpty()) {
            bedList.getSelectionModel().select(0); // select the first bed
        }
    }

    private void refreshKpis() {// refresh the kpis
        Department selectedDepartment = selectedDepartment();
        if (selectedDepartment == null) {// if no department, set the kpis to -
            occupancyLabel.setText("Occupancy: -");
            waitingLabel.setText("Waiting: -");
            unassignedLabel.setText("Unassigned Estimate: -");
            return;
        }
        // gettign the kpi values from the current state
        AssignmentState currentState = currentStateFor(selectedDepartment);
        int occupied = currentState.size();
        int capacity = selectedDepartment.getAllBeds().size();
        int waiting = workflowService.buildWaitingQueueView(selectedDepartment).size();
        int unassigned = Math.max(0, waiting - Math.max(0, capacity - occupied));
        double occupancyPct = capacity == 0 ? 0.0 : (100.0 * occupied / capacity);
        // setting the kpi values to the labels
        occupancyLabel.setText(String.format("Occupancy: %d/%d (%.1f%%)", occupied, capacity, occupancyPct));
        waitingLabel.setText("Waiting: " + waiting);
        unassignedLabel.setText("Unassigned Estimate: " + unassigned);
    }
    // summary of the department
    private void refreshDepartmentOverview() {// refresh the department overview
        Department selectedDepartment = selectedDepartment();
        if (selectedDepartment == null) {// if no department, set the department overview to -
            departmentNameLabel.setText("Department: -");
            departmentRoomsLabel.setText("Rooms: -");
            departmentCapacityLabel.setText("Capacity: -");
            return;
        }
        departmentNameLabel.setText("Department: " + selectedDepartment.getName() + " (" + selectedDepartment.getId() + ")");
        departmentRoomsLabel.setText("Rooms: " + selectedDepartment.getRooms().size());
        departmentCapacityLabel.setText("Capacity: " + selectedDepartment.getAllBeds().size() + " beds");
    }
// starts heavy optimization work on a background thread so the javafx ui will not freeze
//when the worker thread finishes (or fails or is cancelled) it updates charts, labels, warnings, and the preview diff list
    private void runOptimizationAsync() {// run the optimization asynchronously
        if (!RolePermissions.mayRunOptimization(currentRole)) {
            warningsArea.setText("Log in as nurse or admin to run optimization.");
            return;
        }
        if (runningOptimizationTask != null && runningOptimizationTask.isRunning()) {
            warningsArea.setText("Optimization is already running.");
            return;
        }
        Department selectedDepartment = selectedDepartment();
        if (selectedDepartment == null) {
            warningsArea.setText("Select a department before optimization.");
            return;
        }
        warningsArea.setText("Running optimization asynchronously...");// set the warnings area to running optimization asynchronously
        Task<AssignmentProposal> task = new Task<>() {// dfiens call, what runs on the background thread
            @Override
            protected AssignmentProposal call() {
                return workflowService.proposeAssignment(// propose the assignment (run the sa)
                        selectedDepartment,
                        patientByDepartmentId.getOrDefault(selectedDepartment.getId(), Map.of()),
                        currentStateFor(selectedDepartment)
                );
            }
        };
        runningOptimizationTask = task;// set the running optimization task to the task
        task.setOnSucceeded(evt -> {// when the task succeeds, update the charts, labels, warnings, and the preview diff list
            AssignmentProposal proposal = task.getValue();// get the proposal from the task
            workflowService.setPendingProposal(selectedDepartment.getId(), proposal);// set the pending proposal to the proposal
            AssignmentPreview preview = workflowService.buildPreview(proposal);// build the preview from the proposal
            optimizeRunIndex++;// increment the optimize run index
            bestZSeries.getData().add(new XYChart.Data<>(optimizeRunIndex, proposal.proposedZ()));// add the proposed z to the best z series
            currentZSeries.getData().add(new XYChart.Data<>(optimizeRunIndex, proposal.baselineZ()));// add the baseline z to the current z series
            currentZLabel.setText(String.format("Current Z: %.2f", preview.baselineZ()));// set the current z label to the baseline z
            bestZLabel.setText(String.format("Best Z: %.2f", preview.proposedZ()));// set the best z label to the proposed z
            warningsArea.setText(buildWarningText(proposal, preview));
            previewDiffList.getItems().setAll(visiblePreviewDiffs(preview));// set the preview diff list to the visible preview diffs
            runningOptimizationTask = null;
        });// set the running optimization task to null
        task.setOnFailed(evt -> {// when the task fails, update the warnings area and the preview diff list
            Throwable ex = task.getException();
            warningsArea.setText("Optimization failed: " + (ex == null ? "unknown error" : ex.getMessage()));
            previewDiffList.getItems().clear();
            runningOptimizationTask = null;
        });
        task.setOnCancelled(evt -> {// when the task is cancelled, update the warnings area and the preview diff list
            warningsArea.setText("Optimization cancelled.");
            previewDiffList.getItems().clear();
            runningOptimizationTask = null;
        });
        Thread worker = new Thread(task, "opticare-optimizer");// create a new thread for the optimization
        worker.setDaemon(true);// set the thread to daemon so it will not block the program from exiting
        worker.start();// start the thread
    }

    private void cancelOptimization() {// cancel the optimization 
        if (!RolePermissions.mayRunOptimization(currentRole)) {
            warningsArea.setText("Log in as nurse or admin to cancel optimization.");
            return;
        }
        if (runningOptimizationTask != null && runningOptimizationTask.isRunning()) {// if the optimization is running, cancel it
            runningOptimizationTask.cancel();
        } else {
            warningsArea.setText("No active optimization task to cancel.");
        }
    }
    // turns an assignmentProposal plus its assignmentPreview into one line string shown in warningsArea after optimization finishes 
    private String buildWarningText(AssignmentProposal proposal, AssignmentPreview preview) {
        if (!proposal.feasible()) {
            return "Hard constraint violations:\n- " + String.join("\n- ", proposal.feasibilityViolations());
        }
        int listed = (int) preview.patientDiffs().stream()
                .filter(d -> d.changeType() != AssignmentChangeType.UNCHANGED)
                .count(); // how many patients have changed (how many rows)
        String base = "Preview ready.\nChanged patients: " + preview.changedPatients()
                + "\nUnchanged patients: " + preview.unchangedPatients()
                + "\nDelta Z: " + String.format("%.2f", preview.deltaZ())
                + (listed == 0 && preview.changedPatients() == 0
                        ? "\n(No bed changes from baseline.)"
                        : "\nPer-patient moves: " + listed + " row(s) below (unchanged hidden).")
                + "\nUse Approve/Reject to apply.";
        if (proposal.warnings() != null && !proposal.warnings().isEmpty()) {
            base += "\nWarnings:\n- " + String.join("\n- ", proposal.warnings());
        }
        return base;
    }


    // keeps patient lifecycle and waiting-list state consistent with approved assignments.
    // prevents double counting in feasibility (assigned + still waiting).
    private void reconcileDepartmentAfterApproval(Department department, AssignmentState approvedState) {
        if (department == null || approvedState == null) return;
        Map<String, Patient> patientById = patientByDepartmentId.getOrDefault(department.getId(), Map.of());
        for (Patient p : patientById.values()) {
            if (p != null && p.getId() != null) {
                boolean assigned = approvedState.getBed(p.getId()) != null;
                if (assigned) {
                    p.setStatus(PatientStatus.ASSIGNED);
                    department.getWaitingList().removeIf(w -> w != null && p.getId().equals(w.getId()));//removed from wating list if assigned
                } else if (p.getStatus() != PatientStatus.DISCHARGED) {
                    p.setStatus(PatientStatus.WAITING);
                    boolean alreadyWaiting = department.getWaitingList().stream()
                            .anyMatch(w -> w != null && p.getId().equals(w.getId()));
                    if (!alreadyWaiting) {
                        department.getWaitingList().add(p);// if not already waiting, add to waiting list
                    }
                }
            }
        }
    }

    private void renderWhyPanel(String patientId) {// renders the why panel for the selected bed
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

    private void applyManualOverride() {// applies a manual override to the selected bed (must select depatment room bed patient)
        if (!RolePermissions.mayManualOverride(currentRole)) {
            warningsArea.setText("Log in as nurse or admin for manual override.");
            return;
        }
        Department selectedDepartment = selectedDepartment();// get the selected department
        Patient selectedPatient = waitingPatientList.getSelectionModel().getSelectedItem();// get the selected patient
        Bed selectedBed = bedList.getSelectionModel().getSelectedItem();// get the selected bed
        if (selectedDepartment == null || selectedPatient == null || selectedBed == null) {
            warningsArea.setText("Manual override requires department + waiting patient + target bed selection.");
            return;
        }
        AssignmentState state = currentStateFor(selectedDepartment);// get the current state for the selected department
        if (state.isBedOccupied(selectedBed)) {// is the bed already occupied?
            String occ = state.getPatientIdInBed(selectedBed);
            warningsArea.setText("Manual override blocked: this bed is already OCCUPIED"
                    + (occ != null ? " (" + occ + ")." : ".")
                    + " In the bed list, choose a row that says EMPTY — available.");
            return;
        }
        HardConstraints hardConstraints = new HardConstraints(// creates a new hard constraints object
                RiskMatrixFactory.fromConfig(config),
                selectedDepartment
        );
        Map<String, Patient> byId = patientByDepartmentId.getOrDefault(selectedDepartment.getId(), Map.of());// get the patients by id for the selected department
        if (!hardConstraints.isLegalAssignOrMoveToFreeBed(selectedPatient, selectedBed, state, byId)) {// can this move be done?
            warningsArea.setText("Manual override blocked by hard constraints (clinical/cohort/isolation).");
            return;
        }
        state.assign(selectedPatient, selectedBed);// assign the patient to the bed
        selectedPatient.setStatus(PatientStatus.ASSIGNED);// set the patient status to assigned
        if (!selectedDepartment.getWaitingList().remove(selectedPatient)) {
            selectedDepartment.getWaitingList().removeIf(// remove the patient from the waiting list if it is in the waiting list
                    p -> p != null && selectedPatient.getId() != null && selectedPatient.getId().equals(p.getId()));
        }
        workflowService.setPendingProposal(selectedDepartment.getId(), null);// set the pending proposal to null
        previewDiffList.getItems().clear();// clear the preview diff list
        warningsArea.setText("Manual override applied for patient " + selectedPatient.getId() + " -> bed " + selectedBed.getId() + ". Pending optimizer proposal cleared—run Find Assignment again before Approve.");
        refreshKpis();
        refreshBeds();
        refreshWaitingPatients();
        persistWardSnapshot();
    }
// seed demo data (for testing), will be used if no json file is found
    private void seedDemoData() {
        departments.clear();// clear the departments
        patientByDepartmentId.clear();// clear the patient by department id
        currentStateByDepartmentId.clear();// clear the current state by department id

        Room r1 = new Room("R1", "D1", 2, new java.util.ArrayList<>(), 5.0, true);
        Room r2 = new Room("R2", "D1", 2, new java.util.ArrayList<>(), 12.0, false);
        r1.getBeds().add(new Bed("B1", "R1", BedType.REGULAR, false));
        r1.getBeds().add(new Bed("B2", "R1", BedType.ICU, true));
        r2.getBeds().add(new Bed("B3", "R2", BedType.REGULAR, false));
        r2.getBeds().add(new Bed("B4", "R2", BedType.BARIATRIC, false));
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

        Room r3 = new Room("R3", "D2", 2, new java.util.ArrayList<>(), 4.0, true);
        Room r4 = new Room("R4", "D2", 1, new java.util.ArrayList<>(), 9.0, false);
        r3.getBeds().add(new Bed("B5", "R3", BedType.ICU, true));
        r3.getBeds().add(new Bed("B6", "R3", BedType.REGULAR, false));
        r4.getBeds().add(new Bed("B7", "R4", BedType.REGULAR, false));
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

        // Apply seeded assignment before lists bind / paint so occupancy matches reality on first view.
        // (Deferring via runLater mutated state without refreshing beds—cells showed EMPTY until selection changed.) 
        stateD1.assign(p2, r1.getBeds().get(0));
        p2.setStatus(PatientStatus.ASSIGNED);
        d1.getWaitingList().removeIf(p -> p != null && p2.getId().equals(p.getId()));
    }

    private Department selectedDepartment() {// gets the selected department
        return departmentList.getSelectionModel().getSelectedItem(); // what depatmet was cliked last
    }

    private AssignmentState currentStateFor(Department department) {// gets the current state for the selected department
        return currentStateByDepartmentId.computeIfAbsent(department.getId(), ignored -> new AssignmentState());// if the department is not in the map, create a new assignment state
    }

    private Patient waiting(String id, RiskLevel risk, int severity, Instant admittedAt, boolean temporarilyUnavailable) {// creates a new patient and sets the status to waiting
        Patient p = new Patient(id, null, new ClinicalData(risk, severity, false, null), admittedAt, temporarilyUnavailable);
        p.setStatus(PatientStatus.WAITING);
        return p;
    }
}
