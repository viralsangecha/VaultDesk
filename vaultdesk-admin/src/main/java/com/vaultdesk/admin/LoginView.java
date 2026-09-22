package com.vaultdesk.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.*;
import java.util.List;
import java.util.Optional;

public class LoginView {

    public Scene getScene(Stage stage) {

        // ── Logo ──────────────────────────────────────────
        Label appTitle = new Label("VaultDesk");
        appTitle.getStyleClass().add("login-title");
        Label appSubtitle = new Label("IT Asset Management System");
        appSubtitle.getStyleClass().add("login-subtitle");

        // ── Fields ────────────────────────────────────────
        Label userLabel = new Label("Username");
        userLabel.getStyleClass().add("login-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");
        usernameField.getStyleClass().add("login-field");

        Label passLabel = new Label("Password");
        passLabel.getStyleClass().add("login-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.getStyleClass().add("login-field");

        // ── Forgot password ───────────────────────────────
        Label forgotLabel = new Label("Forgot password?");
        forgotLabel.setStyle("-fx-text-fill: #58a6ff; -fx-font-size: 11px;" +
                "-fx-cursor: hand; -fx-underline: true;");
        forgotLabel.setOnMouseClicked(e -> showForgotPasswordDialog());

        // ── Sign in button ────────────────────────────────
        Button loginButton = new Button("Sign In to Dashboard →");
        loginButton.getStyleClass().add("login-btn");
        loginButton.setStyle("-fx-pref-width: 340px; -fx-pref-height: 42px;");

        // ── Theme toggle ──────────────────────────────────
        Button themeBtn = new Button("☀ Light Mode");
        themeBtn.getStyleClass().add("text-muted");
        themeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-font-size: 11px;" +
                        "-fx-cursor: hand; -fx-border-width: 0;");

        // ── Status ────────────────────────────────────────
        Label statusLabel = new Label("");
        statusLabel.getStyleClass().add("login-status-error");

        // ── Card ──────────────────────────────────────────
        VBox card = new VBox(14);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(480);
        card.setPrefWidth(480);
        card.getChildren().addAll(
                appTitle, appSubtitle,
                new Label(""),
                userLabel, usernameField,
                passLabel, passwordField,
                forgotLabel,
                new Label(""),
                loginButton,
                statusLabel,
                themeBtn
        );


        StackPane root = new StackPane(card);
        StackPane.setAlignment(card, Pos.CENTER);
        root.getStyleClass().add("login-bg");
        root.setPadding(new Insets(40));
        AnimationUtil.popIn(card);

        javafx.scene.shape.Rectangle sweep = new javafx.scene.shape.Rectangle(0, 5);
        sweep.setFill(javafx.scene.paint.Color.web("#3E7BFA"));
        sweep.widthProperty().bind(card.widthProperty());
        sweep.setOpacity(0);
        StackPane.setAlignment(sweep, Pos.TOP_CENTER);
        root.getChildren().add(sweep); // root is the existing StackPane(card) — sweep sits beside card, not inside it

        javafx.animation.PauseTransition sweepDelay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
        sweepDelay.setOnFinished(e -> {
            sweep.setOpacity(0.9);
            javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(5000), sweep);
            fade.setFromValue(0.9);
            fade.setToValue(0);
            fade.setOnFinished(ev -> root.getChildren().remove(sweep));
            fade.play();
        });
        sweepDelay.play();

        Scene scene = new Scene(root, 1200, 800);
        ThemeManager.apply(scene);

        // ── Login action ──────────────────────────────────
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
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigManager.getBaseUrl() + "/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String rb       = response.body();
                    String fullName = extractValue(rb, "fullName");
                    String role     = extractValue(rb, "role");
                    int userId      = extractInt(rb, "userId");
                    int deptId      = extractInt(rb, "deptId");
                    String token    = extractValue(rb, "token");

                    // ── Parse permissions array ───────────────────────
                    List<String> permissions = extractList(rb, "permissions");

                    SessionManager.get().login(userId, fullName,
                            role, permissions);
                    SessionManager.get().setDeptId(deptId);
                    SessionManager.get().setToken(token);

                    SessionStore.save(userId, fullName, role,
                            username, sha256(password), deptId);

                    System.out.println("Login OK — user: " + fullName
                            + " role: " + role + " id: " + userId);


                    try {
                        Scene dash = new DashboardView(fullName, role).getScene(stage);
                        ThemeManager.apply(dash);
                        stage.setScene(dash);
                    } catch (Exception dashEx) {
                        System.out.println("Dashboard load error: " + dashEx.getMessage());
                        dashEx.printStackTrace();
                        statusLabel.setText("Dashboard load failed: " + dashEx.getMessage());
                    }
                } else {
                    statusLabel.setText("Invalid username or password.");
                }
            } catch (Exception ex) {
                System.out.println("Connection error: " + ex.getMessage());
                ex.printStackTrace();
                statusLabel.setText("Cannot connect to server.");
            }
        };

        loginButton.setOnAction(e -> doLogin.run());

        // ── Enter key triggers login ──────────────────────
        passwordField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) doLogin.run();
        });
        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) doLogin.run();
        });

        // ── Theme toggle ──────────────────────────────────
        themeBtn.setOnAction(e -> {
            ThemeManager.toggle();
            ThemeManager.apply(scene);
            themeBtn.setText(ThemeManager.getCurrent() ==
                    ThemeManager.Theme.DARK ? "☀ Light Mode" : "🌙 Dark Mode");
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
    private List<String> extractList(String json, String key) {
        List<String> result = new java.util.ArrayList<>();
        String search = "\"" + key + "\":[";
        int start = json.indexOf(search);
        if (start == -1) return result;
        start += search.length();
        int end = json.indexOf("]", start);
        if (end == -1) return result;
        String array = json.substring(start, end);
        if (array.trim().isEmpty()) return result;
        for (String item : array.split(",")) {
            String cleaned = item.trim()
                    .replace("\"", "").trim();
            if (!cleaned.isEmpty()) result.add(cleaned);
        }
        return result;
    }
    private void showForgotPasswordDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Forgot Password");
        dialog.setHeaderText("Enter your username to receive a reset link by email.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        VBox content = new VBox(10, new Label("Username:"), usernameField);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String username = usernameField.getText().trim();
            if (username.isEmpty()) return;
            try {
                String body = "{\"username\":\"" + username + "\",\"userType\":\"ADMIN\"}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigManager.getBaseUrl() + "/api/forgot-password"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                boolean success = resp.body().contains("\"success\":true");
                String message = extractValue(resp.body(), "message");

                Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                ThemeManager.applyToDialog(alert);
                alert.setTitle(success ? "Check Your Email" : "Not Found");
                alert.setHeaderText(null);
                alert.setContentText(!message.isEmpty() ? message : "Something went wrong.");
                alert.showAndWait();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                ThemeManager.applyToDialog(alert);
                alert.setContentText("Cannot connect to server.");
                alert.showAndWait();
            }
        }
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