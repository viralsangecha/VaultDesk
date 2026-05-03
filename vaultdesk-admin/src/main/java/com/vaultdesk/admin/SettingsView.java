package com.vaultdesk.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;
import java.util.prefs.Preferences;

public class SettingsView {

    // ── Java Preferences — persists across sessions ───────
    private static final Preferences prefs =
            Preferences.userNodeForPackage(SettingsView.class);

    public VBox getView() {

        Label pageTitle = new Label("Settings");
        pageTitle.getStyleClass().add("page-title");
        Label pageSub = new Label("Configure application preferences.");
        pageSub.getStyleClass().add("page-subtitle");

        VBox root = new VBox(20, pageTitle, pageSub);
        root.setPadding(new Insets(0));

        // ── Section: Server ───────────────────────────────
        root.getChildren().add(sectionCard("🖥  Server Configuration",
                buildServerSection()));

        // ── Section: Appearance ───────────────────────────
        root.getChildren().add(sectionCard("🎨  Appearance",
                buildAppearanceSection()));

        // ── Section: Account ──────────────────────────────
        root.getChildren().add(sectionCard("🔒  Change Password",
                buildPasswordSection()));

        // ── Section: Data ─────────────────────────────────
        root.getChildren().add(sectionCard("📁  Data & Import",
                buildDataSection()));

        // ── Section: About ────────────────────────────────
        root.getChildren().add(sectionCard("ℹ  About VaultDesk",
                buildAboutSection()));

        return root;
    }

    // ── Server section ────────────────────────────────────
    private VBox buildServerSection() {
        Label hostLabel = new Label("Server Host");
        hostLabel.getStyleClass().add("login-label");

        TextField hostField = new TextField(
                prefs.get("server.host", "localhost"));
        hostField.setPromptText("e.g. localhost or 192.168.1.100");
        hostField.setPrefWidth(320);

        Label portLabel = new Label("Server Port");
        portLabel.getStyleClass().add("login-label");

        TextField portField = new TextField(
                prefs.get("server.port", "8080"));
        portField.setPromptText("e.g. 8080");
        portField.setPrefWidth(120);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px;");

        Button testBtn = new Button("Test Connection");
        testBtn.getStyleClass().setAll("btn-primary");
        testBtn.setStyle(
                "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 8 16 8 16;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");
        testBtn.setOnAction(e -> {
            String url = "http://" + hostField.getText().trim()
                    + ":" + portField.getText().trim()
                    + "/api/dashboard/stats";
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET().build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    statusLabel.setText("✔ Connected successfully.");
                    statusLabel.setStyle(
                            "-fx-text-fill: #3fb950; -fx-font-size: 12px;");
                } else {
                    statusLabel.setText("⚠ Server returned: "
                            + resp.statusCode());
                    statusLabel.setStyle(
                            "-fx-text-fill: #d29922; -fx-font-size: 12px;");
                }
            } catch (Exception ex) {
                statusLabel.setText("✘ Cannot connect: " + ex.getMessage());
                statusLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 12px;");
            }
        });

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
            statusLabel.setText("✔ Server settings saved.");
            statusLabel.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 12px;");
        });

        Label note = new Label(
                "Changes take effect after restarting the application.");
        note.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.add(hostLabel,  0, 0); grid.add(hostField, 1, 0);
        grid.add(portLabel,  0, 1); grid.add(portField, 1, 1);
        grid.add(note,       1, 2);
        grid.add(statusLabel,1, 3);

        HBox btnRow = new HBox(10, testBtn, saveBtn);
        return new VBox(12, grid, btnRow);
    }

    // ── Appearance section ────────────────────────────────
    private VBox buildAppearanceSection() {

        Label themeLabel = new Label("Theme");
        themeLabel.getStyleClass().add("login-label");

        ToggleGroup themeGroup = new ToggleGroup();
        RadioButton darkBtn  = new RadioButton("Dark Mode");
        RadioButton lightBtn = new RadioButton("Light Mode");
        darkBtn.setToggleGroup(themeGroup);
        lightBtn.setToggleGroup(themeGroup);

        darkBtn.setStyle("-fx-text-fill: #c9d1d9;");
        lightBtn.setStyle("-fx-text-fill: #c9d1d9;");

        if (ThemeManager.getCurrent() == ThemeManager.Theme.DARK)
            darkBtn.setSelected(true);
        else
            lightBtn.setSelected(true);

        Label themeStatus = new Label("");
        themeStatus.setStyle("-fx-font-size: 12px;");

        themeGroup.selectedToggleProperty().addListener((obs, ov, nv) -> {
            if (nv == darkBtn) {
                if (ThemeManager.getCurrent() != ThemeManager.Theme.DARK) {
                    ThemeManager.toggle();
                    applyThemeToAll();
                    themeStatus.setText("✔ Dark mode applied.");
                    themeStatus.setStyle(
                            "-fx-text-fill: #3fb950; -fx-font-size: 12px;");
                }
            } else {
                if (ThemeManager.getCurrent() != ThemeManager.Theme.LIGHT) {
                    ThemeManager.toggle();
                    applyThemeToAll();
                    themeStatus.setText("✔ Light mode applied.");
                    themeStatus.setStyle(
                            "-fx-text-fill: #3fb950; -fx-font-size: 12px;");
                }
            }
        });

        Label fontLabel = new Label("Font Size");
        fontLabel.getStyleClass().add("login-label");

        ComboBox<String> fontBox = new ComboBox<>();
        fontBox.getItems().addAll("Small (12px)", "Medium (13px)", "Large (15px)");
        fontBox.setValue(prefs.get("font.size", "Medium (13px)"));
        fontBox.setPrefWidth(200);

        Button saveFontBtn = new Button("Apply Font Size");
        saveFontBtn.setOnAction(e -> {
            prefs.put("font.size", fontBox.getValue());
            String size = fontBox.getValue().contains("12") ? "12px"
                    : fontBox.getValue().contains("15") ? "15px" : "13px";

            javafx.stage.Stage stage =
                    (javafx.stage.Stage) javafx.stage.Window.getWindows()
                            .stream()
                            .filter(w -> w instanceof javafx.stage.Stage)
                            .findFirst().orElse(null);
            if (stage != null && stage.getScene() != null) {
                // Apply font size to root
                stage.getScene().getRoot().setStyle(
                        "-fx-font-size: " + size + ";");
            }
            themeStatus.setText("✔ Font size applied: " + size);
            themeStatus.setStyle(
                    "-fx-text-fill: #3fb950; -fx-font-size: 12px;");
        });

        HBox themeRow = new HBox(16, darkBtn, lightBtn);
        themeRow.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.add(themeLabel, 0, 0); grid.add(themeRow,   1, 0);
        grid.add(fontLabel,  0, 1); grid.add(fontBox,    1, 1);
        grid.add(themeStatus,1, 2);

        return new VBox(12, grid, saveFontBtn);
    }

    // ── Password section ──────────────────────────────────
    private VBox buildPasswordSection() {

        Label currentLabel = new Label("Current Password");
        currentLabel.getStyleClass().add("login-label");
        PasswordField currentField = new PasswordField();
        currentField.setPromptText("Enter current password");
        currentField.setPrefWidth(280);

        Label newLabel = new Label("New Password");
        newLabel.getStyleClass().add("login-label");
        PasswordField newField = new PasswordField();
        newField.setPromptText("Minimum 6 characters");
        newField.setPrefWidth(280);

        Label confirmLabel = new Label("Confirm New Password");
        confirmLabel.getStyleClass().add("login-label");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Re-enter new password");
        confirmField.setPrefWidth(280);

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
                statusLabel.setText("All fields are required.");
                statusLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 12px;");
                return;
            }
            if (newPwd.length() < 6) {
                statusLabel.setText("New password must be at least 6 characters.");
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

            // Call password change endpoint
            try {
                int userId = SessionManager.get().getUserId();
                String body = "{" +
                        "\"currentPassword\":\"" + current + "\"," +
                        "\"newPassword\":\"" + newPwd + "\"" +
                        "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigManager.getBaseUrl() + "/api/users/"
                                + userId + "/password"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body)).build();
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
                    statusLabel.setText("✘ Current password is incorrect.");
                    statusLabel.setStyle(
                            "-fx-text-fill: #f85149; -fx-font-size: 12px;");
                } else {
                    statusLabel.setText("Error: " + resp.statusCode());
                    statusLabel.setStyle(
                            "-fx-text-fill: #f85149; -fx-font-size: 12px;");
                }
            } catch (Exception ex) {
                statusLabel.setText("Cannot connect: " + ex.getMessage());
                statusLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 12px;");
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.add(currentLabel, 0, 0); grid.add(currentField, 1, 0);
        grid.add(newLabel,     0, 1); grid.add(newField,     1, 1);
        grid.add(confirmLabel, 0, 2); grid.add(confirmField, 1, 2);
        grid.add(statusLabel,  1, 3);

        return new VBox(12, grid, changeBtn);
    }

    // ── Data section ──────────────────────────────────────
    private VBox buildDataSection() {

        Label infoLabel = new Label(
                "Use the Import CSV buttons in Assets, Employees, and " +
                        "Departments views to bulk import data.\n" +
                        "Use the Export buttons in Reports to download Excel files.");
        infoLabel.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        infoLabel.setWrapText(true);

        Label formatTitle = new Label("CSV Format Reference");
        formatTitle.setStyle(
                "-fx-text-fill: #e6edf3; -fx-font-weight: bold;" +
                        "-fx-font-size: 13px;");

        TextArea formatArea = new TextArea(
                "ASSETS (11 columns):\n" +
                        "assetTag, name, category, brand, model, serialNumber,\n" +
                        "departmentId, location, status, purchaseCost, notes\n\n" +
                        "EMPLOYEES (8 columns):\n" +
                        "name, empCode, departmentId, designation,\n" +
                        "email, phone, joinDate (YYYY-MM-DD), notes\n\n" +
                        "DEPARTMENTS (2 columns):\n" +
                        "name, location\n\n" +
                        "Note: First row is treated as header and skipped.\n" +
                        "Wrap values with commas in double quotes."
        );
        formatArea.setEditable(false);
        formatArea.setPrefRowCount(12);
        formatArea.setStyle(
                "-fx-control-inner-background: #21262d;" +
                        "-fx-text-fill: #c9d1d9; -fx-font-family: monospace;" +
                        "-fx-font-size: 12px;");

        return new VBox(12, infoLabel, formatTitle, formatArea);
    }

    // ── About section ─────────────────────────────────────
    private VBox buildAboutSection() {

        Label appName = new Label("VaultDesk Admin");
        appName.setStyle(
                "-fx-text-fill: #e6edf3; -fx-font-size: 18px;" +
                        "-fx-font-weight: bold;");

        Label version = new Label("Version 1.0.0  —  Phase 10e");
        version.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");

        Label desc = new Label(
                "IT Helpdesk & Asset Management Platform\n" +
                        "Built for Saurashtra Cement Ltd IT Department\n" +
                        "Developed by Viral Sangecha");
        desc.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 13px;");
        desc.setWrapText(true);

        Separator sep = new Separator();

        Label techTitle = new Label("Technology Stack");
        techTitle.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 11px;" +
                        "-fx-font-weight: bold;");

        Label tech = new Label(
                "Backend  :  Java 21  •  Spring Boot 3.5  •  SQLite\n" +
                        "Frontend :  JavaFX 21  •  AtlantaFX PrimerDark\n" +
                        "Reports  :  Apache POI 5.2.3\n" +
                        "Styling  :  Custom CSS + Light/Dark theme");
        tech.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 12px;" +
                        "-fx-font-family: monospace;");

        Separator sep2 = new Separator();

        Label loggedIn = new Label(
                "Logged in as: " + SessionManager.get().getFullName()
                        + "  |  Role: " + SessionManager.get().getRole()
                        + "  |  User ID: " + SessionManager.get().getUserId());
        loggedIn.setStyle(
                "-fx-text-fill: #58a6ff; -fx-font-size: 11px;");

        return new VBox(10,
                appName, version, desc, sep,
                techTitle, tech, sep2, loggedIn);
    }

    // ── Section card wrapper ──────────────────────────────
    private VBox sectionCard(String heading, VBox content) {
        Label headLabel = new Label(heading);
        headLabel.getStyleClass().add("section-title");

        Separator sep = new Separator();

        VBox card = new VBox(12, headLabel, sep, content);
        card.getStyleClass().add("settings-card");
        return card;
    }

    private void applyThemeToAll() {
        javafx.stage.Stage stage =
                (javafx.stage.Stage) javafx.stage.Window.getWindows()
                        .stream()
                        .filter(w -> w instanceof javafx.stage.Stage)
                        .findFirst().orElse(null);
        if (stage != null && stage.getScene() != null)
            ThemeManager.apply(stage.getScene());
    }
}