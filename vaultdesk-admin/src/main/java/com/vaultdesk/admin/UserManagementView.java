package com.vaultdesk.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.http.*;
import java.util.Optional;

public class UserManagementView {

    public VBox getView() {
        Label bcRoot = new Label("SYSTEM");
        bcRoot.getStyleClass().add("breadcrumb-root");
        Label bcSep = new Label("  /  ");
        bcSep.getStyleClass().add("breadcrumb-sep");
        Label bcCurrent = new Label("USER MANAGEMENT");
        bcCurrent.getStyleClass().add("breadcrumb-current");
        HBox breadcrumb = new HBox(bcRoot, bcSep, bcCurrent);

        Label title = new Label("User Management");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage system users and their roles.");
        subtitle.getStyleClass().add("page-subtitle");

        TableView<AdminUser> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> {
            TableRow<AdminUser> row = new TableRow<>();

            ContextMenu rowMenu = new ContextMenu();
            MenuItem editItem = new MenuItem("✏ Edit User");
            editItem.setOnAction(e -> showEditDialog(row.getItem(), table));
            rowMenu.getItems().add(editItem);

            MenuItem permItem = new MenuItem("🔑 Permissions");
            permItem.setOnAction(e -> showPermissionDialog(row.getItem()));
            rowMenu.getItems().add(permItem);

            MenuItem toggleItem = new MenuItem();
            rowMenu.getItems().add(toggleItem);
            rowMenu.setOnShowing(ev -> {
                AdminUser u = row.getItem();
                if (u == null) return;
                if (u.getId() == SessionManager.get().getUserId()) {
                    toggleItem.setText("(Cannot deactivate own account)");
                    toggleItem.setDisable(true);
                } else {
                    toggleItem.setDisable(false);
                    toggleItem.setText(u.isActive() ? "🚫 Deactivate" : "✔ Reactivate");
                    toggleItem.setOnAction(e -> {
                        if (u.isActive()) {
                            showDeactivateConfirm(u, table);
                        } else {
                            reactivateUser(u, table);
                        }
                    });
                }
            });

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu)
            );

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty())
                    showEditDialog(row.getItem(), table);
            });
            return row;
        });

        TableColumn<AdminUser, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d ->
                new SimpleStringProperty(
                        String.valueOf(d.getValue().getId())));
        idCol.setMaxWidth(50);

        TableColumn<AdminUser, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getUsername()));
        usernameCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); getStyleClass().remove("data-mono"); return; }
                setText(item);
                if (!getStyleClass().contains("data-mono")) getStyleClass().add("data-mono");
            }
        });

        TableColumn<AdminUser, String> fullNameCol = new TableColumn<>("Full Name");
        fullNameCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getFullName()));

        TableColumn<AdminUser, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getRole()));
        roleCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "ADMIN"    -> setStyle(
                            "-fx-text-fill: #f85149; -fx-font-weight: bold;");
                    case "ENGINEER" -> setStyle(
                            "-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
                    case "CONTRACT" -> setStyle(
                            "-fx-text-fill: #d29922; -fx-font-weight: bold;");
                    default -> setStyle("-fx-text-fill: #8b949e;");
                }
            }
        });

        TableColumn<AdminUser, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().isActive() ? "Active" : "Inactive"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if ("Active".equals(item))
                    setStyle("-fx-text-fill: #3fb950; -fx-font-weight: bold;");
                else
                    setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
            }
        });

        TableColumn<AdminUser, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(d ->
                new SimpleStringProperty(DateTimeFormatUtil.toIndianDateTime(d.getValue().getCreatedAt())));

        TableColumn<AdminUser, String> lastLoginCol = new TableColumn<>("Last Login");
        lastLoginCol.setCellValueFactory(d -> {
            String ll = d.getValue().getLastLogin();
            return new SimpleStringProperty(
                    ll == null || ll.trim().isEmpty() ? "Never" : DateTimeFormatUtil.toIndianDateTime(ll));
        });

        table.getColumns().addAll(idCol, usernameCol, fullNameCol,
                roleCol, statusCol, createdCol, lastLoginCol);

        Button addBtn = new Button("＋ Add User");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddDialog(table));
        AnimationUtil.addHoverScale(addBtn);

        Label rightClickHint = new Label("Right-click a row for Edit / Permissions / Deactivate.");
        rightClickHint.getStyleClass().add("text-muted");
        rightClickHint.setStyle("-fx-font-size: 11px;");
        HBox topBar = new HBox(10, addBtn, rightClickHint);
        topBar.setAlignment(Pos.CENTER_LEFT);

        loadUsers(table);

        VBox tableWrapper = new VBox(table);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);

        VBox root = new VBox(12, breadcrumb, new VBox(4, title, subtitle), topBar, tableWrapper);
        return root;
    }

    // ── Load users (now includes inactive, so Reactivate is reachable) ──
    private void loadUsers(TableView<AdminUser> table) {
        table.getItems().clear();
        try {
            HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/users/all");
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    table.getItems().add(new AdminUser(
                            extractInt(obj, "id"),
                            extractValue(obj, "username"),
                            extractValue(obj, "fullName"),
                            extractValue(obj, "role"),
                            extractInt(obj, "active"),
                            extractValue(obj, "createdAt"),
                            extractValue(obj, "lastLogin")
                    ));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading users: " + ex.getMessage());
        }
    }

    private void reactivateUser(AdminUser user, TableView<AdminUser> table) {
        try {
            HttpResponse<String> resp = ApiClient.putNoBody(
                    ConfigManager.getBaseUrl() + "/api/users/" + user.getId() + "/reactivate");
            if (resp.statusCode() == 200) {
                ToastUtil.success(user.getFullName() + " reactivated.");
                loadUsers(table);
            } else {
                showAlert("Error", "Server returned: " + resp.statusCode());
            }
        } catch (Exception ex) {
            showAlert("Error", ex.getMessage());
        }
    }

    // ── Add user dialog ───────────────────────────────────
    private void showAddDialog(TableView<AdminUser> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Add User");
        dialog.setHeaderText("Create a new system user");
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField usernameField = new TextField();
        TextField emailField = new TextField();
        emailField.setPromptText("User email address");
        usernameField.setPromptText("Login username");
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full display name");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Minimum 6 characters");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Re-enter password");

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("ADMIN", "ENGINEER", "DEPT_HOD");
        roleBox.setValue("ENGINEER");

        NumberField deptIdField = new NumberField();
        deptIdField.setPromptText("Required for DEPT_ADMIN");

        Label errorLabel = new Label("");
        errorLabel.setStyle(
                "-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(100);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

        grid.add(new Label("Email *:"),0,0);
        grid.add(emailField,1,0);
        grid.add(new Label("Username *:"),  0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(new Label("Full Name *:"), 0, 2);
        grid.add(fullNameField, 1, 2);
        grid.add(new Label("Password *:"),  0, 3);
        grid.add(passwordField, 1, 3);
        grid.add(new Label("Confirm *:"),   0, 4);
        grid.add(confirmField,  1, 4);
        grid.add(new Label("Role:"),        0, 5);
        grid.add(roleBox,       1, 5);
        grid.add(new Label("Dept ID:"),     0, 6);
        grid.add(deptIdField,   1, 6);
        grid.add(errorLabel,                1, 7);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable check = () -> okButton.setDisable(
                emailField.getText().trim().isEmpty()
                        || usernameField.getText().trim().isEmpty()
                        || fullNameField.getText().trim().isEmpty()
                        || passwordField.getText().isEmpty());

        emailField.textProperty().addListener((o, ov, nv) -> check.run());
        usernameField.textProperty().addListener((o, ov, nv) -> check.run());
        fullNameField.textProperty().addListener((o, ov, nv) -> check.run());
        passwordField.textProperty().addListener((o, ov, nv) -> check.run());

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String err = validateNewUser(
                    usernameField.getText(),
                    fullNameField.getText(),
                    emailField.getText(),
                    passwordField.getText(),
                    confirmField.getText());
            if (err != null) {
                errorLabel.setText(err);
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String body = "{" +
                        "\"email\":\"" + escapeJson(emailField.getText().trim()) + "\"," +
                        "\"username\":\"" + escapeJson(usernameField.getText().trim()) + "\"," +
                        "\"password\":\"" + passwordField.getText() + "\"," +
                        "\"fullName\":\"" + escapeJson(fullNameField.getText().trim()) + "\"," +
                        "\"role\":\"" + roleBox.getValue() + "\"," +
                        "\"deptId\":" + deptIdField.getIntValue() +
                        "}";
                HttpResponse<String> resp = ApiClient.post(ConfigManager.getBaseUrl() + "/api/users", body);
                if (resp.statusCode() == 201) {
                    ToastUtil.success("User created successfully.");
                    loadUsers(table);
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private String validateNewUser(String username, String fullName, String email,
                                   String password, String confirm) {
        if (!ValidationUtil.isNotBlank(username))  return "Username is required.";
        if (!ValidationUtil.isNotBlank(fullName))  return "Full name is required.";
        if (!ValidationUtil.isNotBlank(email))     return "Email is required.";
        if (!ValidationUtil.isValidEmail(email))   return "Please enter a valid email address.";
        if (password.length() < 6)
            return "Password must be at least 6 characters.";
        if (!password.equals(confirm))
            return "Passwords do not match.";
        return null;
    }

    // ── Edit dialog ───────────────────────────────────────
    private void showEditDialog(AdminUser user, TableView<AdminUser> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Editing: " + user.getUsername());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField fullNameField = new TextField(user.getFullName());
        TextField username  =new TextField(user.getUsername());
        TextField emailField = new TextField();
        NumberField deptIdField = new NumberField();
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("ADMIN", "ENGINEER", "DEPT_HOD");
        roleBox.setValue(user.getRole());

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        try {
            HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/users/all");
            String body = resp.body();
            for (String obj : body.substring(1, body.length() - 1).split("\\},\\{")) {
                String cleaned = obj.replace("{", "").replace("}", "");
                if (extractInt(cleaned, "id") == user.getId()) {
                    emailField.setText(extractValue(cleaned, "email"));
                    deptIdField.setText(String.valueOf(extractInt(cleaned, "deptId")));
                    break;
                }
            }
        } catch (Exception ignored) {}

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(100);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);

        grid.add(new Label("Full Name *:"), 0, 0); grid.add(fullNameField, 1, 0);
        grid.add(new Label("Username *:"), 0, 1); grid.add(username, 1, 1);
        grid.add(new Label("Email:"), 0, 2);       grid.add(emailField, 1, 2);
        grid.add(new Label("Role:"), 0, 3);        grid.add(roleBox, 1, 3);
        grid.add(new Label("Dept ID:"), 0, 4);     grid.add(deptIdField, 1, 4);
        grid.add(errorLabel, 1, 5);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!ValidationUtil.isNotBlank(fullNameField.getText())) {
                errorLabel.setText("Full name is required.");
                event.consume();
            } else if (!emailField.getText().trim().isEmpty() && !ValidationUtil.isValidEmail(emailField.getText())) {
                errorLabel.setText("Please enter a valid email address.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String body = "{" +
                        "\"fullName\":\"" + escapeJson(fullNameField.getText()) + "\"," +
                        "\"username\":\"" + escapeJson(username.getText()) + "\"," +
                        "\"email\":\"" + escapeJson(emailField.getText()) + "\"," +
                        "\"role\":\"" + roleBox.getValue() + "\"," +
                        "\"deptId\":" + deptIdField.getIntValue() +
                        "}";
                HttpResponse<String> resp = ApiClient.put(ConfigManager.getBaseUrl() + "/api/users/" + user.getId(), body);
                if (resp.statusCode() == 200) {
                    ToastUtil.success("User updated.");
                    loadUsers(table);
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "'");
    }

    // ── Deactivate confirm ────────────────────────────────
    private void showDeactivateConfirm(AdminUser user,
                                       TableView<AdminUser> table) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        ThemeManager.applyToDialog(confirm);
        confirm.setTitle("Deactivate User");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Deactivate user '" + user.getUsername() + "'?\n" +
                        "They will no longer be able to log in.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                HttpResponse<String> resp = ApiClient.delete(
                        ConfigManager.getBaseUrl() + "/api/users/" + user.getId());
                if (resp.statusCode() == 200) {
                    ToastUtil.success("User deactivated.");
                    loadUsers(table);
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private void showPermissionDialog(AdminUser user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Permissions — " + user.getFullName());
        dialog.setHeaderText("Manage permissions for: "
                + user.getUsername());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(520);

        Label presetLabel = new Label("Quick Preset:");
        presetLabel.getStyleClass().add("text-muted");
        presetLabel.setStyle("-fx-font-size: 12px;");
        ComboBox<String> presetBox = new ComboBox<>();
        presetBox.getItems().addAll(
                "ADMIN", "ENGINEER", "DEPT_HOD", "VIEWER", "CUSTOM");
        presetBox.setValue("CUSTOM");

        HBox presetRow = new HBox(10, presetLabel, presetBox);
        presetRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        java.util.Map<String, java.util.List<String>> groups =
                new java.util.LinkedHashMap<>();
        groups.put("🎫 Tickets", java.util.List.of(
                "VIEW_ALL_TICKETS", "VIEW_ASSIGNED_TICKETS",
                "UPDATE_TICKET_STATUS", "ASSIGN_TICKET","VIEW_TICKET_DETAILS"));
        groups.put("▣ Assets", java.util.List.of(
                "VIEW_ALL_ASSETS", "VIEW_DEPT_ASSETS",
                "ADD_ASSET", "EDIT_ASSET", "IMPORT_ASSETS","VIEW_ASSET_DETAILS"));
        groups.put("👤 Employees", java.util.List.of(
                "VIEW_EMPLOYEES", "ADD_EMPLOYEE",
                "EDIT_EMPLOYEE", "IMPORT_EMPLOYEES", "SET_LOGIN"));
        groups.put("🏢 Departments", java.util.List.of(
                "VIEW_DEPARTMENTS", "ADD_DEPARTMENT"));
        groups.put("🔑 Licenses", java.util.List.of(
                "VIEW_LICENSES", "ADD_LICENSE"));
        groups.put("📦 Consumables", java.util.List.of(
                "VIEW_CONSUMABLES", "ADD_CONSUMABLE"));
        groups.put("🔧 Maintenance", java.util.List.of(
                "VIEW_MAINTENANCE", "ADD_MAINTENANCE"));
        groups.put("🤝 Vendors", java.util.List.of(
                "VIEW_VENDORS", "ADD_VENDOR"));
        groups.put("⚙ System", java.util.List.of(
                "MANAGE_USERS", "MANAGE_SETTINGS"));

        java.util.Set<String> currentPerms = new java.util.HashSet<>();
        try {
            HttpResponse<String> resp = ApiClient.get(
                    ConfigManager.getBaseUrl() + "/api/users/" + user.getId() + "/permissions");
            String body = resp.body();
            String search = "\"permissions\":[";
            int start = body.indexOf(search);
            if (start != -1) {
                start += search.length();
                int end = body.indexOf("]", start);
                if (end != -1) {
                    String array = body.substring(start, end);
                    for (String item : array.split(",")) {
                        String cleaned = item.trim()
                                .replace("\"", "").trim();
                        if (!cleaned.isEmpty())
                            currentPerms.add(cleaned);
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading permissions: "
                    + ex.getMessage());
        }

        java.util.Map<String, CheckBox> checkBoxMap =
                new java.util.LinkedHashMap<>();
        VBox allGroups = new VBox(12);

        for (java.util.Map.Entry<String,
                java.util.List<String>> entry : groups.entrySet()) {
            Label groupLabel = new Label(entry.getKey());
            groupLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

            javafx.scene.layout.GridPane grid =
                    new javafx.scene.layout.GridPane();
            grid.setHgap(16); grid.setVgap(6);
            grid.setPadding(new javafx.geometry.Insets(4, 0, 4, 8));

            int col = 0, row = 0;
            for (String perm : entry.getValue()) {
                CheckBox cb = new CheckBox(
                        perm.replace("_", " ").toLowerCase());
                cb.setSelected(currentPerms.contains(perm));
                checkBoxMap.put(perm, cb);
                grid.add(cb, col, row);
                col++;
                if (col >= 2) { col = 0; row++; }
            }

            VBox groupBox = new VBox(6, groupLabel, grid);
            groupBox.getStyleClass().add("surface-card");
            groupBox.setStyle("-fx-background-radius: 6; -fx-padding: 10;");
            allGroups.getChildren().add(groupBox);
        }

        presetBox.setOnAction(e -> {
            String preset = presetBox.getValue();
            if ("CUSTOM".equals(preset)) return;

            checkBoxMap.values().forEach(cb -> cb.setSelected(false));

            java.util.List<String> presetPerms =
                    getPresetPermissions(preset);
            for (String perm : presetPerms) {
                CheckBox cb = checkBoxMap.get(perm);
                if (cb != null) cb.setSelected(true);
            }
        });

        ScrollPane scroll = new ScrollPane(allGroups);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);
        scroll.getStyleClass().add("content-scroll");
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox content = new VBox(12, presetRow, scroll);
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            java.util.List<String> selected = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, CheckBox> entry
                    : checkBoxMap.entrySet()) {
                if (entry.getValue().isSelected())
                    selected.add(entry.getKey());
            }

            StringBuilder json = new StringBuilder(
                    "{\"permissions\":[");
            for (int i = 0; i < selected.size(); i++) {
                json.append("\"").append(selected.get(i)).append("\"");
                if (i < selected.size() - 1) json.append(",");
            }
            json.append("]}");

            try {
                HttpResponse<String> resp = ApiClient.put(
                        ConfigManager.getBaseUrl() + "/api/users/" + user.getId() + "/permissions", json.toString());
                if (resp.statusCode() == 200) {
                    ToastUtil.success("Permissions updated for " + user.getFullName());
                } else {
                    showAlert("Error",
                            "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private java.util.List<String> getPresetPermissions(String preset) {
        return switch (preset) {
            case "ADMIN" -> java.util.List.of(
                    "VIEW_ALL_TICKETS", "VIEW_ASSIGNED_TICKETS",
                    "UPDATE_TICKET_STATUS", "ASSIGN_TICKET",
                    "DELETE_TICKET", "VIEW_TICKET_DETAILS",
                    "VIEW_ALL_ASSETS", "VIEW_DEPT_ASSETS",
                    "ADD_ASSET", "EDIT_ASSET", "IMPORT_ASSETS",
                    "VIEW_ASSET_DETAILS",
                    "VIEW_EMPLOYEES", "ADD_EMPLOYEE",
                    "EDIT_EMPLOYEE", "SET_LOGIN", "VIEW_DEPARTMENTS",
                    "ADD_DEPARTMENT", "VIEW_REPORTS", "VIEW_LICENSES",
                    "ADD_LICENSE", "VIEW_CONSUMABLES", "ADD_CONSUMABLE",
                    "VIEW_MAINTENANCE", "ADD_MAINTENANCE",
                    "VIEW_VENDORS", "ADD_VENDOR",
                    "MANAGE_USERS", "MANAGE_SETTINGS");
            case "ENGINEER" -> java.util.List.of(
                    "VIEW_ASSIGNED_TICKETS", "UPDATE_TICKET_STATUS",
                    "VIEW_TICKET_DETAILS",
                    "VIEW_ALL_ASSETS", "VIEW_ASSET_DETAILS",
                    "VIEW_LICENSES",
                    "VIEW_CONSUMABLES", "VIEW_MAINTENANCE",
                    "VIEW_VENDORS");
            case "DEPT_HOD" -> java.util.List.of(
                    "VIEW_ALL_TICKETS", "UPDATE_TICKET_STATUS",
                    "ASSIGN_TICKET", "VIEW_TICKET_DETAILS",
                    "VIEW_DEPT_ASSETS", "VIEW_ASSET_DETAILS",
                    "ADD_ASSET", "EDIT_ASSET", "VIEW_EMPLOYEES",
                    "ADD_EMPLOYEE", "EDIT_EMPLOYEE", "SET_LOGIN",
                    "VIEW_REPORTS", "VIEW_LICENSES",
                    "VIEW_CONSUMABLES", "VIEW_MAINTENANCE",
                    "VIEW_VENDORS");
            case "VIEWER" -> java.util.List.of(
                    "VIEW_ALL_TICKETS", "VIEW_TICKET_DETAILS",
                    "VIEW_ALL_ASSETS", "VIEW_ASSET_DETAILS",
                    "VIEW_EMPLOYEES", "VIEW_DEPARTMENTS",
                    "VIEW_REPORTS", "VIEW_LICENSES",
                    "VIEW_CONSUMABLES", "VIEW_MAINTENANCE",
                    "VIEW_VENDORS");
            default -> java.util.List.of();
        };
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