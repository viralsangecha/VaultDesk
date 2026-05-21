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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MaintenanceView {

    private TableView<Maintenance> table;
    public VBox getView() {
        Label title = new Label("Maintenance");
        title.getStyleClass().add("section-title");
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> {
            TableRow<Maintenance> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty())
                    showFullEditDialog(row.getItem(), table);
            });
            return row;
        });

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
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "Completed"   -> setStyle("-fx-text-fill: #3fb950; -fx-font-weight: bold;");
                    case "In Progress" -> setStyle("-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
                    case "Pending"     -> setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
                    default            -> setStyle("-fx-text-fill: #8b949e;");
                }
            }
        });

        table.getColumns().addAll(idCol, assetIdCol, typeCol,
                descCol, costCol, dateCol, statusCol);

        Button addBtn = new Button("+ Add Maintenance Log");
        addBtn.getStyleClass().setAll("btn-primary");
        addBtn.setStyle("-fx-background-color: #238636; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-font-weight: bold;");
        addBtn.setOnAction(e -> showAddDialog(table));
        Button exportBtn = new Button("⬇ Export");
        exportBtn.getStyleClass().setAll("btn-primary");
        exportBtn.setStyle(
                "-fx-background-color: #6e40c9;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 6 12 6 12;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;");
        exportBtn.setOnAction(e -> {
            List<String> headers = List.of(
                    "Asset ID", "Type", "Description",
                    "Cost", "Date", "Status");
            List<List<String>> rows = new ArrayList<>();
            for (Maintenance m : table.getItems()) {
                rows.add(List.of(
                        String.valueOf(m.getAssetId()),
                        m.getMaintenanceType(),
                        m.getDescription(),
                        String.valueOf(m.getCost()),
                        m.getMaintenanceDate(),
                        m.getStatus()
                ));
            }
            ExcelExporter.export("Maintenance", headers, rows);
        });

        HBox topBar = new HBox(10, addBtn, exportBtn);

        loadMaintenance(table);

        VBox root = new VBox(10);
        root.getChildren().addAll(title, topBar, table);
        return root;
    }
    private void showFullEditDialog(Maintenance m,
                                    TableView<Maintenance> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Maintenance Details");
        dialog.setHeaderText("Asset #" + m.getAssetId());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField assetIdField = new TextField(
                String.valueOf(m.getAssetId()));
        assetIdField.setEditable(false);
        assetIdField.setStyle("-fx-opacity: 0.6;");

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Repair", "AMC Visit",
                "Preventive", "Upgrade", "Cleaning", "Other");
        typeBox.setValue(m.getMaintenanceType());

        TextField descField = new TextField(
                m.getDescription());
        NumberField costField = new NumberField(true);
        costField.setText(String.valueOf(m.getCost()));

        TextField dateField = new TextField(
                m.getMaintenanceDate());
        TextField nextDueDateField = new TextField();
        TextField notesField = new TextField();

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll(
                "Completed", "Pending", "In Progress");
        statusBox.setValue(m.getStatus());

        Label errorLabel = new Label("");
        errorLabel.setStyle(
                "-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        int r = 0;
        grid.add(new Label("Asset ID:"),      0, r);
        grid.add(assetIdField,                1, r++);
        grid.add(new Label("Type:"),          0, r);
        grid.add(typeBox,                     1, r++);
        grid.add(new Label("Description:"),   0, r);
        grid.add(descField,                   1, r++);
        grid.add(new Label("Cost (₹):"),       0, r);
        grid.add(costField,                   1, r++);
        grid.add(new Label("Date *:"),        0, r);
        grid.add(DatePickerUtil.dateField(
                dateField),                   1, r++);
        grid.add(new Label("Next Due Date:"), 0, r);
        grid.add(DatePickerUtil.dateField(
                nextDueDateField),            1, r++);
        grid.add(new Label("Status:"),        0, r);
        grid.add(statusBox,                   1, r++);
        grid.add(new Label("Notes:"),         0, r);
        grid.add(notesField,                  1, r++);
        grid.add(errorLabel,                  1, r);
        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okBtn.addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    if (dateField.getText().trim().isEmpty()) {
                        errorLabel.setText("Date is required.");
                        event.consume();
                    }
                });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()
                && result.get() == ButtonType.OK) {
            try {
                String body = "{" +
                        "\"id\":" + m.getId() + "," +
                        "\"assetId\":" + m.getAssetId() + "," +
                        "\"maintenanceType\":\"" +
                        typeBox.getValue() + "\"," +
                        "\"description\":\"" + escape(
                        descField.getText()) + "\"," +
                        "\"doneByInternal\":0," +
                        "\"doneByVendor\":0," +
                        "\"cost\":" +
                        costField.getDoubleValue() + "," +
                        "\"maintenanceDate\":\"" +
                        dateField.getText() + "\"," +
                        "\"nextDueDate\":\"" +
                        nextDueDateField.getText() + "\"," +
                        "\"status\":\"" +
                        statusBox.getValue() + "\"," +
                        "\"notes\":\"" + escape(
                        notesField.getText()) + "\"," +
                        "\"loggedBy\":" +
                        SessionManager.get().getUserId() +
                        "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(
                                ConfigManager.getBaseUrl()
                                        + "/api/maintenance/"
                                        + m.getId()))
                        .header("Content-Type",
                                "application/json")
                        .PUT(HttpRequest.BodyPublishers
                                .ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(
                        req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    showAlert("Success",
                            "Maintenance log updated.");
                    loadMaintenance(table);
                } else {
                    showAlert("Error", "Server returned: "
                            + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "'")
                .replace("\n", " ")
                .replace("\r", "");
    }

    private void loadMaintenance(TableView<Maintenance> table) {
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading maintenance logs...");
        try {
            String url = SessionManager.get().isDeptHod()
                    ? ConfigManager.getBaseUrl() + "/api/maintenance/department/"
                    + SessionManager.get().getDeptId()
                    : ConfigManager.getBaseUrl() + "/api/maintenance";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().build();
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
                if (table.getItems().isEmpty()) {
                    LoadingUtil.setEmpty(table, "🔧",
                            "No maintenance logs found",
                            "Add a maintenance log using the button above.");
                }
            } else {
                LoadingUtil.setEmpty(table, "🔧",
                        "No maintenance logs found",
                        "Add a maintenance log using the button above.");
            }
        } catch (Exception ex) {
            LoadingUtil.setEmpty(table, "⚠",
                    "Could not load maintenance logs",
                    "Check server connection and try again.");
        }
    }

    private void showAddDialog(TableView<Maintenance> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Maintenance Log");
        dialog.setHeaderText("Enter maintenance details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField assetIdField     = new TextField();
        ComboBox<String> typeBox   = new ComboBox<>();
        typeBox.getItems().addAll("Repair", "AMC Visit", "Preventive",
                "Upgrade", "Cleaning", "Other");
        typeBox.setValue("Repair");
        TextField descField        = new TextField();
        TextField costField        = new TextField();
        costField.setPromptText("0.0");
        TextField dateField        = new TextField();
        dateField.setPromptText("YYYY-MM-DD");
        TextField nextDueDateField = new TextField();
        nextDueDateField.setPromptText("YYYY-MM-DD");
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Completed", "Pending", "In Progress");
        statusBox.setValue("Completed");
        TextField notesField       = new TextField();
        Label errorLabel           = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Asset ID *:"),    0, 0); grid.add(assetIdField,    1, 0);
        grid.add(new Label("Type:"),          0, 1); grid.add(typeBox,         1, 1);
        grid.add(new Label("Description:"),   0, 2); grid.add(descField,       1, 2);
        grid.add(new Label("Cost:"),          0, 3); grid.add(costField,       1, 3);
        grid.add(new Label("Date *:"),        0, 4); grid.add(dateField,       1, 4);
        grid.add(new Label("Next Due Date:"), 0, 5); grid.add(nextDueDateField,1, 5);
        grid.add(new Label("Status:"),        0, 6); grid.add(statusBox,       1, 6);
        grid.add(new Label("Notes:"),         0, 7); grid.add(notesField,      1, 7);
        grid.add(errorLabel,                  1, 8);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable checkFields = () -> {
            boolean valid = !assetIdField.getText().trim().isEmpty()
                    && !dateField.getText().trim().isEmpty();
            okButton.setDisable(!valid);
        };

        assetIdField.textProperty().addListener((o, ov, nv) -> checkFields.run());
        dateField.textProperty().addListener((o, ov, nv) -> checkFields.run());

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validateMaintenance(
                    assetIdField.getText(),
                    dateField.getText(),
                    nextDueDateField.getText(),
                    costField.getText()
            );
            if (error != null) {
                errorLabel.setText(error);
                event.consume();
            }
        });

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
                        .uri(URI.create(ConfigManager.getBaseUrl() + "/api/maintenance"))
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
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private String validateMaintenance(String assetId, String date,
                                       String nextDate, String cost) {
        if (assetId.trim().isEmpty()) return "Asset ID is required.";
        try {
            if (Integer.parseInt(assetId.trim()) <= 0)
                return "Asset ID must be greater than 0.";
        } catch (NumberFormatException e) {
            return "Asset ID must be a number.";
        }
        if (date.trim().isEmpty()) return "Maintenance Date is required.";
        if (!date.trim().matches("\\d{4}-\\d{2}-\\d{2}"))
            return "Date must be YYYY-MM-DD format.";
        if (!nextDate.trim().isEmpty() &&
                !nextDate.trim().matches("\\d{4}-\\d{2}-\\d{2}"))
            return "Next Due Date must be YYYY-MM-DD format.";
        if (!cost.trim().isEmpty()) {
            try { Double.parseDouble(cost.trim()); }
            catch (NumberFormatException e) { return "Cost must be a valid number."; }
        }
        return null;
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

    private double extractDouble(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.length();
        try {
            return Double.parseDouble(
                    json.substring(start, end).trim().replace("}", ""));
        } catch (NumberFormatException e) { return 0.0; }
    }
}