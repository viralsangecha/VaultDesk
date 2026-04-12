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

public class VendorView {

    public VBox getView() {
        Label title = new Label("Vendors");
        title.getStyleClass().add("section-title");
        TableView<Vendor> table = new TableView<>();

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
        HBox topBar = new HBox(10);
        topBar.getChildren().add(addBtn);

        loadVendors(table);

        VBox root = new VBox(10);
        root.getChildren().addAll(title, topBar, table);
        return root;
    }

    private void loadVendors(TableView<Vendor> table) {
        table.getItems().clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/vendors"))
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
            }
        } catch (Exception ex) {
            System.out.println("Error loading vendors: " + ex.getMessage());
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
                        .uri(URI.create("http://localhost:8080/api/vendors"))
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