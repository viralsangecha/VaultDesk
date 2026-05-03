package com.vaultdesk.admin;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.*;
import java.util.Properties;

public class AdminApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        Application.setUserAgentStylesheet(
                new PrimerDark().getUserAgentStylesheet());

        stage.setTitle("VaultDesk Admin v"
                + VersionChecker.getCurrentVersion());
        stage.setWidth(1200);
        stage.setHeight(800);

        // Step 1 — server not configured yet
        if (!ConfigManager.isConfigured()) {
            Scene scene = new ServerConfigView().getScene(stage);
            stage.setScene(scene);
            stage.show();
            return;
        }

        // Step 2 — check session
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

        // Step 3 — check for update in background
        // Don't block startup — check after window is shown
        Thread updateThread = new Thread(() -> {
            VersionChecker.UpdateInfo info =
                    VersionChecker.checkForUpdate();
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
            String fullName     = session.getProperty("fullName", "");
            String role         = session.getProperty("role", "");
            int userId          = Integer.parseInt(
                    session.getProperty("userId", "0"));

            String body = "{\"username\":\"" + username
                    + "\",\"passwordHash\":\"" + passwordHash + "\"}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/auth/validate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                // Also restore deptId from session file
                int deptId = Integer.parseInt(
                        session.getProperty("deptId", "0"));
                SessionManager.get().login(userId, fullName, role);
                SessionManager.get().setDeptId(deptId);         // ← ADD
                Scene dash = new DashboardView(fullName, role)
                        .getScene(stage);
                ThemeManager.apply(dash);
                return dash;
            }else {
                SessionStore.clear();
            }
        } catch (Exception ex) {
            SessionStore.clear();
        }

        Scene loginScene = new LoginView().getScene(stage);
        ThemeManager.apply(loginScene);
        return loginScene;
    }

    public static void main(String[] args) {
        launch(args);
    }
}