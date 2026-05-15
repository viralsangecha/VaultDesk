package com.vaultdesk.employee;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;

public class SettingsView {

    public VBox getView() {
        Label pageTitle = new Label("Settings");
        pageTitle.getStyleClass().add("page-title");
        Label pageSub = new Label("Manage your account and preferences.");
        pageSub.getStyleClass().add("page-subtitle");

        VBox root = new VBox(20, pageTitle, pageSub);

        // ── Change Password ───────────────────────────────
        root.getChildren().add(
                sectionCard("🔒  Change Password",
                        buildPasswordSection()));

        // ── Server config ─────────────────────────────────
        root.getChildren().add(
                sectionCard("🖥  Server Configuration",
                        buildServerSection()));

        // ── About ─────────────────────────────────────────
        root.getChildren().add(
                sectionCard("ℹ  About",
                        buildAboutSection()));

        return root;
    }

    private VBox buildPasswordSection() {
        PasswordField currentField = new PasswordField();
        currentField.setPromptText("Current password");
        currentField.setMaxWidth(300);

        PasswordField newField = new PasswordField();
        newField.setPromptText("New password (min 6 chars)");
        newField.setMaxWidth(300);

        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Confirm new password");
        confirmField.setMaxWidth(300);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px;");

        Button changeBtn = new Button("Change Password");
        changeBtn.getStyleClass().setAll("btn-warning");
        changeBtn.setStyle(
                "-fx-background-color: #b45309; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 8 16 8 16;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");

        changeBtn.setOnAction(e -> {
            String current = currentField.getText();
            String newPwd  = newField.getText();
            String confirm = confirmField.getText();

            if (current.isEmpty() || newPwd.isEmpty()) {
                statusLabel.setText("All fields required.");
                statusLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 12px;");
                return;
            }
            if (newPwd.length() < 6) {
                statusLabel.setText(
                        "Password must be at least 6 characters.");
                statusLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 12px;");
                return;
            }
            if (!newPwd.equals(confirm)) {
                statusLabel.setText("Passwords do not match.");
                statusLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 12px;");
                return;
            }
            try {
                String body = "{\"currentPassword\":\"" + current
                        + "\",\"newPassword\":\"" + newPwd + "\"}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigManager.getBaseUrl()
                                + "/api/employee/auth/password/"
                                + SessionManager.get().getEmployeeId()))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    statusLabel.setText("✔ Password changed successfully.");
                    statusLabel.setStyle(
                            "-fx-text-fill: #3fb950; -fx-font-size: 12px;");
                    currentField.clear();
                    newField.clear();
                    confirmField.clear();
                } else if (resp.statusCode() == 401) {
                    statusLabel.setText("✘ Current password incorrect.");
                    statusLabel.setStyle(
                            "-fx-text-fill: #f85149; -fx-font-size: 12px;");
                } else {
                    statusLabel.setText("Error: " + resp.statusCode());
                    statusLabel.setStyle(
                            "-fx-text-fill: #f85149; -fx-font-size: 12px;");
                }
            } catch (Exception ex) {
                statusLabel.setText("Cannot connect.");
                statusLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 12px;");
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.add(new Label("Current:"), 0, 0);
        grid.add(currentField,          1, 0);
        grid.add(new Label("New:"),     0, 1);
        grid.add(newField,              1, 1);
        grid.add(new Label("Confirm:"), 0, 2);
        grid.add(confirmField,          1, 2);
        grid.add(statusLabel,           1, 3);

        return new VBox(12, grid, changeBtn);
    }

    private VBox buildServerSection() {
        TextField hostField = new TextField(ConfigManager.getHost());
        hostField.setMaxWidth(300);
        TextField portField = new TextField(ConfigManager.getPort());
        portField.setMaxWidth(150);
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px;");

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().setAll("btn-primary");
        saveBtn.setStyle(
                "-fx-background-color: #238636; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 8 16 8 16;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            ConfigManager.setHost(hostField.getText().trim());
            ConfigManager.setPort(portField.getText().trim());
            ConfigManager.save();
            statusLabel.setText("✔ Saved.");
            statusLabel.setStyle(
                    "-fx-text-fill: #3fb950; -fx-font-size: 12px;");
        });

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.add(new Label("Host:"), 0, 0); grid.add(hostField, 1, 0);
        grid.add(new Label("Port:"), 0, 1); grid.add(portField, 1, 1);
        grid.add(statusLabel,        1, 2);

        return new VBox(12, grid, saveBtn);
    }

    private VBox buildAboutSection() {
        Label name = new Label("VaultDesk Employee Portal");
        name.setStyle(
                "-fx-text-fill: #e6edf3; -fx-font-size: 16px;" +
                        "-fx-font-weight: bold;");
        Label version = new Label("Version 1.0.0");
        version.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        Label desc = new Label(
                "Employee Self-Service Portal\n" +
                        "Saurashtra Cement Ltd\n" +
                        "Developed by Viral Sangecha");
        desc.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");

        Label loggedIn = new Label(
                "Logged in as: " + SessionManager.get().getName()
                        + "  |  " + SessionManager.get().getEmpCode());
        loggedIn.setStyle(
                "-fx-text-fill: #58a6ff; -fx-font-size: 11px;");

        return new VBox(8, name, version, desc,
                new Separator(), loggedIn);
    }

    private VBox sectionCard(String heading, VBox content) {
        Label headLabel = new Label(heading);
        headLabel.getStyleClass().add("section-title");
        Separator sep = new Separator();
        VBox card = new VBox(12, headLabel, sep, content);
        card.getStyleClass().add("settings-card");
        return card;
    }
}