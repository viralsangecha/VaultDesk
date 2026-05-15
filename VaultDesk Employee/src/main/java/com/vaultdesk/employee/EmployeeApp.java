package com.vaultdesk.employee;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.*;
import java.util.Properties;

public class EmployeeApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        Application.setUserAgentStylesheet(
                new PrimerDark().getUserAgentStylesheet());

        stage.setTitle("VaultDesk Employee");
        stage.setWidth(1000);
        stage.setHeight(700);
        stage.setResizable(true);

        // Step 1 — server not configured
        if (!ConfigManager.isConfigured()) {
            Scene scene = new ServerConfigView().getScene(stage);
            ThemeManager.apply(scene);
            stage.setScene(scene);
            stage.show();
            return;
        }

        // Step 2 — check saved session
        Properties session = SessionStore.load();
        Scene initialScene;
        if (session != null) {
            initialScene = tryAutoLogin(stage, session);
        } else {
            initialScene = new LoginView().getScene(stage);
            ThemeManager.apply(initialScene);
        }

        stage.setScene(initialScene);
        stage.show();
        // Add to start() after stage.show():
        Thread updateThread = new Thread(() -> {
            VersionChecker.UpdateInfo info = VersionChecker.checkForUpdate();
            if (info != null) {
                javafx.application.Platform.runLater(() ->
                        UpdateDialog.show(stage, info, null));
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private Scene tryAutoLogin(Stage stage, Properties session) {
        try {
            String username     = session.getProperty("username", "");
            String passwordHash = session.getProperty("passwordHash", "");

            String body = "{\"username\":\"" + username
                    + "\",\"passwordHash\":\"" + passwordHash + "\"}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/employee/auth/validate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                String rb        = resp.body();
                int employeeId   = extractInt(rb, "employeeId");
                String name      = extractValue(rb, "name");
                String empCode   = extractValue(rb, "empCode");
                String desig     = extractValue(rb, "designation");
                int deptId       = extractInt(rb, "departmentId");
                String email     = extractValue(rb, "email");

                SessionManager.get().login(employeeId, name,
                        empCode, desig, deptId, email);

                Scene dash = new DashboardView().getScene(stage);
                ThemeManager.apply(dash);
                return dash;
            } else {
                SessionStore.clear();
            }
        } catch (Exception ex) {
            SessionStore.clear();
        }

        Scene loginScene = new LoginView().getScene(stage);
        ThemeManager.apply(loginScene);
        return loginScene;
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

    public static void main(String[] args) {
        launch(args);
    }
}