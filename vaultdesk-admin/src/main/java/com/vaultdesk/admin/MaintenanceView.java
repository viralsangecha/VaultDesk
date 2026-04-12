package com.vaultdesk.admin;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.*;
import java.util.Optional;

public class MaintenanceView {

    public VBox getView() {
        Label title = new Label("Maintenance");
        title.getStyleClass().add("section-title");
        TableView<Maintenance> table = new TableView<>();

        TableColumn<Maintenance, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Maintenance, Integer> assetIdCol = new TableColumn<>("Asset ID");
        assetIdCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getAssetId()).asObject());

        TableColumn<Maintenance, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getMaintenanceType()));

        TableColumn<Maintenance, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDescription()));

        TableColumn<Maintenance, Double> costCol = new TableColumn<>("Cost");
        costCol.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getCost()).asObject());

        TableColumn<Maintenance, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getMaintenanceDate()));

        TableColumn<Maintenance, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));

        table.getColumns().addAll(idCol, assetIdCol, typeCol,
                descCol, costCol, dateCol, statusCol);

        Button addBtn = new Button("Add Maintenance Log");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddDialog(table));
        HBox topBar = new HBox(10);
        topBar.getChildren().add(addBtn);

        loadMaintenance(table);

        VBox root = new VBox(10);
        root.getChildren().addAll(title, topBar, table);
        return root;
    }

    private void loadMaintenance(TableView<Maintenance> table) {
        table.getItems().clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/maintenance"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    table.getItems().add(new Maintenance(
                            extractInt(obj, "id"),
                            extractInt(obj, "assetId"),
                            extractValue(obj, "maintenanceType"),
                            extractValue(obj, "description"),
                            extractDouble(obj, "cost"),
                            extractValue(obj, "maintenanceDate"),
                            extractValue(obj, "status")
                    ));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading maintenance: " + ex.getMessage());
        }
    }

    private void showAddDialog(TableView<Maintenance> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Maintenance Log");
        dialog.setHeaderText("Enter maintenance details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField assetIdField = new TextField();
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Repair", "AMC Visit", "Preventive",
                "Upgrade", "Cleaning", "Other");
        typeBox.setValue("Repair");
        TextField descField = new TextField();
        TextField costField = new TextField();
        costField.setPromptText("0.0");
        TextField dateField = new TextField();
        dateField.setPromptText("YYYY-MM-DD");
        TextField nextDueDateField = new TextField();
        nextDueDateField.setPromptText("YYYY-MM-DD");
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Completed", "Pending", "In Progress");
        statusBox.setValue("Completed");
        TextField notesField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Asset ID:"),      0, 0); grid.add(assetIdField,    1, 0);
        grid.add(new Label("Type:"),          0, 1); grid.add(typeBox,         1, 1);
        grid.add(new Label("Description:"),   0, 2); grid.add(descField,       1, 2);
        grid.add(new Label("Cost:"),          0, 3); grid.add(costField,       1, 3);
        grid.add(new Label("Date:"),          0, 4); grid.add(dateField,       1, 4);
        grid.add(new Label("Next Due Date:"), 0, 5); grid.add(nextDueDateField,1, 5);
        grid.add(new Label("Status:"),        0, 6); grid.add(statusBox,       1, 6);
        grid.add(new Label("Notes:"),         0, 7); grid.add(notesField,      1, 7);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int assetId = Integer.parseInt(assetIdField.getText().trim());
                double cost = costField.getText().trim().isEmpty() ? 0.0
                        : Double.parseDouble(costField.getText().trim());
                String body = "{" +
                        "\"assetId\":" + assetId + "," +
                        "\"maintenanceType\":\"" + typeBox.getValue() + "\"," +
                        "\"description\":\"" + descField.getText() + "\"," +
                        "\"doneByInternal\":0," +
                        "\"doneByVendor\":0," +
                        "\"cost\":" + cost + "," +
                        "\"maintenanceDate\":\"" + dateField.getText() + "\"," +
                        "\"nextDueDate\":\"" + nextDueDateField.getText() + "\"," +
                        "\"status\":\"" + statusBox.getValue() + "\"," +
                        "\"notes\":\"" + notesField.getText() + "\"," +
                        "\"loggedBy\":1" +
                        "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/maintenance"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201) {
                    showAlert("Success", "Maintenance log added.");
                    loadMaintenance(table);
                } else {
                    showAlert("Error", "Server returned: " + response.statusCode());
                }
            } catch (NumberFormatException ex) {
                showAlert("Error", "Asset ID and Cost must be numbers.");
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        return json.substring(start, json.indexOf("\"", start));
    }

    private int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.length();
        try {
            return Integer.parseInt(json.substring(start, end).trim().replace("}", ""));
        } catch (NumberFormatException e) { return 0; }
    }

    private double extractDouble(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.length();
        try {
            return Double.parseDouble(json.substring(start, end).trim().replace("}", ""));
        } catch (NumberFormatException e) { return 0.0; }
    }
}