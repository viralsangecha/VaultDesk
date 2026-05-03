package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class AssetView {

    private ObservableList<Asset> allAssets = FXCollections.observableArrayList();

    public VBox getView() {

        // ── Breadcrumb ────────────────────────────────────
        Label bcRoot = new Label("INVENTORY");
        bcRoot.getStyleClass().add("breadcrumb-root");
        Label bcSep = new Label("  /  ");
        bcSep.getStyleClass().add("breadcrumb-sep");
        Label bcCurrent = new Label("HARDWARE ASSETS");
        bcCurrent.getStyleClass().add("breadcrumb-current");
        HBox breadcrumb = new HBox(bcRoot, bcSep, bcCurrent);

        // ── Title row ─────────────────────────────────────
        Label title = new Label("Asset Inventory");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Managing all hardware units across departments.");
        subtitle.getStyleClass().add("page-subtitle");

        // ── Add button ────────────────────────────────────
        Button addBtn = new Button("＋  New Asset");
        addBtn.getStyleClass().setAll("btn-primary");
        addBtn.setStyle(
                "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 8 16 8 16;" +
                        "-fx-font-weight: bold; -fx-font-size: 13px; -fx-cursor: hand;");

        Button importBtn = new Button("⬆ Import CSV");
        importBtn.getStyleClass().setAll("btn-primary");
        importBtn.setStyle(
                "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 8 14 8 14;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        HBox titleRow = new HBox(12,
                new VBox(4, title, subtitle),
                titleSpacer);

// Role-based buttons — add once only
        if (SessionManager.get().isAdmin()
                || SessionManager.get().isDeptHod()) {
            titleRow.getChildren().addAll(importBtn, addBtn);
        }
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // ── Filter bar ────────────────────────────────────
        Label filterLabel = new Label("Filters:");
        filterLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");

        ComboBox<String> statusFilter = new ComboBox<>();
        statusFilter.getItems().addAll("ALL STATUSES", "Active",
                "In Repair", "Retired", "Disposed");
        statusFilter.setValue("ALL STATUSES");
        statusFilter.getStyleClass().add("filter-combo");

        ComboBox<String> categoryFilter = new ComboBox<>();
        categoryFilter.getItems().addAll("ALL CATEGORIES", "PC", "Laptop",
                "Server", "Printer", "Switch", "Router", "UPS", "Mobile", "Other");
        categoryFilter.setValue("ALL CATEGORIES");
        categoryFilter.getStyleClass().add("filter-combo");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name or tag...");
        searchField.getStyleClass().add("search-bar");
        searchField.setPrefWidth(240);

        HBox filterBar = new HBox(10, filterLabel, statusFilter,
                categoryFilter, searchField);
        filterBar.getStyleClass().add("filter-bar");

        // ── Table ─────────────────────────────────────────
        TableView<Asset> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Asset, String> assetTagCol = new TableColumn<>("ASSET TAG");
        assetTagCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getAssetTag()));


        importBtn.getStyleClass().setAll("btn-primary");
        importBtn.setStyle(
                "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 8 14 8 14;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");
        importBtn.setOnAction(e -> {
            CsvImporter.importCsv("Import Assets CSV", true, fields -> {
                // CSV columns: assetTag,name,category,brand,model,
                //              serialNumber,departmentId,location,
                //              status,purchaseCost,notes
                if (fields.length < 11)
                    throw new Exception("Expected 11 columns");
                String body = "{" +
                        "\"assetTag\":\"" + fields[0] + "\"," +
                        "\"name\":\"" + fields[1] + "\"," +
                        "\"category\":\"" + fields[2] + "\"," +
                        "\"brand\":\"" + fields[3] + "\"," +
                        "\"model\":\"" + fields[4] + "\"," +
                        "\"serialNumber\":\"" + fields[5] + "\"," +
                        "\"departmentId\":" + (fields[6].isEmpty() ? 0
                        : Integer.parseInt(fields[6])) + "," +
                        "\"location\":\"" + fields[7] + "\"," +
                        "\"status\":\"" + fields[8] + "\"," +
                        "\"assignedTo\":0," +
                        "\"assignedDate\":null," +
                        "\"purchaseDate\":null," +
                        "\"warrantyExpiry\":null," +
                        "\"vendorId\":0," +
                        "\"purchaseCost\":" + (fields[9].isEmpty() ? 0.0
                        : Double.parseDouble(fields[9])) + "," +
                        "\"notes\":\"" + fields[10] + "\"" +
                        "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigManager.getBaseUrl() + "/api/assets"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 201)
                    throw new Exception("Server returned " + resp.statusCode());
            });
            loadAssets(table);
        });
        // Category — colored text
        TableColumn<Asset, String> categoryCol = new TableColumn<>("CATEGORY");
        categoryCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCategory()));
        categoryCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item.toUpperCase()) {
                    case "PC"        -> setStyle("-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
                    case "LAPTOP"    -> setStyle("-fx-text-fill: #a371f7; -fx-font-weight: bold;");
                    case "SERVER"    -> setStyle("-fx-text-fill: #39d353; -fx-font-weight: bold;");
                    case "PRINTER"   -> setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
                    case "SWITCH"    -> setStyle("-fx-text-fill: #f0883e; -fx-font-weight: bold;");
                    case "ROUTER"    -> setStyle("-fx-text-fill: #f0883e; -fx-font-weight: bold;");
                    case "UPS"       -> setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold;");
                    default          -> setStyle("-fx-text-fill: #c9d1d9; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<Asset, String> nameCol = new TableColumn<>("NAME");
        nameCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getName()));

        TableColumn<Asset, String> brandCol = new TableColumn<>("BRAND");
        brandCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getBrand()));

        TableColumn<Asset, String> locationCol = new TableColumn<>("LOCATION");
        locationCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getLocation()));

        // Status — colored text
        TableColumn<Asset, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "Active"    -> setStyle("-fx-text-fill: #3fb950; -fx-font-weight: bold;");
                    case "In Repair" -> setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
                    case "Retired"   -> setStyle("-fx-text-fill: #8b949e;");
                    case "Disposed"  -> setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
                    default          -> setStyle("-fx-text-fill: #c9d1d9;");
                }
            }
        });

        // Actions
        TableColumn<Asset, Void> actionCol = new TableColumn<>("ACTIONS");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit Status");
            {
                editBtn.getStyleClass().setAll("btn-warning");
                editBtn.setStyle(
                        "-fx-background-color: #b45309; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 5 12 5 12;" +
                                "-fx-font-weight: bold; -fx-font-size: 12px;");
                editBtn.setOnAction(e -> {
                    Asset asset = getTableView().getItems().get(getIndex());
                    showEditStatusDialog(asset, getTableView());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : editBtn);
            }
        });

        table.getColumns().addAll(assetTagCol, categoryCol, nameCol,
                brandCol, locationCol, statusCol, actionCol);

        // ── Filter logic ──────────────────────────────────
        statusFilter.setOnAction(e -> applyFilters(table,
                searchField.getText(), statusFilter.getValue(),
                categoryFilter.getValue()));
        categoryFilter.setOnAction(e -> applyFilters(table,
                searchField.getText(), statusFilter.getValue(),
                categoryFilter.getValue()));
        searchField.textProperty().addListener((obs, ov, nv) -> applyFilters(
                table, nv, statusFilter.getValue(), categoryFilter.getValue()));
        addBtn.setOnAction(e -> showAddDialog(table));

        loadAssets(table);

        VBox root = new VBox(12, breadcrumb, titleRow, filterBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    private void applyFilters(TableView<Asset> table, String search,
                              String status, String category) {
        table.getItems().setAll(allAssets.filtered(a -> {
            boolean ms = search == null || search.isEmpty()
                    || a.getName().toLowerCase().contains(search.toLowerCase())
                    || a.getAssetTag().toLowerCase().contains(search.toLowerCase());
            boolean mst = "ALL STATUSES".equals(status) || status.equals(a.getStatus());
            boolean mc  = "ALL CATEGORIES".equals(category)
                    || category.equalsIgnoreCase(a.getCategory());
            return ms && mst && mc;
        }));
    }

    private void loadAssets(TableView<Asset> table) {
        table.getItems().clear();
        allAssets.clear();
        try {
            String url = SessionManager.get().isDeptHod()
                    ? ConfigManager.getBaseUrl() + "/api/assets/department/"
                    + SessionManager.get().getDeptId()
                    : ConfigManager.getBaseUrl() + "/api/assets";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);
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
                            extractValue(obj, "location")
                    );
                    allAssets.add(a);
                    table.getItems().add(a);
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
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField assetTagField  = new TextField();
        TextField nameField      = new TextField();
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("PC", "Laptop", "Server", "Printer",
                "Switch", "Router", "UPS", "Mobile", "Other");
        categoryBox.setValue("PC");
        TextField brandField    = new TextField();
        TextField modelField    = new TextField();
        TextField serialField   = new TextField();
        TextField deptField     = new TextField();
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
        grid.add(new Label("Asset Tag *:"), 0, 0);  grid.add(assetTagField,  1, 0);
        grid.add(new Label("Name *:"),      0, 1);  grid.add(nameField,      1, 1);
        grid.add(new Label("Category *:"),  0, 2);  grid.add(categoryBox,    1, 2);
        grid.add(new Label("Brand *:"),     0, 3);  grid.add(brandField,     1, 3);
        grid.add(new Label("Model:"),       0, 4);  grid.add(modelField,     1, 4);
        grid.add(new Label("Serial No:"),   0, 5);  grid.add(serialField,    1, 5);
        grid.add(new Label("Dept ID:"),     0, 6);  grid.add(deptField,      1, 6);
        grid.add(new Label("Location:"),    0, 7);  grid.add(locationField,  1, 7);
        grid.add(new Label("Status:"),      0, 8);  grid.add(statusBox,      1, 8);
        grid.add(new Label("Cost:"),        0, 9);  grid.add(costField,      1, 9);
        grid.add(new Label("Notes:"),       0, 10); grid.add(notesField,     1, 10);
        grid.add(errorLabel,                1, 11);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable check = () -> okButton.setDisable(
                assetTagField.getText().trim().isEmpty()
                        || nameField.getText().trim().isEmpty()
                        || brandField.getText().trim().isEmpty());

        assetTagField.textProperty().addListener((o, ov, nv) -> check.run());
        nameField.textProperty().addListener((o, ov, nv) -> check.run());
        brandField.textProperty().addListener((o, ov, nv) -> check.run());

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String err = validateAsset(assetTagField.getText(),
                    nameField.getText(), brandField.getText(),
                    costField.getText(), deptField.getText());
            if (err != null) { errorLabel.setText(err); event.consume(); }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int deptId = deptField.getText().trim().isEmpty() ? 0
                        : Integer.parseInt(deptField.getText().trim());
                double cost = costField.getText().trim().isEmpty() ? 0.0
                        : Double.parseDouble(costField.getText().trim());
                if (saveAsset(assetTagField.getText(), nameField.getText(),
                        categoryBox.getValue(), brandField.getText(),
                        modelField.getText(), serialField.getText(),
                        deptId, locationField.getText(),
                        statusBox.getValue(), cost, notesField.getText()))
                    loadAssets(table);
            } catch (NumberFormatException ex) {
                showAlert("Error", "Dept ID and Cost must be numbers.");
            }
        }
    }

    private String validateAsset(String tag, String name, String brand,
                                 String cost, String deptId) {
        if (tag.trim().isEmpty())   return "Asset Tag is required.";
        if (name.trim().isEmpty())  return "Asset Name is required.";
        if (brand.trim().isEmpty()) return "Brand is required.";
        if (!cost.trim().isEmpty()) {
            try { Double.parseDouble(cost.trim()); }
            catch (NumberFormatException e) { return "Cost must be a valid number."; }
        }
        if (!deptId.trim().isEmpty()) {
            try { Integer.parseInt(deptId.trim()); }
            catch (NumberFormatException e) { return "Dept ID must be a number."; }
        }
        return null;
    }

    private void showEditStatusDialog(Asset asset, TableView<Asset> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Asset Status");
        dialog.setHeaderText("Asset: " + asset.getName());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Active", "In Repair", "Retired", "Disposed");
        statusBox.setValue(asset.getStatus());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Status:"), 0, 0);
        grid.add(statusBox, 1, 0);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (updateAssetStatus(asset.getId(), statusBox.getValue()))
                loadAssets(table);
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
                    .uri(URI.create(ConfigManager.getBaseUrl() + "/api/assets"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                showAlert("Success", "Asset added.");
                return true;
            }
            showAlert("Error", "Server returned: " + response.statusCode());
            return false;
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private boolean updateAssetStatus(int id, String status) {
        try {
            String url = ConfigManager.getBaseUrl() + "/api/assets/" + id
                    + "/status?status="
                    + URLEncoder.encode(status, StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                showAlert("Success", "Status updated.");
                return true;
            }
            showAlert("Error", "Server returned: " + response.statusCode());
            return false;
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