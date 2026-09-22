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

public class DepartmentView {

    public VBox getView() {
        Label bcRoot = new Label("ORGANIZATION");
        bcRoot.getStyleClass().add("breadcrumb-root");
        Label bcSep = new Label("  /  ");
        bcSep.getStyleClass().add("breadcrumb-sep");
        Label bcCurrent = new Label("DEPARTMENTS");
        bcCurrent.getStyleClass().add("breadcrumb-current");
        HBox breadcrumb = new HBox(bcRoot, bcSep, bcCurrent);

        Label title = new Label("Departments");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Organizational units used across assets and employees.");
        subtitle.getStyleClass().add("page-subtitle");

        Label rightClickHint = new Label("Right-click anywhere for New / Import.");
        rightClickHint.getStyleClass().add("text-muted");
        rightClickHint.setStyle("-fx-font-size: 11px;");

        HBox titleRow = new HBox(4, new VBox(4, title, subtitle));
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox filterBar = new HBox(10, rightClickHint);
        filterBar.getStyleClass().add("filter-bar");

        TableView<Department> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> {
            TableRow<Department> row = new TableRow<>();

            ContextMenu rowMenu = new ContextMenu();
            MenuItem editItem = new MenuItem("✏ Edit Department");
            editItem.setOnAction(e -> {
                if (PermissionManager.canAddDepartment()) {
                    showFullEditDialog(row.getItem(), table);
                } else {
                    showAlert("Access Denied", "You don't have permission to edit departments.");
                }
            });
            rowMenu.getItems().add(editItem);
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu)
            );

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    if (PermissionManager.canAddDepartment()) {
                        showFullEditDialog(row.getItem(), table);
                    } else {
                        showAlert("Access Denied", "You don't have permission to edit departments.");
                    }
                }
            });
            return row;
        });

        TableColumn<Department, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Department, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Department, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLocation()));

        table.getColumns().addAll(idCol, nameCol, locationCol);

        Button addBtn = new Button("+ Add Department");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddDialog(table));
        addBtn.setVisible(PermissionManager.canAddDepartment());
        addBtn.setManaged(PermissionManager.canAddDepartment());
        AnimationUtil.addHoverScale(addBtn);

        Button importBtn = new Button("⬆ Import CSV");
        importBtn.getStyleClass().add("btn-primary");
        importBtn.setVisible(PermissionManager.canAddDepartment());
        importBtn.setManaged(PermissionManager.canAddDepartment());
        AnimationUtil.addHoverScale(importBtn);
        importBtn.setOnAction(e -> {
            CsvImporter.importCsvAsync("Import Departments CSV", true, fields -> {
                if (fields.length < 2)
                    throw new Exception("Expected 2 columns");
                if (!ValidationUtil.isNotBlank(fields[0]))
                    throw new Exception("Name is required");
                String body = "{" +
                        "\"name\":\"" + escape(fields[0]) + "\"," +
                        "\"location\":\"" + escape(fields[1]) + "\"" +
                        "}";
                HttpResponse<String> resp = ApiClient.post(ConfigManager.getBaseUrl() + "/api/departments", body);
                if (resp.statusCode() != 201)
                    throw new Exception("Server returned " + resp.statusCode());
            }, result -> loadDepartments(table));
        });

        VBox tableWrapper = new VBox(table);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);

        loadDepartments(table);

        VBox root = new VBox(12, breadcrumb, titleRow, filterBar, tableWrapper);

        ContextMenu screenMenu = new ContextMenu();
        screenMenu.setAutoHide(true);
        if (PermissionManager.canAddDepartment()) {
            MenuItem newItem = new MenuItem("+ New Department");
            newItem.setOnAction(e -> showAddDialog(table));
            screenMenu.getItems().add(newItem);

            MenuItem importItem = new MenuItem("⬆ Import Departments");
            importItem.setOnAction(e -> importBtn.fire());
            screenMenu.getItems().add(importItem);
        }
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
            if (!clickedOnRow && !screenMenu.getItems().isEmpty()) {
                screenMenu.show(root, e.getScreenX(), e.getScreenY());
            } else {
                screenMenu.hide();
            }
        });

        return root;
    }

    private void showFullEditDialog(Department dept, TableView<Department> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Edit Department");
        dialog.setHeaderText(dept.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField(dept.getName());
        TextField locationField = new TextField(dept.getLocation());
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(100);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

        grid.add(new Label("Name *:"), 0, 0);    grid.add(nameField, 1, 0);
        grid.add(new Label("Location:"), 0, 1);  grid.add(locationField, 1, 1);
        grid.add(errorLabel, 1, 2);
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
                        "\"id\":" + dept.getId() + "," +
                        "\"name\":\"" + escape(nameField.getText()) + "\"," +
                        "\"location\":\"" + escape(locationField.getText()) + "\"" +
                        "}";
                HttpResponse<String> resp = ApiClient.put(
                        ConfigManager.getBaseUrl() + "/api/departments/" + dept.getId(), body);
                if (resp.statusCode() == 200) {
                    ToastUtil.success("Department updated.");
                    loadDepartments(table);
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        }
    }

    private void showAddDialog(TableView<Department> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Add Department");
        dialog.setHeaderText("Enter department details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField();
        TextField locationField = new TextField();
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(100);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

        grid.add(new Label("Name *:"), 0, 0);   grid.add(nameField, 1, 0);
        grid.add(new Label("Location:"), 0, 1); grid.add(locationField, 1, 1);
        grid.add(errorLabel, 1, 2);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        nameField.textProperty().addListener((o, ov, nv) -> okButton.setDisable(nv.trim().isEmpty()));

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!ValidationUtil.isNotBlank(nameField.getText())) {
                errorLabel.setText("Department Name is required.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String body = "{" +
                    "\"name\":\"" + escape(nameField.getText()) + "\"," +
                    "\"location\":\"" + escape(locationField.getText()) + "\"" +
                    "}";
            try {
                HttpResponse<String> response = ApiClient.post(ConfigManager.getBaseUrl() + "/api/departments", body);
                if (response.statusCode() == 201) {
                    ToastUtil.success("Department added.");
                    loadDepartments(table);
                } else {
                    showAlert("Error", "Server returned: " + response.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private void loadDepartments(TableView<Department> table) {
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading departments...");

        Task<List<Department>> task = new Task<>() {
            @Override
            protected List<Department> call() throws Exception {
                List<Department> result = new ArrayList<>();
                String url = ConfigManager.getBaseUrl() + "/api/departments";
                HttpResponse<String> response = ApiClient.get(url);
                String body = response.body().trim();

                if (body.equals("[]") || body.isEmpty()) return result;
                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    for (String obj : body.split("\\},\\s*\\{")) {
                        String cleanedObj = obj.replace("{", "").replace("}", "");
                        result.add(new Department(
                                extractInt(cleanedObj, "id"),
                                extractValue(cleanedObj, "name"),
                                extractValue(cleanedObj, "location")
                        ));
                    }
                }
                return result;
            }
        };

        task.setOnSucceeded(e -> {
            List<Department> departments = task.getValue();
            table.getItems().addAll(departments);
            if (departments.isEmpty()) {
                LoadingUtil.setEmpty(table, "🏢", "No departments found", "Add your first department.");
            }
        });

        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠", "Could not load departments", "Check server connection and try again.");
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
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
        return s.replace("\\", "\\\\").replace("\"", "'");
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