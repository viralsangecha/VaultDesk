package com.vaultdesk.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;
import java.util.*;

public class AssetDetailView {

    private final Asset asset;
    private final Runnable onUpdate;
    private VBox root;

    // Category → spec keys
    private static final Map<String,
            List<String>> CATEGORY_SPECS = new LinkedHashMap<>();

    static {
        CATEGORY_SPECS.put("PC", List.of(
                "processor", "ram_gb", "storage_gb",
                "os", "ip_address", "mac_address",
                "gpu", "monitor_size"));
        CATEGORY_SPECS.put("Laptop", List.of(
                "processor", "ram_gb", "storage_gb",
                "os", "ip_address", "mac_address",
                "battery_health", "screen_size"));
        CATEGORY_SPECS.put("Server", List.of(
                "processor", "ram_gb", "storage_gb",
                "os", "ip_address", "mac_address",
                "raid_type", "rack_unit"));
        CATEGORY_SPECS.put("Printer", List.of(
                "print_speed_ppm", "print_type",
                "ip_address", "mac_address",
                "supported_cartridge", "duplex"));
        CATEGORY_SPECS.put("Switch", List.of(
                "ports", "managed", "ip_address",
                "mac_address", "poe", "speed"));
        CATEGORY_SPECS.put("Router", List.of(
                "wan_ports", "lan_ports", "ip_address",
                "mac_address", "wifi", "firmware"));
        CATEGORY_SPECS.put("CCTV", List.of(
                "resolution", "lens_mm", "ir_range_m",
                "type", "ip_address", "mac_address"));
        CATEGORY_SPECS.put("DVR", List.of(
                "channels", "hdd_slots",
                "ip_address", "mac_address", "resolution"));
        CATEGORY_SPECS.put("NVR", List.of(
                "channels", "hdd_slots",
                "ip_address", "mac_address", "resolution"));
        CATEGORY_SPECS.put("UPS", List.of(
                "capacity_va", "battery_type",
                "runtime_minutes", "output_voltage"));
        CATEGORY_SPECS.put("Biometric", List.of(
                "ip_address", "mac_address",
                "type", "capacity"));
        CATEGORY_SPECS.put("Mobile", List.of(
                "imei", "sim_number", "os",
                "storage_gb", "ram_gb"));
    }

    public AssetDetailView(Asset asset, Runnable onUpdate) {
        this.asset    = asset;
        this.onUpdate = onUpdate;
    }

    public VBox getView() {
        root = new VBox(0);
        root.setStyle("-fx-background-color: transparent;");
        buildContent();
        AnimationUtil.fadeIn(root);
        return root;
    }

    private void buildContent() {
        root.getChildren().clear();

        // ── Header ────────────────────────────────────────
        Label tagLabel = new Label(asset.getAssetTag());
        tagLabel.setStyle(
                "-fx-text-fill: #58a6ff;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;");
        tagLabel.getStyleClass().add("data-mono");

        Label nameLabel = new Label(asset.getName());
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;"); // color now inherits from theme-aware base .label

        Label categoryLabel = new Label(asset.getCategory());
        categoryLabel.getStyleClass().add("text-muted");
        categoryLabel.setStyle("-fx-font-size: 13px;");

        Label statusBadge = new Label(asset.getStatus());
        statusBadge.setPadding(new Insets(4, 12, 4, 12));
        statusBadge.setStyle(statusStyle(asset.getStatus()));

        HBox headerTop = new HBox(8, tagLabel, statusBadge);
        headerTop.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = new Button("✏ Edit Asset");
        editBtn.getStyleClass().add("btn-warning");
        editBtn.setStyle("-fx-padding: 6 14 6 14;");
        AnimationUtil.addHoverScale(editBtn);
        editBtn.setOnAction(e -> showEditDialog());
        editBtn.setVisible(PermissionManager.canEditAsset());
        editBtn.setManaged(PermissionManager.canEditAsset());

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        HBox titleRow = new HBox(8,
                new VBox(4, headerTop, nameLabel, categoryLabel),
                hSpacer, editBtn);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setPadding(new Insets(20, 20, 16, 20));
        titleRow.getStyleClass().add("top-bar");

        // ── Scrollable content ────────────────────────────
        VBox content = new VBox(0);

        content.getChildren().addAll(
                buildInfoSection(),
                buildSpecsSection(),
                buildComponentsSection(),
                buildLinkedAssetsSection(),
                buildMaintenanceSection(),
                buildHistorySection(),
                buildTicketsSection()
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("content-scroll");
        scroll.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(titleRow, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
    }

    // ── Basic Info + Assignment + Purchase ────────────────
    private VBox buildInfoSection() {
        String json = fetchAssetJson();

        String assignedName = "Unassigned";
        int assignedTo = extractInt(json, "assignedTo");
        if (assignedTo > 0) {
            assignedName = loadEmployeeName(assignedTo);
        }

        VBox section = new VBox(0);

        GridPane basicGrid = new GridPane();
        basicGrid.setHgap(16); basicGrid.setVgap(8);
        basicGrid.setPadding(new Insets(16, 20, 8, 20));
        addGridRow(basicGrid, 0, "Brand", asset.getBrand());
        addGridRow(basicGrid, 1, "Model", extractValue(json, "model"));
        addGridRow(basicGrid, 2, "Serial No", asset.getSerialNumber());
        addGridRow(basicGrid, 3, "Location", asset.getLocation());

        GridPane assignGrid = new GridPane();
        assignGrid.setHgap(16); assignGrid.setVgap(8);
        assignGrid.setPadding(new Insets(8, 20, 8, 20));
        addGridRow(assignGrid, 0, "Assigned To", assignedName);
        addGridRow(assignGrid, 1, "Assigned Date", DateTimeFormatUtil.toIndianDateOnly(extractValue(json, "assignedDate")));
        addGridRow(assignGrid, 2, "Department", loadDeptName(json));

        GridPane purchaseGrid = new GridPane();
        purchaseGrid.setHgap(16); purchaseGrid.setVgap(8);
        purchaseGrid.setPadding(new Insets(8, 20, 16, 20));
        addGridRow(purchaseGrid, 0, "Vendor", loadVendorName(json));
        addGridRow(purchaseGrid, 1, "Purchase Date", DateTimeFormatUtil.toIndianDateOnly(extractValue(json, "purchaseDate")));
        addGridRow(purchaseGrid, 2, "Purchase Cost",
                "₹" + String.format("%.2f", extractDouble(json, "purchaseCost")));
        addGridRow(purchaseGrid, 3, "Warranty Expiry", DateTimeFormatUtil.toIndianDateOnly(extractValue(json, "warrantyExpiry")));
        addGridRow(purchaseGrid, 4, "Notes", asset.getNotes());

        section.getChildren().addAll(
                sectionHeader("📋 Basic Information"), basicGrid,
                sectionHeader("👤 Assignment"), assignGrid,
                sectionHeader("💰 Purchase Information"), purchaseGrid,
                divider());
        return section;
    }

    // ── Specs Section ─────────────────────────────────────
    private VBox buildSpecsSection() {
        VBox section = new VBox(0);

        Button editSpecsBtn = new Button("✏ Edit Specs");
        editSpecsBtn.getStyleClass().add("btn-link-blue");
        editSpecsBtn.setOnAction(e -> showEditSpecsDialog());
        editSpecsBtn.setVisible(PermissionManager.canEditAsset());
        editSpecsBtn.setManaged(PermissionManager.canEditAsset());

        HBox header = sectionHeaderWithBtn(
                "⚙ Specifications", editSpecsBtn);

        VBox specsContent = new VBox(6);
        specsContent.setPadding(new Insets(8, 20, 16, 20));

        // Load specs from server
        try {
            HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl()
                    + "/api/assets/" + asset.getId() + "/specs");
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);

            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String key   = extractValue(obj, "spec_key");
                    String value = extractValue(obj, "spec_value");
                    if (!key.isEmpty()) {
                        HBox row = specRow(
                                key.replace("_", " ")
                                        .toUpperCase(), value);
                        specsContent.getChildren().add(row);
                    }
                }
            } else {
                Label empty = new Label(
                        "No specs recorded. Click Edit Specs.");
                empty.getStyleClass().add("text-muted");
                empty.setStyle("-fx-font-size: 12px;");
                specsContent.getChildren().add(empty);
            }
        } catch (Exception ex) {
            specsContent.getChildren().add(
                    new Label("Error loading specs."));
        }

        section.getChildren().addAll(
                header, specsContent, divider());
        return section;
    }

    // ── Components Section ────────────────────────────────
    private VBox buildComponentsSection() {
        VBox section = new VBox(0);

        Button addCompBtn = new Button("+ Add Component");
        addCompBtn.getStyleClass().add("btn-link-green");

        HBox header = sectionHeaderWithBtn(
                "🔧 Components", addCompBtn);
        addCompBtn.setVisible(PermissionManager.canEditAsset());
        addCompBtn.setManaged(PermissionManager.canEditAsset());

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(160);

        TableColumn<String[], String> typeCol =
                new TableColumn<>("Type");
        typeCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[0]));

        TableColumn<String[], String> brandCol =
                new TableColumn<>("Brand");
        brandCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[1]));

        TableColumn<String[], String> modelCol =
                new TableColumn<>("Model");
        modelCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[2]));

        TableColumn<String[], String> serialCol =
                new TableColumn<>("Serial");
        serialCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[3]));

        TableColumn<String[], String> statusCol =
                new TableColumn<>("Status");
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[4]));

        TableColumn<String[], Void> actionCol =
                new TableColumn<>("");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button delBtn = new Button("✕");
            {
                delBtn.getStyleClass().add("btn-link-red");
                delBtn.setOnAction(e -> {
                    String[] row = getTableView()
                            .getItems().get(getIndex());
                    deleteComponent(Integer.parseInt(row[5]),
                            table);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || !PermissionManager.canEditAsset() ? null : delBtn);
            }
        });
        actionCol.setMaxWidth(40);

        table.getColumns().addAll(typeCol, brandCol,
                modelCol, serialCol, statusCol, actionCol);

        loadComponents(table);

        addCompBtn.setOnAction(e ->
                showAddComponentDialog(table));

        VBox tableWrapper = new VBox(table);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox tableBox = new VBox(tableWrapper);
        tableBox.setPadding(new Insets(8, 20, 16, 20));

        section.getChildren().addAll(
                header, tableBox, divider());
        return section;
    }

    private VBox buildLinkedAssetsSection() {
        VBox section = new VBox(0);

        Button linkBtn = new Button("+ Link Asset");
        linkBtn.getStyleClass().add("btn-link-green");

        HBox header = sectionHeaderWithBtn("🔗 Linked Assets", linkBtn);


        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(140);

        TableColumn<String[], String> tagCol = new TableColumn<>("Asset Tag");
        tagCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[0]));
        TableColumn<String[], String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[1]));
        TableColumn<String[], String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[2]));
        TableColumn<String[], String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[3]));

        TableColumn<String[], Void> actionCol = new TableColumn<>("");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button unlinkBtn = new Button("Unlink");
            {
                unlinkBtn.getStyleClass().add("btn-link-red");
                unlinkBtn.setOnAction(e -> {
                    String[] row = getTableView().getItems().get(getIndex());
                    unlinkAsset(Integer.parseInt(row[4]), table);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || !PermissionManager.canEditAsset() ? null : unlinkBtn);
            }
        });

        table.getColumns().addAll(tagCol, nameCol, catCol, statusCol, actionCol);
        loadLinkedAssets(table);

        linkBtn.setOnAction(e -> showLinkAssetDialog(table));
        linkBtn.setVisible(PermissionManager.canEditAsset());
        linkBtn.setManaged(PermissionManager.canEditAsset());

        VBox tableWrapper = new VBox(table);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox tableBox = new VBox(tableWrapper);
        tableBox.setPadding(new Insets(8, 20, 16, 20));
        section.getChildren().addAll(header, tableBox, divider());
        return section;
    }

    private void loadLinkedAssets(TableView<String[]> table) {
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading linked assets...");
        Task<List<String[]>> task = new Task<>() {
            @Override
            protected List<String[]> call() throws Exception {
                List<String[]> result = new ArrayList<>();
                HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/assets/" + asset.getId() + "/links");
                String body = resp.body().trim();
                if (body.length() < 2) return result;
                body = body.substring(1, body.length() - 1).trim();
                if (body.isEmpty()) return result;
                for (String obj : body.split("\\},\\{")) {
                    String cleaned = obj.replace("{", "").replace("}", "");
                    result.add(new String[]{
                            extractValue(cleaned, "asset_tag"),
                            extractValue(cleaned, "name"),
                            extractValue(cleaned, "category"),
                            extractValue(cleaned, "status"),
                            String.valueOf(extractInt(cleaned, "link_id"))
                    });
                }
                return result;
            }
        };
        task.setOnSucceeded(e -> {
            table.getItems().addAll(task.getValue());
            if (task.getValue().isEmpty()) {
                LoadingUtil.setEmpty(table, "🔗", "No linked assets", "Click + Link Asset to associate accessories.");
            }
        });
        task.setOnFailed(e -> LoadingUtil.setEmpty(table, "⚠", "Error loading links", ""));
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showLinkAssetDialog(TableView<String[]> table) {
        Task<List<PickerOption>> loadTask = new Task<>() {
            @Override
            protected List<PickerOption> call() throws Exception {
                List<PickerOption> result = new ArrayList<>();
                HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/assets");
                String body = resp.body().trim();
                if (body.length() < 2) return result;
                body = body.substring(1, body.length() - 1).trim();
                if (body.isEmpty()) return result;
                for (String obj : body.split("\\},\\{")) {
                    String cleaned = obj.replace("{", "").replace("}", "");
                    int id = extractInt(cleaned, "id");
                    if (id == asset.getId()) continue; // exclude self
                    result.add(new PickerOption(id, extractValue(cleaned, "assetTag") + " — " + extractValue(cleaned, "name")));
                }
                return result;
            }
        };
        loadTask.setOnSucceeded(e -> openLinkAssetDialog(loadTask.getValue(), table));
        Thread t = new Thread(loadTask);
        t.setDaemon(true);
        t.start();
    }

    private void openLinkAssetDialog(List<PickerOption> options, TableView<String[]> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Link Asset");
        dialog.setHeaderText("Associate another asset with " + asset.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        SearchablePickerField assetPicker = new SearchablePickerField(options, "Search asset...");
        TextField linkTypeField = new TextField();
        linkTypeField.setPromptText("e.g. Accessory, Peripheral (optional)");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Asset *:"), 0, 0); grid.add(assetPicker, 1, 0);
        grid.add(new Label("Link Type:"), 0, 1); grid.add(linkTypeField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int linkedId = assetPicker.getSelectedId();
            if (linkedId == 0) {
                showAlert("Error", "Select an asset to link.");
                return;
            }
            try {
                String body = "{\"linkedAssetId\":" + linkedId + ",\"linkType\":\"" + escape(linkTypeField.getText()) + "\"}";
                HttpResponse<String> resp = ApiClient.post(
                        ConfigManager.getBaseUrl() + "/api/assets/" + asset.getId() + "/links", body);
                if (resp.statusCode() == 201) {
                    loadLinkedAssets(table);
                } else {
                    showAlert("Error", resp.body());
                }
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        }
    }

    private void unlinkAsset(int linkId, TableView<String[]> table) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        ThemeManager.applyToDialog(confirm);
        confirm.setContentText("Remove this asset link?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    ApiClient.delete(ConfigManager.getBaseUrl() + "/api/assets/links/" + linkId);
                    loadLinkedAssets(table);
                } catch (Exception ex) {
                    showAlert("Error", ex.getMessage());
                }
            }
        });
    }

    // ── Maintenance History ───────────────────────────────
    private VBox buildMaintenanceSection() {
        VBox section = new VBox(0);

        Button addBtn = new Button("+ Add Maintenance");
        addBtn.getStyleClass().add("btn-link-green");

        HBox header = sectionHeaderWithBtn(
                "🔨 Maintenance History", addBtn);

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(160);

        TableColumn<String[], String> dateCol =
                new TableColumn<>("Date");
        dateCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[0]));

        TableColumn<String[], String> typeCol =
                new TableColumn<>("Type");
        typeCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[1]));

        TableColumn<String[], String> descCol =
                new TableColumn<>("Description");
        descCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[2]));

        TableColumn<String[], String> costCol =
                new TableColumn<>("Cost");
        costCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[3]));

        TableColumn<String[], String> statusCol =
                new TableColumn<>("Status");
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[4]));

        table.getColumns().addAll(dateCol, typeCol,
                descCol, costCol, statusCol);

        loadMaintenance(table);

        addBtn.setOnAction(e ->
                showAddMaintenanceDialog(table));
        addBtn.setVisible(PermissionManager.canAddMaintenance());
        addBtn.setManaged(PermissionManager.canAddMaintenance());

        VBox tableWrapper = new VBox(table);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox tableBox = new VBox(tableWrapper);
        tableBox.setPadding(new Insets(8, 20, 16, 20));
        section.getChildren().addAll(
                header, tableBox, divider());
        return section;
    }

    // ── Movement History ──────────────────────────────────
    private VBox buildHistorySection() {
        VBox section = new VBox(0);

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(140);

        TableColumn<String[], String> dateCol =
                new TableColumn<>("Date");
        dateCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[0]));

        TableColumn<String[], String> actionCol =
                new TableColumn<>("Action");
        actionCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[1]));

        TableColumn<String[], String> fromCol =
                new TableColumn<>("From");
        fromCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[2]));

        TableColumn<String[], String> toCol =
                new TableColumn<>("To");
        toCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[3]));

        table.getColumns().addAll(
                dateCol, actionCol, fromCol, toCol);

        loadHistory(table);

        VBox tableWrapper = new VBox(table);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox tableBox = new VBox(tableWrapper);
        tableBox.setPadding(new Insets(8, 20, 16, 20));

        section.getChildren().addAll(
                sectionHeader("📜 Movement History"),
                tableBox, divider());
        return section;
    }

    // ── Linked Tickets ────────────────────────────────────
    private VBox buildTicketsSection() {
        VBox section = new VBox(0);

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(140);

        TableColumn<String[], String> noCol =
                new TableColumn<>("Ticket No");
        noCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[0]));

        TableColumn<String[], String> titleCol =
                new TableColumn<>("Title");
        titleCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[1]));

        TableColumn<String[], String> statusCol =
                new TableColumn<>("Status");
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[2]));

        table.getColumns().addAll(noCol, titleCol, statusCol);

        loadTickets(table);

        VBox tableWrapper = new VBox(table);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox tableBox = new VBox(tableWrapper);
        tableBox.setPadding(new Insets(8, 20, 16, 20));
        section.getChildren().addAll(
                sectionHeader("🎫 Linked Tickets"),
                tableBox);
        return section;
    }

    // ── Edit Asset Dialog ─────────────────────────────────
    private void showEditDialog() {
        Task<Object[]> loadTask = new Task<>() {
            @Override
            protected Object[] call() throws Exception {
                String json = fetchAssetJson();
                List<PickerOption> employees = fetchEmployeeOptions();
                List<PickerOption> departments = fetchDepartmentOptions();
                List<PickerOption> vendors = fetchVendorOptions();
                return new Object[]{json, employees, departments, vendors};
            }
        };

        loadTask.setOnSucceeded(e -> {
            Object[] data = loadTask.getValue();
            @SuppressWarnings("unchecked")
            List<PickerOption> employees = (List<PickerOption>) data[1];
            @SuppressWarnings("unchecked")
            List<PickerOption> departments = (List<PickerOption>) data[2];
            @SuppressWarnings("unchecked")
            List<PickerOption> vendors = (List<PickerOption>) data[3];
            openEditDialog((String) data[0], employees, departments, vendors);
        });

        loadTask.setOnFailed(e -> showAlert("Error",
                "Could not load edit form: " + loadTask.getException().getMessage()));

        Thread t = new Thread(loadTask);
        t.setDaemon(true);
        t.start();
    }


    private void openEditDialog(String json, List<PickerOption> employees,
                                List<PickerOption> departments, List<PickerOption> vendors) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Edit Asset");
        dialog.setHeaderText(asset.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(520);

        TextField nameField = new TextField(asset.getName());
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("PC", "Laptop", "Server", "Printer",
                "Switch", "Router", "UPS", "Mobile", "CCTV", "DVR", "NVR", "Biometric", "Other");
        categoryBox.setValue(asset.getCategory());

        TextField brandField = new TextField(asset.getBrand());
        TextField modelField = new TextField(extractValue(json, "model"));
        TextField serialField = new TextField(asset.getSerialNumber());
        TextField locationField = new TextField(asset.getLocation());

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Active", "In Repair", "Retired", "Disposed");
        statusBox.setValue(asset.getStatus());

        SearchablePickerField employeePicker = new SearchablePickerField(employees, "Search employee...");
        SearchablePickerField deptPicker = new SearchablePickerField(departments, "Search department...");
        SearchablePickerField vendorPicker = new SearchablePickerField(vendors, "Search vendor...");

        int currentAssignedTo = extractInt(json, "assignedTo");
        int currentDeptId = extractInt(json, "departmentId");
        int currentVendorId = extractInt(json, "vendorId");
        if (currentAssignedTo > 0) employeePicker.preselectSilently(currentAssignedTo);
        if (currentDeptId > 0) deptPicker.preselectSilently(currentDeptId);
        if (currentVendorId > 0) vendorPicker.preselectSilently(currentVendorId);

        // Auto-fill department from the chosen employee; user can still change it afterward.
        employeePicker.setOnSelect(opt -> {
            if (opt.extraId > 0) deptPicker.selectById(opt.extraId);
        });

        TextField assignedDateField = new TextField(DatePickerUtil.fromIso(extractValue(json, "assignedDate")));
        TextField purchaseDateField = new TextField(DatePickerUtil.fromIso(extractValue(json, "purchaseDate")));
        TextField warrantyField = new TextField(DatePickerUtil.fromIso(extractValue(json, "warrantyExpiry")));

        NumberField costField = new NumberField(true);
        costField.setText(String.valueOf(extractDouble(json, "purchaseCost")));

        TextField notesField = new TextField(asset.getNotes());
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(10));

        int r = 0;
        grid.add(new Label("Name *:"), 0, r);          grid.add(nameField, 1, r++);
        grid.add(new Label("Category:"), 0, r);        grid.add(categoryBox, 1, r++);
        grid.add(new Label("Brand:"), 0, r);           grid.add(brandField, 1, r++);
        grid.add(new Label("Model:"), 0, r);           grid.add(modelField, 1, r++);
        grid.add(new Label("Serial No:"), 0, r);       grid.add(serialField, 1, r++);
        grid.add(new Label("Location:"), 0, r);        grid.add(locationField, 1, r++);
        grid.add(new Label("Status:"), 0, r);          grid.add(statusBox, 1, r++);
        grid.add(new Label("Assigned To:"), 0, r);     grid.add(employeePicker, 1, r++);
        grid.add(new Label("Assigned Date:"), 0, r);   grid.add(DatePickerUtil.dateField(assignedDateField), 1, r++);
        grid.add(new Label("Department:"), 0, r);      grid.add(deptPicker, 1, r++);
        grid.add(new Label("Vendor:"), 0, r);          grid.add(vendorPicker, 1, r++);
        grid.add(new Label("Purchase Date:"), 0, r);   grid.add(DatePickerUtil.dateField(purchaseDateField), 1, r++);
        grid.add(new Label("Warranty Expiry:"), 0, r); grid.add(DatePickerUtil.dateField(warrantyField), 1, r++);
        grid.add(new Label("Cost (₹):"), 0, r);        grid.add(costField, 1, r++);
        grid.add(new Label("Notes:"), 0, r);           grid.add(notesField, 1, r++);
        grid.add(errorLabel, 1, r);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(460);
        dialog.getDialogPane().setContent(scroll);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setDisable(nameField.getText().trim().isEmpty());
        nameField.textProperty().addListener((o, ov, nv) -> okBtn.setDisable(nv.trim().isEmpty()));

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String body = "{" +
                        "\"id\":" + asset.getId() + "," +
                        "\"assetTag\":\"" + asset.getAssetTag() + "\"," +
                        "\"name\":\"" + escape(nameField.getText()) + "\"," +
                        "\"category\":\"" + categoryBox.getValue() + "\"," +
                        "\"brand\":\"" + escape(brandField.getText()) + "\"," +
                        "\"model\":\"" + escape(modelField.getText()) + "\"," +
                        "\"serialNumber\":\"" + escape(serialField.getText()) + "\"," +
                        "\"departmentId\":" + deptPicker.getSelectedId() + "," +
                        "\"location\":\"" + escape(locationField.getText()) + "\"," +
                        "\"status\":\"" + statusBox.getValue() + "\"," +
                        "\"assignedTo\":" + employeePicker.getSelectedId() + "," +
                        "\"assignedDate\":\"" + DatePickerUtil.toIso(assignedDateField.getText()) + "\"," +
                        "\"purchaseDate\":\"" + DatePickerUtil.toIso(purchaseDateField.getText()) + "\"," +
                        "\"warrantyExpiry\":\"" + DatePickerUtil.toIso(warrantyField.getText()) + "\"," +
                        "\"vendorId\":" + vendorPicker.getSelectedId() + "," +
                        "\"purchaseCost\":" + costField.getDoubleValue() + "," +
                        "\"notes\":\"" + escape(notesField.getText()) + "\"" +
                        "}";

                HttpResponse<String> resp = ApiClient.put(
                        ConfigManager.getBaseUrl() + "/api/assets/" + asset.getId(), body);
                if (resp.statusCode() == 200) {
                    ToastUtil.success("Asset updated successfully.");
                    if (onUpdate != null) onUpdate.run();
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    // ── Edit Specs Dialog ─────────────────────────────────
    private void showEditSpecsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Edit Specifications");
        dialog.setHeaderText(asset.getName()
                + " — " + asset.getCategory());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        // Get spec keys for this category
        List<String> specKeys = CATEGORY_SPECS
                .getOrDefault(asset.getCategory(),
                        List.of("ip_address", "mac_address",
                                "notes"));

        // Load existing specs
        Map<String, String> existing = loadSpecs();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(150);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

        Map<String, TextField> fields = new LinkedHashMap<>();
        int row = 0;
        for (String key : specKeys) {
            String label = key.replace("_", " ")
                    .toUpperCase();
            TextField field = new TextField(
                    existing.getOrDefault(key, ""));
            grid.add(new Label(label + ":"), 0, row);
            grid.add(field, 1, row++);
            fields.put(key, field);
        }

// Dedicated Notes field — always saved as-is, never routed through the key=value parser
        Label notesLabel = new Label("Notes:");
        TextArea notesArea = new TextArea(existing.getOrDefault("notes", ""));
        notesArea.setPrefRowCount(2);
        notesArea.setPromptText("Any additional notes about the specs...");
        grid.add(notesLabel, 0, row);
        grid.add(notesArea, 1, row++);

        Label customLabel = new Label(
                "Additional specs (key=value, one per line):");
        customLabel.getStyleClass().add("text-muted");
        customLabel.setStyle("-fx-font-size: 11px;");
        TextArea customArea = new TextArea();
        customArea.setPrefRowCount(3);
        customArea.setPromptText(
                "e.g. gpu=GTX 1650\nmonitor=24 inch");

        StringBuilder leftover = new StringBuilder();
        for (Map.Entry<String, String> e : existing.entrySet()) {
            if (!specKeys.contains(e.getKey()) && !"notes".equals(e.getKey())) {
                leftover.append(e.getKey()).append("=").append(e.getValue()).append("\n");
            }
        }
        customArea.setText(leftover.toString().trim());

        grid.add(customLabel, 0, row, 2, 1);
        row++;
        grid.add(customArea, 0, row, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()
                && result.get() == ButtonType.OK) {
            Map<String, String> specs = new LinkedHashMap<>();
            for (Map.Entry<String, TextField> entry
                    : fields.entrySet()) {
                if (!entry.getValue().getText()
                        .trim().isEmpty()) {
                    specs.put(entry.getKey(),
                            entry.getValue().getText().trim());
                }
            }
            if (!notesArea.getText().trim().isEmpty()) {
                specs.put("notes", notesArea.getText().trim());
            }
            // Parse custom specs
            for (String line :
                    customArea.getText().split("\n")) {
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2
                            && !parts[1].trim().isEmpty()) {
                        specs.put(parts[0].trim()
                                        .toLowerCase()
                                        .replace(" ", "_"),
                                parts[1].trim());
                    }
                }
            }

            saveSpecs(specs);
            buildContent(); // Refresh
            AnimationUtil.fadeIn(root);
        }
    }

    // ── Add Component Dialog ──────────────────────────────
    private void showAddComponentDialog(
            TableView<String[]> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Add Component");
        dialog.setHeaderText("Add component to "
                + asset.getName());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(
                "RAM", "HDD", "SSD", "NVMe", "CPU", "GPU",
                "PSU", "NIC", "Monitor", "Keyboard", "Mouse",
                "Battery", "Adapter", "Lens", "IR Board",
                "Fuser Unit", "Drum Unit", "SFP Module",
                "Battery Pack", "DVR Card", "Other");
        typeBox.setValue("RAM");
        typeBox.setEditable(true);

        TextField brandField   = new TextField();
        TextField modelField   = new TextField();
        TextField serialField  = new TextField();
        TextField specsField   = new TextField();
        specsField.setPromptText(
                "e.g. 8GB DDR4 3200MHz");

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll(
                "In Use", "Spare", "Faulty", "Replaced");
        statusBox.setValue("In Use");

        TextField purchaseDateField = new TextField();
        TextField notesField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        int r = 0;
        grid.add(new Label("Type *:"),       0, r);
        grid.add(typeBox,                    1, r++);
        grid.add(new Label("Brand:"),        0, r);
        grid.add(brandField,                 1, r++);
        grid.add(new Label("Model:"),        0, r);
        grid.add(modelField,                 1, r++);
        grid.add(new Label("Serial No:"),    0, r);
        grid.add(serialField,                1, r++);
        grid.add(new Label("Specs:"),        0, r);
        grid.add(specsField,                 1, r++);
        grid.add(new Label("Status:"),       0, r);
        grid.add(statusBox,                  1, r++);
        grid.add(new Label("Purchase Date:"),0, r);
        grid.add(DatePickerUtil.dateField(
                purchaseDateField),          1, r++);
        grid.add(new Label("Notes:"),        0, r);
        grid.add(notesField,                 1, r);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()
                && result.get() == ButtonType.OK) {
            try {
                String body = "{" +
                        "\"componentType\":\"" +
                        typeBox.getValue() + "\"," +
                        "\"brand\":\"" +
                        brandField.getText() + "\"," +
                        "\"model\":\"" +
                        modelField.getText() + "\"," +
                        "\"serialNumber\":\"" +
                        serialField.getText() + "\"," +
                        "\"specs\":\"" +
                        specsField.getText() + "\"," +
                        "\"status\":\"" +
                        statusBox.getValue() + "\"," +
                        "\"purchaseDate\":\"" +
                        purchaseDateField.getText() + "\"," +
                        "\"notes\":\"" +
                        notesField.getText() + "\"" +
                        "}";
                HttpResponse<String> resp = ApiClient.post(
                        ConfigManager.getBaseUrl() + "/api/assets/" + asset.getId() + "/components", body);
                if (resp.statusCode() == 201) {
                    loadComponents(table);
                } else {
                    showAlert("Error", "Server returned: "
                            + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        }
    }

    // ── Add Maintenance Dialog ────────────────────────────
    private void showAddMaintenanceDialog(
            TableView<String[]> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Add Maintenance Log");
        dialog.setHeaderText(asset.getName());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Repair", "AMC Visit",
                "Preventive", "Upgrade", "Cleaning", "Other");
        typeBox.setValue("Repair");

        TextField descField    = new TextField();
        NumberField costField  = new NumberField(true);
        costField.setPromptText("0.0");

        TextField dateField    = new TextField();
        TextField nextDueField = new TextField();

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll(
                "Completed", "Pending", "In Progress");
        statusBox.setValue("Completed");

        TextField notesField   = new TextField();
        Label errorLabel       = new Label("");
        errorLabel.setStyle(
                "-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        int r = 0;
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
                nextDueField),                1, r++);
        grid.add(new Label("Status:"),        0, r);
        grid.add(statusBox,                   1, r++);
        grid.add(new Label("Notes:"),         0, r);
        grid.add(notesField,                  1, r++);
        grid.add(errorLabel,                  1, r);
        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okBtn.setDisable(true);
        dateField.textProperty().addListener((o, ov, nv) ->
                okBtn.setDisable(nv.trim().isEmpty()));

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
                        "\"assetId\":" + asset.getId() + "," +
                        "\"maintenanceType\":\"" +
                        typeBox.getValue() + "\"," +
                        "\"description\":\"" +
                        escape(descField.getText()) + "\"," +
                        "\"doneByInternal\":0," +
                        "\"doneByVendor\":0," +
                        "\"cost\":" +
                        costField.getDoubleValue() + "," +
                        "\"maintenanceDate\":\"" +
                        dateField.getText() + "\"," +
                        "\"nextDueDate\":\"" +
                        nextDueField.getText() + "\"," +
                        "\"status\":\"" +
                        statusBox.getValue() + "\"," +
                        "\"notes\":\"" +
                        escape(notesField.getText()) + "\"," +
                        "\"loggedBy\":" +
                        SessionManager.get().getUserId() +
                        "}";
                HttpResponse<String> resp = ApiClient.post(
                        ConfigManager.getBaseUrl() + "/api/maintenance", body);
                if (resp.statusCode() == 201) {
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

    // ── Load Methods ──────────────────────────────────────
    private void loadComponents(TableView<String[]> table) {
        // 1. Pre-Task UI Setup
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading components...");

        // 2. Background Task
        Task<List<String[]>> task = new Task<>() {
            @Override
            protected List<String[]> call() throws Exception {
                List<String[]> result = new ArrayList<>();

                String url = ConfigManager.getBaseUrl() + "/api/assets/" + asset.getId() + "/components";
                HttpResponse<String> resp = ApiClient.get(url);
                String body = resp.body().trim();

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

                        String[] row = new String[]{
                                extractValue(cleanedObj, "component_type"),
                                extractValue(cleanedObj, "brand"),
                                extractValue(cleanedObj, "model"),
                                extractValue(cleanedObj, "serial_number"),
                                extractValue(cleanedObj, "status"),
                                String.valueOf(extractInt(cleanedObj, "id"))
                        };

                        result.add(row);
                    }
                }
                return result;
            }
        };

        // 3. Success Callback (Runs on JavaFX Application Thread)
        task.setOnSucceeded(e -> {
            List<String[]> components = task.getValue();

            // Update UI collections
            table.getItems().addAll(components);

            // Handle empty state
            if (components.isEmpty()) {
                LoadingUtil.setEmpty(table, "🔧", "No components recorded", "Click + Add Component to add parts.");
            }
        });

        // 4. Failure Callback (Runs on JavaFX Application Thread)
        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠", "Error loading components", "");
            task.getException().printStackTrace(); // Helps with debugging
        });

        // 5. Daemon Thread Execution
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadMaintenance(TableView<String[]> table) {
        // 1. Pre-Task UI Setup
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading maintenance records...");

        // 2. Background Task
        Task<List<String[]>> task = new Task<>() {
            @Override
            protected List<String[]> call() throws Exception {
                List<String[]> result = new ArrayList<>();

                String url = ConfigManager.getBaseUrl() + "/api/maintenance/asset/" + asset.getId();
                HttpResponse<String> resp = ApiClient.get(url);
                String body = resp.body().trim();

                // Handle empty arrays securely
                if (body.equals("[]") || body.isEmpty()) return result;

                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    String[] jsonObjects = body.split("\\},\\s*\\{");
                    for (String obj : jsonObjects) {
                        String cleanedObj = obj.replace("{", "").replace("}", "");

                        String[] row = new String[]{
                                DateTimeFormatUtil.toIndianDateOnly(extractValue(cleanedObj, "maintenanceDate")),
                                extractValue(cleanedObj, "maintenanceType"),
                                extractValue(cleanedObj, "description"),
                                "₹" + extractValue(cleanedObj, "cost"),
                                extractValue(cleanedObj, "status")
                        };
                        result.add(row);
                    }
                }
                return result;
            }
        };

        // 3. Success Callback
        task.setOnSucceeded(e -> {
            List<String[]> records = task.getValue();
            table.getItems().addAll(records);

            if (records.isEmpty()) {
                LoadingUtil.setEmpty(table, "🔨", "No maintenance records", "");
            }
        });

        // 4. Failure Callback
        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠", "Error loading maintenance", "");
            task.getException().printStackTrace();
        });

        // 5. Execute Thread
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadHistory(TableView<String[]> table) {
        // 1. Pre-Task UI Setup
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading history..."); // Added for UI consistency

        // 2. Background Task
        Task<List<String[]>> task = new Task<>() {
            @Override
            protected List<String[]> call() throws Exception {
                List<String[]> result = new ArrayList<>();

                String url = ConfigManager.getBaseUrl() + "/api/assets/" + asset.getId() + "/history";
                HttpResponse<String> resp = ApiClient.get(url);
                String body = resp.body().trim();

                if (body.equals("[]") || body.isEmpty()) return result;

                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    String[] jsonObjects = body.split("\\},\\s*\\{");
                    for (String obj : jsonObjects) {
                        String cleanedObj = obj.replace("{", "").replace("}", "");
                        String date = extractValue(cleanedObj, "action_date");

                        String[] row = new String[]{
                                DateTimeFormatUtil.toIndianDateOnly(extractValue(cleanedObj, "action_date")),
                                extractValue(cleanedObj, "action"),
                                extractValue(cleanedObj, "from_employee_name"),
                                extractValue(cleanedObj, "to_employee_name")
                        };
                        result.add(row);
                    }
                }
                return result;
            }
        };

        // 3. Success Callback
        task.setOnSucceeded(e -> {
            List<String[]> history = task.getValue();
            table.getItems().addAll(history);

            if (history.isEmpty()) {
                LoadingUtil.setEmpty(table, "📜", "No movement history", "");
            }
        });

        // 4. Failure Callback
        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠", "Error loading history", "");
            task.getException().printStackTrace();
        });

        // 5. Execute Thread
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadTickets(TableView<String[]> table) {
        // 1. Pre-Task UI Setup
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading tickets..."); // Added for UI consistency

        // 2. Background Task
        Task<List<String[]>> task = new Task<>() {
            @Override
            protected List<String[]> call() throws Exception {
                List<String[]> result = new ArrayList<>();

                String url = ConfigManager.getBaseUrl() + "/api/tickets/asset/" + asset.getId();
                HttpResponse<String> resp = ApiClient.get(url);
                String body = resp.body().trim();

                if (body.equals("[]") || body.isEmpty()) return result;

                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    String[] jsonObjects = body.split("\\},\\s*\\{");
                    for (String obj : jsonObjects) {
                        String cleanedObj = obj.replace("{", "").replace("}", "");

                        String[] row = new String[]{
                                extractValue(cleanedObj, "ticketNo"),
                                extractValue(cleanedObj, "title"),
                                extractValue(cleanedObj, "status")
                        };
                        result.add(row);
                    }
                }
                return result;
            }
        };

        // 3. Success Callback
        task.setOnSucceeded(e -> {
            List<String[]> tickets = task.getValue();
            table.getItems().addAll(tickets);

            if (tickets.isEmpty()) {
                LoadingUtil.setEmpty(table, "🎫", "No linked tickets", "");
            }
        });

        // 4. Failure Callback
        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠", "Error loading tickets", "");
            task.getException().printStackTrace();
        });

        // 5. Execute Thread
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void deleteComponent(int componentId,
                                 TableView<String[]> table) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        ThemeManager.applyToDialog(confirm);
        confirm.setTitle("Delete Component");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Delete this component record?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    ApiClient.delete(ConfigManager.getBaseUrl() + "/api/assets/components/" + componentId);
                    loadComponents(table);
                } catch (Exception ex) {
                    showAlert("Error", ex.getMessage());
                }
            }
        });
    }

    private void saveSpecs(Map<String, String> specs) {
        try {
            StringBuilder json = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<String, String> e
                    : specs.entrySet()) {
                json.append("\"").append(e.getKey())
                        .append("\":\"")
                        .append(escape(e.getValue()))
                        .append("\"");
                if (++i < specs.size()) json.append(",");
            }
            json.append("}");

            HttpResponse<String> resp = ApiClient.put(
                    ConfigManager.getBaseUrl() + "/api/assets/" + asset.getId() + "/specs", json.toString());
            if (resp.statusCode() == 200) {
                ToastUtil.success("Specs saved.");
            }
        } catch (Exception ex) {
            showAlert("Error", ex.getMessage());
        }
    }

    // ── Helper loaders ────────────────────────────────────
    private String fetchAssetJson() {
        try {
            HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/assets/" + asset.getId());
            return resp.body();
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Map<String, String> loadSpecs() {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            HttpResponse<String> resp = ApiClient.get(
                    ConfigManager.getBaseUrl() + "/api/assets/" + asset.getId() + "/specs");
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "")
                            .replace("}", "");
                    result.put(
                            extractValue(obj, "spec_key"),
                            extractValue(obj, "spec_value"));
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private String loadEmployeeName(int empId) {
        try {
            HttpResponse<String> resp = ApiClient.get(
                    ConfigManager.getBaseUrl() + "/api/employees/" + empId);
            return extractValue(resp.body(), "name");
        } catch (Exception e) {
            return "Employee #" + empId;
        }
    }

    private String loadDeptName(String json) {
        int deptId = extractInt(json, "departmentId");
        if (deptId == 0) return "-";
        try {
            HttpResponse<String> resp = ApiClient.get(
                    ConfigManager.getBaseUrl() + "/api/departments/" + deptId);
            return extractValue(resp.body(), "name");
        } catch (Exception e) {
            return "Dept #" + deptId;
        }
    }

    private String loadVendorName(String json) {
        int vendorId = extractInt(json, "vendorId");
        if (vendorId == 0) return "-";
        try {
            HttpResponse<String> resp = ApiClient.get(
                    ConfigManager.getBaseUrl() + "/api/vendors/" + vendorId);
            return extractValue(resp.body(), "name");
        } catch (Exception e) {
            return "Vendor #" + vendorId;
        }
    }



    // ── UI Helpers ────────────────────────────────────────
    private Label sectionHeader(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 20 4 20;");
        return label; // text color now comes from the theme-aware base .label rule
    }

    private HBox sectionHeaderWithBtn(String text,
                                      Button btn) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(label, spacer, btn);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 20, 4, 20));
        return box;
    }

    private Separator divider() {
        return new Separator();
    }

    private void addGridRow(GridPane grid, int row,
                            String label, String value) {
        Label k = new Label(label + ":");
        k.getStyleClass().add("text-muted");
        k.setStyle("-fx-font-size: 12px; -fx-min-width: 120;");
        Label v = new Label(
                value != null && !value.isEmpty()
                        && !value.equals("0")
                        ? value : "-");
        v.setStyle("-fx-font-size: 13px;"); // color inherits from theme-aware base .label
        v.setWrapText(true);
        grid.add(k, 0, row);
        grid.add(v, 1, row);
    }

    private HBox specRow(String key, String value) {
        Label k = new Label(key + ":");
        k.getStyleClass().add("text-muted");
        k.setStyle("-fx-font-size: 11px; -fx-min-width: 140;");
        Label v = new Label(
                value != null && !value.isEmpty()
                        ? value : "-");
        v.getStyleClass().add("breadcrumb-current"); // reuses the existing theme-aware accent-blue label style
        v.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        HBox row = new HBox(8, k, v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String statusStyle(String status) {
        boolean light = ThemeManager.getCurrent() == ThemeManager.Theme.LIGHT;
        String bg = light ? switch (status) {
            case "Active"    -> "#eafaf1";
            case "In Repair" -> "#fdf2e3";
            case "Retired"   -> "#eceff1";
            case "Disposed"  -> "#fdecea";
            default          -> "#eceff1";
        } : switch (status) {
            case "Active"    -> "#1b2d1f";
            case "In Repair" -> "#2d2008";
            case "Retired"   -> "#21262d";
            case "Disposed"  -> "#3d1f1e";
            default          -> "#21262d";
        };
        String fg = light ? switch (status) {
            case "Active"    -> "#27ae60";
            case "In Repair" -> "#b3650f";
            case "Retired"   -> "#5B6B7D";
            case "Disposed"  -> "#c0392b";
            default          -> "#5B6B7D";
        } : switch (status) {
            case "Active"    -> "#3fb950";
            case "In Repair" -> "#d29922";
            case "Retired"   -> "#8b949e";
            case "Disposed"  -> "#f85149";
            default          -> "#c9d1d9";
        };
        return "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: " + fg + ";" +
                "-fx-background-radius: 10;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;";
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.applyToDialog(alert);
        alert.showAndWait();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "'")
                .replace("\n", " ")
                .replace("\r", "");
    }

    private List<PickerOption> fetchEmployeeOptions() throws Exception {
        return fetchOptions("/api/employees", true);
    }
    private List<PickerOption> fetchDepartmentOptions() throws Exception {
        return fetchOptions("/api/departments", false);
    }
    private List<PickerOption> fetchVendorOptions() throws Exception {
        return fetchOptions("/api/vendors", false);
    }

    private List<PickerOption> fetchOptions(String path, boolean withDept) throws Exception {
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
            int extra = withDept ? extractInt(cleaned, "departmentId") : 0;
            result.add(new PickerOption(id, name, extra));
        }
        return result;
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }

    private double extractDouble(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return 0.0;
        start += search.length();
        int endComma = json.indexOf(",", start);
        int endBrace = json.indexOf("}", start);
        int end = (endComma == -1) ? endBrace : (endBrace == -1 ? endComma : Math.min(endComma, endBrace));
        if (end == -1) end = json.length();
        try {
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.length();
        try {
            return Integer.parseInt(json.substring(
                    start, end).trim().replace("}", ""));
        } catch (NumberFormatException e) { return 0; }
    }
}