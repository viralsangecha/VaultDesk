package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

public class EmployeeView {

    private TabPane tabPane;
    private TableView<Employee> activeTable;
    private TableView<Employee> inactiveTable;
    private ObservableList<Employee> allActiveEmployees = FXCollections.observableArrayList();
    private ObservableList<Employee> allInactiveEmployees = FXCollections.observableArrayList();
    private List<PickerOption> departmentOptions = new ArrayList<>();
    private static final String DEFAULT_PASSWORD = "Welcome@123";


    public VBox getView() {
        Label title = new Label("Employees");
        title.getStyleClass().add("page-title");

        loadDepartmentOptions(opts -> departmentOptions = opts);

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab activeTab   = new Tab("Active Employees");
        Tab inactiveTab = new Tab("Inactive Employees");

        activeTab.setContent(buildEmployeeTable(true));
        inactiveTab.setContent(buildEmployeeTable(false));

        activeTab.setOnSelectionChanged(event -> {
            if (activeTab.isSelected() && activeTable != null) {
                loadEmployees(activeTable, allActiveEmployees);
            }
        });

        inactiveTab.setOnSelectionChanged(event -> {
            if (inactiveTab.isSelected() && inactiveTable != null) {
                loadInactiveEmployees(inactiveTable, allInactiveEmployees);
            }
        });

        tabPane.getTabs().addAll(activeTab, inactiveTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        VBox root = new VBox(10, title, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        return root;
    }

    private VBox buildEmployeeTable(boolean active) {
        TableView<Employee> localTable = new TableView<>();
        ObservableList<Employee> localList = FXCollections.observableArrayList();
        localTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        if (active) {
            activeTable = localTable;
            allActiveEmployees = localList;
        } else {
            inactiveTable = localTable;
            allInactiveEmployees = localList;
        }

        Button exportBtn = new Button("⬇ Export");
        exportBtn.getStyleClass().add("btn-export");
        exportBtn.setOnAction(e -> exportEmployees(localList,
                active ? "Active_Employees" : "Inactive_Employees"));
        AnimationUtil.addHoverScale(exportBtn);

        Button importBtn = new Button("⬆ Import CSV");
        importBtn.getStyleClass().add("btn-primary");
        importBtn.setOnAction(e -> importEmployeesCsv(localTable, localList));
        AnimationUtil.addHoverScale(importBtn);

        localTable.setRowFactory(tv -> {
            TableRow<Employee> row = new TableRow<>();

            ContextMenu rowMenu = new ContextMenu();
            if (PermissionManager.canEditEmployee()) {
                MenuItem editItem = new MenuItem("✏ Edit Employee");
                editItem.setOnAction(e -> ensureDepartmentOptions(() ->
                        showFullEditDialog(row.getItem(), localTable, localList)));
                rowMenu.getItems().add(editItem);
            }
            MenuItem assetsItem = new MenuItem("▣ View Assets");
            assetsItem.setOnAction(e -> showAssetsDialog(row.getItem()));
            rowMenu.getItems().add(assetsItem);
            if (active && PermissionManager.canDeactivateEmployee()) {
                MenuItem deactivateItem = new MenuItem("🚫 Deactivate");
                deactivateItem.setOnAction(e -> showDeactivateConfirm(row.getItem(), localTable, localList));
                rowMenu.getItems().add(deactivateItem);
            } else if (!active && PermissionManager.canDeactivateEmployee()) {
                MenuItem reactivateItem = new MenuItem("✔ Reactivate");
                reactivateItem.setOnAction(e -> reactivateEmployee(row.getItem(), localTable, localList));
                rowMenu.getItems().add(reactivateItem);
            }
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu)
            );

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    if (PermissionManager.canEditEmployee()) {
                        ensureDepartmentOptions(() ->
                                showFullEditDialog(row.getItem(), localTable, localList));
                    } else {
                        showAlert("Access Denied", "You don't have permission to edit employee details.");
                    }
                }
            });
            return row;
        });

        TableColumn<Employee, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Employee, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Employee, String> empCodeCol = new TableColumn<>("Emp Code");
        empCodeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmpCode()));
        empCodeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); getStyleClass().remove("data-mono"); return; }
                setText(item);
                if (!getStyleClass().contains("data-mono")) getStyleClass().add("data-mono");
            }
        });

        TableColumn<Employee, String> designationCol = new TableColumn<>("Designation");
        designationCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDesignation()));

        TableColumn<Employee, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail()));

        TableColumn<Employee, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPhone()));

        TableColumn<Employee, String> activeCol = new TableColumn<>("Status");
        activeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isActive() ? "Active" : "Inactive"));
        activeCol.setCellFactory(col -> new TableCell<>() {
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

        TableColumn<Employee, Void> avatarCol = new TableColumn<>("");
        avatarCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Employee emp = getTableView().getItems().get(getIndex());
                String[] colors = {"#58a6ff", "#3fb950", "#d29922", "#f85149", "#a371f7", "#39d353"};
                String color = colors[Math.abs(emp.getName().hashCode()) % colors.length];
                setGraphic(UIComponents.avatar(emp.getName(), color));
            }
        });
        avatarCol.setMaxWidth(50);
        avatarCol.setMinWidth(50);
        localTable.getColumns().add(avatarCol);



        localTable.getColumns().addAll(idCol, nameCol,
                empCodeCol, designationCol,
                emailCol, phoneCol, activeCol);

        Button addBtn = new Button("+ Add Employee");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> ensureDepartmentOptions(() -> showAddDialog(localTable, localList)));
        AnimationUtil.addHoverScale(addBtn);
        TextField searchField = new TextField();
        searchField.setPromptText("Search by name...");
        searchField.textProperty().addListener((obs, ov, nv) -> {
            if (nv.isEmpty()) {
                localTable.getItems().setAll(localList);
            } else {
                String lower = nv.toLowerCase();
                localTable.getItems().setAll(
                        localList.filtered(e -> e.getName().toLowerCase().contains(lower)));
            }
        });

        HBox topBar = new HBox(10, searchField);
        Label rightClickHint = new Label("Right-click a row or empty space for actions.");
        rightClickHint.getStyleClass().add("text-muted");
        rightClickHint.setStyle("-fx-font-size: 11px;");
        topBar.getChildren().add(rightClickHint);


        VBox tableWrapper = new VBox(localTable);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox.setVgrow(localTable, Priority.ALWAYS);
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);

        VBox content = new VBox(10, topBar, tableWrapper);
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);
        content.setPadding(new Insets(10));

        ContextMenu screenMenu = new ContextMenu();
        screenMenu.setAutoHide(true);
        if (active && PermissionManager.canAddEmployee()) {
            MenuItem newItem = new MenuItem("+ New Employee");
            newItem.setOnAction(e -> ensureDepartmentOptions(() -> showAddDialog(localTable, localList)));
            screenMenu.getItems().add(newItem);
        }
        if (active && PermissionManager.canImportEmployees()) {
            MenuItem importItem = new MenuItem("⬆ Import Employees");
            importItem.setOnAction(e -> importBtn.fire());
            screenMenu.getItems().add(importItem);
        }
        MenuItem exportItem = new MenuItem("⬇ Export");
        exportItem.setOnAction(e -> exportEmployees(localList, active ? "Active_Employees" : "Inactive_Employees"));
        screenMenu.getItems().add(exportItem);

        content.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) screenMenu.hide();
        });
        content.setOnContextMenuRequested(e -> {
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
                screenMenu.show(content, e.getScreenX(), e.getScreenY());
            } else {
                screenMenu.hide();
            }
        });
        return content;
    }

    private void showFullEditDialog(Employee emp, TableView<Employee> table, ObservableList<Employee> list) {
        Task<String> loadTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return ApiClient.get(ConfigManager.getBaseUrl() + "/api/employees/" + emp.getId()).body();
            }
        };
        loadTask.setOnSucceeded(e -> openFullEditDialog(emp, loadTask.getValue(), table, list));
        loadTask.setOnFailed(e -> openFullEditDialog(emp, "{}", table, list));
        Thread t = new Thread(loadTask);
        t.setDaemon(true);
        t.start();
    }

    private void openFullEditDialog(Employee emp, String fullJson,
                                    TableView<Employee> table, ObservableList<Employee> list) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Employee Details");
        dialog.setHeaderText(emp.getName() + " — " + emp.getEmpCode());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(480);

        TextField nameField = new TextField(emp.getName());
        TextField empCodeField = new TextField(emp.getEmpCode()); // now editable

        SearchablePickerField deptPicker = new SearchablePickerField(departmentOptions, "Search department...");
        int currentDeptId = extractInt(fullJson, "departmentId");
        if (currentDeptId > 0) deptPicker.preselectSilently(currentDeptId);

        TextField designationField = new TextField(emp.getDesignation());
        TextField emailField = new TextField(emp.getEmail());
        TextField phoneField = new TextField(emp.getPhone());
        TextField joinDateField = new TextField(DatePickerUtil.fromIso(extractValue(fullJson, "joinDate")));
        TextField leaveDateField = new TextField(DatePickerUtil.fromIso(extractValue(fullJson, "leaveDate")));
        TextField notesField = new TextField(extractValue(fullJson, "notes"));
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(10));
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(120);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);
        int r = 0;
        grid.add(new Label("Name *:"),        0, r); grid.add(nameField,        1, r++);
        grid.add(new Label("Emp Code *:"),    0, r); grid.add(empCodeField,     1, r++);
        grid.add(new Label("Department:"),    0, r); grid.add(deptPicker,       1, r++);
        grid.add(new Label("Designation *:"), 0, r); grid.add(designationField, 1, r++);
        grid.add(new Label("Email:"),         0, r); grid.add(emailField,       1, r++);
        grid.add(new Label("Phone:"),         0, r); grid.add(phoneField,       1, r++);
        grid.add(new Label("Join Date:"),     0, r); grid.add(DatePickerUtil.dateField(joinDateField), 1, r++);
        grid.add(new Label("Leave Date:"),    0, r); grid.add(DatePickerUtil.dateField(leaveDateField), 1, r++);
        grid.add(new Label("Notes:"),         0, r); grid.add(notesField,       1, r++);
        grid.add(errorLabel,                  1, r);
        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!ValidationUtil.isNotBlank(nameField.getText())) {
                errorLabel.setText("Name is required.");
                event.consume();
            } else if (!ValidationUtil.isNotBlank(empCodeField.getText())) {
                errorLabel.setText("Emp Code is required.");
                event.consume();
            } else if (!ValidationUtil.isNotBlank(designationField.getText())) {
                errorLabel.setText("Designation is required.");
                event.consume();
            } else if (!emailField.getText().trim().isEmpty() && !ValidationUtil.isValidEmail(emailField.getText())) {
                errorLabel.setText("Please enter a valid email address.");
                event.consume();
            } else if (!phoneField.getText().trim().isEmpty() && !ValidationUtil.isValidPhone(phoneField.getText())) {
                errorLabel.setText("Phone number must be exactly 10 digits.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String body = "{" +
                    "\"id\":" + emp.getId() + "," +
                    "\"name\":\"" + escape(nameField.getText()) + "\"," +
                    "\"empCode\":\"" + escape(empCodeField.getText()) + "\"," +
                    "\"departmentId\":" + deptPicker.getSelectedId() + "," +
                    "\"designation\":\"" + escape(designationField.getText()) + "\"," +
                    "\"email\":\"" + escape(emailField.getText()) + "\"," +
                    "\"phone\":\"" + escape(phoneField.getText()) + "\"," +
                    "\"joinDate\":\"" + DatePickerUtil.toIso(joinDateField.getText()) + "\"," +
                    "\"leaveDate\":\"" + DatePickerUtil.toIso(leaveDateField.getText()) + "\"," +
                    "\"active\":1," +
                    "\"notes\":\"" + escape(notesField.getText()) + "\"" +
                    "}";
            if (putRequest(ConfigManager.getBaseUrl() + "/api/employees/" + emp.getId(), body))
                loadEmployees(table, list);
        }
    }


    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "'")
                .replace("\n", " ")
                .replace("\r", "");
    }

    private void reactivateEmployee(Employee emp, TableView<Employee> table, ObservableList<Employee> list) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        ThemeManager.applyToDialog(confirm);
        confirm.setTitle("Reactivate Employee");
        confirm.setHeaderText(null);
        confirm.setContentText("Reactivate " + emp.getName() + "? They will be set as active again.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    HttpResponse<String> resp = ApiClient.putNoBody(
                            ConfigManager.getBaseUrl() + "/api/employees/" + emp.getId() + "/reactivate");
                    if (resp.statusCode() == 200) {
                        ToastUtil.success(emp.getName() + " reactivated.");
                        loadInactiveEmployees(table, list);
                    } else {
                        showAlert("Error", "Server returned: " + resp.statusCode());
                    }
                } catch (Exception ex) {
                    showAlert("Error", ex.getMessage());
                }
            }
        });
    }

    private void showDeactivateConfirm(Employee emp, TableView<Employee> table, ObservableList<Employee> list) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        ThemeManager.applyToDialog(confirm);
        confirm.setTitle("Deactivate Employee");
        confirm.setHeaderText(null);
        confirm.setContentText("Deactivate " + emp.getName() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (deleteRequest(ConfigManager.getBaseUrl() + "/api/employees/" + emp.getId()))
                loadEmployees(table, list);
        }
    }

    private void loadEmployees(TableView<Employee> table, ObservableList<Employee> list) {
        table.getItems().clear();
        list.clear();
        LoadingUtil.setLoading(table, "Loading employees...");

        Task<List<Employee>> task = new Task<>() {
            @Override
            protected List<Employee> call() throws Exception {
                List<Employee> result = new ArrayList<>();
                String url = SessionManager.get().isDeptHod()
                        ? ConfigManager.getBaseUrl() + "/api/employees/department/" + SessionManager.get().getDeptId()
                        : ConfigManager.getBaseUrl() + "/api/employees";

                HttpResponse<String> response = ApiClient.get(url);
                String body = response.body().trim();

                if (body.equals("[]") || body.isEmpty()) return result;
                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    for (String obj : body.split("\\},\\s*\\{")) {
                        String cleanedObj = obj.replace("{", "").replace("}", "");
                        result.add(new Employee(
                                extractInt(cleanedObj, "id"),
                                extractValue(cleanedObj, "name"),
                                extractValue(cleanedObj, "empCode"),
                                extractValue(cleanedObj, "designation"),
                                extractValue(cleanedObj, "email"),
                                extractValue(cleanedObj, "phone"),
                                extractInt(cleanedObj, "active")));
                    }
                }
                return result;
            }
        };

        task.setOnSucceeded(e -> {
            List<Employee> employees = task.getValue();
            table.getItems().addAll(employees);
            list.addAll(employees);
            if (employees.isEmpty()) {
                LoadingUtil.setEmpty(table, "👤", "No employees found", "Add employees using the button above.");
            }
        });
        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠", "Could not load employees", "Check server connection and try again.");
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadInactiveEmployees(TableView<Employee> table, ObservableList<Employee> list) {
        table.getItems().clear();
        list.clear();
        LoadingUtil.setLoading(table, "Loading inactive employees...");

        Task<List<Employee>> task = new Task<>() {
            @Override
            protected List<Employee> call() throws Exception {
                List<Employee> result = new ArrayList<>();
                String url = ConfigManager.getBaseUrl() + "/api/employees/inactive";

                HttpResponse<String> resp = ApiClient.get(url);
                String body = resp.body().trim();

                if (body.equals("[]") || body.isEmpty()) return result;
                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    for (String obj : body.split("\\},\\s*\\{")) {
                        String cleanedObj = obj.replace("{", "").replace("}", "");
                        result.add(new Employee(
                                extractInt(cleanedObj, "id"),
                                extractValue(cleanedObj, "name"),
                                extractValue(cleanedObj, "empCode"),
                                extractValue(cleanedObj, "designation"),
                                extractValue(cleanedObj, "email"),
                                extractValue(cleanedObj, "phone"),
                                0));
                    }
                }
                return result;
            }
        };

        task.setOnSucceeded(e -> {
            List<Employee> employees = task.getValue();
            table.getItems().addAll(employees);
            list.addAll(employees);
            if (employees.isEmpty()) {
                LoadingUtil.setEmpty(table, "👤", "No inactive employees", "All employees are currently active.");
            }
        });
        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠", "Could not load employees", "Check server connection.");
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadDepartmentOptions(Consumer<List<PickerOption>> callback) {
        Task<List<PickerOption>> task = new Task<>() {
            @Override
            protected List<PickerOption> call() throws Exception {
                List<PickerOption> result = new ArrayList<>();
                HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/departments");
                String body = resp.body().trim();
                if (body.length() < 2) return result;
                body = body.substring(1, body.length() - 1).trim();
                if (body.isEmpty()) return result;
                for (String obj : body.split("\\},\\s*\\{")) {
                    String cleaned = obj.replace("{", "").replace("}", "");
                    int id = extractInt(cleaned, "id");
                    String name = extractValue(cleaned, "name");
                    if (name.isEmpty()) name = extractValue(cleaned, "departmentName");
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

    private void ensureDepartmentOptions(Runnable next) {
        if (!departmentOptions.isEmpty()) {
            next.run();
            return;
        }
        loadDepartmentOptions(opts -> {
            departmentOptions = opts;
            next.run();
        });
    }

    public void search(String keyword) {
        boolean onActiveTab = tabPane == null || tabPane.getSelectionModel().getSelectedIndex() == 0;
        TableView<Employee> targetTable = onActiveTab ? activeTable : inactiveTable;
        ObservableList<Employee> source = onActiveTab ? allActiveEmployees : allInactiveEmployees;
        if (targetTable == null || source == null) return;

        if (keyword == null || keyword.isEmpty()) {
            targetTable.getItems().setAll(source);
            return;
        }
        String lower = keyword.toLowerCase();
        targetTable.getItems().setAll(source.filtered(e ->
                e.getName().toLowerCase().contains(lower)
                        || e.getEmpCode().toLowerCase().contains(lower)
                        || e.getEmail().toLowerCase().contains(lower)
                        || e.getDesignation().toLowerCase().contains(lower)
        ));
        if (targetTable.getItems().isEmpty()) {
            LoadingUtil.setEmpty(targetTable, "🔍", "No results found", "No employees match \"" + keyword + "\"");
        }
    }

    private void showAddDialog(TableView<Employee> table, ObservableList<Employee> list) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Add Employee");
        dialog.setHeaderText("Enter employee details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField        = new TextField();
        TextField empCodeField     = new TextField();
        SearchablePickerField deptPicker = new SearchablePickerField(departmentOptions, "Search department...");
        TextField designationField = new TextField();
        TextField emailField       = new TextField();
        TextField phoneField       = new TextField();
        TextField joinDateField    = new TextField();
        TextField notesField       = new TextField();
        Label errorLabel           = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Name *:"),        0, 0); grid.add(nameField,        1, 0);
        grid.add(new Label("Emp Code *:"),    0, 1); grid.add(empCodeField,     1, 1);
        grid.add(new Label("Department:"),    0, 2); grid.add(deptPicker,       1, 2);
        grid.add(new Label("Designation *:"), 0, 3); grid.add(designationField, 1, 3);
        grid.add(new Label("Email *:"),       0, 4); grid.add(emailField,       1, 4);
        grid.add(new Label("Phone:"),         0, 5); grid.add(phoneField,       1, 5);
        grid.add(new Label("Join Date:"),     0, 6); grid.add(DatePickerUtil.dateField(joinDateField), 1, 6);
        grid.add(new Label("Notes:"),         0, 7); grid.add(notesField,       1, 7);
        grid.add(errorLabel,                  1, 8);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable checkFields = () -> okButton.setDisable(
                nameField.getText().trim().isEmpty()
                        || empCodeField.getText().trim().isEmpty()
                        || designationField.getText().trim().isEmpty()
                        || emailField.getText().trim().isEmpty());
        nameField.textProperty().addListener((o, ov, nv) -> checkFields.run());
        empCodeField.textProperty().addListener((o, ov, nv) -> checkFields.run());
        designationField.textProperty().addListener((o, ov, nv) -> checkFields.run());
        emailField.textProperty().addListener((o, ov, nv) -> checkFields.run());

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validateEmployee(nameField.getText(), empCodeField.getText(),
                    designationField.getText(), joinDateField.getText(), emailField.getText());
            if (error == null && !phoneField.getText().trim().isEmpty()
                    && !ValidationUtil.isValidPhone(phoneField.getText())) {
                error = "Phone number must be exactly 10 digits.";
            }
            if (error != null) {
                errorLabel.setText(error);
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String body = "{" +
                    "\"name\":\"" + escape(nameField.getText()) + "\"," +
                    "\"empCode\":\"" + escape(empCodeField.getText()) + "\"," +
                    "\"departmentId\":" + deptPicker.getSelectedId() + "," +
                    "\"designation\":\"" + escape(designationField.getText()) + "\"," +
                    "\"email\":\"" + escape(emailField.getText()) + "\"," +
                    "\"phone\":\"" + escape(phoneField.getText()) + "\"," +
                    "\"joinDate\":\"" + DatePickerUtil.toIso(joinDateField.getText()) + "\"," +
                    "\"leaveDate\":null," +
                    "\"active\":1," +
                    "\"notes\":\"" + escape(notesField.getText()) + "\"" +
                    "}";
            boolean created = postRequest(ConfigManager.getBaseUrl() + "/api/employees", body, 201);
            if (created) {
                showAlert("Success",
                        "Employee added.\nLogin — username: " + emailField.getText().trim()
                                + ", default password: " + DEFAULT_PASSWORD);
                loadEmployees(table, list);
            }
        }
    }
    private void showAssetsDialog(Employee emp) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Assets — " + emp.getName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(560);

        VBox container = new VBox(10);
        container.setPadding(new Insets(10));

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return ApiClient.get(ConfigManager.getBaseUrl() + "/api/employee/assets-full/" + emp.getId()).body();
            }
        };
        task.setOnSucceeded(e -> renderAssetsFull(task.getValue(), container));
        task.setOnFailed(e -> container.getChildren().add(new Label("Could not load assets.")));
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();

        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);
        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    private void renderAssetsFull(String json, VBox container) {
        String body = json.trim();
        if (body.length() < 2) { container.getChildren().add(new Label("No assets assigned.")); return; }
        body = body.substring(1, body.length() - 1).trim();
        if (body.isEmpty()) { container.getChildren().add(new Label("No assets assigned.")); return; }

        for (String assetObj : splitTopLevelObjects("[" + body + "]")) {
            String tag = extractValue(assetObj, "assetTag");
            String name = extractValue(assetObj, "name");
            String category = extractValue(assetObj, "category");
            String status = extractValue(assetObj, "status");

            Label header = new Label(tag + " — " + name + " (" + category + ", " + status + ")");
            header.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            VBox card = new VBox(4, header);
            card.getStyleClass().add("surface-card");
            card.setStyle("-fx-background-radius: 6; -fx-padding: 10;");

            String linksArray = extractNestedArray(assetObj, "linkedAssets");
            List<String> accessories = new ArrayList<>();
            String linksBody = linksArray.trim();
            if (linksBody.length() > 2) {
                linksBody = linksBody.substring(1, linksBody.length() - 1).trim();
                if (!linksBody.isEmpty()) {
                    for (String acc : linksBody.split("\\},\\{")) {
                        String cleaned = acc.replace("{", "").replace("}", "");
                        accessories.add(extractValue(cleaned, "assetTag") + " — " + extractValue(cleaned, "name")
                                + " (" + extractValue(cleaned, "category") + ")");
                    }
                }
            }
            if (accessories.isEmpty()) {
                Label none = new Label("No linked accessories.");
                none.getStyleClass().add("text-muted");
                none.setStyle("-fx-font-size: 11px;");
                card.getChildren().add(none);
            } else {
                for (String acc : accessories) {
                    Label accLabel = new Label("  ↳ " + acc);
                    accLabel.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 11px;");
                    card.getChildren().add(accLabel);
                }
            }
            container.getChildren().add(card);
        }
    }

    private List<String> splitTopLevelObjects(String arrayBody) {
        List<String> result = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < arrayBody.length(); i++) {
            char c = arrayBody.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0) result.add(arrayBody.substring(start, i + 1)); }
        }
        return result;
    }

    private String extractNestedArray(String json, String key) {
        String search = "\"" + key + "\":[";
        int start = json.indexOf(search);
        if (start == -1) return "[]";
        start += search.length() - 1;
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '[') depth++;
            else if (json.charAt(i) == ']') { depth--; if (depth == 0) return json.substring(start, i + 1); }
        }
        return "[]";
    }

    private String validateEmployee(String name, String empCode,
                                    String designation, String joinDate, String email) {
        if (!ValidationUtil.isNotBlank(name))        return "Employee Name is required.";
        if (!ValidationUtil.isNotBlank(empCode))     return "Employee Code is required.";
        if (!ValidationUtil.isNotBlank(designation)) return "Designation is required.";
        if (!ValidationUtil.isNotBlank(email))       return "Email is required (used as login username).";
        if (!ValidationUtil.isValidEmail(email))     return "Please enter a valid email address.";
        if (!joinDate.trim().isEmpty() && DatePickerUtil.toIso(joinDate).isEmpty())
            return "Join Date must be a valid date (dd-MM-yyyy).";
        return null;
    }

    private boolean postRequest(String url, String body, int expectedStatus) {
        try {
            HttpResponse<String> response = ApiClient.post(url, body);
            if (response.statusCode() == expectedStatus) {
                return true;
            }
            showAlert("Error", "Server returned: " + response.statusCode());
            return false;
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }
    private boolean putRequest(String url, String body) {
        try {
            HttpResponse<String> response = ApiClient.put(url, body);
            if (response.statusCode() == 200) {
                ToastUtil.success("Updated successfully.");
                return true;
            }
            showAlert("Error", "Server returned: " + response.statusCode());
            return false;
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private boolean deleteRequest(String url) {
        try {
            HttpResponse<String> response = ApiClient.delete(url);
            if (response.statusCode() == 200) {
                ToastUtil.success("Employee deactivated.");
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
        ThemeManager.applyToDialog(alert);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void importEmployeesCsv(TableView<Employee> table, ObservableList<Employee> list) {
        CsvImporter.importCsvAsync("Import Employees CSV", true, fields -> {
            if (fields.length < 8)
                throw new Exception("Expected 8 columns: Name, EmpCode, DepartmentId, Designation, Email, Phone, JoinDate, Notes");

            String email = fields[4].trim();
            if (email.isEmpty())
                throw new Exception("Email is required for row: " + fields[0]);

            String body = "{" +
                    "\"name\":\"" + escape(fields[0]) + "\"," +
                    "\"empCode\":\"" + escape(fields[1]) + "\"," +
                    "\"departmentId\":" + (fields[2].trim().isEmpty() ? 0 : Integer.parseInt(fields[2].trim())) + "," +
                    "\"designation\":\"" + escape(fields[3]) + "\"," +
                    "\"email\":\"" + escape(email) + "\"," +
                    "\"phone\":\"" + escape(fields[5]) + "\"," +
                    "\"joinDate\":\"" + DatePickerUtil.toIso(fields[6]) + "\"," +
                    "\"leaveDate\":null," +
                    "\"active\":1," +
                    "\"notes\":\"" + escape(fields[7]) + "\"" +
                    "}";
            boolean created = postRequest(ConfigManager.getBaseUrl() + "/api/employees", body, 201);
            if (!created) throw new Exception("Failed to create employee: " + fields[0]);
        }, result -> loadEmployees(table, list));
    }

    private void exportEmployees(ObservableList<Employee> employees, String filename) {
        Task<List<List<String>>> task = new Task<>() {
            @Override
            protected List<List<String>> call() {
                List<List<String>> rows = new ArrayList<>();
                for (Employee emp : employees) {
                    String deptName = "-", joinDate = "-", leaveDate = "-", notes = "";
                    try {
                        HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/employees/" + emp.getId());
                        String body = resp.body();
                        int deptId = extractInt(body, "departmentId");
                        if (deptId > 0) {
                            deptName = departmentOptions.stream()
                                    .filter(o -> o.id == deptId).map(o -> o.name)
                                    .findFirst().orElse("Dept #" + deptId);
                        }
                        String jd = extractValue(body, "joinDate");
                        if (!jd.isEmpty()) joinDate = DateTimeFormatUtil.toIndianDateOnly(jd);
                        String ld = extractValue(body, "leaveDate");
                        if (!ld.isEmpty()) leaveDate = DateTimeFormatUtil.toIndianDateOnly(ld);
                        notes = extractValue(body, "notes");
                    } catch (Exception ignored) {}

                    rows.add(List.of(
                            String.valueOf(emp.getId()), emp.getName(), emp.getEmpCode(),
                            deptName, emp.getDesignation(), emp.getEmail(), emp.getPhone(),
                            joinDate, leaveDate, emp.isActive() ? "Active" : "Inactive", notes));
                }
                return rows;
            }
        };
        task.setOnSucceeded(e -> {
            List<String> headers = List.of("ID", "Name", "Emp Code", "Department",
                    "Designation", "Email", "Phone", "Join Date", "Leave Date", "Status", "Notes");
            ExcelExporter.export(filename, headers, task.getValue());
        });
        task.setOnFailed(e -> showAlert("Error", "Export failed."));
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
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