package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class LicenseView {
    private TableView<License> table;
    private List<PickerOption> vendorOptions = new ArrayList<>();
    private List<PickerOption> employeeOptions = new ArrayList<>();

    public VBox getView() {
        Label bcRoot = new Label("SOFTWARE");
        bcRoot.getStyleClass().add("breadcrumb-root");
        Label bcSep = new Label("  /  ");
        bcSep.getStyleClass().add("breadcrumb-sep");
        Label bcCurrent = new Label("LICENSES");
        bcCurrent.getStyleClass().add("breadcrumb-current");
        HBox breadcrumb = new HBox(bcRoot, bcSep, bcCurrent);

        Label title = new Label("Licenses");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Software licenses, seat usage, and assignments.");
        subtitle.getStyleClass().add("page-subtitle");

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        loadVendorOptions(opts -> vendorOptions = opts);

        table.setRowFactory(tv -> {
            TableRow<License> row = new TableRow<>();

            ContextMenu rowMenu = new ContextMenu();
            if (PermissionManager.canAddLicense()) {
                MenuItem editItem = new MenuItem("✏ Edit License");
                editItem.setOnAction(e -> ensureVendorOptions(() -> showFullEditDialog(row.getItem(), table)));
                rowMenu.getItems().add(editItem);

                MenuItem manageItem = new MenuItem("👥 Manage Users");
                manageItem.setOnAction(e -> ensureEmployeeOptions(() -> showManageUsersDialog(row.getItem(), table)));
                rowMenu.getItems().add(manageItem);
            }
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu)
            );

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    if (PermissionManager.canAddLicense()) {
                        ensureVendorOptions(() -> showFullEditDialog(row.getItem(), table));
                    } else {
                        showAlert("Access Denied", "You don't have permission to edit licenses.");
                    }
                }
            });
            return row;
        });

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
                new SimpleStringProperty(DateTimeFormatUtil.toIndianDateOnly(data.getValue().getExpiryDate())));

        TableColumn<License, Integer> seatsTotalCol = new TableColumn<>("Total Seats");
        seatsTotalCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getSeatsTotal()).asObject());

        TableColumn<License, Integer> seatsUsedCol = new TableColumn<>("Used Seats");
        seatsUsedCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getSeatsUsed()).asObject());

        // ── Seat usage status — colored text ──────────────────
        TableColumn<License, String> usageCol = new TableColumn<>("Usage");
        usageCol.setCellValueFactory(data -> {
            License l = data.getValue();
            int pct = l.getSeatsTotal() == 0 ? 0
                    : (l.getSeatsUsed() * 100 / l.getSeatsTotal());
            String label = pct >= 100 ? "Full"
                    : pct >= 80 ? "High"
                    : pct >= 50 ? "Medium"
                    : "Low";
            return new SimpleStringProperty(label + " (" + pct + "%)");
        });
        usageCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item,
                                      boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null); return;
                }
                License l = getTableView()
                        .getItems().get(getIndex());
                if (l.getSeatsTotal() == 0) {
                    setText(item); setGraphic(null); return;
                }
                double pct = (double) l.getSeatsUsed()
                        / l.getSeatsTotal() * 100;
                String color = pct >= 100 ? "#f85149"
                        : pct >= 80 ? "#d29922" : "#3fb950";
                StackPane ring = UIComponents.progressRing(
                        pct, color);
                setText(null);
                setGraphic(ring);
            }
        });


        table.getColumns().addAll(idCol, softwareNameCol, licenseTypeCol,
                vendorCol, expiryCol, seatsTotalCol, seatsUsedCol,usageCol);

        Button addBtn = new Button("+ Add License");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> ensureVendorOptions(() -> showAddDialog(table, vendorOptions)));
        AnimationUtil.addHoverScale(addBtn);
        Button exportBtn = new Button("⬇ Export");
        exportBtn.getStyleClass().add("btn-export");
        AnimationUtil.addHoverScale(exportBtn);
        exportBtn.setOnAction(e -> {
            List<String> headers = List.of(
                    "Software", "Type", "Vendor",
                    "Expiry", "Total Seats", "Used Seats");
            List<List<String>> rows = new ArrayList<>();
            for (License l : table.getItems()) {
                rows.add(List.of(
                        l.getSoftwareName(),
                        l.getLicenseType(),
                        l.getVendor(),
                        l.getExpiryDate(),
                        String.valueOf(l.getSeatsTotal()),
                        String.valueOf(l.getSeatsUsed())
                ));
            }
            ExcelExporter.export("Licenses", headers, rows);
        });
        Label rightClickHint = new Label("Right-click a row or empty space for actions.");
        rightClickHint.getStyleClass().add("text-muted");
        rightClickHint.setStyle("-fx-font-size: 11px;");
        HBox topBar = new HBox(10, rightClickHint);

        loadLicenses(table);

        VBox tableWrapper = new VBox(table);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);

        VBox root = new VBox(12, breadcrumb, new VBox(4, title, subtitle), topBar, tableWrapper);

        ContextMenu screenMenu = new ContextMenu();
        screenMenu.setAutoHide(true);
        if (PermissionManager.canAddLicense()) {
            MenuItem newItem = new MenuItem("+ New License");
            newItem.setOnAction(e -> ensureVendorOptions(() -> showAddDialog(table, vendorOptions)));
            screenMenu.getItems().add(newItem);
        }
        MenuItem exportItem = new MenuItem("⬇ Export Licenses");
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

    private void showFullEditDialog(License license,
                                    TableView<License> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("License Details");
        dialog.setHeaderText(license.getSoftwareName());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField softwareNameField = new TextField(
                license.getSoftwareName());

        ComboBox<String> licenseTypeBox = new ComboBox<>();
        licenseTypeBox.getItems().addAll(
                "Perpetual", "Subscription", "OEM", "Trial");
        licenseTypeBox.setValue(license.getLicenseType());

        TextField licenseKeyField = new TextField();
        SearchablePickerField vendorPicker = new SearchablePickerField(vendorOptions, "Search vendor...");
        vendorPicker.setTextSilently(license.getVendor());
        TextField purchaseDateField = new TextField();
        TextField expiryDateField = new TextField(DatePickerUtil.fromIso(license.getExpiryDate()));
        NumberField seatsTotalField = new NumberField();
        seatsTotalField.setText(
                String.valueOf(license.getSeatsTotal()));
        NumberField costField = new NumberField(true);
        TextField notesField = new TextField();

        // Load full details
        try {
            HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/licenses");
            // Parse to find this license
            String body = resp.body();
            // Find by id — simplified
            licenseKeyField.setText(
                    extractValueFromId(body,
                            license.getId(), "licenseKey"));
            purchaseDateField.setText(
                    DatePickerUtil.fromIso(extractValueFromId(body,
                            license.getId(), "purchaseDate")));
            costField.setText(
                    extractValueFromId(body,
                            license.getId(), "cost"));
            notesField.setText(
                    extractValueFromId(body,
                            license.getId(), "notes"));
        } catch (Exception ignored) {}

        Label errorLabel = new Label("");
        errorLabel.setStyle(
                "-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(130);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);
        int r = 0;
        grid.add(new Label("Software Name *:"), 0, r);
        grid.add(softwareNameField,             1, r++);
        grid.add(new Label("License Type:"),    0, r);
        grid.add(licenseTypeBox,                1, r++);
        grid.add(new Label("License Key:"),     0, r);
        grid.add(licenseKeyField,               1, r++);
        grid.add(new Label("Total Seats *:"),   0, r);
        grid.add(seatsTotalField,               1, r++);
        grid.add(new Label("Vendor:"),          0, r);
        grid.add(vendorPicker,                   1, r++);
        grid.add(new Label("Purchase Date:"),   0, r);
        grid.add(DatePickerUtil.dateField(
                purchaseDateField),             1, r++);
        grid.add(new Label("Expiry Date:"),     0, r);
        grid.add(DatePickerUtil.dateField(
                expiryDateField),               1, r++);
        grid.add(new Label("Cost (₹):"),         0, r);
        grid.add(costField,                     1, r++);
        grid.add(new Label("Notes:"),           0, r);
        grid.add(notesField,                    1, r++);
        grid.add(errorLabel,                    1, r);
        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okBtn.addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    if (softwareNameField.getText().trim().isEmpty()) {
                        errorLabel.setText(
                                "Software name is required.");
                        event.consume();
                    } else if (seatsTotalField.getIntValue() <= 0) {
                        errorLabel.setText(
                                "Total seats must be greater than 0.");
                        event.consume();
                    }
                });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()
                && result.get() == ButtonType.OK) {
            try {
                String body = "{" +
                        "\"id\":" + license.getId() + "," +
                        "\"softwareName\":\"" + escape(
                        softwareNameField.getText()) + "\"," +
                        "\"licenseType\":\"" +
                        licenseTypeBox.getValue() + "\"," +
                        "\"licenseKey\":\"" + escape(
                        licenseKeyField.getText()) + "\"," +
                        "\"seatsTotal\":" +
                        seatsTotalField.getIntValue() + "," +
                        "\"seatsUsed\":" +
                        license.getSeatsUsed() + "," +
                        "\"vendor\":\"" + escape(
                        vendorPicker.getText()) + "\"," +
                        "\"purchaseDate\":\"" +
                        DatePickerUtil.toIso(purchaseDateField.getText()) + "\"," +
                        "\"expiryDate\":\"" +
                        DatePickerUtil.toIso(expiryDateField.getText()) + "\"," +
                        "\"cost\":" +
                        costField.getDoubleValue() + "," +
                        "\"notes\":\"" + escape(
                        notesField.getText()) + "\"" +
                        "}";
                HttpResponse<String> resp = ApiClient.put(
                        ConfigManager.getBaseUrl() + "/api/licenses/" + license.getId(), body);
                if (resp.statusCode() == 200) {
                    ToastUtil.success("License updated.");
                    loadLicenses(table);
                } else {
                    showAlert("Error", "Server returned: "
                            + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        }
    }

    private String extractValueFromId(String json,
                                      int id, String key) {
        // Find the object with matching id
        for (String obj : json.split("\\},\\{")) {
            obj = obj.replace("[", "").replace("]", "")
                    .replace("{", "").replace("}", "");
            String idStr = extractIntStr(obj, "id");
            if (String.valueOf(id).equals(idStr.trim())) {
                return extractValue(obj, key);
            }
        }
        return "";
    }

    private String extractIntStr(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.length();
        return json.substring(start, end)
                .trim().replace("}", "");
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "'")
                .replace("\n", " ")
                .replace("\r", "");
    }

    private void loadLicenses(TableView<License> table) {
        // 1. Pre-Task UI Setup
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading licenses...");

        // 2. Background Task
        Task<List<License>> task = new Task<>() {
            @Override
            protected List<License> call() throws Exception {
                List<License> result = new ArrayList<>();

                String url = ConfigManager.getBaseUrl() + "/api/licenses";
                HttpResponse<String> response = ApiClient.get(url);
                String body = response.body().trim();

                // Handle empty arrays securely
                if (body.equals("[]") || body.isEmpty()) {
                    return result;
                }

                // Strip leading '[' and trailing ']'
                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    // Split JSON objects correctly and safely
                    String[] jsonObjects = body.split("\\},\\s*\\{");
                    for (String obj : jsonObjects) {
                        // Clean curly braces safely
                        String cleanedObj = obj.replace("{", "").replace("}", "");

                        License l = new License(
                                extractInt(cleanedObj, "id"),
                                extractValue(cleanedObj, "softwareName"),
                                extractValue(cleanedObj, "licenseType"),
                                extractValue(cleanedObj, "vendor"),
                                extractValue(cleanedObj, "expiryDate"),
                                extractInt(cleanedObj, "seatsTotal"),
                                extractInt(cleanedObj, "seatsUsed")
                        );
                        result.add(l);
                    }
                }
                return result;
            }
        };

        // 3. Success Callback (Runs on JavaFX Application Thread)
        task.setOnSucceeded(e -> {
            List<License> licenses = task.getValue();

            // Update UI collections
            table.getItems().addAll(licenses);

            // Handle empty state
            if (licenses.isEmpty()) {
                LoadingUtil.setEmpty(table, "🔑", "No licenses found", "Add your first software license.");
            }
        });

        // 4. Failure Callback (Runs on JavaFX Application Thread)
        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠", "Could not load licenses", "Check server connection and try again.");
            task.getException().printStackTrace(); // Helps with debugging
        });

        // 5. Daemon Thread Execution
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showAddDialog(TableView<License> table,List<PickerOption> vendors) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Add License");
        dialog.setHeaderText("Enter license details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField softwareNameField  = new TextField();
        ComboBox<String> licenseTypeBox = new ComboBox<>();
        licenseTypeBox.getItems().addAll("Perpetual", "Subscription", "OEM", "Trial");
        licenseTypeBox.setValue("Subscription");
        TextField licenseKeyField    = new TextField();
        NumberField seatsTotalField    = new NumberField();
        SearchablePickerField vendorPicker = new SearchablePickerField(vendors, "Search vendor...");
        TextField purchaseDateField  = new TextField();
        TextField expiryDateField    = new TextField();
        NumberField costField          = new NumberField();
        TextField notesField         = new TextField();
        Label errorLabel             = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(130);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

        grid.add(new Label("Software Name *:"), 0, 0);
        grid.add(softwareNameField, 1, 0);
        grid.add(new Label("License Type:"),    0, 1);
        grid.add(licenseTypeBox,    1, 1);
        grid.add(new Label("License Key:"),     0, 2);
        grid.add(licenseKeyField,   1, 2);
        grid.add(new Label("Total Seats *:"),   0, 3);
        grid.add(seatsTotalField,   1, 3);
        grid.add(new Label("Vendor:"),          0, 4);
        grid.add(vendorPicker,       1, 4);
        grid.add(new Label("Purchase Date:"), 0, 5);
        grid.add(DatePickerUtil.dateField(purchaseDateField), 1, 5);
        grid.add(new Label("Expiry Date:"),   0, 6);
        grid.add(DatePickerUtil.dateField(expiryDateField),   1, 6);
        grid.add(new Label("Cost:"),            0, 7);
        grid.add(costField,         1, 7);
        grid.add(new Label("Notes:"),           0, 8);
        grid.add(notesField,        1, 8);
        grid.add(errorLabel,                    1, 9);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable checkFields = () -> {
            boolean valid = !softwareNameField.getText().trim().isEmpty()
                    && !seatsTotalField.getText().trim().isEmpty();
            okButton.setDisable(!valid);
        };

        softwareNameField.textProperty().addListener((o, ov, nv) -> checkFields.run());
        seatsTotalField.textProperty().addListener((o, ov, nv) -> checkFields.run());

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validateLicense(
                    softwareNameField.getText(),
                    seatsTotalField.getText(),
                    expiryDateField.getText(),
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
                int seats = Integer.parseInt(seatsTotalField.getText().trim());
                double cost = costField.getText().trim().isEmpty() ? 0.0
                        : Double.parseDouble(costField.getText().trim());
                String body = "{" +
                        "\"softwareName\":\"" + escape(softwareNameField.getText()) + "\"," +
                        "\"licenseType\":\"" + licenseTypeBox.getValue() + "\"," +
                        "\"licenseKey\":\"" + escape(licenseKeyField.getText()) + "\"," +
                        "\"seatsTotal\":" + seats + "," +
                        "\"seatsUsed\":0," +
                        "\"vendor\":\"" + escape(vendorPicker.getSelectedName()) + "\"," +
                        "\"purchaseDate\":\"" + DatePickerUtil.toIso(purchaseDateField.getText()) + "\"," +
                        "\"expiryDate\":\"" + DatePickerUtil.toIso(expiryDateField.getText()) + "\"," +
                        "\"cost\":" + cost + "," +
                        "\"notes\":\"" + escape(notesField.getText()) + "\"" +
                        "}";
                HttpResponse<String> response = ApiClient.post(ConfigManager.getBaseUrl() + "/api/licenses", body);
                if (response.statusCode() == 201) {
                    ToastUtil.success("License added.");
                    loadLicenses(table);
                } else {
                    showAlert("Error", "Server returned: " + response.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private String validateLicense(String softwareName, String seatsTotal,
                                   String expiryDate, String cost) {
        if (!ValidationUtil.isNotBlank(softwareName)) return "Software Name is required.";
        if (!ValidationUtil.isNotBlank(seatsTotal))   return "Total Seats is required.";
        try {
            if (Integer.parseInt(seatsTotal.trim()) <= 0)
                return "Total Seats must be greater than 0.";
        } catch (NumberFormatException e) {
            return "Total Seats must be a number.";
        }
        if (!expiryDate.trim().isEmpty() && DatePickerUtil.toIso(expiryDate).isEmpty())
            return "Expiry Date must be a valid date (dd-MM-yyyy).";
        if (!cost.trim().isEmpty()) {
            try { Double.parseDouble(cost.trim()); }
            catch (NumberFormatException e) { return "Cost must be a valid number."; }
        }
        return null;
    }


    private void loadEmployeeOptions(Consumer<List<PickerOption>> callback) {
        Task<List<PickerOption>> task = new Task<>() {
            @Override
            protected List<PickerOption> call() throws Exception {
                List<PickerOption> result = new ArrayList<>();
                HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/employees");
                String body = resp.body().trim();
                if (body.length() < 2) return result;
                body = body.substring(1, body.length() - 1).trim();
                if (body.isEmpty()) return result;
                for (String obj : body.split("\\},\\s*\\{")) {
                    String cleaned = obj.replace("{", "").replace("}", "");
                    result.add(new PickerOption(extractInt(cleaned, "id"), extractValue(cleaned, "name")));
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

    private void ensureEmployeeOptions(Runnable next) {
        if (!employeeOptions.isEmpty()) { next.run(); return; }
        loadEmployeeOptions(opts -> { employeeOptions = opts; next.run(); });
    }

    private void showManageUsersDialog(License license, TableView<License> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Manage Users — " + license.getSoftwareName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(480);

        Label seatsLabel = new Label();
        TableView<String[]> assignTable = new TableView<>();
        assignTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        assignTable.setPrefHeight(200);

        TableColumn<String[], String> nameCol = new TableColumn<>("Employee");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        TableColumn<String[], String> dateCol = new TableColumn<>("Assigned");
        dateCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        TableColumn<String[], Void> actionCol2 = new TableColumn<>("");
        actionCol2.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("Unassign");
            {
                removeBtn.getStyleClass().add("btn-link-red");
                removeBtn.setOnAction(e -> {
                    String[] row = getTableView().getItems().get(getIndex());
                    try {
                        ApiClient.delete(ConfigManager.getBaseUrl() + "/api/licenses/assignments/" + row[2] + "?licenseId=" + license.getId());
                        loadAssignments(license, assignTable, seatsLabel, table);
                    } catch (Exception ignored) {}
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });
        assignTable.getColumns().addAll(nameCol, dateCol, actionCol2);

        SearchablePickerField empPicker = new SearchablePickerField(employeeOptions, "Search employee to assign...");
        Button assignBtn = new Button("+ Assign");
        assignBtn.getStyleClass().add("btn-primary");
        assignBtn.setStyle("-fx-padding: 6 12 6 12;");
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        assignBtn.setOnAction(e -> {
            int empId = empPicker.getSelectedId();
            if (empId == 0) { errorLabel.setText("Select an employee first."); return; }
            try {
                String body = "{\"employeeId\":" + empId + ",\"notes\":\"\"}";
                HttpResponse<String> resp = ApiClient.post(ConfigManager.getBaseUrl() + "/api/licenses/" + license.getId() + "/assignments", body);
                if (resp.statusCode() == 201) {
                    errorLabel.setText("");
                    loadAssignments(license, assignTable, seatsLabel, table);
                } else {
                    errorLabel.setText(resp.body());
                }
            } catch (Exception ex) {
                errorLabel.setText(ex.getMessage());
            }
        });

        HBox assignRow = new HBox(8, empPicker, assignBtn);
        VBox content = new VBox(10, seatsLabel, assignTable, new Label("Assign to a new employee:"), assignRow, errorLabel);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        loadAssignments(license, assignTable, seatsLabel, table);
        dialog.showAndWait();
    }

    private void loadAssignments(License license, TableView<String[]> assignTable, Label seatsLabel, TableView<License> mainTable) {
        Task<List<String[]>> task = new Task<>() {
            @Override
            protected List<String[]> call() throws Exception {
                List<String[]> result = new ArrayList<>();
                HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/licenses/" + license.getId() + "/assignments");
                String body = resp.body().trim();
                if (body.length() < 2) return result;
                body = body.substring(1, body.length() - 1).trim();
                if (body.isEmpty()) return result;
                for (String obj : body.split("\\},\\{")) {
                    String cleaned = obj.replace("{", "").replace("}", "");
                    result.add(new String[]{
                            extractValue(cleaned, "employee_name"),
                            extractValue(cleaned, "assigned_date"),
                            String.valueOf(extractInt(cleaned, "assignment_id"))
                    });
                }
                return result;
            }
        };
        task.setOnSucceeded(e -> {
            assignTable.getItems().setAll(task.getValue());
            seatsLabel.setText("Seats: " + task.getValue().size() + " / " + license.getSeatsTotal() + " used");
            loadLicenses(mainTable); // refresh main table's seatsUsed display too
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadVendorOptions(Consumer<List<PickerOption>> callback) {
        Task<List<PickerOption>> task = new Task<>() {
            @Override
            protected List<PickerOption> call() throws Exception {
                List<PickerOption> result = new ArrayList<>();
                HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/vendors");
                String body = resp.body().trim();
                if (body.length() < 2) return result;
                body = body.substring(1, body.length() - 1).trim();
                if (body.isEmpty()) return result;
                for (String obj : body.split("\\},\\s*\\{")) {
                    String cleaned = obj.replace("{", "").replace("}", "");
                    result.add(new PickerOption(extractInt(cleaned, "id"), extractValue(cleaned, "name")));
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

    private void ensureVendorOptions(Runnable next) {
        if (!vendorOptions.isEmpty()) {
            next.run();
            return;
        }
        loadVendorOptions(opts -> {
            vendorOptions = opts;
            next.run();
        });
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
}