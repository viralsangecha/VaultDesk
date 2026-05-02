package com.vaultdesk.admin;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AdminApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        Scene scene;
        if (!ConfigManager.isConfigured()) {
            // First launch — show server config screen
            scene = new ServerConfigView().getScene(stage);
        } else {
            // Already configured — go to login
            scene = new LoginView().getScene(stage);
        }

        stage.setScene(scene);
        stage.setTitle("VaultDesk Admin");
        stage.setWidth(1200);
        stage.setHeight(800);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}