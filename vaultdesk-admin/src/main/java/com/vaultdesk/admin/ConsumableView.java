package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConsumableView {

    private TableView<Consumable> table;

    public VBox getView() {
        Label bcRoot = new Label("INVENTORY");
        bcRoot.getStyleClass().add("breadcrumb-root");
        Label bcSep = new Label("  /  ");
        bcSep.getStyleClass().add("breadcrumb-sep");
        Label bcCurrent = new Label("CONSUMABLES");
        bcCurrent.getStyleClass().add("breadcrumb-current");
        HBox breadcrumb = new HBox(bcRoot, bcSep, bcCurrent);

        Label title = new Label("Consumables");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Stock levels, restocks, and issuance to assets/employees.");
        subtitle.getStyleClass().add("page-subtitle");

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> {
            TableRow<Consumable> row = new TableRow<>();

            ContextMenu rowMenu = new ContextMenu();
            if (PermissionManager.canAddConsumable()) {
                MenuItem editItem = new MenuItem("✏ Edit Consumable");
                editItem.setOnAction(e -> showFullEditDialog(row.getItem(), table));
                rowMenu.getItems().add(editItem);

                MenuItem updateQtyItem = new MenuItem("📦 Update Quantity");
                updateQtyItem.setOnAction(e -> showUpdateQtyDialog(row.getItem(), table));
                rowMenu.getItems().add(updateQtyItem);

                MenuItem issueItem = new MenuItem("➜ Issue");
                issueItem.setOnAction(e -> showIssueDialog(row.getItem(), table));
                rowMenu.getItems().add(issueItem);
            }
            MenuItem historyItem = new MenuItem("📋 Stock History");
            historyItem.setOnAction(e -> showStockHistoryDialog(row.getItem()));
            rowMenu.getItems().add(historyItem);

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu)
            );

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    if (PermissionManager.canAddConsumable()) {
                        showFullEditDialog(row.getItem(), table);
                    } else {
                        showAlert("Access Denied", "You don't have permission to edit consumables.");
                    }
                }
            });
            return row;
        });

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

        table.getColumns().addAll(idCol, nameCol, categoryCol,
                unitCol, stockCol, reorderCol, stockLevelCol);

        Button addBtn = new Button("+ Add Consumable");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddDialog(table));
        addBtn.setVisible(PermissionManager.canAddConsumable());
        addBtn.setManaged(PermissionManager.canAddConsumable());
        AnimationUtil.addHoverScale(addBtn);

        Button exportBtn = new Button("⬇ Export");
        exportBtn.getStyleClass().add("btn-export");
        AnimationUtil.addHoverScale(exportBtn);
        exportBtn.setOnAction(e -> {
            List<String> headers = List.of(
                    "Name", "Category", "Unit",
                    "In Stock", "Reorder Level", "Stock Level");
            List<List<String>> rows = new ArrayList<>();
            for (Consumable c : table.getItems()) {
                String level = c.getQuantityInStock() == 0
                        ? "Out of Stock"
                        : c.getQuantityInStock()
                        <= c.getReorderLevel() ? "Low" : "OK";
                rows.add(List.of(
                        c.getName(),
                        c.getCategory(),
                        c.getUnit(),
                        String.valueOf(c.getQuantityInStock()),
                        String.valueOf(c.getReorderLevel()),
                        level
                ));
            }
            ExcelExporter.export("Consumables", headers, rows);
        });

        Label rightClickHint = new Label("Right-click a row or empty space for actions.");
        rightClickHint.getStyleClass().add("text-muted");
        rightClickHint.setStyle("-fx-font-size: 11px;");
        HBox topBar = new HBox(10, rightClickHint);

        loadConsumables(table);

        VBox tableWrapper = new VBox(table);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);

        VBox root = new VBox(12, breadcrumb, new VBox(4, title, subtitle), topBar, tableWrapper);

        ContextMenu screenMenu = new ContextMenu();
        screenMenu.setAutoHide(true);
        if (PermissionManager.canAddConsumable()) {
            MenuItem newItem = new MenuItem("+ New Consumable");
            newItem.setOnAction(e -> showAddDialog(table));
            screenMenu.getItems().add(newItem);
        }
        MenuItem exportItem = new MenuItem("⬇ Export Consumables");
        exportItem.setOnAction(e -> exportBtn.fire());
        screenMenu.getItems().add(exportItem);

        root.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) screenMenu.hide();
        });
        root.setOnContextMenuRequested(e -> {
            boolean clickedOnRow = false;
            if (e.getTarget() instanceof javafx.scene.Node) {
                javafx.scene.Node current = (javafx.scene.Node) e.getTarget();
                while (current != null) {
                    if (current instanceof TableRow) {
                        TableRow<?> tr = (TableRow<?>) current;
                        if (!tr.isEmpty()) clickedOnRow = true;
                        break;
                    }
                    current = current.getParent();
                }
            }
            if (!clickedOnRow) {
                screenMenu.show(root, e.getScreenX(), e.getScreenY());
            } else {
                screenMenu.hide();
            }
        });

        return root;
    }

    private void showFullEditDialog(Consumable c, TableView<Consumable> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Consumable Details");
        dialog.setHeaderText(c.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField(c.getName());
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Toner", "Ink", "Cable", "Paper", "Battery", "Cleaning Kit", "Other");
        categoryBox.setValue(c.getCategory());
        TextField compatibleField = new TextField();
        ComboBox<String> unitBox = new ComboBox<>();
        unitBox.getItems().addAll("pieces", "boxes", "reams", "meters");
        unitBox.setValue(c.getUnit());
        NumberField reorderField = new NumberField();
        reorderField.setText(String.valueOf(c.getReorderLevel()));
        NumberField costField = new NumberField();
        TextField locationField = new TextField();
        TextField notesField = new TextField();
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        try {
            HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/consumables");
            String body = resp.body();
            for (String obj : body.substring(1, body.length() - 1).split("\\},\\{")) {
                String cleaned = obj.replace("{", "").replace("}", "");
                if (extractInt(cleaned, "id") == c.getId()) {
                    compatibleField.setText(extractValue(cleaned, "compatibleModels"));
                    costField.setText(String.valueOf(extractDouble(cleaned, "unitCost")));
                    locationField.setText(extractValue(cleaned, "storageLocation"));
                    notesField.setText(extractValue(cleaned, "notes"));
                    break;
                }
            }
        } catch (Exception ignored) {}

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(140);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

        int r = 0;
        grid.add(new Label("Name *:"), 0, r);              grid.add(nameField, 1, r++);
        grid.add(new Label("Category:"), 0, r);            grid.add(categoryBox, 1, r++);
        grid.add(new Label("Compatible Models:"), 0, r);   grid.add(compatibleField, 1, r++);
        grid.add(new Label("Unit:"), 0, r);                grid.add(unitBox, 1, r++);
        grid.add(new Label("Reorder Level:"), 0, r);       grid.add(reorderField, 1, r++);
        grid.add(new Label("Unit Cost:"), 0, r);           grid.add(costField, 1, r++);
        grid.add(new Label("Storage Location:"), 0, r);    grid.add(locationField, 1, r++);
        grid.add(new Label("Notes:"), 0, r);               grid.add(notesField, 1, r++);
        grid.add(errorLabel, 1, r);
        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!ValidationUtil.isNotBlank(nameField.getText())) {
                errorLabel.setText("Name is required.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String body = "{" +
                        "\"id\":" + c.getId() + "," +
                        "\"name\":\"" + escapeJson(nameField.getText()) + "\"," +
                        "\"category\":\"" + categoryBox.getValue() + "\"," +
                        "\"compatibleModels\":\"" + escapeJson(compatibleField.getText()) + "\"," +
                        "\"quantityInStock\":" + c.getQuantityInStock() + "," +
                        "\"reorderLevel\":" + reorderField.getIntValue() + "," +
                        "\"unit\":\"" + unitBox.getValue() + "\"," +
                        "\"vendorId\":0," +
                        "\"unitCost\":" + costField.getDoubleValue() + "," +
                        "\"storageLocation\":\"" + escapeJson(locationField.getText()) + "\"," +
                        "\"notes\":\"" + escapeJson(notesField.getText()) + "\"" +
                        "}";
                HttpResponse<String> resp = ApiClient.put(ConfigManager.getBaseUrl() + "/api/consumables/" + c.getId(), body);
                if (resp.statusCode() == 200) {
                    ToastUtil.success("Consumable updated.");
                    loadConsumables(table);
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        }
    }

    private double extractDouble(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return 0.0;
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) end = json.length();
        try { return Double.parseDouble(json.substring(start, end).trim()); }
        catch (Exception e) { return 0.0; }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "'").replace("\n", " ").replace("\r", "");
    }

    private void loadConsumables(TableView<Consumable> table) {
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading consumables...");

        Task<List<Consumable>> task = new Task<>() {
            @Override
            protected List<Consumable> call() throws Exception {
                List<Consumable> result = new ArrayList<>();

                String url = ConfigManager.getBaseUrl() + "/api/consumables";
                HttpResponse<String> response = ApiClient.get(url);
                String body = response.body().trim();

                if (body.equals("[]") || body.isEmpty()) {
                    return result;
                }

                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    String[] jsonObjects = body.split("\\},\\s*\\{");
                    for (String obj : jsonObjects) {
                        String cleanedObj = obj.replace("{", "").replace("}", "");

                        Consumable c = new Consumable(
                                extractInt(cleanedObj, "id"),
                                extractValue(cleanedObj, "name"),
                                extractValue(cleanedObj, "category"),
                                extractValue(cleanedObj, "unit"),
                                extractInt(cleanedObj, "quantityInStock"),
                                extractInt(cleanedObj, "reorderLevel")
                        );
                        result.add(c);
                    }
                }
                return result;
            }
        };

        task.setOnSucceeded(e -> {
            List<Consumable> consumables = task.getValue();
            table.getItems().addAll(consumables);
            if (consumables.isEmpty()) {
                LoadingUtil.setEmpty(table, "📦", "No consumables found", "Add your first consumable item.");
            }
        });

        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠", "Could not load consumables", "Check server connection and try again.");
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showStockHistoryDialog(Consumable c) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Stock History — " + c.getName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(560);

        TableView<String[]> historyTable = new TableView<>();
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        historyTable.setPrefHeight(320);

        TableColumn<String[], String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        TableColumn<String[], String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        TableColumn<String[], String> changeCol = new TableColumn<>("Change");
        changeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        TableColumn<String[], String> byCol = new TableColumn<>("Changed By");
        byCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[3]));
        TableColumn<String[], String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[4]));
        historyTable.getColumns().addAll(dateCol, typeCol, changeCol, byCol, notesCol);

        Task<List<String[]>> task = new Task<>() {
            @Override
            protected List<String[]> call() throws Exception {
                List<String[]> result = new ArrayList<>();
                HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/consumables/" + c.getId() + "/stock-history");
                String body = resp.body().trim();
                if (body.length() < 2) return result;
                body = body.substring(1, body.length() - 1).trim();
                if (body.isEmpty()) return result;
                for (String obj : body.split("\\},\\{")) {
                    String cleaned = obj.replace("{", "").replace("}", "");
                    int oldQty = extractInt(cleaned, "old_quantity");
                    int newQty = extractInt(cleaned, "new_quantity");
                    int change = extractInt(cleaned, "change_amount");
                    String changeStr = (change >= 0 ? "+" : "") + change + " (" + oldQty + " → " + newQty + ")";
                    result.add(new String[]{
                            DateTimeFormatUtil.toIndianDateTime(extractValue(cleaned, "changed_at")),
                            extractValue(cleaned, "change_type"),
                            changeStr,
                            extractValue(cleaned, "changed_by_name"),
                            extractValue(cleaned, "notes")
                    });
                }
                return result;
            }
        };
        task.setOnSucceeded(e -> {
            historyTable.getItems().setAll(task.getValue());
            if (task.getValue().isEmpty()) {
                LoadingUtil.setEmpty(historyTable, "📋", "No stock changes recorded yet", "");
            }
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();

        VBox tableWrapper = new VBox(historyTable);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox content = new VBox(10, tableWrapper);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    private void showAddDialog(TableView<Consumable> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Add Consumable");
        dialog.setHeaderText("Enter consumable details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField       = new TextField();
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Toner", "Ink", "Cable", "Paper",
                "Battery", "Cleaning Kit", "Other");
        categoryBox.setValue("Toner");
        TextField compatibleField = new TextField();
        NumberField qtyField        = new NumberField();
        NumberField reorderField    = new NumberField();
        ComboBox<String> unitBox  = new ComboBox<>();
        unitBox.getItems().addAll("pieces", "boxes", "reams", "meters");
        unitBox.setValue("pieces");
        NumberField costField       = new NumberField();
        TextField locationField   = new TextField();
        TextField notesField      = new TextField();
        Label errorLabel          = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(140);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

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
                        "\"name\":\"" + escapeJson(nameField.getText()) + "\"," +
                        "\"category\":\"" + categoryBox.getValue() + "\"," +
                        "\"compatibleModels\":\"" + escapeJson(compatibleField.getText()) + "\"," +
                        "\"quantityInStock\":" + qty + "," +
                        "\"reorderLevel\":" + reorder + "," +
                        "\"unit\":\"" + unitBox.getValue() + "\"," +
                        "\"vendorId\":0," +
                        "\"unitCost\":" + cost + "," +
                        "\"storageLocation\":\"" + escapeJson(locationField.getText()) + "\"," +
                        "\"notes\":\"" + escapeJson(notesField.getText()) + "\"" +
                        "}";
                HttpResponse<String> response = ApiClient.post(ConfigManager.getBaseUrl() + "/api/consumables", body);
                if (response.statusCode() == 201) {
                    ToastUtil.success("Consumable added.");
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
        if (!ValidationUtil.isNotBlank(name)) return "Consumable Name is required.";
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
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Update Stock Quantity");
        dialog.setHeaderText(c.getName() + " (Current: " + c.getQuantityInStock() + " " + c.getUnit() + ")");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Restock", "Damaged/Written-off", "Correction");
        typeBox.setValue("Restock");

        Label qtyLabel = new Label("Quantity to Add:");
        TextField qtyField = new TextField();
        qtyField.setPromptText("e.g. 5");

        Label previewLabel = new Label("New total: " + c.getQuantityInStock() + " " + c.getUnit());
        previewLabel.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 12px; -fx-font-weight: bold;");

        TextField notesField = new TextField();
        notesField.setPromptText("e.g. New stock arrived from vendor invoice #123");
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        Runnable updatePreview = () -> {
            try {
                int entered = Integer.parseInt(qtyField.getText().trim());
                int newQty = switch (typeBox.getValue()) {
                    case "Restock" -> c.getQuantityInStock() + entered;
                    case "Damaged/Written-off" -> Math.max(0, c.getQuantityInStock() - entered);
                    default -> entered;
                };
                previewLabel.setText("New total: " + newQty + " " + c.getUnit());
                previewLabel.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 12px; -fx-font-weight: bold;");
            } catch (NumberFormatException ex) {
                previewLabel.setText("New total: —");
            }
        };

        typeBox.setOnAction(e -> {
            qtyLabel.setText(switch (typeBox.getValue()) {
                case "Restock" -> "Quantity to Add:";
                case "Damaged/Written-off" -> "Quantity to Remove:";
                default -> "Correct Quantity To:";
            });
            qtyField.clear();
            updatePreview.run();
        });
        qtyField.textProperty().addListener((o, ov, nv) -> updatePreview.run());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(140);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

        grid.add(new Label("Reason:"), 0, 0);   grid.add(typeBox, 1, 0);
        grid.add(qtyLabel, 0, 1);               grid.add(qtyField, 1, 1);
        grid.add(new Label(""), 0, 2);          grid.add(previewLabel, 1, 2);
        grid.add(new Label("Notes:"), 0, 3);    grid.add(notesField, 1, 3);
        grid.add(errorLabel, 1, 4);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                int entered = Integer.parseInt(qtyField.getText().trim());
                if (entered < 0) {
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
                int entered = Integer.parseInt(qtyField.getText().trim());
                int finalQty = switch (typeBox.getValue()) {
                    case "Restock" -> c.getQuantityInStock() + entered;
                    case "Damaged/Written-off" -> Math.max(0, c.getQuantityInStock() - entered);
                    default -> entered;
                };
                String url = ConfigManager.getBaseUrl() + "/api/consumables/" + c.getId()
                        + "/quantity?quantity=" + finalQty
                        + "&changeType=" + java.net.URLEncoder.encode(typeBox.getValue(), java.nio.charset.StandardCharsets.UTF_8)
                        + "&notes=" + java.net.URLEncoder.encode(notesField.getText(), java.nio.charset.StandardCharsets.UTF_8);
                HttpResponse<String> response = ApiClient.putNoBody(url);
                if (response.statusCode() == 200) {
                    ToastUtil.success("Quantity updated to " + finalQty + ".");
                    loadConsumables(table);
                } else {
                    showAlert("Error", "Server returned: " + response.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private void showIssueDialog(Consumable c, TableView<Consumable> table) {
        Task<Object[]> loadTask = new Task<>() {
            @Override
            protected Object[] call() throws Exception {
                List<PickerOption> assets = fetchOptions("/api/assets", true);
                List<PickerOption> employees = fetchOptions("/api/employees", false);
                return new Object[]{assets, employees};
            }
        };

        loadTask.setOnSucceeded(e -> {
            Object[] data = loadTask.getValue();
            @SuppressWarnings("unchecked")
            List<PickerOption> assets = (List<PickerOption>) data[0];
            @SuppressWarnings("unchecked")
            List<PickerOption> employees = (List<PickerOption>) data[1];

            openIssueDialog(c, table, assets, employees);
        });

        loadTask.setOnFailed(e -> showAlert("Error", "Could not load data for issue dialog."));

        Thread t = new Thread(loadTask);
        t.setDaemon(true);
        t.start();
    }

    private void openIssueDialog(Consumable c, TableView<Consumable> table,
                                 List<PickerOption> assets, List<PickerOption> employees) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Issue Consumable");
        dialog.setHeaderText(c.getName() + " (In Stock: " + c.getQuantityInStock() + " " + c.getUnit() + ")");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        NumberField qtyField = new NumberField();
        qtyField.setText("1");
        qtyField.setPromptText("Quantity to issue");

        SearchablePickerField assetPicker = new SearchablePickerField(assets, "Search Asset...");
        SearchablePickerField empPicker = new SearchablePickerField(employees, "Search Employee...");

        assetPicker.setOnSelect(opt -> {
            if (opt.extraId > 0) {
                empPicker.preselectSilently(opt.extraId);
            }
        });

        empPicker.setOnSelect(opt -> {
            assets.stream()
                    .filter(a -> a.extraId == opt.id)
                    .findFirst()
                    .ifPresent(a -> assetPicker.preselectSilently(a.id));
        });

        TextField notesField = new TextField();
        notesField.setPromptText("e.g. Installed in HP Printer");

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(100);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

        int r = 0;
        grid.add(new Label("Quantity *:"),  0, r); grid.add(qtyField,    1, r++);
        grid.add(new Label("Asset:"),       0, r); grid.add(assetPicker, 1, r++);
        grid.add(new Label("Employee:"),    0, r); grid.add(empPicker,   1, r++);
        grid.add(new Label("Notes:"),       0, r); grid.add(notesField,  1, r++);
        grid.add(errorLabel,                1, r);
        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            int qty = qtyField.getIntValue();
            if (qty <= 0) {
                errorLabel.setText("Quantity must be greater than 0.");
                event.consume();
                return;
            }
            if (qty > c.getQuantityInStock()) {
                errorLabel.setText("Not enough stock. Available: " + c.getQuantityInStock());
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int assetId = assetPicker.getSelectedId();
                int empId = empPicker.getSelectedId();

                String body = "{" +
                        "\"quantityUsed\":" + qtyField.getIntValue() + "," +
                        "\"assetId\":" + assetId + "," +
                        "\"employeeId\":" + empId + "," +
                        "\"usedBy\":" + SessionManager.get().getUserId() + "," +
                        "\"notes\":\"" + escapeJson(notesField.getText()) + "\"" +
                        "}";

                HttpResponse<String> resp = ApiClient.post(
                        ConfigManager.getBaseUrl() + "/api/consumables/" + c.getId() + "/usage", body);

                if (resp.statusCode() == 201) {
                    ToastUtil.success(qtyField.getIntValue() + " " + c.getUnit() + " issued. Stock auto-updated.");
                    loadConsumables(table);
                } else if (resp.statusCode() == 400) {
                    showAlert("Error", "Insufficient stock on server.");
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        }
    }

    private List<PickerOption> fetchOptions(String path, boolean isAsset) throws Exception {
        List<PickerOption> result = new ArrayList<>();
        HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + path);
        String body = resp.body().trim();
        if (body.length() < 2) return result;

        body = body.substring(1, body.length() - 1).trim();
        if (body.isEmpty()) return result;

        for (String obj : body.split("\\},\\s*\\{")) {
            String cleaned = obj.replace("{", "").replace("}", "");
            int id = extractInt(cleaned, "id");
            String name = extractValue(cleaned, "name");
            int extra = isAsset ? extractInt(cleaned, "assignedTo") : 0;
            result.add(new PickerOption(id, name, extra));
        }
        return result;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        ThemeManager.applyToDialog(alert);
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
        int start = json.indexOf(search);
        if (start == -1) return 0;
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.length();
        try {
            return Integer.parseInt(json.substring(start, end).trim().replace("}", ""));
        } catch (NumberFormatException e) { return 0; }
    }
}