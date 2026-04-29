package View;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

final class OptiCareViewFactory {

    private OptiCareViewFactory() {
    }

    static HBox buildKpiBar(Label occupancyLabel, Label waitingLabel, Label unassignedLabel,
                            Label currentZLabel, Label bestZLabel) {
        HBox row = new HBox(18, occupancyLabel, waitingLabel, unassignedLabel, currentZLabel, bestZLabel);
        row.setPadding(new Insets(10));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #f5f8ff; -fx-border-color: #d5ddef;");
        return row;
    }

    static GridPane buildDepartmentOverviewCard(Label departmentNameLabel, Label departmentRoomsLabel,
                                                Label departmentCapacityLabel) {
        GridPane card = new GridPane();
        card.setHgap(8);
        card.setVgap(4);
        card.setPadding(new Insets(8));
        card.setStyle("-fx-background-color: #f7fbf7; -fx-border-color: #cfe5cf;");
        Label header = new Label("Department Overview");
        card.add(header, 0, 0, 2, 1);
        card.add(departmentNameLabel, 0, 1, 2, 1);
        card.add(departmentRoomsLabel, 0, 2);
        card.add(departmentCapacityLabel, 1, 2);
        return card;
    }

    static VBox buildDepartmentWardAndRoomPanel(GridPane departmentOverviewCard,
                                                ListView<?> departmentList,
                                                ListView<?> roomList,
                                                ListView<?> waitingPatientList,
                                                Label selectedRoomLabel) {
        Label deptTitle = new Label("Departments");
        Label roomTitle = new Label("Rooms");
        Label waitingTitle = new Label("Waiting Patients");
        VBox box = new VBox(8, departmentOverviewCard, deptTitle, departmentList, roomTitle, roomList,
                waitingTitle, waitingPatientList, selectedRoomLabel);
        VBox.setVgrow(departmentList, Priority.ALWAYS);
        VBox.setVgrow(roomList, Priority.ALWAYS);
        VBox.setVgrow(waitingPatientList, Priority.ALWAYS);
        box.setPrefWidth(320);
        return box;
    }

    static VBox buildBedPanel(ListView<?> bedList, Label selectedBedLabel, Label selectedPatientLabel) {
        Label title = new Label("Beds in Selected Room");
        VBox box = new VBox(8, title, bedList, selectedBedLabel, selectedPatientLabel);
        VBox.setVgrow(bedList, Priority.ALWAYS);
        return box;
    }

    static VBox buildInsightsPanel(TextArea warningsArea, TextArea whyArea,
                                   XYChart.Series<Number, Number> bestZSeries,
                                   XYChart.Series<Number, Number> currentZSeries) {
        Label warningsTitle = new Label("Conflict / Warning Panel");
        warningsArea.setEditable(false);
        warningsArea.setWrapText(true);
        warningsArea.setPrefRowCount(10);

        Label whyTitle = new Label("Why this assignment?");
        whyArea.setEditable(false);
        whyArea.setWrapText(true);
        whyArea.setPrefRowCount(10);
        Tooltip whyTooltip = new Tooltip("Compact patient cost breakdown (C_safety/C_clinical/C_policy/C_transfer)");
        Tooltip.install(whyTitle, whyTooltip);

        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Optimization Run");
        yAxis.setLabel("Z Score");
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setCreateSymbols(true);
        chart.setLegendVisible(true);
        chart.setAnimated(false);
        bestZSeries.setName("bestZ");
        currentZSeries.setName("currentZ");
        chart.getData().add(bestZSeries);
        chart.getData().add(currentZSeries);
        chart.setPrefHeight(250);

        VBox box = new VBox(8, warningsTitle, warningsArea, whyTitle, whyArea, chart);
        box.setPrefWidth(430);
        return box;
    }

    static HBox buildActionsPanel(Button findAssignmentButton, Button cancelButton, ProgressIndicator optimizeSpinner,
                                  Button approveButton, Button rejectButton, Button manualOverrideButton) {
        optimizeSpinner.setVisible(false);
        optimizeSpinner.setPrefSize(22, 22);
        HBox row = new HBox(10, findAssignmentButton, cancelButton, optimizeSpinner, approveButton, rejectButton, manualOverrideButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
