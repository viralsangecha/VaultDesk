package com.vaultdesk.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.*;

public class LoginView {

    public Scene getScene(Stage stage) {

        Label appTitle = new Label("VaultDesk");
        appTitle.getStyleClass().add("login-title");

        Label appSubtitle = new Label("IT Asset Management System");
        appSubtitle.getStyleClass().add("login-subtitle");

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

        Button loginButton = new Button("Sign In to Dashboard →");
        loginButton.getStyleClass().setAll("login-btn");
        loginButton.setStyle(
                "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-pref-width: 324px; -fx-pref-height: 42px;" +
                        "-fx-background-radius: 6; -fx-border-radius: 6; -fx-cursor: hand;");

        Label statusLabel = new Label("");
        statusLabel.getStyleClass().add("login-status-error");

        VBox card = new VBox(14);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(420);
        card.getChildren().addAll(
                appTitle, appSubtitle,
                new Label(""),
                userLabel, usernameField,
                passLabel, passwordField,
                new Label(""),
                loginButton,
                statusLabel
        );

        StackPane root = new StackPane(card);
        StackPane.setAlignment(card, Pos.CENTER);
        root.getStyleClass().add("login-bg");

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm());

        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            String body = "{\"username\":\"" + username
                    + "\",\"password\":\"" + password + "\"}";

            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = client.send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    String fullName = extractValue(responseBody, "fullName");
                    String role     = extractValue(responseBody, "role");
                    stage.setScene(new DashboardView(fullName, role).getScene(stage));
                } else {
                    statusLabel.setText("Invalid username or password.");
                }
            } catch (Exception ex) {
                statusLabel.setText("Cannot connect to server.");
            }
        });

        return scene;
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search) + search.length();
        int end   = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}