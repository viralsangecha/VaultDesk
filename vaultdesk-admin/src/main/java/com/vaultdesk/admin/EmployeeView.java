package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmployeeView {

    private TableView<Employee> table;
    private ObservableList<Employee> allEmployees =
            FXCollections.observableArrayList();

    public VBox getView() {
        Label title = new Label("Employees");
        title.getStyleClass().add("page-title");

        // ── Tabs ──────────────────────────────────────────────
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(
                TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab activeTab   = new Tab("Active Employees");
        Tab inactiveTab = new Tab("Inactive Employees");

        activeTab.setContent(
                buildEmployeeTable(true));
        inactiveTab.setContent(
                buildEmployeeTable(false));

        tabPane.getTabs().addAll(activeTab, inactiveTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        VBox root = new VBox(10, title, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        return root;
    }

    private VBox buildEmployeeTable(boolean active) {
        table = new TableView<>();
        allEmployees = FXCollections.observableArrayList();
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);

        Button exportBtn = new Button("⬇ Export");
        exportBtn.getStyleClass().setAll("btn-primary");
        exportBtn.setStyle(
                "-fx-background-color: #6e40c9;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 6 12 6 12;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;");
        exportBtn.setOnAction(e -> {
            List<String> headers = List.of(
                    "ID", "Name", "Emp Code",
                    "Designation", "Email", "Phone",
                    "Status");
            List<List<String>> rows = new ArrayList<>();
            for (Employee emp : allEmployees) {
                rows.add(List.of(
                        String.valueOf(emp.getId()),
                        emp.getName(),
                        emp.getEmpCode(),
                        emp.getDesignation(),
                        emp.getEmail(),
                        emp.getPhone(),
                        emp.isActive() ? "Active" : "Inactive"
                ));
            }
            ExcelExporter.export(
                    active ? "Active_Employees"
                            : "Inactive_Employees",
                    headers, rows);
        });
        table.setRowFactory(tv -> {
            TableRow<Employee> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty())
                    showFullEditDialog(row.getItem(), table);
            });
            return row;
        });

        // ── All existing columns stay the same ────────────────
        TableColumn<Employee, Integer> idCol =
                new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(
                        data.getValue().getId()).asObject());

        TableColumn<Employee, String> nameCol =
                new TableColumn<>("Name");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getName()));

        TableColumn<Employee, String> empCodeCol =
                new TableColumn<>("Emp Code");
        empCodeCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getEmpCode()));

        TableColumn<Employee, String> designationCol =
                new TableColumn<>("Designation");
        designationCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDesignation()));

        TableColumn<Employee, String> emailCol =
                new TableColumn<>("Email");
        emailCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getEmail()));

        TableColumn<Employee, String> phoneCol =
                new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getPhone()));

        TableColumn<Employee, String> activeCol =
                new TableColumn<>("Status");
        activeCol.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().isActive()
                                ? "Active" : "Inactive"));
        activeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item,
                                      boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle(""); return;
                }
                setText(item);
                if ("Active".equals(item))
                    setStyle("-fx-text-fill: #3fb950;" +
                            " -fx-font-weight: bold;");
                else
                    setStyle("-fx-text-fill: #f85149;" +
                            " -fx-font-weight: bold;");
            }
        });

        // Add avatar column:
        TableColumn<Employee, Void> avatarCol =
                new TableColumn<>("");
        avatarCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item,
                                      boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Employee emp = getTableView()
                        .getItems().get(getIndex());
                String[] colors = {
                        "#58a6ff", "#3fb950", "#d29922",
                        "#f85149", "#a371f7", "#39d353"};
                String color = colors[
                        Math.abs(emp.getName().hashCode())
                                % colors.length];
                setGraphic(UIComponents.avatar(
                        emp.getName(), color));
            }
        });
        avatarCol.setMaxWidth(50);
        avatarCol.setMinWidth(50);
        table.getColumns().add(0, avatarCol);

        // ── Action column changes based on active/inactive ────
        TableColumn<Employee, Void> actionCol =
                new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn =
                    new Button("Edit");
            private final Button deactivateBtn =
                    new Button("Deactivate");
            private final Button reactivateBtn =
                    new Button("Reactivate");
            private final Button setLoginBtn =
                    new Button("Set Login");

            {
                editBtn.getStyleClass().setAll("btn-warning");
                editBtn.setStyle(
                        "-fx-background-color: #b45309;" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 6;" +
                                "-fx-padding: 5 10 5 10;" +
                                "-fx-font-size: 11px;" +
                                "-fx-font-weight: bold;");

                deactivateBtn.getStyleClass()
                        .setAll("btn-danger");
                deactivateBtn.setStyle(
                        "-fx-background-color: #da3633;" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 6;" +
                                "-fx-padding: 5 10 5 10;" +
                                "-fx-font-size: 11px;" +
                                "-fx-font-weight: bold;");

                reactivateBtn.getStyleClass()
                        .setAll("btn-primary");
                reactivateBtn.setStyle(
                        "-fx-background-color: #238636;" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 6;" +
                                "-fx-padding: 5 10 5 10;" +
                                "-fx-font-size: 11px;" +
                                "-fx-font-weight: bold;");

                setLoginBtn.getStyleClass().setAll("btn-primary");
                setLoginBtn.setStyle(
                        "-fx-background-color: #1f6feb;" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 6;" +
                                "-fx-padding: 5 10 5 10;" +
                                "-fx-font-size: 11px;" +
                                "-fx-font-weight: bold;");

                editBtn.setOnAction(e -> {
                    Employee emp = getTableView()
                            .getItems().get(getIndex());
                    showEditDialog(emp, getTableView());
                });
                deactivateBtn.setOnAction(e -> {
                    Employee emp = getTableView()
                            .getItems().get(getIndex());
                    showDeactivateConfirm(emp, getTableView());
                });
                reactivateBtn.setOnAction(e -> {
                    Employee emp = getTableView()
                            .getItems().get(getIndex());
                    reactivateEmployee(emp, getTableView());
                });
                setLoginBtn.setOnAction(e -> {
                    Employee emp = getTableView()
                            .getItems().get(getIndex());
                    showSetLoginDialog(emp);
                });
            }


            @Override
            protected void updateItem(Void item,
                                      boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(5);
                if (active) {
                    if (PermissionManager.canEditEmployee())
                        box.getChildren().add(editBtn);
                    if (PermissionManager.canSetLogin())
                        box.getChildren().add(setLoginBtn);
                    if (PermissionManager.has("DELETE_TICKET"))
                        box.getChildren().add(deactivateBtn);
                } else {
                    box.getChildren().add(reactivateBtn);
                }
                setGraphic(box.getChildren().isEmpty()
                        ? null : box);
            }
        });

        table.getColumns().addAll(idCol, nameCol,
                empCodeCol, designationCol,
                emailCol, phoneCol, activeCol, actionCol);

        // ── Buttons ───────────────────────────────────────────
        Button addBtn = new Button("+ Add Employee");
        addBtn.getStyleClass().setAll("btn-primary");
        addBtn.setStyle(
                "-fx-background-color: #238636;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 6 14 6 14;" +
                        "-fx-font-weight: bold;");
        addBtn.setOnAction(e -> showAddDialog(table));

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name...");
        searchField.textProperty().addListener(
                (obs, ov, nv) -> {
                    if (nv.isEmpty()) {
                        table.getItems().setAll(allEmployees);
                    } else {
                        String lower = nv.toLowerCase();
                        table.getItems().setAll(
                                allEmployees.filtered(e ->
                                        e.getName().toLowerCase()
                                                .contains(lower)));
                    }
                });

        HBox topBar = new HBox(10);
        if (active && (PermissionManager.canAddEmployee())) {
            topBar.getChildren().add(addBtn);
            topBar.getChildren().add(exportBtn);
        }
        topBar.getChildren().add(searchField);

        if (active) {
            loadEmployees(table);
        } else {
            loadInactiveEmployees(table);
        }

        VBox content = new VBox(10, topBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        content.setPadding(new Insets(10));
        return content;
    }

    private void showFullEditDialog(Employee emp,
                                    TableView<Employee> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Employee Details");
        dialog.setHeaderText(emp.getName()
                + " — " + emp.getEmpCode());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(480);

        TextField nameField = new TextField(emp.getName());
        TextField empCodeField = new TextField(
                emp.getEmpCode());
        empCodeField.setEditable(false);
        empCodeField.setStyle(
                "-fx-opacity: 0.6;");

        TextField deptField = new TextField();
        deptField.setPromptText("Department ID");

        TextField designationField = new TextField(
                emp.getDesignation());
        TextField emailField = new TextField(emp.getEmail());
        TextField phoneField = new TextField(emp.getPhone());

        TextField joinDateField = new TextField();
        TextField leaveDateField = new TextField();
        TextField notesField = new TextField();

        // Load full details
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/employees/" + emp.getId()))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            deptField.setText(extractIntStr(body,
                    "departmentId"));
            joinDateField.setText(extractValue(body,
                    "joinDate"));
            leaveDateField.setText(extractValue(body,
                    "leaveDate"));
            notesField.setText(extractValue(body, "notes"));
        } catch (Exception ignored) {}

        Label errorLabel = new Label("");
        errorLabel.setStyle(
                "-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(10));
        int r = 0;
        grid.add(new Label("Name *:"),        0, r);
        grid.add(nameField,                   1, r++);
        grid.add(new Label("Emp Code:"),      0, r);
        grid.add(empCodeField,                1, r++);
        grid.add(new Label("Dept ID:"),       0, r);
        grid.add(deptField,                   1, r++);
        grid.add(new Label("Designation *:"), 0, r);
        grid.add(designationField,            1, r++);
        grid.add(new Label("Email:"),         0, r);
        grid.add(emailField,                  1, r++);
        grid.add(new Label("Phone:"),         0, r);
        grid.add(phoneField,                  1, r++);
        grid.add(new Label("Join Date:"),     0, r);
        grid.add(DatePickerUtil.dateField(
                joinDateField),               1, r++);
        grid.add(new Label("Leave Date:"),    0, r);
        grid.add(DatePickerUtil.dateField(
                leaveDateField),              1, r++);
        grid.add(new Label("Notes:"),         0, r);
        grid.add(notesField,                  1, r++);
        grid.add(errorLabel,                  1, r);
        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okBtn.addEventFilter(
                javafx.event.ActionEvent.ACTION, event -> {
                    if (nameField.getText().trim().isEmpty()) {
                        errorLabel.setText("Name is required.");
                        event.consume();
                    } else if (designationField.getText()
                            .trim().isEmpty()) {
                        errorLabel.setText(
                                "Designation is required.");
                        event.consume();
                    }
                });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()
                && result.get() == ButtonType.OK) {
            try {
                int deptId = deptField.getText()
                        .trim().isEmpty() ? 0
                        : Integer.parseInt(
                        deptField.getText().trim());
                String body = "{" +
                        "\"id\":" + emp.getId() + "," +
                        "\"name\":\"" + escape(
                        nameField.getText()) + "\"," +
                        "\"empCode\":\"" +
                        emp.getEmpCode() + "\"," +
                        "\"departmentId\":" + deptId + "," +
                        "\"designation\":\"" + escape(
                        designationField.getText()) + "\"," +
                        "\"email\":\"" +
                        emailField.getText() + "\"," +
                        "\"phone\":\"" +
                        phoneField.getText() + "\"," +
                        "\"joinDate\":\"" +
                        joinDateField.getText() + "\"," +
                        "\"leaveDate\":\"" +
                        leaveDateField.getText() + "\"," +
                        "\"active\":1," +
                        "\"notes\":\"" + escape(
                        notesField.getText()) + "\"" +
                        "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(
                                ConfigManager.getBaseUrl()
                                        + "/api/employees/"
                                        + emp.getId()))
                        .header("Content-Type",
                                "application/json")
                        .PUT(HttpRequest.BodyPublishers
                                .ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(
                        req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    showAlert("Success",
                            "Employee updated.");
                    loadEmployees(table);
                } else {
                    showAlert("Error", "Server returned: "
                            + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        }
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

    private void reactivateEmployee(Employee emp,
                                    TableView<Employee> table) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reactivate Employee");
        confirm.setHeaderText(null);
        confirm.setContentText("Reactivate " + emp.getName()
                + "? They will be set as active again.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    HttpClient client =
                            HttpClient.newHttpClient();
                    HttpRequest req =
                            HttpRequest.newBuilder()
                                    .uri(URI.create(
                                            ConfigManager.getBaseUrl()
                                                    + "/api/employees/"
                                                    + emp.getId()
                                                    + "/reactivate"))
                                    .PUT(HttpRequest.BodyPublishers
                                            .noBody())
                                    .build();
                    HttpResponse<String> resp = client.send(
                            req,
                            HttpResponse.BodyHandlers
                                    .ofString());
                    if (resp.statusCode() == 200) {
                        showAlert("Success",
                                emp.getName()
                                        + " reactivated.");
                        loadInactiveEmployees(table);
                    } else {
                        showAlert("Error",
                                "Server returned: "
                                        + resp.statusCode());
                    }
                } catch (Exception ex) {
                    showAlert("Error", ex.getMessage());
                }
            }
        });
    }

    private void loadInactiveEmployees(
            TableView<Employee> table) {
        table.getItems().clear();
        allEmployees.clear();
        LoadingUtil.setLoading(table,
                "Loading inactive employees...");
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/employees/inactive"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "")
                            .replace("}", "");
                    Employee e = new Employee(
                            extractInt(obj, "id"),
                            extractValue(obj, "name"),
                            extractValue(obj, "empCode"),
                            extractValue(obj, "designation"),
                            extractValue(obj, "email"),
                            extractValue(obj, "phone"),
                            0
                    );
                    table.getItems().add(e);
                    allEmployees.add(e);
                }
                if (table.getItems().isEmpty()) {
                    LoadingUtil.setEmpty(table, "👤",
                            "No inactive employees",
                            "All employees are currently active.");
                }
            } else {
                LoadingUtil.setEmpty(table, "👤",
                        "No inactive employees",
                        "All employees are currently active.");
            }
        } catch (Exception ex) {
            LoadingUtil.setEmpty(table, "⚠",
                    "Could not load employees",
                    "Check server connection.");
        }
    }

    private void filterTable(TableView<Employee> table, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            loadEmployees(table);
            return;
        }
        String lower = keyword.toLowerCase();
        table.getItems().removeIf(e ->
                !e.getName().toLowerCase().contains(lower));
    }

    private void loadEmployees(TableView<Employee> table) {
        table.getItems().clear();
        allEmployees.clear();
        LoadingUtil.setLoading(table, "Loading employees...");
        try {
            String url = SessionManager.get().isDeptHod()
                    ? ConfigManager.getBaseUrl()
                    + "/api/employees/department/"
                    + SessionManager.get().getDeptId()
                    : ConfigManager.getBaseUrl() + "/api/employees";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    Employee e = new Employee(
                            extractInt(obj, "id"),
                            extractValue(obj, "name"),
                            extractValue(obj, "empCode"),
                            extractValue(obj, "designation"),
                            extractValue(obj, "email"),
                            extractValue(obj, "phone"),
                            extractInt(obj, "active")
                    );
                    table.getItems().add(e);
                    allEmployees.add(e);
                }
                if (table.getItems().isEmpty()) {
                    LoadingUtil.setEmpty(table, "👤",
                            "No employees found",
                            "Add employees using the button above.");
                }
            } else {
                LoadingUtil.setEmpty(table, "👤",
                        "No employees found",
                        "Add employees using the button above.");
            }
        } catch (Exception ex) {
            LoadingUtil.setEmpty(table, "⚠",
                    "Could not load employees",
                    "Check server connection and try again.");
        }
    }

    public void search(String keyword) {
        if (keyword.isEmpty()) {
            table.getItems().setAll(allEmployees);
            return;
        }
        table.getItems().setAll(allEmployees.filtered(e ->
                e.getName().toLowerCase().contains(keyword)
                        || e.getEmpCode().toLowerCase().contains(keyword)
                        || e.getEmail().toLowerCase().contains(keyword)
                        || e.getDesignation().toLowerCase().contains(keyword)
        ));
        if (table.getItems().isEmpty()) {
            LoadingUtil.setEmpty(table, "🔍",
                    "No results found",
                    "No employees match \"" + keyword + "\"");
        }
    }

    private void showAddDialog(TableView<Employee> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Employee");
        dialog.setHeaderText("Enter employee details");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField        = new TextField();
        TextField empCodeField     = new TextField();
        TextField deptField        = new TextField();
        TextField designationField = new TextField();
        TextField emailField       = new TextField();
        TextField phoneField       = new TextField();
        TextField joinDateField    = new TextField();
        TextField notesField       = new TextField();
        Label errorLabel           = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Name *:"),        0, 0);
        grid.add(nameField,        1, 0);
        grid.add(new Label("Emp Code *:"),    0, 1);
        grid.add(empCodeField,     1, 1);
        grid.add(new Label("Dept ID:"),       0, 2);
        grid.add(deptField,        1, 2);
        grid.add(new Label("Designation *:"), 0, 3);
        grid.add(designationField, 1, 3);
        grid.add(new Label("Email:"),         0, 4);
        grid.add(emailField,       1, 4);
        grid.add(new Label("Phone:"),         0, 5);
        grid.add(phoneField,       1, 5);
        grid.add(new Label("Join Date:"), 0, 6);
        grid.add(DatePickerUtil.dateField(joinDateField), 1, 6);
        grid.add(new Label("Notes:"),         0, 7);
        grid.add(notesField,       1, 7);
        grid.add(errorLabel,                  1, 8);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable checkFields = () -> {
            boolean valid = !nameField.getText().trim().isEmpty()
                    && !empCodeField.getText().trim().isEmpty()
                    && !designationField.getText().trim().isEmpty();
            okButton.setDisable(!valid);
        };

        nameField.textProperty().addListener((o, ov, nv) -> checkFields.run());
        empCodeField.textProperty().addListener((o, ov, nv) -> checkFields.run());
        designationField.textProperty().addListener((o, ov, nv) -> checkFields.run());

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validateEmployee(
                    nameField.getText(), empCodeField.getText(),
                    designationField.getText(), joinDateField.getText(),
                    deptField.getText()
            );
            if (error != null) {
                errorLabel.setText(error);
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int deptId = deptField.getText().trim().isEmpty() ? 0
                        : Integer.parseInt(deptField.getText().trim());
                String body = "{" +
                        "\"name\":\"" + nameField.getText() + "\"," +
                        "\"empCode\":\"" + empCodeField.getText() + "\"," +
                        "\"departmentId\":" + deptId + "," +
                        "\"designation\":\"" + designationField.getText() + "\"," +
                        "\"email\":\"" + emailField.getText() + "\"," +
                        "\"phone\":\"" + phoneField.getText() + "\"," +
                        "\"joinDate\":\"" + joinDateField.getText() + "\"," +
                        "\"leaveDate\":null," +
                        "\"active\":1," +
                        "\"notes\":\"" + notesField.getText() + "\"" +
                        "}";
                if (postRequest(ConfigManager.getBaseUrl() + "/api/employees", body, 201))
                    loadEmployees(table);
            } catch (NumberFormatException ex) {
                showAlert("Error", "Dept ID must be a number.");
            }
        }
    }

    private String validateEmployee(String name, String empCode,
                                    String designation, String joinDate,
                                    String deptId) {
        if (name.trim().isEmpty())        return "Employee Name is required.";
        if (empCode.trim().isEmpty())     return "Employee Code is required.";
        if (designation.trim().isEmpty()) return "Designation is required.";
        if (!joinDate.trim().isEmpty() &&
                !joinDate.trim().matches("\\d{4}-\\d{2}-\\d{2}"))
            return "Join Date must be YYYY-MM-DD format.";
        if (!deptId.trim().isEmpty()) {
            try { Integer.parseInt(deptId.trim()); }
            catch (NumberFormatException e) { return "Dept ID must be a number."; }
        }
        return null;
    }

    private void showEditDialog(Employee emp, TableView<Employee> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Employee");
        dialog.setHeaderText("Editing: " + emp.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField        = new TextField(emp.getName());
        TextField designationField = new TextField(emp.getDesignation());
        TextField emailField       = new TextField(emp.getEmail());
        TextField phoneField       = new TextField(emp.getPhone());
        Label errorLabel           = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Name *:"),        0, 0); grid.add(nameField,        1, 0);
        grid.add(new Label("Designation *:"), 0, 1); grid.add(designationField, 1, 1);
        grid.add(new Label("Email:"),         0, 2); grid.add(emailField,       1, 2);
        grid.add(new Label("Phone:"),         0, 3); grid.add(phoneField,       1, 3);
        grid.add(errorLabel,                  1, 4);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (nameField.getText().trim().isEmpty()) {
                errorLabel.setText("Name is required.");
                event.consume();
            } else if (designationField.getText().trim().isEmpty()) {
                errorLabel.setText("Designation is required.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String body = "{" +
                    "\"id\":" + emp.getId() + "," +
                    "\"name\":\"" + nameField.getText() + "\"," +
                    "\"empCode\":\"" + emp.getEmpCode() + "\"," +
                    "\"departmentId\":0," +
                    "\"designation\":\"" + designationField.getText() + "\"," +
                    "\"email\":\"" + emailField.getText() + "\"," +
                    "\"phone\":\"" + phoneField.getText() + "\"," +
                    "\"joinDate\":null," +
                    "\"leaveDate\":null," +
                    "\"active\":1," +
                    "\"notes\":\"\"" +
                    "}";
            if (putRequest(ConfigManager.getBaseUrl() + "/api/employees/" + emp.getId(), body))
                loadEmployees(table);
        }
    }

    private void showDeactivateConfirm(Employee emp, TableView<Employee> table) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate Employee");
        confirm.setHeaderText(null);
        confirm.setContentText("Deactivate " + emp.getName() + "?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (deleteRequest(ConfigManager.getBaseUrl() + "/api/employees/" + emp.getId()))
                loadEmployees(table);
        }
    }

    private boolean postRequest(String url, String body, int expectedStatus) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == expectedStatus) {
                showAlert("Success", "Done successfully.");
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
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                showAlert("Success", "Updated successfully.");
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
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .DELETE().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                showAlert("Success", "Employee deactivated.");
                return true;
            }
            showAlert("Error", "Server returned: " + response.statusCode());
            return false;
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private void showSetLoginDialog(Employee emp) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Set Employee Login");
        dialog.setHeaderText("Set credentials for: " + emp.getName());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Choose a username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Minimum 6 characters");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Re-enter password");
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Username *:"), 0, 0); grid.add(usernameField, 1, 0);
        grid.add(new Label("Password *:"), 0, 1); grid.add(passwordField, 1, 1);
        grid.add(new Label("Confirm *:"),  0, 2); grid.add(confirmField,  1, 2);
        grid.add(errorLabel,               1, 3);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable check = () -> okButton.setDisable(
                usernameField.getText().trim().isEmpty()
                        || passwordField.getText().isEmpty());
        usernameField.textProperty().addListener((o, ov, nv) -> check.run());
        passwordField.textProperty().addListener((o, ov, nv) -> check.run());

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (passwordField.getText().length() < 6) {
                errorLabel.setText("Password must be at least 6 characters.");
                event.consume();
                return;
            }
            if (!passwordField.getText().equals(confirmField.getText())) {
                errorLabel.setText("Passwords do not match.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String body = "{" +
                        "\"username\":\"" + usernameField.getText() + "\"," +
                        "\"password\":\"" + passwordField.getText() + "\"" +
                        "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigManager.getBaseUrl()
                                + "/api/employee/credentials/" + emp.getId()))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    showAlert("Success",
                            "Login credentials set for " + emp.getName() + ".\n" +
                                    "Username: " + usernameField.getText());
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
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