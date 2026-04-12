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

public class ConsumableView {

    public VBox getView() {
        Label title = new Label("Consumables");
        title.getStyleClass().add("section-title");
        TableView<Consumable> table = new TableView<>();

        TableColumn<Consumable, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Consumable, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Consumable, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory()));

        TableColumn<Consumable, String> unitCol = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUnit()));

        TableColumn<Consumable, Integer> stockCol = new TableColumn<>("In Stock");
        stockCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getQuantityInStock()).asObject());

        TableColumn<Consumable, Integer> reorderCol = new TableColumn<>("Reorder Level");
        reorderCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getReorderLevel()).asObject());

        TableColumn<Consumable, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button updateQtyBtn = new Button("Update Qty");
            {
                updateQtyBtn.getStyleClass().add("btn-warning");
                updateQtyBtn.setOnAction(e -> {
                    Consumable c = getTableView().getItems().get(getIndex());
                    showUpdateQtyDialog(c, getTableView());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, updateQtyBtn));
            }
        });

        table.getColumns().addAll(idCol, nameCol, categoryCol,
                unitCol, stockCol, reorderCol, actionCol);

        Button addBtn = new Button("Add Consumable");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddDialog(table));
        HBox topBar = new HBox(10);
        topBar.getChildren().add(addBtn);

        loadConsumables(table);

        VBox root = new VBox(10);
        root.getChildren().addAll(title, topBar, table);
        return root;
    }

    private void loadConsumables(TableView<Consumable> table) {
        table.getItems().clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/consumables"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    table.getItems().add(new Consumable(
                            extractInt(obj, "id"),
                            extractValue(obj, "name"),
                            extractValue(obj, "category"),
                            extractValue(obj, "unit"),
                            extractInt(obj, "quantityInStock"),
                            extractInt(obj, "reorderLevel")
                    ));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading consumables: " + ex.getMessage());
        }
    }

    private void showAddDialog(TableView<Consumable> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Consumable");
        dialog.setHeaderText("Enter consumable details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField();
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Toner", "Ink", "Cable", "Paper",
                "Battery", "Cleaning Kit", "Other");
        categoryBox.setValue("Toner");
        TextField compatibleField = new TextField();
        TextField qtyField = new TextField();
        qtyField.setPromptText("0");
        TextField reorderField = new TextField();
        reorderField.setPromptText("0");
        ComboBox<String> unitBox = new ComboBox<>();
        unitBox.getItems().addAll("pieces", "boxes", "reams", "meters");
        unitBox.setValue("pieces");
        TextField costField = new TextField();
        costField.setPromptText("0.0");
        TextField locationField = new TextField();
        TextField notesField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Name:"),             0, 0); grid.add(nameField,       1, 0);
        grid.add(new Label("Category:"),         0, 1); grid.add(categoryBox,     1, 1);
        grid.add(new Label("Compatible Models:"),0, 2); grid.add(compatibleField, 1, 2);
        grid.add(new Label("Quantity:"),         0, 3); grid.add(qtyField,        1, 3);
        grid.add(new Label("Reorder Level:"),    0, 4); grid.add(reorderField,    1, 4);
        grid.add(new Label("Unit:"),             0, 5); grid.add(unitBox,         1, 5);
        grid.add(new Label("Unit Cost:"),        0, 6); grid.add(costField,       1, 6);
        grid.add(new Label("Storage Location:"), 0, 7); grid.add(locationField,   1, 7);
        grid.add(new Label("Notes:"),            0, 8); grid.add(notesField,      1, 8);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int qty = qtyField.getText().trim().isEmpty() ? 0
                        : Integer.parseInt(qtyField.getText().trim());
                int reorder = reorderField.getText().trim().isEmpty() ? 0
                        : Integer.parseInt(reorderField.getText().trim());
                double cost = costField.getText().trim().isEmpty() ? 0.0
                        : Double.parseDouble(costField.getText().trim());
                String body = "{" +
                        "\"name\":\"" + nameField.getText() + "\"," +
                        "\"category\":\"" + categoryBox.getValue() + "\"," +
                        "\"compatibleModels\":\"" + compatibleField.getText() + "\"," +
                        "\"quantityInStock\":" + qty + "," +
                        "\"reorderLevel\":" + reorder + "," +
                        "\"unit\":\"" + unitBox.getValue() + "\"," +
                        "\"vendorId\":0," +
                        "\"unitCost\":" + cost + "," +
                        "\"storageLocation\":\"" + locationField.getText() + "\"," +
                        "\"notes\":\"" + notesField.getText() + "\"" +
                        "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/consumables"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201) {
                    showAlert("Success", "Consumable added.");
                    loadConsumables(table);
                } else {
                    showAlert("Error", "Server returned: " + response.statusCode());
                }
            } catch (NumberFormatException ex) {
                showAlert("Error", "Quantity, Reorder Level and Cost must be numbers.");
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private void showUpdateQtyDialog(Consumable c, TableView<Consumable> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Quantity");
        dialog.setHeaderText(c.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField qtyField = new TextField(String.valueOf(c.getQuantityInStock()));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("New Quantity:"), 0, 0);
        grid.add(qtyField, 1, 0);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int qty = Integer.parseInt(qtyField.getText().trim());
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/consumables/"
                                + c.getId() + "/quantity?quantity=" + qty))
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    showAlert("Success", "Quantity updated.");
                    loadConsumables(table);
                } else {
                    showAlert("Error", "Server returned: " + response.statusCode());
                }
            } catch (NumberFormatException ex) {
                showAlert("Error", "Quantity must be a number.");
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