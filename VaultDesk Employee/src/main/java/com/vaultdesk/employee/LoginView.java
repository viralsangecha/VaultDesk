package com.vaultdesk.employee;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.*;

public class LoginView {

    public Scene getScene(Stage stage) {

        Label appTitle = new Label("VaultDesk Employee");
        appTitle.getStyleClass().add("login-title");
        Label appSubtitle = new Label("Employee Self-Service Portal");
        appSubtitle.getStyleClass().add("login-subtitle");

        Label userLabel = new Label("Username");
        userLabel.getStyleClass().add("login-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.getStyleClass().add("login-field");

        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("login-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().add("login-field");

        Label forgotLabel = new Label(
                "Forgot password? Contact your IT administrator.");
        forgotLabel.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 11px;");

        Button loginButton = new Button("Sign In →");
        loginButton.getStyleClass().setAll("login-btn");
        loginButton.setStyle(
                "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-pref-width: 340px; -fx-pref-height: 42px;" +
                        "-fx-background-radius: 6; -fx-cursor: hand;");

        Label statusLabel = new Label("");
        statusLabel.getStyleClass().add("login-status-error");

        VBox card = new VBox(14);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(480);
        card.setPrefWidth(480);
        card.getChildren().addAll(
                appTitle, appSubtitle, new Label(""),
                userLabel, usernameField,
                passLabel, passwordField,
                forgotLabel, new Label(""),
                loginButton, statusLabel);

        StackPane root = new StackPane(card);
        StackPane.setAlignment(card, Pos.CENTER);
        root.getStyleClass().add("login-bg");
        root.setPadding(new Insets(40));

        Scene scene = new Scene(root, 1000, 700);
        ThemeManager.apply(scene);

        Runnable doLogin = () -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Please enter username and password.");
                return;
            }
            String body = "{\"username\":\"" + username
                    + "\",\"password\":\"" + password + "\"}";
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigManager.getBaseUrl()
                                + "/api/employee/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200) {
                    String rb      = resp.body();
                    int empId      = extractInt(rb, "employeeId");
                    String name    = extractValue(rb, "name");
                    String empCode = extractValue(rb, "empCode");
                    String desig   = extractValue(rb, "designation");
                    int deptId     = extractInt(rb, "departmentId");
                    String email   = extractValue(rb, "email");

                    SessionManager.get().login(empId, name,
                            empCode, desig, deptId, email);

                    // Save session
                    SessionStore.save(empId, name, empCode, desig,
                            deptId, email, username, sha256(password));

                    Scene dash = new DashboardView().getScene(stage);
                    ThemeManager.apply(dash);
                    stage.setScene(dash);
                } else {
                    statusLabel.setText("Invalid username or password.");
                }
            } catch (Exception ex) {
                statusLabel.setText("Cannot connect to server.");
            }
        };

        loginButton.setOnAction(e -> doLogin.run());
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) doLogin.run();
        });
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) doLogin.run();
        });

        return scene;
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
        int start = json.indexOf(search);
        if (start == -1) return 0;
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) end = json.length();
        try {
            return Integer.parseInt(
                    json.substring(start, end).trim().replace("}", ""));
        } catch (NumberFormatException e) { return 0; }
    }

    private String sha256(String input) {
        try {
            java.security.MessageDigest md =
                    java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }
}