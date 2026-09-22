package com.vaultdesk.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


public class AssetView {

    private ObservableList<Asset> allAssets = FXCollections.observableArrayList();
    private TableView<Asset> table;
    private final Map<Integer, String> deptMap = new LinkedHashMap<>();
    private Integer selectedDeptId = 0;
    private Stage detailStage = null;

    public VBox getView() {

        Label bcRoot = new Label("INVENTORY");
        bcRoot.getStyleClass().add("breadcrumb-root");
        Label bcSep = new Label("  /  ");
        bcSep.getStyleClass().add("breadcrumb-sep");
        Label bcCurrent = new Label("HARDWARE ASSETS");
        bcCurrent.getStyleClass().add("breadcrumb-current");
        HBox breadcrumb = new HBox(bcRoot, bcSep, bcCurrent);

        Label title = new Label("Asset Inventory");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label(
                "Managing all hardware units across departments.");
        subtitle.getStyleClass().add("page-subtitle");

        Button addBtn = new Button("＋  New Asset");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddDialog());


        Button exportBtn = new Button("⬇ Export");
        exportBtn.getStyleClass().add("btn-export");
        exportBtn.setOnAction(e -> exportAssets());

        Button importBtn = new Button("⬆ Import CSV");
        importBtn.getStyleClass().add("btn-primary");
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        HBox titleRow = new HBox(12,
                new VBox(4, title, subtitle), titleSpacer);

        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // ── Filter bar ────────────────────────────────────
        Label filterLabel = new Label("Filters:");
        filterLabel.getStyleClass().add("text-muted");
        filterLabel.setStyle("-fx-font-size: 12px;");
        Label rightClickHint = new Label("Right-click anywhere for New / Import / Export.");
        rightClickHint.getStyleClass().add("text-muted");
        rightClickHint.setStyle("-fx-font-size: 11px;");

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("ALL STATUSES", "Active",
                "In Repair", "Retired", "Disposed");
        statusFilter.setValue("ALL STATUSES");
        statusFilter.getStyleClass().add("filter-combo");

        ComboBox<String> categoryFilter = new ComboBox<>();
        categoryFilter.getItems().addAll("ALL CATEGORIES", "PC",
                "Laptop", "Server", "Printer", "Switch",
                "Router", "UPS", "Mobile", "Other");
        categoryFilter.setValue("ALL CATEGORIES");
        categoryFilter.getStyleClass().add("filter-combo");


        // ── Table ─────────────────────────────────────────
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> {
            TableRow<Asset> row = new TableRow<>();

            ContextMenu rowMenu = new ContextMenu();
            MenuItem viewItem = new MenuItem("🔍 View Details");
            viewItem.setOnAction(e -> {
                if (PermissionManager.canViewAssetDetails()) {
                    openAssetDetail(row.getItem());
                } else {
                    showAlert("Access Denied", "You don't have permission to view asset details.\nContact your administrator.");
                }
            });
            rowMenu.getItems().add(viewItem);
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu)
            );

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    if (PermissionManager.canViewAssetDetails())
                    {
                        openAssetDetail(row.getItem());
                    }
                    else {
                        showAlert("Access Denied",
                                "You don't have permission to view asset details.\nContact your administrator.");
                    }
                }
            });
            return row;
        });

        TableColumn<Asset, String> assetTagCol =
                new TableColumn<>("ASSET TAG");
        assetTagCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getAssetTag()));
        assetTagCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); getStyleClass().remove("data-mono"); return; }
                setText(item);
                if (!getStyleClass().contains("data-mono")) getStyleClass().add("data-mono");
            }
        });

        TableColumn<Asset, String> categoryCol =
                new TableColumn<>("CATEGORY");
        categoryCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCategory()));
        categoryCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle(""); return;
                }
                setText(item);
                switch (item.toUpperCase()) {
                    case "PC"      -> setStyle(
                            "-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
                    case "LAPTOP"  -> setStyle(
                            "-fx-text-fill: #a371f7; -fx-font-weight: bold;");
                    case "SERVER"  -> setStyle(
                            "-fx-text-fill: #39d353; -fx-font-weight: bold;");
                    case "PRINTER" -> setStyle(
                            "-fx-text-fill: #d29922; -fx-font-weight: bold;");
                    case "SWITCH"  -> setStyle(
                            "-fx-text-fill: #f0883e; -fx-font-weight: bold;");
                    case "ROUTER"  -> setStyle(
                            "-fx-text-fill: #f0883e; -fx-font-weight: bold;");
                    case "UPS"     -> setStyle(
                            "-fx-text-fill: #8b949e; -fx-font-weight: bold;");
                    default        -> setStyle(
                            "-fx-text-fill: #c9d1d9; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<Asset, String> nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getName()));

        TableColumn<Asset, String> brandCol = new TableColumn<>("BRAND");
        brandCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getBrand()));

        TableColumn<Asset, String> locationCol =
                new TableColumn<>("LOCATION");
        locationCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getLocation()));

        TableColumn<Asset, String> statusCol =
                new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle(""); return;
                }
                setText(item);
                switch (item) {
                    case "Active"    -> setStyle(
                            "-fx-text-fill: #3fb950; -fx-font-weight: bold;");
                    case "In Repair" -> setStyle(
                            "-fx-text-fill: #d29922; -fx-font-weight: bold;");
                    case "Retired"   -> setStyle(
                            "-fx-text-fill: #8b949e;");
                    case "Disposed"  -> setStyle(
                            "-fx-text-fill: #f85149; -fx-font-weight: bold;");
                    default          -> setStyle(
                            "-fx-text-fill: #c9d1d9;");
                }
            }
        });

        // ── Bulk action bar ───────────────────────────────────
        Label bulkLabel = new Label("Bulk Actions:");
        bulkLabel.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 12px;");

        ComboBox<String> bulkStatusBox = new ComboBox<>();
        bulkStatusBox.getItems().addAll(
                "Active", "In Repair", "Retired", "Disposed");
        bulkStatusBox.setPromptText("Select status...");
        bulkStatusBox.setStyle(
                "-fx-background-color: #21262d;" +
                        "-fx-text-fill: #c9d1d9;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-radius: 6;");

        Button bulkApplyBtn = new Button("Apply to Selected");
        bulkApplyBtn.getStyleClass().setAll("btn-warning");
        bulkApplyBtn.setStyle(
                "-fx-background-color: #b45309; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 6 14 6 14;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");

        Label bulkResultLabel = new Label("");
        bulkResultLabel.setStyle(
                "-fx-text-fill: #3fb950; -fx-font-size: 11px;");

        HBox bulkBar = new HBox(10, bulkLabel, bulkStatusBox,
                bulkApplyBtn, bulkResultLabel);
        bulkBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bulkBar.getStyleClass().add("filter-bar");

// ── Enable multi select ───────────────────────────────
        table.getSelectionModel().setSelectionMode(
                SelectionMode.MULTIPLE);

        bulkApplyBtn.setOnAction(e -> {
            String status = bulkStatusBox.getValue();
            if (status == null || status.isEmpty()) {
                bulkResultLabel.setText("Select a status first.");
                bulkResultLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 11px;");
                return;
            }
            var selected = table.getSelectionModel()
                    .getSelectedItems();
            if (selected.isEmpty()) {
                bulkResultLabel.setText("Select assets first.");
                bulkResultLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 11px;");
                return;
            }
            int success = 0;
            for (Asset a : selected) {
                if (updateAssetStatus(a.getId(), status)) success++;
            }
            int finalSuccess = success;
            bulkResultLabel.setText(
                    "✔ Updated " + finalSuccess + " assets.");
            bulkResultLabel.setStyle(
                    "-fx-text-fill: #3fb950; -fx-font-size: 11px;");
            loadAssets();
            table.getSelectionModel().clearSelection();
        });
        table.getColumns().addAll(assetTagCol, categoryCol,
                nameCol, brandCol, locationCol, statusCol);

        // ── Filter logic ──────────────────────────────────
        statusFilter.setOnAction(e -> applyFilters(
                statusFilter.getValue(), categoryFilter.getValue()));
        categoryFilter.setOnAction(e -> applyFilters(
                statusFilter.getValue(), categoryFilter.getValue()));

        importBtn.setOnAction(e -> {
            CsvImporter.importCsvAsync("Import Assets CSV", true, fields -> {
                if (fields.length < 16)
                    throw new Exception("Expected 16 columns, got " + fields.length);
                String body = "{" +
                        "\"assetTag\":\"" + escapeJson(fields[0]) + "\"," +
                        "\"name\":\"" + escapeJson(fields[1]) + "\"," +
                        "\"category\":\"" + escapeJson(fields[2]) + "\"," +
                        "\"brand\":\"" + escapeJson(fields[3]) + "\"," +
                        "\"model\":\"" + escapeJson(fields[4]) + "\"," +
                        "\"serialNumber\":\"" + escapeJson(fields[5]) + "\"," +
                        "\"departmentId\":" + (fields[6].isEmpty() ? 0 : Integer.parseInt(fields[6])) + "," +
                        "\"location\":\"" + escapeJson(fields[7]) + "\"," +
                        "\"status\":\"" + (fields[8].isEmpty() ? "Active" : fields[8]) + "\"," +
                        "\"assignedTo\":" + (fields[9].isEmpty() ? 0 : Integer.parseInt(fields[9])) + "," +
                        "\"assignedDate\":" + (fields[10].isEmpty() ? "null" : "\"" + fields[10] + "\"") + "," +
                        "\"purchaseDate\":" + (fields[11].isEmpty() ? "null" : "\"" + fields[11] + "\"") + "," +
                        "\"warrantyExpiry\":" + (fields[12].isEmpty() ? "null" : "\"" + fields[12] + "\"") + "," +
                        "\"vendorId\":" + (fields[13].isEmpty() ? 0 : Integer.parseInt(fields[13])) + "," +
                        "\"purchaseCost\":" + (fields[14].isEmpty() ? 0.0 : Double.parseDouble(fields[14])) + "," +
                        "\"notes\":\"" + escapeJson(fields[15]) + "\"" +
                        "}";
                HttpResponse<String> resp = ApiClient.post(ConfigManager.getBaseUrl() + "/api/assets", body);
                if (resp.statusCode() != 201)
                    throw new Exception("Server returned " + resp.statusCode() + ": " + resp.body());
            }, result -> loadAssets());
        });

        // ── Department Filter Bar ────────────────────────────
        ComboBox<String> deptFilterBox = new ComboBox<>();
        deptFilterBox.getStyleClass().add("filter-combo");

        loadAssets();
        loadDepartments(deptFilterBox);

        // Listener to trigger data reload when a department is selected
        deptFilterBox.setOnAction(e -> {
            String selected = deptFilterBox.getValue();
            if (selected != null) {
                // Find ID from the selected String name
                selectedDeptId = deptMap.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(selected))
                        .map(Map.Entry::getKey)
                        .findFirst().orElse(0);

                    loadAssets();
            }
        });

        HBox deptbar = new HBox(10, deptFilterBox);
        deptbar.setAlignment(Pos.CENTER_LEFT);


        // ── Hide filter for HODs ────────────────────────
        if (SessionManager.get().isDeptHod()) {
            bulkBar.setVisible(false);
        }

        HBox filterBar = new HBox(10, filterLabel, statusFilter, categoryFilter,deptbar,rightClickHint);
        filterBar.getStyleClass().add("filter-bar");

        VBox tableWrapper = new VBox(table);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);

        VBox root = new VBox(12, breadcrumb, titleRow, filterBar, /*bulkBar,*/ tableWrapper);

        ContextMenu screenMenu = new ContextMenu();
        screenMenu.setAutoHide(true);
        if (PermissionManager.canAddAsset()) {
            MenuItem newAssetItem = new MenuItem("＋ New Asset");
            newAssetItem.setOnAction(e -> showAddDialog());
            screenMenu.getItems().add(newAssetItem);
        }
        if (PermissionManager.canImportAssets()) {
            MenuItem importItem = new MenuItem("⬆ Import Assets");
            importItem.setOnAction(e -> importBtn.fire());
            screenMenu.getItems().add(importItem);
        }
        MenuItem exportItem = new MenuItem("⬇ Export Assets");
        exportItem.setOnAction(e -> exportAssets());
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
    // ── Load departments into map and update ComboBox ────────────────────
    private void loadDepartments(ComboBox<String> deptFilterBox) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/departments");
                String body = resp.body().trim();

                deptMap.clear();
                deptMap.put(0, "All Departments"); // Default fallback option

                if (body.equals("[]") || body.isEmpty()) return null;

                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    for (String obj : body.split("\\},\\s*\\{")) {
                        obj = obj.replace("{", "").replace("}", "");
                        int id = extractInt(obj, "id");
                        // Assuming your department JSON uses "name" or "departmentName"
                        String name = extractValue(obj, "name");
                        if (name.isEmpty()) name = extractValue(obj, "departmentName");
                        deptMap.put(id, name);
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            deptFilterBox.getItems().clear();
            deptFilterBox.getItems().addAll(deptMap.values());
            deptFilterBox.getSelectionModel().select("All Departments");
        });

        task.setOnFailed(e -> {
            System.err.println("Failed to load departments. Fallback triggered.");
            if (task.getException() != null) {
                task.getException().printStackTrace();
            }

            // Ensure the UI still gets a fallback option
            deptMap.clear();
            deptMap.put(0, "All Departments");
            deptFilterBox.getItems().clear();
            deptFilterBox.getItems().add("All Departments");

            // Trigger the selection anyway so loadAssets() is guaranteed to run
            deptFilterBox.getSelectionModel().select("All Departments");
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ── Public search method called by DashboardView ──────
    public void search(String keyword) {
        if (keyword.isEmpty()) {
            table.getItems().setAll(allAssets);
            return;
        }
        table.getItems().setAll(allAssets.filtered(a ->
                a.getName().toLowerCase().contains(keyword)
                        || a.getAssetTag().toLowerCase().contains(keyword)
                        || a.getCategory().toLowerCase().contains(keyword)
                        || a.getBrand().toLowerCase().contains(keyword)
                        || a.getLocation().toLowerCase().contains(keyword)
        ));
        if (table.getItems().isEmpty()) {
            LoadingUtil.setEmpty(table, "🔍",
                    "No results found",
                    "No assets match \"" + keyword + "\"");
        }
    }

    private void applyFilters(String status, String category) {
        table.getItems().setAll(allAssets.filtered(a -> {
            boolean ms = "ALL STATUSES".equals(status)
                    || status.equals(a.getStatus());
            boolean mc = "ALL CATEGORIES".equals(category)
                    || category.equalsIgnoreCase(a.getCategory());
            return ms && mc;
        }));
    }

    private void exportAssets() {
        Task<Object[]> prepTask = new Task<>() {
            @Override
            protected Object[] call() throws Exception {
                Map<Integer, String> deptMap = fetchNameMap("/api/departments/all", "departmentName");
                Map<Integer, String> vendorMap = fetchNameMap("/api/vendors/all", "name");
                Map<Integer, String> empMap = fetchNameMap("/api/employees", "name");
                return new Object[]{deptMap, vendorMap, empMap};
            }
        };
        prepTask.setOnSucceeded(e -> {
            Object[] data = prepTask.getValue();
            @SuppressWarnings("unchecked") Map<Integer, String> deptMap = (Map<Integer, String>) data[0];
            @SuppressWarnings("unchecked") Map<Integer, String> vendorMap = (Map<Integer, String>) data[1];
            @SuppressWarnings("unchecked") Map<Integer, String> empMap = (Map<Integer, String>) data[2];

            List<String> headers = List.of("Asset Tag", "Name", "Category", "Brand", "Model", "Serial No",
                    "Department", "Location", "Status", "Assigned To", "Assigned Date",
                    "Purchase Date", "Warranty Expiry", "Vendor", "Purchase Cost", "Notes");

            ExcelExporter.exportWithMapping("Assets", headers, new ArrayList<>(allAssets), asset -> List.of(
                    asset.getAssetTag(), asset.getName(), asset.getCategory(), asset.getBrand(),
                    "-", asset.getSerialNumber(),
                    deptMap.getOrDefault(0, "-"), // department id not on the row model — see note below
                    asset.getLocation(), asset.getStatus(),
                    asset.getAssignedTo() == 0 ? "Unassigned" : empMap.getOrDefault(asset.getAssignedTo(), "Employee #" + asset.getAssignedTo()),
                    "-", "-", "-",
                    vendorMap.getOrDefault(0, "-"),
                    "-",
                    asset.getNotes() != null ? asset.getNotes() : ""
            ));
        });
        prepTask.setOnFailed(e -> showAlert("Error", "Could not prepare export data."));
        Thread t = new Thread(prepTask);
        t.setDaemon(true);
        t.start();
    }

    private Map<Integer, String> fetchNameMap(String path, String nameField) throws Exception {
        Map<Integer, String> map = new HashMap<>();
        HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + path);
        String body = resp.body().trim();
        if (body.length() < 2) return map;
        body = body.substring(1, body.length() - 1).trim();
        if (body.isEmpty()) return map;
        for (String obj : body.split("\\},\\s*\\{")) {
            String cleaned = obj.replace("{", "").replace("}", "");
            map.put(extractInt(cleaned, "id"), extractValue(cleaned, nameField));
        }
        return map;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "'").replace("\n", " ").replace("\r", "");
    }

    private void loadAssets() {

        LoadingUtil.setLoading(table, "Loading assets...");
        Task<List<Asset>> task = new Task<List<Asset>>() {
            @Override
            protected List<Asset> call() throws Exception {
                List<Asset> result = new ArrayList<>();

                String url;
                if (SessionManager.get().isDeptHod())
                {
                    url=ConfigManager.getBaseUrl() + "/api/assets/department/" + SessionManager.get().getDeptId();
                } else
                {
                    if (selectedDeptId != null && selectedDeptId > 0)
                    {
                    url = ConfigManager.getBaseUrl() + "/api/assets/department/"+selectedDeptId;
                    }
                    else {
                    url = ConfigManager.getBaseUrl() + "/api/assets" ;
                    }
                }

                HttpResponse<String> response = ApiClient.get(url);
                String body = response.body().trim();
                body = body.substring(1, body.length() - 1);

                // Handle empty arrays securely
                if (body.equals("[]") || body.isEmpty()) {
                    return result;
                }

                // Strip leading '[' and trailing ']'
                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    for (String obj : body.split("\\},\\{")) {
                        obj = obj.replace("{", "").replace("}", "");
                        Asset a = new Asset(
                                extractInt(obj, "id"),
                                extractValue(obj, "assetTag"),
                                extractValue(obj, "name"),
                                extractValue(obj, "category"),
                                extractValue(obj, "brand"),
                                extractValue(obj, "serialNumber"),
                                extractValue(obj, "notes"),
                                extractValue(obj, "status"),
                                extractValue(obj, "location"),
                                extractInt(obj, "assignedTo")
                        );
                        result.add(a);
                    }
                }
                return result;
            }
        };
        task.setOnSucceeded(e -> {
            List<Asset> assets = task.getValue();
            // Update UI collections on the JavaFX Application Thread
            table.getItems().clear();
            allAssets.clear();
            table.getItems().addAll(assets);
            allAssets.addAll(assets);


            if (assets.isEmpty()) {
                LoadingUtil.setEmpty(table, "▣",
                        "No assets found",
                        "Add your first asset using the button above.");
            }
        });

        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠",
                    "Could not load assets",
                    "Check server connection and try again.");
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showAddDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Add Asset");
        dialog.setHeaderText("Enter asset details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField assetTagField  = new TextField();
        TextField nameField      = new TextField();
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("PC", "Laptop", "Server",
                "Printer", "Switch", "Router", "UPS", "Mobile", "Other");
        categoryBox.setValue("PC");
        TextField brandField    = new TextField();
        TextField modelField    = new TextField();
        TextField serialField   = new TextField();

        List<PickerOption> deptOptions = deptMap.entrySet().stream()
                .filter(en -> en.getKey() != 0)
                .map(en -> new PickerOption(en.getKey(), en.getValue()))
                .collect(Collectors.toList());
        SearchablePickerField deptPicker = new SearchablePickerField(deptOptions, "Search department...");

        TextField locationField = new TextField();
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Active", "In Repair", "Retired", "Disposed");
        statusBox.setValue("Active");
        TextField costField = new TextField();
        costField.setPromptText("0.0");
        TextField notesField = new TextField();
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Asset Tag *:"), 0, 0); grid.add(assetTagField, 1, 0);
        grid.add(new Label("Name *:"),      0, 1); grid.add(nameField, 1, 1);
        grid.add(new Label("Category *:"),  0, 2); grid.add(categoryBox, 1, 2);
        grid.add(new Label("Brand *:"),     0, 3); grid.add(brandField, 1, 3);
        grid.add(new Label("Model:"),       0, 4); grid.add(modelField, 1, 4);
        grid.add(new Label("Serial No:"),   0, 5); grid.add(serialField, 1, 5);
        grid.add(new Label("Department:"),  0, 6); grid.add(deptPicker, 1, 6);
        grid.add(new Label("Location:"),    0, 7); grid.add(locationField, 1, 7);
        grid.add(new Label("Status:"),      0, 8); grid.add(statusBox, 1, 8);
        grid.add(new Label("Cost:"),        0, 9); grid.add(costField, 1, 9);
        grid.add(new Label("Notes:"),       0, 10); grid.add(notesField, 1, 10);
        grid.add(errorLabel, 1, 11);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable check = () -> okButton.setDisable(
                assetTagField.getText().trim().isEmpty()
                        || nameField.getText().trim().isEmpty()
                        || brandField.getText().trim().isEmpty());
        assetTagField.textProperty().addListener((o, ov, nv) -> check.run());
        nameField.textProperty().addListener((o, ov, nv) -> check.run());
        brandField.textProperty().addListener((o, ov, nv) -> check.run());

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String err = validateAsset(assetTagField.getText(), nameField.getText(),
                    brandField.getText(), costField.getText());
            if (err != null) {
                errorLabel.setText(err);
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                double cost = costField.getText().trim().isEmpty()
                        ? 0.0 : Double.parseDouble(costField.getText().trim());
                if (saveAsset(assetTagField.getText(), nameField.getText(), categoryBox.getValue(),
                        brandField.getText(), modelField.getText(), serialField.getText(),
                        deptPicker.getSelectedId(), locationField.getText(), statusBox.getValue(),
                        cost, notesField.getText()))
                    loadAssets();
            } catch (NumberFormatException ex) {
                showAlert("Error", "Cost must be a number.");
            }
        }
    }

    private String validateAsset(String tag, String name, String brand, String cost) {
        if (tag.trim().isEmpty())   return "Asset Tag is required.";
        if (name.trim().isEmpty())  return "Asset Name is required.";
        if (brand.trim().isEmpty()) return "Brand is required.";
        if (!cost.trim().isEmpty()) {
            try { Double.parseDouble(cost.trim()); }
            catch (NumberFormatException e) { return "Cost must be a valid number."; }
        }
        return null;
    }

    private boolean saveAsset(String assetTag, String name,
                              String category, String brand,
                              String model, String serialNumber,
                              int departmentId, String location,
                              String status, double purchaseCost,
                              String notes) {
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
            HttpResponse<String> response = ApiClient.post(ConfigManager.getBaseUrl() + "/api/assets", body);
            if (response.statusCode() == 201) {
                ToastUtil.success("Asset added.");
                return true;
            }
            showAlert("Error",
                    "Server returned: " + response.statusCode());
            return false;
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private boolean updateAssetStatus(int id, String status) {
        try {
            String url = ConfigManager.getBaseUrl()
                    + "/api/assets/" + id + "/status?status="
                    + URLEncoder.encode(status, StandardCharsets.UTF_8);
            HttpResponse<String> response = ApiClient.putNoBody(url);
            if (response.statusCode() == 200) {
                ToastUtil.success("Status updated.");
                return true;
            }
            showAlert("Error",
                    "Server returned: " + response.statusCode());
            return false;
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private void openAssetDetail(Asset asset) {
        if (detailStage != null) detailStage.close();

        detailStage = new Stage();
        detailStage.setTitle(
                "Asset Detail — " + asset.getName());
        detailStage.setWidth(800);
        detailStage.setHeight(700);
        detailStage.setResizable(true);

        AssetDetailView detail = new AssetDetailView(
                asset, () -> {
            loadAssets();
            detailStage.close();
        });

        javafx.scene.Scene scene = new javafx.scene.Scene(
                detail.getView());
        ThemeManager.apply(scene);
        detailStage.setScene(scene);
        detailStage.show();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.applyToDialog(alert);
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
            return Integer.parseInt(json.substring(
                    start, end).trim().replace("}", ""));
        } catch (NumberFormatException e) { return 0; }
    }
}