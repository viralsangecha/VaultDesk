package com.vaultdesk.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.*;

public class ServerConfigView {

    public Scene getScene(Stage stage) {

        Label appTitle = new Label("VaultDesk");
        appTitle.getStyleClass().add("login-title");
        Label appSubtitle = new Label("First-time Server Setup");
        appSubtitle.getStyleClass().add("login-subtitle");

        Label infoLabel = new Label(
                "Enter the IP address of the machine running VaultDesk server.\n" +
                        "If running on this machine, use: localhost");
        infoLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        infoLabel.setWrapText(true);

        Label hostLabel = new Label("Server IP / Hostname");
        hostLabel.getStyleClass().add("login-label");
        TextField hostField = new TextField(
                ConfigManager.getHost().isEmpty() ? "localhost" : ConfigManager.getHost());
        hostField.setPromptText("e.g. 192.168.1.100 or localhost");
        hostField.getStyleClass().add("login-field");

        Label portLabel = new Label("Server Port");
        portLabel.getStyleClass().add("login-label");
        TextField portField = new TextField(ConfigManager.getPort().isEmpty() ? "2008" : ConfigManager.getPort());
        portField.setPromptText("e.g. 2008");
        portField.getStyleClass().add("login-field");

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px;");

        Button testBtn = new Button("Test Connection");
        testBtn.getStyleClass().setAll("btn-primary");
        testBtn.setStyle(
                "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 10 20 10 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-pref-width: 340px; -fx-cursor: hand;");

        Button saveBtn = new Button("Save & Continue to Login →");
        saveBtn.getStyleClass().setAll("login-btn");
        saveBtn.setStyle(
                "-fx-background-color: #238636; -fx-text-fill: white;" +
                        "-fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-pref-width: 340px; -fx-pref-height: 42px;" +
                        "-fx-background-radius: 6; -fx-cursor: hand;");
        saveBtn.setDisable(true);

        testBtn.setOnAction(e -> {
            String host = hostField.getText().trim();
            String port = portField.getText().trim();
            if (host.isEmpty() || port.isEmpty()) {
                statusLabel.setText("Please enter host and port.");
                statusLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");
                return;
            }
            String url = "http://" + host + ":" + port + "/api/dashboard/stats";
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET().build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    statusLabel.setText("✔ Connected successfully! Server is reachable.");
                    statusLabel.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 12px;");
                    saveBtn.setDisable(false);
                } else {
                    statusLabel.setText("⚠ Server responded with code: " + resp.statusCode());
                    statusLabel.setStyle("-fx-text-fill: #d29922; -fx-font-size: 12px;");
                }
            } catch (Exception ex) {
                statusLabel.setText("✘ Cannot connect. Check IP and port.");
                statusLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");
                saveBtn.setDisable(true);
            }
        });

        saveBtn.setOnAction(e -> {
            ConfigManager.setHost(hostField.getText().trim());
            ConfigManager.setPort(portField.getText().trim());
            ConfigManager.save();
            Scene loginScene = new LoginView().getScene(stage);
            ThemeManager.apply(loginScene);
            stage.setScene(loginScene);
        });

        VBox card = new VBox(14);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(480);
        card.setPrefWidth(480);
        card.getChildren().addAll(
                appTitle, appSubtitle,
                new Label(""),
                infoLabel,
                new Label(""),
                hostLabel, hostField,
                portLabel, portField,
                testBtn,
                statusLabel,
                new Label(""),
                saveBtn
        );

        StackPane root = new StackPane(card);
        StackPane.setAlignment(card, Pos.CENTER);
        root.getStyleClass().add("login-bg");
        root.setPadding(new Insets(40));

        Scene scene = new Scene(root, 1200, 800);
        ThemeManager.apply(scene);
        return scene;
    }
}