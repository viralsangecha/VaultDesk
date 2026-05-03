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

        // Apply AtlantaFX base theme first
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        stage.setTitle("VaultDesk Admin");
        stage.setWidth(1200);
        stage.setHeight(800);

        Scene scene;

        if (!ConfigManager.isConfigured()) {
            scene = new ServerConfigView().getScene(stage);
            stage.setScene(scene);
            stage.show();
            return;
        }

        Properties session = SessionStore.load();
        if (session != null) {
            scene = tryAutoLogin(stage, session);
        } else {
            scene = new LoginView().getScene(stage);
            ThemeManager.apply(scene);
        }

        stage.setScene(scene);
        stage.show();
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