package com.vaultdesk.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;
import java.util.Optional;

public class UserManagementView {

    public VBox getView() {

        Label title = new Label("User Management");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage system users and their roles.");
        subtitle.getStyleClass().add("page-subtitle");

        TableView<AdminUser> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<AdminUser, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d ->
                new SimpleStringProperty(
                        String.valueOf(d.getValue().getId())));
        idCol.setMaxWidth(50);

        TableColumn<AdminUser, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getUsername()));

        TableColumn<AdminUser, String> fullNameCol = new TableColumn<>("Full Name");
        fullNameCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getFullName()));

        // Role — colored
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

        // Status — colored
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
        createdCol.setCellValueFactory(d -> {
            String ca = d.getValue().getCreatedAt();
            return new SimpleStringProperty(
                    ca != null && ca.length() >= 10
                            ? ca.substring(0, 10) : ca);
        });

        // Actions
        TableColumn<AdminUser, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn       = new Button("Edit");
            private final Button deactivateBtn = new Button("Deactivate");
            {
                editBtn.getStyleClass().setAll("btn-warning");
                editBtn.setStyle(
                        "-fx-background-color: #b45309; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 5 10 5 10;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;");
                deactivateBtn.getStyleClass().setAll("btn-danger");
                deactivateBtn.setStyle(
                        "-fx-background-color: #da3633; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 5 10 5 10;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;");

                editBtn.setOnAction(e -> {
                    AdminUser u = getTableView().getItems().get(getIndex());
                    showEditDialog(u, getTableView());
                });
                deactivateBtn.setOnAction(e -> {
                    AdminUser u = getTableView().getItems().get(getIndex());
                    // Prevent deactivating yourself
                    if (u.getId() == SessionManager.get().getUserId()) {
                        showAlert("Error",
                                "You cannot deactivate your own account.");
                        return;
                    }
                    showDeactivateConfirm(u, getTableView());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, editBtn, deactivateBtn));
            }
        });

        table.getColumns().addAll(idCol, usernameCol, fullNameCol,
                roleCol, statusCol, createdCol, actionCol);

        // ── Add user button ───────────────────────────────
        Button addBtn = new Button("＋ Add User");
        addBtn.getStyleClass().setAll("btn-primary");
        addBtn.setStyle(
                "-fx-background-color: #238636; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 8 16 8 16;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showAddDialog(table));

        HBox topBar = new HBox(10, addBtn);

        loadUsers(table);

        VBox root = new VBox(12, title, subtitle, topBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    // ── Load users ────────────────────────────────────────
    private void loadUsers(TableView<AdminUser> table) {
        table.getItems().clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/users"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
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
                            extractValue(obj, "createdAt")
                    ));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading users: " + ex.getMessage());
        }
    }

    // ── Add user dialog ───────────────────────────────────
    private void showAddDialog(TableView<AdminUser> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add User");
        dialog.setHeaderText("Create a new system user");
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Login username");
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full display name");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Minimum 6 characters");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Re-enter password");

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("ADMIN", "ENGINEER", "CONTRACT");
        roleBox.setValue("ENGINEER");

        Label errorLabel = new Label("");
        errorLabel.setStyle(
                "-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Username *:"),  0, 0); grid.add(usernameField, 1, 0);
        grid.add(new Label("Full Name *:"), 0, 1); grid.add(fullNameField, 1, 1);
        grid.add(new Label("Password *:"),  0, 2); grid.add(passwordField, 1, 2);
        grid.add(new Label("Confirm *:"),   0, 3); grid.add(confirmField,  1, 3);
        grid.add(new Label("Role:"),        0, 4); grid.add(roleBox,       1, 4);
        grid.add(errorLabel,                1, 5);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        Runnable check = () -> okButton.setDisable(
                usernameField.getText().trim().isEmpty()
                        || fullNameField.getText().trim().isEmpty()
                        || passwordField.getText().isEmpty());

        usernameField.textProperty().addListener((o, ov, nv) -> check.run());
        fullNameField.textProperty().addListener((o, ov, nv) -> check.run());
        passwordField.textProperty().addListener((o, ov, nv) -> check.run());

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String err = validateNewUser(
                    usernameField.getText(),
                    fullNameField.getText(),
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
                        "\"username\":\"" + usernameField.getText() + "\"," +
                        "\"password\":\"" + passwordField.getText() + "\"," +
                        "\"fullName\":\"" + fullNameField.getText() + "\"," +
                        "\"role\":\"" + roleBox.getValue() + "\"" +
                        "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/users"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 201) {
                    showAlert("Success", "User created successfully.");
                    loadUsers(table);
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private String validateNewUser(String username, String fullName,
                                   String password, String confirm) {
        if (username.trim().isEmpty())  return "Username is required.";
        if (fullName.trim().isEmpty())  return "Full name is required.";
        if (password.length() < 6)
            return "Password must be at least 6 characters.";
        if (!password.equals(confirm))
            return "Passwords do not match.";
        return null;
    }

    // ── Edit dialog ───────────────────────────────────────
    private void showEditDialog(AdminUser user,
                                TableView<AdminUser> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Editing: " + user.getUsername());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField fullNameField = new TextField(user.getFullName());
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("ADMIN", "ENGINEER", "CONTRACT");
        roleBox.setValue(user.getRole());

        Label noteLabel = new Label(
                "Note: password change not supported here.");
        noteLabel.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 11px;");
        Label errorLabel = new Label("");
        errorLabel.setStyle(
                "-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Full Name *:"), 0, 0); grid.add(fullNameField, 1, 0);
        grid.add(new Label("Role:"),        0, 1); grid.add(roleBox,       1, 1);
        grid.add(noteLabel,                 1, 2);
        grid.add(errorLabel,                1, 3);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (fullNameField.getText().trim().isEmpty()) {
                errorLabel.setText("Full name is required.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String body = "{" +
                        "\"fullName\":\"" + fullNameField.getText() + "\"," +
                        "\"role\":\"" + roleBox.getValue() + "\"" +
                        "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "http://localhost:8080/api/users/" + user.getId()))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    showAlert("Success", "User updated.");
                    loadUsers(table);
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    // ── Deactivate confirm ────────────────────────────────
    private void showDeactivateConfirm(AdminUser user,
                                       TableView<AdminUser> table) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Deactivate User");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "Deactivate user '" + user.getUsername() + "'?\n" +
                        "They will no longer be able to log in.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(
                                "http://localhost:8080/api/users/" + user.getId()))
                        .DELETE().build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    showAlert("Success", "User deactivated.");
                    loadUsers(table);
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