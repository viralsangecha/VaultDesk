package com.vaultdesk.admin;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.http.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class MaintenanceView {

    private TableView<Maintenance> table;
    private List<PickerOption> assetOptions = new ArrayList<>();
    private final Map<Integer, String> assetNameMap = new HashMap<>();

    public VBox getView() {
        loadAssetOptions(opts -> {
            assetOptions = opts;
            for (PickerOption o : opts) assetNameMap.put(o.id, o.name);
        });

        Label bcRoot = new Label("OPERATIONS");
        bcRoot.getStyleClass().add("breadcrumb-root");
        Label bcSep = new Label("  /  ");
        bcSep.getStyleClass().add("breadcrumb-sep");
        Label bcCurrent = new Label("MAINTENANCE");
        bcCurrent.getStyleClass().add("breadcrumb-current");
        HBox breadcrumb = new HBox(bcRoot, bcSep, bcCurrent);

        Label title = new Label("Maintenance");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Repair, upgrade, and preventive maintenance history.");
        subtitle.getStyleClass().add("page-subtitle");

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> {
            TableRow<Maintenance> row = new TableRow<>();

            ContextMenu rowMenu = new ContextMenu();
            if (PermissionManager.canAddMaintenance()) {
                MenuItem editItem = new MenuItem("✏ Edit Log");
                editItem.setOnAction(e -> ensureAssetOptions(() -> showFullEditDialog(row.getItem(), table)));
                rowMenu.getItems().add(editItem);
            }
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu)
            );

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    if (PermissionManager.canAddMaintenance()) {
                        ensureAssetOptions(() -> showFullEditDialog(row.getItem(), table));
                    } else {
                        showAlert("Access Denied", "You don't have permission to edit maintenance logs.");
                    }
                }
            });
            return row;
        });

        TableColumn<Maintenance, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Maintenance, String> assetCol = new TableColumn<>("Asset");
        assetCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        assetNameMap.getOrDefault(data.getValue().getAssetId(),
                                "Asset #" + data.getValue().getAssetId())));

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
                new SimpleStringProperty(DateTimeFormatUtil.toIndianDateOnly(data.getValue().getMaintenanceDate())));

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

        table.getColumns().addAll(idCol, assetCol, typeCol,
                descCol, costCol, dateCol, statusCol);

        Button addBtn = new Button("+ Add Maintenance Log");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> ensureAssetOptions(() -> showAddDialog(table)));
        addBtn.setVisible(PermissionManager.canAddMaintenance());
        addBtn.setManaged(PermissionManager.canAddMaintenance());
        AnimationUtil.addHoverScale(addBtn);

        Button exportBtn = new Button("⬇ Export");
        exportBtn.getStyleClass().add("btn-export");
        AnimationUtil.addHoverScale(exportBtn);
        exportBtn.setOnAction(e -> {
            List<String> headers = List.of(
                    "Asset", "Type", "Description",
                    "Cost", "Date", "Status");
            List<List<String>> rows = new ArrayList<>();
            for (Maintenance m : table.getItems()) {
                rows.add(List.of(
                        assetNameMap.getOrDefault(m.getAssetId(), "Asset #" + m.getAssetId()),
                        m.getMaintenanceType(),
                        m.getDescription(),
                        String.valueOf(m.getCost()),
                        DateTimeFormatUtil.toIndianDateOnly(m.getMaintenanceDate()),
                        m.getStatus()
                ));
            }
            ExcelExporter.export("Maintenance", headers, rows);
        });

        Label rightClickHint = new Label("Right-click a row or empty space for actions.");
        rightClickHint.getStyleClass().add("text-muted");
        rightClickHint.setStyle("-fx-font-size: 11px;");
        HBox topBar = new HBox(10, rightClickHint);

        loadMaintenance(table);

        VBox tableWrapper = new VBox(table);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);

        VBox root = new VBox(12, breadcrumb, new VBox(4, title, subtitle), topBar, tableWrapper);

        ContextMenu screenMenu = new ContextMenu();
        screenMenu.setAutoHide(true);
        if (PermissionManager.canAddMaintenance()) {
            MenuItem newItem = new MenuItem("+ New Maintenance Log");
            newItem.setOnAction(e -> ensureAssetOptions(() -> showAddDialog(table)));
            screenMenu.getItems().add(newItem);
        }
        MenuItem exportItem = new MenuItem("⬇ Export Maintenance");
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

    private void showFullEditDialog(Maintenance m, TableView<Maintenance> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Maintenance Details");
        dialog.setHeaderText(assetNameMap.getOrDefault(m.getAssetId(), "Asset #" + m.getAssetId()));
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        SearchablePickerField assetPicker = new SearchablePickerField(assetOptions, "Asset...");
        assetPicker.preselectSilently(m.getAssetId());
        assetPicker.setDisable(true);
        assetPicker.setStyle("-fx-opacity: 0.8;");

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Repair", "AMC Visit",
                "Preventive", "Upgrade", "Cleaning", "Other");
        typeBox.setValue(m.getMaintenanceType());

        TextField descField = new TextField(m.getDescription());
        NumberField costField = new NumberField(true);
        costField.setText(String.valueOf(m.getCost()));

        TextField dateField = new TextField(DatePickerUtil.fromIso(m.getMaintenanceDate()));
        TextField nextDueDateField = new TextField();
        TextField notesField = new TextField();

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Completed", "Pending", "In Progress");
        statusBox.setValue(m.getStatus());

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(120);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

        int r = 0;
        grid.add(new Label("Asset:"),         0, r);
        grid.add(assetPicker,                 1, r++);
        grid.add(new Label("Type:"),          0, r);
        grid.add(typeBox,                     1, r++);
        grid.add(new Label("Description:"),   0, r);
        grid.add(descField,                   1, r++);
        grid.add(new Label("Cost (₹):"),      0, r);
        grid.add(costField,                   1, r++);
        grid.add(new Label("Date *:"),        0, r);
        grid.add(DatePickerUtil.dateField(dateField), 1, r++);
        grid.add(new Label("Next Due Date:"), 0, r);
        grid.add(DatePickerUtil.dateField(nextDueDateField), 1, r++);
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
                    if (!ValidationUtil.isNotBlank(dateField.getText())) {
                        errorLabel.setText("Date is required.");
                        event.consume();
                    } else if (DatePickerUtil.toIso(dateField.getText()).isEmpty()) {
                        errorLabel.setText("Date must be a valid date (dd-MM-yyyy).");
                        event.consume();
                    }
                });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
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
                        DatePickerUtil.toIso(dateField.getText()) + "\"," +
                        "\"nextDueDate\":\"" +
                        DatePickerUtil.toIso(nextDueDateField.getText()) + "\"," +
                        "\"status\":\"" +
                        statusBox.getValue() + "\"," +
                        "\"notes\":\"" + escape(
                        notesField.getText()) + "\"," +
                        "\"loggedBy\":" +
                        SessionManager.get().getUserId() +
                        "}";
                HttpResponse<String> resp = ApiClient.put(
                        ConfigManager.getBaseUrl() + "/api/maintenance/" + m.getId(), body);
                if (resp.statusCode() == 200) {
                    ToastUtil.success("Maintenance log updated.");
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

    private void showAddDialog(TableView<Maintenance> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Add Maintenance Log");
        dialog.setHeaderText("Enter maintenance details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        SearchablePickerField assetPicker = new SearchablePickerField(assetOptions, "Search asset...");

        ComboBox<String> typeBox   = new ComboBox<>();
        typeBox.getItems().addAll("Repair", "AMC Visit", "Preventive",
                "Upgrade", "Cleaning", "Other");
        typeBox.setValue("Repair");

        TextField descField        = new TextField();
        NumberField costField      = new NumberField();
        costField.setPromptText("0.0");
        TextField dateField        = new TextField();
        dateField.setPromptText("dd-MM-yyyy");
        TextField nextDueDateField = new TextField();
        nextDueDateField.setPromptText("dd-MM-yyyy");
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Completed", "Pending", "In Progress");
        statusBox.setValue("Completed");
        TextField notesField       = new TextField();
        Label errorLabel           = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(120);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

        grid.add(new Label("Asset *:"),       0, 0); grid.add(assetPicker,     1, 0);
        grid.add(new Label("Type:"),          0, 1); grid.add(typeBox,         1, 1);
        grid.add(new Label("Description:"),   0, 2); grid.add(descField,       1, 2);
        grid.add(new Label("Cost:"),          0, 3); grid.add(costField,       1, 3);
        grid.add(new Label("Date *:"),        0, 4);
        grid.add(DatePickerUtil.dateField(dateField),        1, 4);
        grid.add(new Label("Next Due Date:"), 0, 5);
        grid.add(DatePickerUtil.dateField(nextDueDateField), 1, 5);
        grid.add(new Label("Status:"),        0, 6); grid.add(statusBox,       1, 6);
        grid.add(new Label("Notes:"),         0, 7); grid.add(notesField,      1, 7);
        grid.add(errorLabel,                  1, 8);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable checkFields = () -> {
            boolean valid = assetPicker.getSelectedId() > 0
                    && !dateField.getText().trim().isEmpty();
            okButton.setDisable(!valid);
        };

        assetPicker.setOnSelect(opt -> checkFields.run());
        dateField.textProperty().addListener((o, ov, nv) -> checkFields.run());

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validateMaintenance(
                    assetPicker.getSelectedId(),
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
                int assetId = assetPicker.getSelectedId();
                double cost = costField.getText().trim().isEmpty() ? 0.0
                        : Double.parseDouble(costField.getText().trim());
                String body = "{" +
                        "\"assetId\":" + assetId + "," +
                        "\"maintenanceType\":\"" + typeBox.getValue() + "\"," +
                        "\"description\":\"" + escape(descField.getText()) + "\"," +
                        "\"doneByInternal\":0," +
                        "\"doneByVendor\":0," +
                        "\"cost\":" + cost + "," +
                        "\"maintenanceDate\":\"" + DatePickerUtil.toIso(dateField.getText()) + "\"," +
                        "\"nextDueDate\":\"" + DatePickerUtil.toIso(nextDueDateField.getText()) + "\"," +
                        "\"status\":\"" + statusBox.getValue() + "\"," +
                        "\"notes\":\"" + escape(notesField.getText()) + "\"," +
                        "\"loggedBy\":" + SessionManager.get().getUserId() +
                        "}";
                HttpResponse<String> response = ApiClient.post(
                        ConfigManager.getBaseUrl() + "/api/maintenance", body);
                if (response.statusCode() == 201) {
                    ToastUtil.success("Maintenance log added.");
                    loadMaintenance(table);
                } else {
                    showAlert("Error", "Server returned: " + response.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private String validateMaintenance(int assetId, String date,
                                       String nextDate, String cost) {
        if (assetId <= 0) return "Please select a valid Asset.";
        if (!ValidationUtil.isNotBlank(date)) return "Maintenance Date is required.";
        if (DatePickerUtil.toIso(date).isEmpty())
            return "Date must be a valid date (dd-MM-yyyy).";
        if (!nextDate.trim().isEmpty() && DatePickerUtil.toIso(nextDate).isEmpty())
            return "Next Due Date must be a valid date (dd-MM-yyyy).";
        if (!cost.trim().isEmpty()) {
            try { Double.parseDouble(cost.trim()); }
            catch (NumberFormatException e) { return "Cost must be a valid number."; }
        }
        return null;
    }

    private void loadAssetOptions(Consumer<List<PickerOption>> callback) {
        Task<List<PickerOption>> task = new Task<>() {
            @Override
            protected List<PickerOption> call() throws Exception {
                List<PickerOption> result = new ArrayList<>();
                HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/assets");
                String body = resp.body().trim();

                if (body.length() < 2) return result;
                body = body.substring(1, body.length() - 1).trim();
                if (body.isEmpty()) return result;

                for (String obj : body.split("\\},\\s*\\{")) {
                    String cleaned = obj.replace("{", "").replace("}", "");
                    int id = extractInt(cleaned, "id");
                    String name = extractValue(cleaned, "name");
                    result.add(new PickerOption(id, name));
                }
                return result;
            }
        };
        task.setOnSucceeded(e -> callback.accept(task.getValue()));
        task.setOnFailed(e -> callback.accept(new ArrayList<>()));
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void ensureAssetOptions(Runnable next) {
        if (!assetOptions.isEmpty()) {
            next.run();
            return;
        }
        loadAssetOptions(opts -> {
            assetOptions = opts;
            for (PickerOption o : opts) assetNameMap.put(o.id, o.name);
            next.run();
        });
    }

    private void loadMaintenance(TableView<Maintenance> table) {
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading maintenance logs...");

        Task<List<Maintenance>> task = new Task<>() {
            @Override
            protected List<Maintenance> call() throws Exception {
                List<Maintenance> result = new ArrayList<>();

                String url = SessionManager.get().isDeptHod()
                        ? ConfigManager.getBaseUrl() + "/api/maintenance/department/" + SessionManager.get().getDeptId()
                        : ConfigManager.getBaseUrl() + "/api/maintenance";

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

                        Maintenance m = new Maintenance(
                                extractInt(cleanedObj, "id"),
                                extractInt(cleanedObj, "assetId"),
                                extractValue(cleanedObj, "maintenanceType"),
                                extractValue(cleanedObj, "description"),
                                extractDouble(cleanedObj, "cost"),
                                extractValue(cleanedObj, "maintenanceDate"),
                                extractValue(cleanedObj, "status")
                        );
                        result.add(m);
                    }
                }
                return result;
            }
        };

        task.setOnSucceeded(e -> {
            List<Maintenance> logs = task.getValue();
            table.getItems().addAll(logs);
            if (logs.isEmpty()) {
                LoadingUtil.setEmpty(table, "🔧", "No maintenance logs found", "Add a maintenance log using the button above.");
            }
        });

        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠", "Could not load maintenance logs", "Check server connection and try again.");
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "'")
                .replace("\n", " ")
                .replace("\r", "");
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