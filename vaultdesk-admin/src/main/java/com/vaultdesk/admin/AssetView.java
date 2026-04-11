package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class AssetView {

    public VBox getView() {
        Label title = new Label("Assets");
        TableView<Asset> table = new TableView<>();

        TableColumn<Asset, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Asset, String> assetTagCol = new TableColumn<>("Asset Tag");
        assetTagCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAssetTag()));

        TableColumn<Asset, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Asset, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory()));

        TableColumn<Asset, String> brandCol = new TableColumn<>("Brand");
        brandCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getBrand()));

        TableColumn<Asset, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));

        TableColumn<Asset, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLocation()));

        TableColumn<Asset, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit Status");
            {
                editBtn.setOnAction(e -> {
                    Asset asset = getTableView().getItems().get(getIndex());
                    showEditStatusDialog(asset, getTableView());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, editBtn));
            }
        });

        table.getColumns().addAll(idCol, assetTagCol, nameCol, categoryCol,
                brandCol, statusCol, locationCol, actionCol);

        Button addBtn = new Button("Add Asset");
        addBtn.setOnAction(e -> showAddDialog(table));
        HBox topBar = new HBox(10);
        topBar.getChildren().add(addBtn);

        loadAssets(table);

        VBox root = new VBox(10);
        root.getChildren().addAll(title, topBar, table);
        return root;
    }

    private void loadAssets(TableView<Asset> table) {
        table.getItems().clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/assets"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);

            if (!body.isEmpty()) {
                String[] objects = body.split("\\},\\{");
                for (String obj : objects) {
                    obj = obj.replace("{", "").replace("}", "");
                    int id = extractInt(obj, "id");
                    String assetTag = extractValue(obj, "assetTag");
                    String name = extractValue(obj, "name");
                    String category = extractValue(obj, "category");
                    String brand = extractValue(obj, "brand");
                    String seserialNumber=extractValue(obj,"serialNumber");
                    String notes=extractValue(obj,"notes");
                    String status = extractValue(obj, "status");
                    String location = extractValue(obj, "location");
                    table.getItems().add(new Asset(id, assetTag, name,
                            category, brand,seserialNumber,notes, status, location));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading assets: " + ex.getMessage());
        }
    }

    private void showAddDialog(TableView<Asset> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Asset");
        dialog.setHeaderText("Enter asset details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField assetTagField = new TextField();
        TextField nameField = new TextField();
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("PC", "Laptop", "Server", "Printer",
                "Switch", "Router", "UPS", "Mobile", "Other");
        categoryBox.setValue("PC");
        TextField brandField = new TextField();
        TextField modelField = new TextField();
        TextField serialField = new TextField();
        TextField deptField = new TextField();
        TextField locationField = new TextField();
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Active", "In Repair", "Retired", "Disposed");
        statusBox.setValue("Active");
        TextField costField = new TextField();
        costField.setPromptText("0.0");
        TextField notesField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Asset Tag:"),    0, 0);  grid.add(assetTagField, 1, 0);
        grid.add(new Label("Name:"),         0, 1);  grid.add(nameField,     1, 1);
        grid.add(new Label("Category:"),     0, 2);  grid.add(categoryBox,   1, 2);
        grid.add(new Label("Brand:"),        0, 3);  grid.add(brandField,    1, 3);
        grid.add(new Label("Model:"),        0, 4);  grid.add(modelField,    1, 4);
        grid.add(new Label("Serial No:"),    0, 5);  grid.add(serialField,   1, 5);
        grid.add(new Label("Dept ID:"),      0, 6);  grid.add(deptField,     1, 6);
        grid.add(new Label("Location:"),     0, 7);  grid.add(locationField, 1, 7);
        grid.add(new Label("Status:"),       0, 8);  grid.add(statusBox,     1, 8);
        grid.add(new Label("Cost:"),         0, 9);  grid.add(costField,     1, 9);
        grid.add(new Label("Notes:"),        0, 10); grid.add(notesField,    1, 10);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int deptId = deptField.getText().trim().isEmpty() ? 0
                        : Integer.parseInt(deptField.getText().trim());
                double cost = costField.getText().trim().isEmpty() ? 0.0
                        : Double.parseDouble(costField.getText().trim());

                boolean success = saveAsset(
                        assetTagField.getText(), nameField.getText(),
                        categoryBox.getValue(), brandField.getText(),
                        modelField.getText(), serialField.getText(),
                        deptId, locationField.getText(),
                        statusBox.getValue(), cost, notesField.getText()
                );
                if (success) loadAssets(table);

            } catch (NumberFormatException ex) {
                showAlert("Error", "Dept ID must be a number. Cost must be a number.");
            }
        }
    }

    private void showEditStatusDialog(Asset asset, TableView<Asset> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Asset Status");
        dialog.setHeaderText("Asset: " + asset.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Active", "In Repair", "Retired", "Disposed");
        statusBox.setValue(asset.getStatus());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Status:"), 0, 0);
        grid.add(statusBox, 1, 0);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = updateAssetStatus(asset.getId(), statusBox.getValue());
            if (success) loadAssets(table);
        }
    }

    private boolean saveAsset(String assetTag, String name, String category,
                              String brand, String model, String serialNumber,
                              int departmentId, String location, String status,
                              double purchaseCost, String notes) {
        try {
            String body = "{" +
                    "\"assetTag\":\"" + assetTag + "\"," +
                    "\"name\":\"" + name + "\"," +
                    "\"category\":\"" + category + "\"," +
                    "\"brand\":\"" + brand + "\"," +
                    "\"model\":\"" + model + "\"," +
                    "\"serialNumber\":\"" + serialNumber + "\"," +
                    "\"departmentId\":" + departmentId + "," +
                    "\"location\":\"" + location + "\"," +
                    "\"status\":\"" + status + "\"," +
                    "\"assignedTo\":0," +
                    "\"assignedDate\":null," +
                    "\"purchaseDate\":null," +
                    "\"warrantyExpiry\":null," +
                    "\"vendorId\":0," +
                    "\"purchaseCost\":" + purchaseCost + "," +
                    "\"notes\":\"" + notes + "\"" +
                    "}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/assets"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                showAlert("Success", "Asset added successfully.");
                return true;
            } else {
                showAlert("Error", "Server returned: " + response.statusCode());
                return false;
            }
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private boolean updateAssetStatus(int id, String status) {
        try {
            String encodedStatus = URLEncoder.encode(status, StandardCharsets.UTF_8);
            String url = "http://localhost:8080/api/assets/" + id
                    + "/status?status=" + encodedStatus;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                showAlert("Success", "Status updated to: " + status);
                return true;
            } else {
                showAlert("Error", "Server returned: " + response.statusCode());
                return false;
            }
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
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
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.length();
        String value = json.substring(start, end).trim().replace("}", "");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}