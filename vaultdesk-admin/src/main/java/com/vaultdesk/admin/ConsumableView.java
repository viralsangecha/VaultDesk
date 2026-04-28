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
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

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

        // ── Stock level — colored text ─────────────────────────
        TableColumn<Consumable, String> stockLevelCol = new TableColumn<>("Stock Level");
        stockLevelCol.setCellValueFactory(data -> {
            Consumable c = data.getValue();
            String label;
            if (c.getQuantityInStock() == 0)
                label = "Out of Stock";
            else if (c.getQuantityInStock() <= c.getReorderLevel())
                label = "Low";
            else
                label = "OK";
            return new SimpleStringProperty(label);
        });
        stockLevelCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "Out of Stock" -> setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
                    case "Low"          -> setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
                    default             -> setStyle("-fx-text-fill: #3fb950;");
                }
            }
        });

        TableColumn<Consumable, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button updateQtyBtn = new Button("Update Qty");
            private final HBox box = new HBox(5, updateQtyBtn);
            {
                updateQtyBtn.getStyleClass().setAll("btn-warning");
                updateQtyBtn.setStyle("-fx-background-color: #b45309; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-font-weight: bold;");
                updateQtyBtn.setOnAction(e -> {
                    Consumable c = getTableView().getItems().get(getIndex());
                    showUpdateQtyDialog(c, getTableView());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(idCol, nameCol, categoryCol,
                unitCol, stockCol, reorderCol,stockLevelCol, actionCol);

        Button addBtn = new Button("+ Add Consumable");
        addBtn.getStyleClass().setAll("btn-primary");
        addBtn.setStyle("-fx-background-color: #238636; -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-font-weight: bold;");
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

        TextField nameField       = new TextField();
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Toner", "Ink", "Cable", "Paper",
                "Battery", "Cleaning Kit", "Other");
        categoryBox.setValue("Toner");
        TextField compatibleField = new TextField();
        TextField qtyField        = new TextField();
        qtyField.setPromptText("0");
        TextField reorderField    = new TextField();
        reorderField.setPromptText("0");
        ComboBox<String> unitBox  = new ComboBox<>();
        unitBox.getItems().addAll("pieces", "boxes", "reams", "meters");
        unitBox.setValue("pieces");
        TextField costField       = new TextField();
        costField.setPromptText("0.0");
        TextField locationField   = new TextField();
        TextField notesField      = new TextField();
        Label errorLabel          = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Name *:"),              0, 0); grid.add(nameField,       1, 0);
        grid.add(new Label("Category:"),            0, 1); grid.add(categoryBox,     1, 1);
        grid.add(new Label("Compatible Models:"),   0, 2); grid.add(compatibleField, 1, 2);
        grid.add(new Label("Quantity:"),            0, 3); grid.add(qtyField,        1, 3);
        grid.add(new Label("Reorder Level:"),       0, 4); grid.add(reorderField,    1, 4);
        grid.add(new Label("Unit:"),                0, 5); grid.add(unitBox,         1, 5);
        grid.add(new Label("Unit Cost:"),           0, 6); grid.add(costField,       1, 6);
        grid.add(new Label("Storage Location:"),    0, 7); grid.add(locationField,   1, 7);
        grid.add(new Label("Notes:"),               0, 8); grid.add(notesField,      1, 8);
        grid.add(errorLabel,                        1, 9);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        nameField.textProperty().addListener((o, ov, nv) ->
                okButton.setDisable(nv.trim().isEmpty()));

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validateConsumable(
                    nameField.getText(),
                    qtyField.getText(),
                    reorderField.getText(),
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
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private String validateConsumable(String name, String qty,
                                      String reorder, String cost) {
        if (name.trim().isEmpty()) return "Consumable Name is required.";
        if (!qty.trim().isEmpty()) {
            try {
                if (Integer.parseInt(qty.trim()) < 0)
                    return "Quantity cannot be negative.";
            } catch (NumberFormatException e) {
                return "Quantity must be a number.";
            }
        }
        if (!reorder.trim().isEmpty()) {
            try {
                if (Integer.parseInt(reorder.trim()) < 0)
                    return "Reorder Level cannot be negative.";
            } catch (NumberFormatException e) {
                return "Reorder Level must be a number.";
            }
        }
        if (!cost.trim().isEmpty()) {
            try { Double.parseDouble(cost.trim()); }
            catch (NumberFormatException e) { return "Cost must be a valid number."; }
        }
        return null;
    }

    private void showUpdateQtyDialog(Consumable c, TableView<Consumable> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Quantity");
        dialog.setHeaderText(c.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField qtyField = new TextField(String.valueOf(c.getQuantityInStock()));
        Label errorLabel   = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("New Quantity:"), 0, 0);
        grid.add(qtyField, 1, 0);
        grid.add(errorLabel, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                if (Integer.parseInt(qtyField.getText().trim()) < 0) {
                    errorLabel.setText("Quantity cannot be negative.");
                    event.consume();
                }
            } catch (NumberFormatException e) {
                errorLabel.setText("Must be a valid number.");
                event.consume();
            }
        });

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