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

public class LicenseView {

    public VBox getView() {
        Label title = new Label("Licenses");
        title.getStyleClass().add("section-title");
        TableView<License> table = new TableView<>();

        TableColumn<License, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<License, String> softwareNameCol = new TableColumn<>("Software");
        softwareNameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSoftwareName()));

        TableColumn<License, String> licenseTypeCol = new TableColumn<>("Type");
        licenseTypeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLicenseType()));

        TableColumn<License, String> vendorCol = new TableColumn<>("Vendor");
        vendorCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getVendor()));

        TableColumn<License, String> expiryCol = new TableColumn<>("Expiry");
        expiryCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getExpiryDate()));

        TableColumn<License, Integer> seatsTotalCol = new TableColumn<>("Total Seats");
        seatsTotalCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getSeatsTotal()).asObject());

        TableColumn<License, Integer> seatsUsedCol = new TableColumn<>("Used Seats");
        seatsUsedCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getSeatsUsed()).asObject());

        TableColumn<License, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button updateSeatsBtn = new Button("Update Seats");
            {
                updateSeatsBtn.getStyleClass().add("btn-warning");
                updateSeatsBtn.setOnAction(e -> {
                    License license = getTableView().getItems().get(getIndex());
                    showUpdateSeatsDialog(license, getTableView());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, updateSeatsBtn));
            }
        });

        table.getColumns().addAll(idCol, softwareNameCol, licenseTypeCol,
                vendorCol, expiryCol, seatsTotalCol, seatsUsedCol, actionCol);

        Button addBtn = new Button("Add License");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddDialog(table));
        HBox topBar = new HBox(10);
        topBar.getChildren().add(addBtn);

        loadLicenses(table);

        VBox root = new VBox(10);
        root.getChildren().addAll(title, topBar, table);
        return root;
    }

    private void loadLicenses(TableView<License> table) {
        table.getItems().clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/licenses"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    table.getItems().add(new License(
                            extractInt(obj, "id"),
                            extractValue(obj, "softwareName"),
                            extractValue(obj, "licenseType"),
                            extractValue(obj, "vendor"),
                            extractValue(obj, "expiryDate"),
                            extractInt(obj, "seatsTotal"),
                            extractInt(obj, "seatsUsed")
                    ));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading licenses: " + ex.getMessage());
        }
    }

    private void showAddDialog(TableView<License> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add License");
        dialog.setHeaderText("Enter license details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField softwareNameField = new TextField();
        ComboBox<String> licenseTypeBox = new ComboBox<>();
        licenseTypeBox.getItems().addAll("Perpetual", "Subscription", "OEM", "Trial");
        licenseTypeBox.setValue("Subscription");
        TextField licenseKeyField = new TextField();
        TextField seatsTotalField = new TextField();
        TextField vendorField = new TextField();
        TextField purchaseDateField = new TextField();
        purchaseDateField.setPromptText("YYYY-MM-DD");
        TextField expiryDateField = new TextField();
        expiryDateField.setPromptText("YYYY-MM-DD");
        TextField costField = new TextField();
        costField.setPromptText("0.0");
        TextField notesField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Software Name:"),  0, 0);  grid.add(softwareNameField, 1, 0);
        grid.add(new Label("License Type:"),   0, 1);  grid.add(licenseTypeBox,    1, 1);
        grid.add(new Label("License Key:"),    0, 2);  grid.add(licenseKeyField,   1, 2);
        grid.add(new Label("Total Seats:"),    0, 3);  grid.add(seatsTotalField,   1, 3);
        grid.add(new Label("Vendor:"),         0, 4);  grid.add(vendorField,       1, 4);
        grid.add(new Label("Purchase Date:"),  0, 5);  grid.add(purchaseDateField, 1, 5);
        grid.add(new Label("Expiry Date:"),    0, 6);  grid.add(expiryDateField,   1, 6);
        grid.add(new Label("Cost:"),           0, 7);  grid.add(costField,         1, 7);
        grid.add(new Label("Notes:"),          0, 8);  grid.add(notesField,        1, 8);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int seats = Integer.parseInt(seatsTotalField.getText().trim());
                double cost = costField.getText().trim().isEmpty() ? 0.0
                        : Double.parseDouble(costField.getText().trim());
                String body = "{" +
                        "\"softwareName\":\"" + softwareNameField.getText() + "\"," +
                        "\"licenseType\":\"" + licenseTypeBox.getValue() + "\"," +
                        "\"licenseKey\":\"" + licenseKeyField.getText() + "\"," +
                        "\"seatsTotal\":" + seats + "," +
                        "\"seatsUsed\":0," +
                        "\"vendor\":\"" + vendorField.getText() + "\"," +
                        "\"purchaseDate\":\"" + purchaseDateField.getText() + "\"," +
                        "\"expiryDate\":\"" + expiryDateField.getText() + "\"," +
                        "\"cost\":" + cost + "," +
                        "\"notes\":\"" + notesField.getText() + "\"" +
                        "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/licenses"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201) {
                    showAlert("Success", "License added.");
                    loadLicenses(table);
                } else {
                    showAlert("Error", "Server returned: " + response.statusCode());
                }
            } catch (NumberFormatException ex) {
                showAlert("Error", "Seats and Cost must be numbers.");
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private void showUpdateSeatsDialog(License license, TableView<License> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Seats Used");
        dialog.setHeaderText(license.getSoftwareName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField seatsField = new TextField(String.valueOf(license.getSeatsUsed()));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Seats Used:"), 0, 0);
        grid.add(seatsField, 1, 0);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int used = Integer.parseInt(seatsField.getText().trim());
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/licenses/"
                                + license.getId() + "/seats?used=" + used))
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    showAlert("Success", "Seats updated.");
                    loadLicenses(table);
                } else {
                    showAlert("Error", "Server returned: " + response.statusCode());
                }
            } catch (NumberFormatException ex) {
                showAlert("Error", "Seats must be a number.");
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
}