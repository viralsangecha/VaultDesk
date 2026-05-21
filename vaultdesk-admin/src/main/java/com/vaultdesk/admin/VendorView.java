package com.vaultdesk.admin;

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

public class VendorView {

    private TableView<Vendor> table;
    public VBox getView() {
        Label title = new Label("Vendors");
        title.getStyleClass().add("section-title");
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> {
            TableRow<Vendor> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty())
                    showFullEditDialog(row.getItem(), table);
            });
            return row;
        });

        TableColumn<Vendor, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Vendor, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Vendor, String> contactCol = new TableColumn<>("Contact Person");
        contactCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getContactPerson()));

        TableColumn<Vendor, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPhone()));

        TableColumn<Vendor, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail()));

        TableColumn<Vendor, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory()));

        table.getColumns().addAll(idCol, nameCol, contactCol,
                phoneCol, emailCol, categoryCol);

        Button addBtn = new Button("+ Add Vendor");
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
                    "Name", "Contact Person", "Phone",
                    "Email", "Category");
            List<List<String>> rows = new ArrayList<>();
            for (Vendor v : table.getItems()) {
                rows.add(List.of(
                        v.getName(),
                        v.getContactPerson(),
                        v.getPhone(),
                        v.getEmail(),
                        v.getCategory()
                ));
            }
            ExcelExporter.export("Vendors", headers, rows);
        });

        HBox topBar = new HBox(10, addBtn, exportBtn);;

        loadVendors(table);

        VBox root = new VBox(10);
        root.getChildren().addAll(title, topBar, table);
        return root;
    }
    private void showFullEditDialog(Vendor vendor,
                                    TableView<Vendor> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Vendor Details");
        dialog.setHeaderText(vendor.getName());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField(vendor.getName());
        TextField contactField = new TextField(
                vendor.getContactPerson());
        TextField phoneField = new TextField(vendor.getPhone());
        TextField emailField = new TextField(vendor.getEmail());

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(
                "Hardware", "Software", "AMC",
                "Service", "Other");
        categoryBox.setValue(vendor.getCategory());

        TextField addressField = new TextField();
        TextField notesField = new TextField();

        // Load full details
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/vendors/" + vendor.getId()))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            addressField.setText(
                    extractValue(resp.body(), "address"));
            notesField.setText(
                    extractValue(resp.body(), "notes"));
        } catch (Exception ignored) {}

        Label errorLabel = new Label("");
        errorLabel.setStyle(
                "-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        int r = 0;
        grid.add(new Label("Name *:"),         0, r);
        grid.add(nameField,                    1, r++);
        grid.add(new Label("Contact Person:"), 0, r);
        grid.add(contactField,                 1, r++);
        grid.add(new Label("Phone *:"),        0, r);
        grid.add(phoneField,                   1, r++);
        grid.add(new Label("Email:"),          0, r);
        grid.add(emailField,                   1, r++);
        grid.add(new Label("Category:"),       0, r);
        grid.add(categoryBox,                  1, r++);
        grid.add(new Label("Address:"),        0, r);
        grid.add(addressField,                 1, r++);
        grid.add(new Label("Notes:"),          0, r);
        grid.add(notesField,                   1, r++);
        grid.add(errorLabel,                   1, r);
        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okBtn.addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    if (nameField.getText().trim().isEmpty()) {
                        errorLabel.setText("Name is required.");
                        event.consume();
                    } else if (phoneField.getText().trim().isEmpty()) {
                        errorLabel.setText("Phone is required.");
                        event.consume();
                    }
                });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()
                && result.get() == ButtonType.OK) {
            try {
                String body = "{" +
                        "\"id\":" + vendor.getId() + "," +
                        "\"name\":\"" + escape(
                        nameField.getText()) + "\"," +
                        "\"contactPerson\":\"" + escape(
                        contactField.getText()) + "\"," +
                        "\"phone\":\"" +
                        phoneField.getText() + "\"," +
                        "\"email\":\"" +
                        emailField.getText() + "\"," +
                        "\"category\":\"" +
                        categoryBox.getValue() + "\"," +
                        "\"address\":\"" + escape(
                        addressField.getText()) + "\"," +
                        "\"notes\":\"" + escape(
                        notesField.getText()) + "\"" +
                        "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(
                                ConfigManager.getBaseUrl()
                                        + "/api/vendors/"
                                        + vendor.getId()))
                        .header("Content-Type",
                                "application/json")
                        .PUT(HttpRequest.BodyPublishers
                                .ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(
                        req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    showAlert("Success", "Vendor updated.");
                    loadVendors(table);
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

    private void loadVendors(TableView<Vendor> table) {
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading vendors...");
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/vendors"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    table.getItems().add(new Vendor(
                            extractInt(obj, "id"),
                            extractValue(obj, "name"),
                            extractValue(obj, "contactPerson"),
                            extractValue(obj, "phone"),
                            extractValue(obj, "email"),
                            extractValue(obj, "category")
                    ));
                }
                if (table.getItems().isEmpty()) {
                    LoadingUtil.setEmpty(table, "🤝",
                            "No vendors found",
                            "Add your first vendor contact.");
                }
            } else {
                LoadingUtil.setEmpty(table, "🤝",
                        "No vendors found",
                        "Add your first vendor contact.");
            }
        } catch (Exception ex) {
            LoadingUtil.setEmpty(table, "⚠",
                    "Could not load vendors",
                    "Check server connection and try again.");
        }
    }

    private void showAddDialog(TableView<Vendor> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Vendor");
        dialog.setHeaderText("Enter vendor details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField    = new TextField();
        TextField contactField = new TextField();
        TextField phoneField   = new TextField();
        TextField emailField   = new TextField();
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Hardware", "Software", "AMC", "Service", "Other");
        categoryBox.setValue("Hardware");
        TextField addressField = new TextField();
        TextField notesField   = new TextField();
        Label errorLabel       = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Name *:"),           0, 0); grid.add(nameField,    1, 0);
        grid.add(new Label("Contact Person:"),   0, 1); grid.add(contactField, 1, 1);
        grid.add(new Label("Phone *:"),          0, 2); grid.add(phoneField,   1, 2);
        grid.add(new Label("Email:"),            0, 3); grid.add(emailField,   1, 3);
        grid.add(new Label("Category:"),         0, 4); grid.add(categoryBox,  1, 4);
        grid.add(new Label("Address:"),          0, 5); grid.add(addressField, 1, 5);
        grid.add(new Label("Notes:"),            0, 6); grid.add(notesField,   1, 6);
        grid.add(errorLabel,                     1, 7);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable checkFields = () -> {
            boolean valid = !nameField.getText().trim().isEmpty()
                    && !phoneField.getText().trim().isEmpty();
            okButton.setDisable(!valid);
        };

        nameField.textProperty().addListener((o, ov, nv) -> checkFields.run());
        phoneField.textProperty().addListener((o, ov, nv) -> checkFields.run());

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validateVendor(nameField.getText(), phoneField.getText());
            if (error != null) {
                errorLabel.setText(error);
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String body = "{" +
                    "\"name\":\"" + nameField.getText() + "\"," +
                    "\"contactPerson\":\"" + contactField.getText() + "\"," +
                    "\"phone\":\"" + phoneField.getText() + "\"," +
                    "\"email\":\"" + emailField.getText() + "\"," +
                    "\"category\":\"" + categoryBox.getValue() + "\"," +
                    "\"address\":\"" + addressField.getText() + "\"," +
                    "\"notes\":\"" + notesField.getText() + "\"" +
                    "}";
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigManager.getBaseUrl() + "/api/vendors"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201) {
                    showAlert("Success", "Vendor added.");
                    loadVendors(table);
                } else {
                    showAlert("Error", "Server returned: " + response.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private String validateVendor(String name, String phone) {
        if (name.trim().isEmpty())  return "Vendor Name is required.";
        if (phone.trim().isEmpty()) return "Phone number is required.";
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
}