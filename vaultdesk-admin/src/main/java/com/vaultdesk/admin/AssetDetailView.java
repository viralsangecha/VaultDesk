package com.vaultdesk.admin;

import javafx.beans.property.SimpleStringProperty;
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
        root.setStyle(
                "-fx-background-color: #0d1117;");

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

        Label nameLabel = new Label(asset.getName());
        nameLabel.setStyle(
                "-fx-text-fill: #e6edf3;" +
                        "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;");

        Label categoryLabel = new Label(asset.getCategory());
        categoryLabel.setStyle(
                "-fx-text-fill: #8b949e;" +
                        "-fx-font-size: 13px;");

        Label statusBadge = new Label(asset.getStatus());
        statusBadge.setPadding(new Insets(4, 12, 4, 12));
        statusBadge.setStyle(statusStyle(asset.getStatus()));

        HBox headerTop = new HBox(8, tagLabel, statusBadge);
        headerTop.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = new Button("✏ Edit Asset");
        editBtn.getStyleClass().setAll("btn-warning");
        editBtn.setStyle(
                "-fx-background-color: #b45309;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 6 14 6 14;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;");
        editBtn.setOnAction(e -> showEditDialog());

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        HBox titleRow = new HBox(8,
                new VBox(4, headerTop, nameLabel, categoryLabel),
                hSpacer, editBtn);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.setPadding(new Insets(20, 20, 16, 20));
        titleRow.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-width: 0 0 1 0;");

        // ── Scrollable content ────────────────────────────
        VBox content = new VBox(0);

        content.getChildren().addAll(
                buildInfoSection(),
                buildSpecsSection(),
                buildComponentsSection(),
                buildMaintenanceSection(),
                buildHistorySection(),
                buildTicketsSection()
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background-color: #0d1117;" +
                        "-fx-background: #0d1117;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(titleRow, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
    }

    // ── Basic Info + Assignment + Purchase ────────────────
    private VBox buildInfoSection() {
        // Load employee name for assigned_to
        String assignedName = "Unassigned";
        String deptName     = "-";
        String vendorName   = "-";

        if (asset.getAssignedTo() > 0) {
            assignedName = loadEmployeeName(asset.getAssignedTo());
        }

        VBox section = new VBox(0);

        // Basic info
        GridPane basicGrid = new GridPane();
        basicGrid.setHgap(16); basicGrid.setVgap(8);
        basicGrid.setPadding(new Insets(16, 20, 8, 20));

        addGridRow(basicGrid, 0, "Brand", asset.getBrand());
        addGridRow(basicGrid, 1, "Model",
                loadAssetField("model"));
        addGridRow(basicGrid, 2, "Serial No",
                asset.getSerialNumber());
        addGridRow(basicGrid, 3, "Location",
                asset.getLocation());

        // Assignment info
        GridPane assignGrid = new GridPane();
        assignGrid.setHgap(16); assignGrid.setVgap(8);
        assignGrid.setPadding(new Insets(8, 20, 8, 20));

        addGridRow(assignGrid, 0, "Assigned To", assignedName);
        addGridRow(assignGrid, 1, "Assigned Date",
                loadAssetField("assigned_date"));
        addGridRow(assignGrid, 2, "Department",
                loadDeptName());

        // Purchase info
        GridPane purchaseGrid = new GridPane();
        purchaseGrid.setHgap(16); purchaseGrid.setVgap(8);
        purchaseGrid.setPadding(new Insets(8, 20, 16, 20));

        addGridRow(purchaseGrid, 0, "Vendor",
                loadVendorName());
        addGridRow(purchaseGrid, 1, "Purchase Date",
                loadAssetField("purchase_date"));
        addGridRow(purchaseGrid, 2, "Purchase Cost",
                "₹" + String.format("%.2f",
                        loadAssetDouble("purchase_cost")));
        addGridRow(purchaseGrid, 3, "Warranty Expiry",
                loadAssetField("warranty_expiry"));
        addGridRow(purchaseGrid, 4, "Notes",
                asset.getNotes());

        section.getChildren().addAll(
                sectionHeader("📋 Basic Information"),
                basicGrid,
                sectionHeader("👤 Assignment"),
                assignGrid,
                sectionHeader("💰 Purchase Information"),
                purchaseGrid,
                divider());

        return section;
    }

    // ── Specs Section ─────────────────────────────────────
    private VBox buildSpecsSection() {
        VBox section = new VBox(0);

        Button editSpecsBtn = new Button("✏ Edit Specs");
        editSpecsBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #58a6ff;" +
                        "-fx-font-size: 11px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-width: 0;");
        editSpecsBtn.setOnAction(e -> showEditSpecsDialog());

        HBox header = sectionHeaderWithBtn(
                "⚙ Specifications", editSpecsBtn);

        VBox specsContent = new VBox(6);
        specsContent.setPadding(new Insets(8, 20, 16, 20));

        // Load specs from server
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/assets/" + asset.getId()
                            + "/specs"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
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
                empty.setStyle(
                        "-fx-text-fill: #484f58;" +
                                "-fx-font-size: 12px;");
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
        addCompBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #3fb950;" +
                        "-fx-font-size: 11px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-width: 0;");

        HBox header = sectionHeaderWithBtn(
                "🔧 Components", addCompBtn);

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(160);
        table.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: #30363d;");

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
                delBtn.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: #f85149;" +
                                "-fx-cursor: hand;" +
                                "-fx-border-width: 0;");
                delBtn.setOnAction(e -> {
                    String[] row = getTableView()
                            .getItems().get(getIndex());
                    deleteComponent(Integer.parseInt(row[5]),
                            table);
                });
            }
            @Override
            protected void updateItem(Void item,
                                      boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : delBtn);
            }
        });
        actionCol.setMaxWidth(40);

        table.getColumns().addAll(typeCol, brandCol,
                modelCol, serialCol, statusCol, actionCol);

        loadComponents(table);

        addCompBtn.setOnAction(e ->
                showAddComponentDialog(table));

        VBox tableBox = new VBox(table);
        tableBox.setPadding(new Insets(8, 20, 16, 20));

        section.getChildren().addAll(
                header, tableBox, divider());
        return section;
    }

    // ── Maintenance History ───────────────────────────────
    private VBox buildMaintenanceSection() {
        VBox section = new VBox(0);

        Button addBtn = new Button("+ Add Maintenance");
        addBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #3fb950;" +
                        "-fx-font-size: 11px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-width: 0;");

        HBox header = sectionHeaderWithBtn(
                "🔨 Maintenance History", addBtn);

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(160);
        table.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: #30363d;");

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

        VBox tableBox = new VBox(table);
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
        table.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: #30363d;");

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

        VBox tableBox = new VBox(table);
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
        table.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: #30363d;");

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

        VBox tableBox = new VBox(table);
        tableBox.setPadding(
                new Insets(8, 20, 20, 20));

        section.getChildren().addAll(
                sectionHeader("🎫 Linked Tickets"),
                tableBox);
        return section;
    }

    // ── Edit Asset Dialog ─────────────────────────────────
    private void showEditDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Asset");
        dialog.setHeaderText(asset.getName());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(520);

        // Load full asset details from server
        Map<String, String> fullAsset = loadFullAsset();

        TextField nameField = new TextField(asset.getName());
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(
                "PC", "Laptop", "Server", "Printer",
                "Switch", "Router", "UPS", "Mobile",
                "CCTV", "DVR", "NVR", "Biometric", "Other");
        categoryBox.setValue(asset.getCategory());

        TextField brandField = new TextField(asset.getBrand());
        TextField modelField = new TextField(
                fullAsset.getOrDefault("model", ""));
        TextField serialField = new TextField(
                asset.getSerialNumber());
        TextField locationField = new TextField(
                asset.getLocation());

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll(
                "Active", "In Repair", "Retired", "Disposed");
        statusBox.setValue(asset.getStatus());

        // Assigned to — employee picker
        TextField assignedField = new TextField(
                fullAsset.getOrDefault("assigned_to", "0"));
        assignedField.setPromptText("Employee ID");

        // Date fields with picker
        TextField assignedDateField = new TextField(
                fullAsset.getOrDefault("assigned_date", ""));
        TextField purchaseDateField = new TextField(
                fullAsset.getOrDefault("purchase_date", ""));
        TextField warrantyField = new TextField(
                fullAsset.getOrDefault("warranty_expiry", ""));

        NumberField costField = new NumberField(true);
        costField.setText(
                fullAsset.getOrDefault("purchase_cost", "0"));

        TextField vendorField = new TextField(
                fullAsset.getOrDefault("vendor_id", "0"));
        vendorField.setPromptText("Vendor ID");

        TextField deptField = new TextField(
                fullAsset.getOrDefault("department_id", "0"));
        deptField.setPromptText("Department ID");

        TextField notesField = new TextField(asset.getNotes());

        Label errorLabel = new Label("");
        errorLabel.setStyle(
                "-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(10));

        int r = 0;
        grid.add(new Label("Name *:"),         0, r);
        grid.add(nameField,                    1, r++);
        grid.add(new Label("Category:"),       0, r);
        grid.add(categoryBox,                  1, r++);
        grid.add(new Label("Brand:"),          0, r);
        grid.add(brandField,                   1, r++);
        grid.add(new Label("Model:"),          0, r);
        grid.add(modelField,                   1, r++);
        grid.add(new Label("Serial No:"),      0, r);
        grid.add(serialField,                  1, r++);
        grid.add(new Label("Location:"),       0, r);
        grid.add(locationField,                1, r++);
        grid.add(new Label("Status:"),         0, r);
        grid.add(statusBox,                    1, r++);
        grid.add(new Label("Assigned To (ID):"),0, r);
        grid.add(assignedField,                1, r++);
        grid.add(new Label("Assigned Date:"),  0, r);
        grid.add(DatePickerUtil.dateField(
                assignedDateField),            1, r++);
        grid.add(new Label("Dept ID:"),        0, r);
        grid.add(deptField,                    1, r++);
        grid.add(new Label("Vendor ID:"),      0, r);
        grid.add(vendorField,                  1, r++);
        grid.add(new Label("Purchase Date:"),  0, r);
        grid.add(DatePickerUtil.dateField(
                purchaseDateField),            1, r++);
        grid.add(new Label("Warranty Expiry:"),0, r);
        grid.add(DatePickerUtil.dateField(
                warrantyField),                1, r++);
        grid.add(new Label("Cost (₹):"),       0, r);
        grid.add(costField,                    1, r++);
        grid.add(new Label("Notes:"),          0, r);
        grid.add(notesField,                   1, r++);
        grid.add(errorLabel,                   1, r);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);
        dialog.getDialogPane().setContent(scroll);

        Button okBtn = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okBtn.setDisable(nameField.getText().trim().isEmpty());
        nameField.textProperty().addListener((o, ov, nv) ->
                okBtn.setDisable(nv.trim().isEmpty()));

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()
                && result.get() == ButtonType.OK) {
            try {
                int assignedTo = assignedField.getText()
                        .trim().isEmpty() ? 0
                        : Integer.parseInt(
                        assignedField.getText().trim());
                int deptId = deptField.getText()
                        .trim().isEmpty() ? 0
                        : Integer.parseInt(
                        deptField.getText().trim());
                int vendorId = vendorField.getText()
                        .trim().isEmpty() ? 0
                        : Integer.parseInt(
                        vendorField.getText().trim());

                String body = "{" +
                        "\"id\":" + asset.getId() + "," +
                        "\"assetTag\":\"" + asset.getAssetTag()
                        + "\"," +
                        "\"name\":\"" + escape(
                        nameField.getText()) + "\"," +
                        "\"category\":\"" +
                        categoryBox.getValue() + "\"," +
                        "\"brand\":\"" + escape(
                        brandField.getText()) + "\"," +
                        "\"model\":\"" + escape(
                        modelField.getText()) + "\"," +
                        "\"serialNumber\":\"" + escape(
                        serialField.getText()) + "\"," +
                        "\"departmentId\":" + deptId + "," +
                        "\"location\":\"" + escape(
                        locationField.getText()) + "\"," +
                        "\"status\":\"" +
                        statusBox.getValue() + "\"," +
                        "\"assignedTo\":" + assignedTo + "," +
                        "\"assignedDate\":\"" +
                        assignedDateField.getText() + "\"," +
                        "\"purchaseDate\":\"" +
                        purchaseDateField.getText() + "\"," +
                        "\"warrantyExpiry\":\"" +
                        warrantyField.getText() + "\"," +
                        "\"vendorId\":" + vendorId + "," +
                        "\"purchaseCost\":" +
                        costField.getDoubleValue() + "," +
                        "\"notes\":\"" + escape(
                        notesField.getText()) + "\"" +
                        "}";

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(
                                ConfigManager.getBaseUrl()
                                        + "/api/assets/"
                                        + asset.getId()))
                        .header("Content-Type",
                                "application/json")
                        .PUT(HttpRequest.BodyPublishers
                                .ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(
                        req,
                        HttpResponse.BodyHandlers
                                .ofString());
                if (resp.statusCode() == 200) {
                    showAlert("Success",
                            "Asset updated successfully.");
                    if (onUpdate != null) onUpdate.run();
                } else {
                    showAlert("Error", "Server returned: "
                            + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error",
                        "Cannot connect: " + ex.getMessage());
            }
        }
    }

    // ── Edit Specs Dialog ─────────────────────────────────
    private void showEditSpecsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
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
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(10));

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

        // Allow custom specs
        Label customLabel = new Label(
                "Additional specs (key=value, one per line):");
        customLabel.setStyle(
                "-fx-text-fill: #8b949e;" +
                        "-fx-font-size: 11px;");
        TextArea customArea = new TextArea();
        customArea.setPrefRowCount(3);
        customArea.setPromptText(
                "e.g. gpu=GTX 1650\nmonitor=24 inch");

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
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(
                                ConfigManager.getBaseUrl()
                                        + "/api/assets/"
                                        + asset.getId()
                                        + "/components"))
                        .header("Content-Type",
                                "application/json")
                        .POST(HttpRequest.BodyPublishers
                                .ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(
                        req,
                        HttpResponse.BodyHandlers
                                .ofString());
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
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(
                                ConfigManager.getBaseUrl()
                                        + "/api/maintenance"))
                        .header("Content-Type",
                                "application/json")
                        .POST(HttpRequest.BodyPublishers
                                .ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(
                        req,
                        HttpResponse.BodyHandlers
                                .ofString());
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
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading components...");
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/assets/" + asset.getId()
                            + "/components"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "")
                            .replace("}", "");
                    table.getItems().add(new String[]{
                            extractValue(obj, "component_type"),
                            extractValue(obj, "brand"),
                            extractValue(obj, "model"),
                            extractValue(obj, "serial_number"),
                            extractValue(obj, "status"),
                            String.valueOf(
                                    extractInt(obj, "id"))
                    });
                }
                if (table.getItems().isEmpty())
                    LoadingUtil.setEmpty(table, "🔧",
                            "No components recorded",
                            "Click + Add Component to add parts.");
            } else {
                LoadingUtil.setEmpty(table, "🔧",
                        "No components recorded",
                        "Click + Add Component to add parts.");
            }
        } catch (Exception ex) {
            LoadingUtil.setEmpty(table, "⚠",
                    "Error loading components", "");
        }
    }

    private void loadMaintenance(TableView<String[]> table) {
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading...");
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/maintenance/asset/"
                            + asset.getId()))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "")
                            .replace("}", "");
                    table.getItems().add(new String[]{
                            extractValue(obj,
                                    "maintenanceDate"),
                            extractValue(obj,
                                    "maintenanceType"),
                            extractValue(obj, "description"),
                            "₹" + extractValue(obj, "cost"),
                            extractValue(obj, "status")
                    });
                }
                if (table.getItems().isEmpty())
                    LoadingUtil.setEmpty(table, "🔨",
                            "No maintenance records", "");
            } else {
                LoadingUtil.setEmpty(table, "🔨",
                        "No maintenance records", "");
            }
        } catch (Exception ex) {
            LoadingUtil.setEmpty(table, "⚠",
                    "Error loading maintenance", "");
        }
    }

    private void loadHistory(TableView<String[]> table) {
        table.getItems().clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/assets/" + asset.getId()
                            + "/history"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "")
                            .replace("}", "");
                    String date = extractValue(
                            obj, "action_date");
                    table.getItems().add(new String[]{
                            date.length() >= 10
                                    ? date.substring(0, 10)
                                    : date,
                            extractValue(obj, "action"),
                            extractValue(obj,
                                    "from_employee_name"),
                            extractValue(obj,
                                    "to_employee_name")
                    });
                }
                if (table.getItems().isEmpty())
                    LoadingUtil.setEmpty(table, "📜",
                            "No movement history", "");
            } else {
                LoadingUtil.setEmpty(table, "📜",
                        "No movement history", "");
            }
        } catch (Exception ex) {
            LoadingUtil.setEmpty(table, "⚠",
                    "Error loading history", "");
        }
    }

    private void loadTickets(TableView<String[]> table) {
        table.getItems().clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/tickets/asset/"
                            + asset.getId()))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "")
                            .replace("}", "");
                    table.getItems().add(new String[]{
                            extractValue(obj, "ticketNo"),
                            extractValue(obj, "title"),
                            extractValue(obj, "status")
                    });
                }
                if (table.getItems().isEmpty())
                    LoadingUtil.setEmpty(table, "🎫",
                            "No linked tickets", "");
            } else {
                LoadingUtil.setEmpty(table, "🎫",
                        "No linked tickets", "");
            }
        } catch (Exception ex) {
            LoadingUtil.setEmpty(table, "⚠",
                    "Error loading tickets", "");
        }
    }

    private void deleteComponent(int componentId,
                                 TableView<String[]> table) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Component");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Delete this component record?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    HttpClient client =
                            HttpClient.newHttpClient();
                    HttpRequest req =
                            HttpRequest.newBuilder()
                                    .uri(URI.create(
                                            ConfigManager.getBaseUrl()
                                                    + "/api/assets/components/"
                                                    + componentId))
                                    .DELETE().build();
                    client.send(req,
                            HttpResponse.BodyHandlers
                                    .ofString());
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

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/assets/" + asset.getId()
                            + "/specs"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers
                            .ofString(json.toString()))
                    .build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                showAlert("Success", "Specs saved.");
            }
        } catch (Exception ex) {
            showAlert("Error", ex.getMessage());
        }
    }

    // ── Helper loaders ────────────────────────────────────
    private Map<String, String> loadFullAsset() {
        Map<String, String> result = new HashMap<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/assets/" + asset.getId()))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().replace("{", "")
                    .replace("}", "");
            for (String field : body.split(",")) {
                if (field.contains(":")) {
                    String[] parts = field.split(":", 2);
                    String key = parts[0].trim()
                            .replace("\"", "");
                    String val = parts[1].trim()
                            .replace("\"", "");
                    result.put(key, val);
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private Map<String, String> loadSpecs() {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/assets/" + asset.getId()
                            + "/specs"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
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
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/employees/" + empId))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            return extractValue(resp.body(), "name");
        } catch (Exception e) {
            return "Employee #" + empId;
        }
    }

    private String loadDeptName() {
        Map<String, String> full = loadFullAsset();
        String deptId = full.getOrDefault(
                "departmentId", "0");
        if ("0".equals(deptId) || deptId.isEmpty())
            return "-";
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/departments/" + deptId))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            return extractValue(resp.body(), "name");
        } catch (Exception e) {
            return "Dept #" + deptId;
        }
    }

    private String loadVendorName() {
        Map<String, String> full = loadFullAsset();
        String vendorId = full.getOrDefault("vendorId", "0");
        if ("0".equals(vendorId) || vendorId.isEmpty())
            return "-";
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/vendors/" + vendorId))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            return extractValue(resp.body(), "name");
        } catch (Exception e) {
            return "Vendor #" + vendorId;
        }
    }

    private String loadAssetField(String field) {
        return loadFullAsset().getOrDefault(field, "-");
    }

    private double loadAssetDouble(String field) {
        try {
            return Double.parseDouble(
                    loadFullAsset().getOrDefault(field, "0"));
        } catch (Exception e) { return 0.0; }
    }

    // ── UI Helpers ────────────────────────────────────────
    private Label sectionHeader(String text) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-text-fill: #e6edf3;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12 20 4 20;");
        return label;
    }

    private HBox sectionHeaderWithBtn(String text,
                                      Button btn) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-text-fill: #e6edf3;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(label, spacer, btn);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 20, 4, 20));
        return box;
    }

    private Separator divider() {
        Separator sep = new Separator();
        sep.setStyle(
                "-fx-background-color: #21262d;");
        return sep;
    }

    private void addGridRow(GridPane grid, int row,
                            String label, String value) {
        Label k = new Label(label + ":");
        k.setStyle(
                "-fx-text-fill: #8b949e;" +
                        "-fx-font-size: 12px;" +
                        "-fx-min-width: 120;");
        Label v = new Label(
                value != null && !value.isEmpty()
                        && !value.equals("0")
                        ? value : "-");
        v.setStyle(
                "-fx-text-fill: #e6edf3;" +
                        "-fx-font-size: 13px;");
        v.setWrapText(true);
        grid.add(k, 0, row);
        grid.add(v, 1, row);
    }

    private HBox specRow(String key, String value) {
        Label k = new Label(key + ":");
        k.setStyle(
                "-fx-text-fill: #8b949e;" +
                        "-fx-font-size: 11px;" +
                        "-fx-min-width: 140;");
        Label v = new Label(
                value != null && !value.isEmpty()
                        ? value : "-");
        v.setStyle(
                "-fx-text-fill: #58a6ff;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;");
        HBox row = new HBox(8, k, v);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String statusStyle(String status) {
        String bg = switch (status) {
            case "Active"    -> "#1b2d1f";
            case "In Repair" -> "#2d2008";
            case "Retired"   -> "#21262d";
            case "Disposed"  -> "#3d1f1e";
            default          -> "#21262d";
        };
        String fg = switch (status) {
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
        alert.showAndWait();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "'")
                .replace("\n", " ")
                .replace("\r", "");
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