package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VendorView {

    private TableView<Vendor> table;

    public VBox getView() {
        Label bcRoot = new Label("PROCUREMENT");
        bcRoot.getStyleClass().add("breadcrumb-root");
        Label bcSep = new Label("  /  ");
        bcSep.getStyleClass().add("breadcrumb-sep");
        Label bcCurrent = new Label("VENDORS");
        bcCurrent.getStyleClass().add("breadcrumb-current");
        HBox breadcrumb = new HBox(bcRoot, bcSep, bcCurrent);

        Label title = new Label("Vendors");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Vendor and supplier contacts used across assets and licenses.");
        subtitle.getStyleClass().add("page-subtitle");

        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> {
            TableRow<Vendor> row = new TableRow<>();

            ContextMenu rowMenu = new ContextMenu();
            if (PermissionManager.canAddVendor()) {
                MenuItem editItem = new MenuItem("✏ Edit Vendor");
                editItem.setOnAction(e -> showFullEditDialog(row.getItem(), table));
                rowMenu.getItems().add(editItem);

                MenuItem toggleItem = new MenuItem();
                rowMenu.getItems().add(toggleItem);
                rowMenu.setOnShowing(ev -> {
                    Vendor v = row.getItem();
                    if (v == null) return;
                    toggleItem.setText(v.isActive() ? "🚫 Deactivate" : "✔ Reactivate");
                    toggleItem.setOnAction(e -> toggleVendorActive(v, table));
                });
            }
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu)
            );

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    if (PermissionManager.canAddVendor()) {
                        showFullEditDialog(row.getItem(), table);
                    } else {
                        showAlert("Access Denied", "You don't have permission to edit vendor.");
                    }
                }
            });
            return row;
        });

        TableColumn<Vendor, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Vendor, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Vendor, String> contactCol = new TableColumn<>("Contact Person");
        contactCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getContactPerson()));

        TableColumn<Vendor, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPhone()));

        TableColumn<Vendor, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail()));

        TableColumn<Vendor, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory()));

        TableColumn<Vendor, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("Active".equals(item)
                        ? "-fx-text-fill: #3fb950; -fx-font-weight: bold;"
                        : "-fx-text-fill: #f85149; -fx-font-weight: bold;");
            }
        });

        table.getColumns().addAll(idCol, nameCol, contactCol,
                phoneCol, emailCol, categoryCol, statusCol);

        Button addBtn = new Button("+ Add Vendor");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddDialog(table));
        addBtn.setVisible(PermissionManager.canAddVendor());
        addBtn.setManaged(PermissionManager.canAddVendor());
        AnimationUtil.addHoverScale(addBtn);

        Button exportBtn = new Button("⬇ Export");
        exportBtn.getStyleClass().add("btn-export");
        AnimationUtil.addHoverScale(exportBtn);
        exportBtn.setOnAction(e -> {
            List<String> headers = List.of(
                    "Name", "Contact Person", "Phone",
                    "Email", "Category", "Status");
            List<List<String>> rows = new ArrayList<>();
            for (Vendor v : table.getItems()) {
                rows.add(List.of(
                        v.getName(),
                        v.getContactPerson(),
                        v.getPhone(),
                        v.getEmail(),
                        v.getCategory(),
                        v.isActive() ? "Active" : "Inactive"
                ));
            }
            ExcelExporter.export("Vendors", headers, rows);
        });

        Label rightClickHint = new Label("Right-click a row or empty space for actions.");
        rightClickHint.getStyleClass().add("text-muted");
        rightClickHint.setStyle("-fx-font-size: 11px;");
        HBox topBar = new HBox(10, rightClickHint);

        loadVendors(table);

        VBox tableWrapper = new VBox(table);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);

        VBox root = new VBox(12, breadcrumb, new VBox(4, title, subtitle), topBar, tableWrapper);

        ContextMenu screenMenu = new ContextMenu();
        screenMenu.setAutoHide(true);
        if (PermissionManager.canAddVendor()) {
            MenuItem newItem = new MenuItem("+ New Vendor");
            newItem.setOnAction(e -> showAddDialog(table));
            screenMenu.getItems().add(newItem);
        }
        MenuItem exportItem = new MenuItem("⬇ Export Vendors");
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

    private void showFullEditDialog(Vendor vendor,
                                    TableView<Vendor> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Vendor Details");
        dialog.setHeaderText(vendor.getName());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField(vendor.getName());
        TextField contactField = new TextField(
                vendor.getContactPerson());
        TextField phoneField = new TextField(vendor.getPhone());
        TextField emailField = new TextField(vendor.getEmail());

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(
                "Hardware", "Software", "AMC",
                "Service", "Other");
        categoryBox.setValue(vendor.getCategory());

        TextField addressField = new TextField();
        TextField notesField = new TextField();
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        // Load full details
        try {
            HttpResponse<String> resp = ApiClient.get(
                    ConfigManager.getBaseUrl() + "/api/vendors/" + vendor.getId());
            addressField.setText(
                    extractValue(resp.body(), "address"));
            notesField.setText(
                    extractValue(resp.body(), "notes"));
        } catch (Exception ignored) {}

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(120);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

        grid.add(new Label("Name *:"), 0, 0);           grid.add(nameField, 1, 0);
        grid.add(new Label("Contact Person:"), 0, 1);   grid.add(contactField, 1, 1);
        grid.add(new Label("Phone *:"), 0, 2);          grid.add(phoneField, 1, 2);
        grid.add(new Label("Email:"), 0, 3);            grid.add(emailField, 1, 3);
        grid.add(new Label("Category:"), 0, 4);         grid.add(categoryBox, 1, 4);
        grid.add(new Label("Address:"), 0, 5);          grid.add(addressField, 1, 5);
        grid.add(new Label("Notes:"), 0, 6);            grid.add(notesField, 1, 6);
        grid.add(errorLabel, 1, 7);
        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validateVendor(nameField.getText(), phoneField.getText(), emailField.getText());
            if (error != null) {
                errorLabel.setText(error);
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String body = "{" +
                        "\"id\":" + vendor.getId() + "," +
                        "\"name\":\"" + escape(nameField.getText()) + "\"," +
                        "\"contactPerson\":\"" + escape(contactField.getText()) + "\"," +
                        "\"phone\":\"" + escape(phoneField.getText()) + "\"," +
                        "\"email\":\"" + escape(emailField.getText()) + "\"," +
                        "\"category\":\"" + categoryBox.getValue() + "\"," +
                        "\"address\":\"" + escape(addressField.getText()) + "\"," +
                        "\"notes\":\"" + escape(notesField.getText()) + "\"" +
                        "}";
                HttpResponse<String> resp = ApiClient.put(
                        ConfigManager.getBaseUrl() + "/api/vendors/" + vendor.getId(), body);
                if (resp.statusCode() == 200) {
                    ToastUtil.success("Vendor updated.");
                    loadVendors(table);
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        }
    }

    private void loadVendors(TableView<Vendor> table) {
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading vendors...");

        Task<List<Vendor>> task = new Task<>() {
            @Override
            protected List<Vendor> call() throws Exception {
                List<Vendor> result = new ArrayList<>();

                String url = ConfigManager.getBaseUrl() + "/api/vendors/all";
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

                        Vendor v = new Vendor(
                                extractInt(cleanedObj, "id"),
                                extractValue(cleanedObj, "name"),
                                extractValue(cleanedObj, "contactPerson"),
                                extractValue(cleanedObj, "phone"),
                                extractValue(cleanedObj, "email"),
                                extractValue(cleanedObj, "category"),
                                extractBoolean(cleanedObj, "active")
                        );
                        result.add(v);
                    }
                }
                return result;
            }
        };

        task.setOnSucceeded(e -> {
            List<Vendor> vendors = task.getValue();
            table.getItems().addAll(vendors);
            if (vendors.isEmpty()) {
                LoadingUtil.setEmpty(table, "🤝", "No vendors found", "Add your first vendor contact.");
            }
        });

        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠", "Could not load vendors", "Check server connection and try again.");
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void toggleVendorActive(Vendor v, TableView<Vendor> table) {
        try {
            HttpResponse<String> resp = v.isActive()
                    ? ApiClient.delete(ConfigManager.getBaseUrl() + "/api/vendors/" + v.getId())
                    : ApiClient.putNoBody(ConfigManager.getBaseUrl() + "/api/vendors/" + v.getId() + "/reactivate");
            if (resp.statusCode() == 200) {
                ToastUtil.success(v.isActive() ? "Vendor deactivated." : "Vendor reactivated.");
                loadVendors(table);
            } else {
                showAlert("Error", "Server returned: " + resp.statusCode());
            }
        } catch (Exception ex) {
            showAlert("Error", ex.getMessage());
        }
    }

    private void showAddDialog(TableView<Vendor> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Add Vendor");
        dialog.setHeaderText("Enter vendor details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField    = new TextField();
        TextField contactField = new TextField();
        TextField phoneField   = new TextField();
        TextField emailField   = new TextField();
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll("Hardware", "Software", "AMC", "Service", "Other");
        categoryBox.setValue("Hardware");
        TextField addressField = new TextField();
        TextField notesField   = new TextField();
        Label errorLabel       = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(120);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

        grid.add(new Label("Name *:"),           0, 0); grid.add(nameField,    1, 0);
        grid.add(new Label("Contact Person:"),   0, 1); grid.add(contactField, 1, 1);
        grid.add(new Label("Phone *:"),          0, 2); grid.add(phoneField,   1, 2);
        grid.add(new Label("Email:"),            0, 3); grid.add(emailField,   1, 3);
        grid.add(new Label("Category:"),         0, 4); grid.add(categoryBox,  1, 4);
        grid.add(new Label("Address:"),          0, 5); grid.add(addressField, 1, 5);
        grid.add(new Label("Notes:"),            0, 6); grid.add(notesField,   1, 6);
        grid.add(errorLabel,                     1, 7);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable checkFields = () -> {
            boolean valid = !nameField.getText().trim().isEmpty()
                    && !phoneField.getText().trim().isEmpty();
            okButton.setDisable(!valid);
        };

        nameField.textProperty().addListener((o, ov, nv) -> checkFields.run());
        phoneField.textProperty().addListener((o, ov, nv) -> checkFields.run());

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validateVendor(nameField.getText(), phoneField.getText(), emailField.getText());
            if (error != null) {
                errorLabel.setText(error);
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String body = "{" +
                    "\"name\":\"" + escape(nameField.getText()) + "\"," +
                    "\"contactPerson\":\"" + escape(contactField.getText()) + "\"," +
                    "\"phone\":\"" + escape(phoneField.getText()) + "\"," +
                    "\"email\":\"" + escape(emailField.getText()) + "\"," +
                    "\"category\":\"" + categoryBox.getValue() + "\"," +
                    "\"address\":\"" + escape(addressField.getText()) + "\"," +
                    "\"notes\":\"" + escape(notesField.getText()) + "\"" +
                    "}";
            try {
                HttpResponse<String> response = ApiClient.post(ConfigManager.getBaseUrl() + "/api/vendors", body);
                if (response.statusCode() == 201) {
                    ToastUtil.success("Vendor added.");
                    loadVendors(table);
                } else {
                    showAlert("Error", "Server returned: " + response.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private String validateVendor(String name, String phone, String email) {
        if (!ValidationUtil.isNotBlank(name))  return "Vendor Name is required.";
        if (!ValidationUtil.isNotBlank(phone)) return "Phone number is required.";
        if (!ValidationUtil.isValidPhone(phone)) return "Phone number must be exactly 10 digits.";
        if (!email.trim().isEmpty() && !ValidationUtil.isValidEmail(email))
            return "Please enter a valid email address.";
        return null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        ThemeManager.applyToDialog(alert);
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
        return json.substring(start, json.indexOf("\"", start));
    }

    private boolean extractBoolean(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return false;
        start += search.length();
        return json.startsWith("true", start) || json.startsWith("1", start);
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