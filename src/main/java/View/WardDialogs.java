package View;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import Auth.JsonFileUsersRepository;
import Auth.LoginResult;
import Auth.Role;
import Model.entety.Department;
import Model.entety.Room;
import Model.enums.BedType;
import Model.enums.Gender;
import Model.enums.RiskLevel;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * Minimal modal dialogs for Stage 7 (login, admit, ward structure, users).
 */
public final class WardDialogs {

    private WardDialogs() {
    }

    public record AdmitPatientInput(
            Department department,
            String patientId,
            RiskLevel riskLevel,
            int severityScore,
            Instant admittedAt,
            boolean needsVentilator,
            BedType requiredBedType,
            Integer weightKg,
            boolean temporarilyUnavailable,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            Gender gender
    ) {
    }

    public record AddDepartmentInput(String departmentId, String name) {
    }

    public record AddRoomInput(
            Department department,
            String roomId,
            int capacity,
            double distanceFromNurseStation,
            boolean hasNegativePressure
    ) {
    }

    public record AddBedInput(
            Department department,
            Room room,
            String bedId,
            BedType bedType,
            boolean hasVentilator,
            boolean broken
    ) {
    }

    public record AddUserInput(String username, String password, Role role) {
    }

    public static Optional<LoginResult> showLoginDialog(Window owner, JsonFileUsersRepository repo) {
        Dialog<LoginResult> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Log in");
        dialog.setHeaderText("log in");

        ButtonType loginButtonType = new ButtonType("Log in", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Button loginBtn = (Button) dialog.getDialogPane().lookupButton(loginButtonType);
        loginBtn.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                Optional<LoginResult> res = repo.authenticate(username.getText(), password.getText());
                if (res.isEmpty()) {
                    event.consume();
                    alert(Alert.AlertType.ERROR, "Login failed", "Unknown username/password.");
                }
            } catch (IOException ex) {
                event.consume();
                alert(Alert.AlertType.ERROR, "Login error", ex.getMessage());
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType != loginButtonType) {
                return null;
            }
            try {
                return repo.authenticate(username.getText(), password.getText()).orElse(null);
            } catch (IOException ex) {
                alert(Alert.AlertType.ERROR, "Login error", ex.getMessage());
                return null;
            }
        });

        return dialog.showAndWait();
    }

    public static Optional<AdmitPatientInput> showAdmitPatientDialog(Window owner, List<Department> departments) {
        Dialog<AdmitPatientInput> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Admit patient");
        dialog.setHeaderText("Patient joins department waiting list");

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        ComboBox<Department> deptBox = new ComboBox<>(FXCollections.observableArrayList(departments));
        applyDepartmentComboFormatting(deptBox);
        TextField patientId = new TextField();
        ComboBox<RiskLevel> riskBox = new ComboBox<>(FXCollections.observableArrayList(RiskLevel.values()));
        riskBox.getSelectionModel().select(RiskLevel.UNKNOWN);
        TextField severity = new TextField("0");
        TextField admittedAt = new TextField();
        admittedAt.setPromptText("instant (empty = now)");
        CheckBox vent = new CheckBox("Needs ventilator");
        ComboBox<BedType> bedTypeBox = new ComboBox<>(FXCollections.observableArrayList(BedType.values()));
        bedTypeBox.getSelectionModel().select(BedType.REGULAR);
        TextField weightKg = new TextField();
        weightKg.setPromptText("kg (optional)");
        CheckBox tempUn = new CheckBox("Temporarily unavailable");
        TextField first = new TextField();
        TextField last = new TextField();
        TextField dob = new TextField();
        dob.setPromptText("yyyy-MM-dd (optional)");
        ComboBox<String> genderBox = new ComboBox<>(FXCollections.observableArrayList("", "MALE", "FEMALE"));

        int r = 0;
        grid.add(new Label("Department:"), 0, r);
        grid.add(deptBox, 1, r++);
        grid.add(new Label("Patient ID:"), 0, r);
        grid.add(patientId, 1, r++);
        grid.add(new Label("Risk:"), 0, r);
        grid.add(riskBox, 1, r++);
        grid.add(new Label("Severity:"), 0, r);
        grid.add(severity, 1, r++);
        grid.add(new Label("Admitted at:"), 0, r);
        grid.add(admittedAt, 1, r++);
        grid.add(vent, 1, r++);
        grid.add(new Label("Required bed type:"), 0, r);
        grid.add(bedTypeBox, 1, r++);
        grid.add(new Label("Weight (kg):"), 0, r);
        grid.add(weightKg, 1, r++);
        grid.add(tempUn, 1, r++);
        grid.add(new Label("First name:"), 0, r);
        grid.add(first, 1, r++);
        grid.add(new Label("Last name:"), 0, r);
        grid.add(last, 1, r++);
        grid.add(new Label("DOB:"), 0, r);
        grid.add(dob, 1, r++);
        grid.add(new Label("Gender:"), 0, r);
        grid.add(genderBox, 1, r++);

        dialog.getDialogPane().setContent(grid);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, event -> {
            if (deptBox.getValue() == null || patientId.getText() == null || patientId.getText().isBlank()) {
                event.consume();
                alert(Alert.AlertType.WARNING, "Missing data", "Department and patient ID are required.");
                return;
            }
            try {
                Integer.parseInt(severity.getText().trim());
            } catch (NumberFormatException ex) {
                event.consume();
                alert(Alert.AlertType.WARNING, "Invalid severity", "Severity must be an integer.");
            }
        });

        dialog.setResultConverter(bt -> {
            if (bt != saveType) {
                return null;
            }
            Department d = deptBox.getValue();
            String pid = patientId.getText().trim();
            if (d == null || pid.isEmpty()) {
                return null;
            }
            int sev;
            try {
                sev = Integer.parseInt(severity.getText().trim());
            } catch (NumberFormatException ex) {
                return null;
            }
            Instant adm;
            String at = admittedAt.getText() == null ? "" : admittedAt.getText().trim();
            if (at.isEmpty()) {
                adm = Instant.now();
            } else {
                try {
                    adm = Instant.parse(at);
                } catch (DateTimeParseException ex) {
                    return null;
                }
            }
            Integer wKg = null;
            String w = weightKg.getText() == null ? "" : weightKg.getText().trim();
            if (!w.isEmpty()) {
                try {
                    wKg = Integer.parseInt(w);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            LocalDate ld = null;
            String ds = dob.getText() == null ? "" : dob.getText().trim();
            if (!ds.isEmpty()) {
                try {
                    ld = LocalDate.parse(ds);
                } catch (DateTimeParseException ex) {
                    return null;
                }
            }
            Gender g = null;
            String gs = genderBox.getValue();
            if (gs != null && !gs.isEmpty()) {
                g = Gender.valueOf(gs);
            }
            String fn = first.getText() == null ? "" : first.getText().trim();
            String ln = last.getText() == null ? "" : last.getText().trim();
            return new AdmitPatientInput(
                    d,
                    pid,
                    riskBox.getValue(),
                    sev,
                    adm,
                    vent.isSelected(),
                    bedTypeBox.getValue(),
                    wKg,
                    tempUn.isSelected(),
                    fn.isEmpty() ? null : fn,
                    ln.isEmpty() ? null : ln,
                    ld,
                    g
            );
        });

        return dialog.showAndWait();
    }

    public static Optional<AddDepartmentInput> showAddDepartmentDialog(Window owner) {
        Dialog<AddDepartmentInput> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Add department");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        TextField id = new TextField();
        TextField name = new TextField();
        grid.add(new Label("Department ID:"), 0, 0);
        grid.add(id, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(name, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, event -> {
            if (id.getText() == null || id.getText().isBlank() || name.getText() == null || name.getText().isBlank()) {
                event.consume();
                alert(Alert.AlertType.WARNING, "Missing data", "ID and name are required.");
            }
        });

        dialog.setResultConverter(bt -> {
            if (bt != saveType) {
                return null;
            }
            if (id.getText() == null || id.getText().isBlank() || name.getText() == null || name.getText().isBlank()) {
                return null;
            }
            return new AddDepartmentInput(id.getText().trim(), name.getText().trim());
        });
        return dialog.showAndWait();
    }

    public static Optional<AddRoomInput> showAddRoomDialog(Window owner, List<Department> departments) {
        Dialog<AddRoomInput> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Add room");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        ComboBox<Department> deptBox = new ComboBox<>(FXCollections.observableArrayList(departments));
        applyDepartmentComboFormatting(deptBox);
        TextField roomId = new TextField();
        TextField capacity = new TextField("1");
        TextField distance = new TextField("10");
        CheckBox neg = new CheckBox("Negative pressure");
        neg.setSelected(false);

        grid.add(new Label("Department:"), 0, 0);
        grid.add(deptBox, 1, 0);
        grid.add(new Label("Room ID:"), 0, 1);
        grid.add(roomId, 1, 1);
        grid.add(new Label("Capacity:"), 0, 2);
        grid.add(capacity, 1, 2);
        grid.add(new Label("Distance (m):"), 0, 3);
        grid.add(distance, 1, 3);
        grid.add(neg, 1, 4);
        dialog.getDialogPane().setContent(grid);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, event -> {
            if (deptBox.getValue() == null || roomId.getText() == null || roomId.getText().isBlank()) {
                event.consume();
                alert(Alert.AlertType.WARNING, "Missing data", "Department and room ID are required.");
                return;
            }
            try {
                int cap = Integer.parseInt(capacity.getText().trim());
                if (cap < 1) {
                    throw new NumberFormatException();
                }
                Double.parseDouble(distance.getText().trim());
            } catch (NumberFormatException ex) {
                event.consume();
                alert(Alert.AlertType.WARNING, "Invalid numbers", "Capacity must be >= 1; distance must be a number.");
            }
        });

        dialog.setResultConverter(bt -> {
            if (bt != saveType) {
                return null;
            }
            if (deptBox.getValue() == null || roomId.getText() == null || roomId.getText().isBlank()) {
                return null;
            }
            try {
                int cap = Integer.parseInt(capacity.getText().trim());
                double dist = Double.parseDouble(distance.getText().trim());
                return new AddRoomInput(deptBox.getValue(), roomId.getText().trim(), cap, dist, neg.isSelected());
            } catch (NumberFormatException ex) {
                return null;
            }
        });
        return dialog.showAndWait();
    }

    public static Optional<AddBedInput> showAddBedDialog(Window owner, List<Department> departments) {
        Dialog<AddBedInput> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Add bed");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<Department> deptBox = new ComboBox<>(FXCollections.observableArrayList(departments));
        ComboBox<Room> roomBox = new ComboBox<>();
        applyDepartmentComboFormatting(deptBox);
        applyRoomComboFormatting(roomBox);
        deptBox.setOnAction(e -> {
            Department d = deptBox.getValue();
            roomBox.setItems(FXCollections.observableArrayList(d == null ? List.of() : d.getRooms()));
            if (!roomBox.getItems().isEmpty()) {
                roomBox.getSelectionModel().select(0);
            }
        });

        TextField bedId = new TextField();
        ComboBox<BedType> typeBox = new ComboBox<>(FXCollections.observableArrayList(BedType.values()));
        typeBox.getSelectionModel().select(BedType.REGULAR);
        CheckBox vent = new CheckBox("Ventilator");
        CheckBox broken = new CheckBox("Broken");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Department:"), 0, 0);
        grid.add(deptBox, 1, 0);
        grid.add(new Label("Room:"), 0, 1);
        grid.add(roomBox, 1, 1);
        grid.add(new Label("Bed ID:"), 0, 2);
        grid.add(bedId, 1, 2);
        grid.add(new Label("Type:"), 0, 3);
        grid.add(typeBox, 1, 3);
        grid.add(vent, 1, 4);
        grid.add(broken, 1, 5);
        dialog.getDialogPane().setContent(grid);

        if (!departments.isEmpty()) {
            deptBox.getSelectionModel().select(0);
            Department d0 = deptBox.getValue();
            roomBox.setItems(FXCollections.observableArrayList(d0 == null ? List.of() : d0.getRooms()));
            if (!roomBox.getItems().isEmpty()) {
                roomBox.getSelectionModel().select(0);
            }
        }

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, event -> {
            if (deptBox.getValue() == null || roomBox.getValue() == null || bedId.getText() == null || bedId.getText().isBlank()) {
                event.consume();
                alert(Alert.AlertType.WARNING, "Missing data", "Department, room, and bed ID are required.");
            }
        });

        dialog.setResultConverter(bt -> {
            if (bt != saveType) {
                return null;
            }
            if (deptBox.getValue() == null || roomBox.getValue() == null || bedId.getText() == null || bedId.getText().isBlank()) {
                return null;
            }
            return new AddBedInput(
                    deptBox.getValue(),
                    roomBox.getValue(),
                    bedId.getText().trim(),
                    typeBox.getValue(),
                    vent.isSelected(),
                    broken.isSelected()
            );
        });
        return dialog.showAndWait();
    }

    public static Optional<AddUserInput> showAddUserDialog(Window owner) {
        Dialog<AddUserInput> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Add user");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField user = new TextField();
        PasswordField pass = new PasswordField();
        ComboBox<Role> roleBox = new ComboBox<>(FXCollections.observableArrayList(Role.NURSE, Role.ADMIN));
        roleBox.getSelectionModel().select(Role.NURSE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Username:"), 0, 0);
        grid.add(user, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(pass, 1, 1);
        grid.add(new Label("Role:"), 0, 2);
        grid.add(roleBox, 1, 2);
        dialog.getDialogPane().setContent(grid);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(ActionEvent.ACTION, event -> {
            if (user.getText() == null || user.getText().isBlank() || pass.getText() == null || pass.getText().isBlank()) {
                event.consume();
                alert(Alert.AlertType.WARNING, "Missing data", "Username and password are required.");
            }
        });

        dialog.setResultConverter(bt -> {
            if (bt != saveType) {
                return null;
            }
            if (user.getText() == null || user.getText().isBlank() || pass.getText() == null || pass.getText().isBlank()) {
                return null;
            }
            return new AddUserInput(user.getText().trim(), pass.getText(), roleBox.getValue());
        });
        return dialog.showAndWait();
    }

    private static void alert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private static void applyDepartmentComboFormatting(ComboBox<Department> box) {
        box.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Department item, boolean empty) {
                super.updateItem(item, empty);
                setText(formatDepartment(item, empty));
            }
        });
        box.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Department item, boolean empty) {
                super.updateItem(item, empty);
                setText(formatDepartment(item, empty));
            }
        });
    }

    private static String formatDepartment(Department item, boolean empty) {
        if (empty || item == null) {
            return null;
        }
        String name = item.getName() == null ? "" : item.getName();
        return name + " (" + item.getId() + ")";
    }

    private static void applyRoomComboFormatting(ComboBox<Room> box) {
        box.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Room item, boolean empty) {
                super.updateItem(item, empty);
                setText(formatRoom(item, empty));
            }
        });
        box.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Room item, boolean empty) {
                super.updateItem(item, empty);
                setText(formatRoom(item, empty));
            }
        });
    }

    private static String formatRoom(Room item, boolean empty) {
        if (empty || item == null) {
            return null;
        }
        return item.getId() + " (beds: " + item.getBeds().size() + ")";
    }
}
