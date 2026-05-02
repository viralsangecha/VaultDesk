package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.*;
import java.util.Optional;

public class DepartmentView {

    public VBox getView() {
        Label title = new Label("Departments");
        title.getStyleClass().add("section-title");
        TableView<Department> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Department, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Department, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Department, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLocation()));

        table.getColumns().addAll(idCol, nameCol, locationCol);

        Button addBtn = new Button("+ Add Department");
        addBtn.getStyleClass().setAll("btn-primary");
        addBtn.setStyle("-fx-background-color: #238636; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-font-weight: bold;");
        addBtn.setOnAction(e -> showAddDialog(table));


        Button importBtn = new Button("⬆ Import CSV");
        importBtn.getStyleClass().setAll("btn-primary");
        importBtn.setStyle(
                "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 8 14 8 14;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");
        importBtn.setOnAction(e -> {
            CsvImporter.importCsv("Import Departments CSV", true, fields -> {
                // CSV columns: name,location
                if (fields.length < 2)
                    throw new Exception("Expected 2 columns");
                String body = "{" +
                        "\"name\":\"" + fields[0] + "\"," +
                        "\"location\":\"" + fields[1] + "\"" +
                        "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigManager.getBaseUrl() + "/api/departments"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 201)
                    throw new Exception("Server returned " + resp.statusCode());
            });
            loadDepartments(table);
        });

        HBox topBar = new HBox(10, addBtn, importBtn);



        loadDepartments(table);

        VBox root = new VBox(10);
        root.getChildren().addAll(title, topBar, table);
        return root;
    }

    private void loadDepartments(TableView<Department> table) {
        table.getItems().clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl() + "/api/departments"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    table.getItems().add(new Department(
                            extractInt(obj, "id"),
                            extractValue(obj, "name"),
                            extractValue(obj, "location")
                    ));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading departments: " + ex.getMessage());
        }
    }

    private void showAddDialog(TableView<Department> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Department");
        dialog.setHeaderText("Enter department details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField     = new TextField();
        TextField locationField = new TextField();
        Label errorLabel        = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Name *:"),     0, 0); grid.add(nameField,     1, 0);
        grid.add(new Label("Location:"),   0, 1); grid.add(locationField, 1, 1);
        grid.add(errorLabel,               1, 2);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        nameField.textProperty().addListener((o, ov, nv) ->
                okButton.setDisable(nv.trim().isEmpty()));

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (nameField.getText().trim().isEmpty()) {
                errorLabel.setText("Department Name is required.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String body = "{" +
                    "\"name\":\"" + nameField.getText() + "\"," +
                    "\"location\":\"" + locationField.getText() + "\"" +
                    "}";
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigManager.getBaseUrl() + "/api/departments"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201) {
                    showAlert("Success", "Department added.");
                    loadDepartments(table);
                } else {
                    showAlert("Error", "Server returned: " + response.statusCode());
                }
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
            return Integer.parseInt(
                    json.substring(start, end).trim().replace("}", ""));
        } catch (NumberFormatException e) { return 0; }
    }
}