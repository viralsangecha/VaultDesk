package com.vaultdesk.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class UpdateDialog {

    public static void show(Stage owner,
                            VersionChecker.UpdateInfo info,
                            Runnable onSkip) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Update Available");
        dialog.setResizable(false);

        Label titleLabel = new Label("🔄  Update Available");
        titleLabel.setStyle(
                "-fx-text-fill: #58a6ff; -fx-font-size: 18px;" +
                        "-fx-font-weight: bold;");

        Label versionLabel = new Label(
                "Version " + info.version + " is available.\n" +
                        "You are running v"
                        + VersionChecker.getCurrentVersion() + ".");
        versionLabel.setStyle(
                "-fx-text-fill: #c9d1d9; -fx-font-size: 13px;");

        Label changelogTitle = new Label("What's new:");
        changelogTitle.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 11px;" +
                        "-fx-font-weight: bold;");

        Label changelogLabel = new Label(info.changelog);
        changelogLabel.setStyle(
                "-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");
        changelogLabel.setWrapText(true);
        changelogLabel.setMaxWidth(380);

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(380);
        progressBar.setVisible(false);

        Label statusLabel = new Label("");
        statusLabel.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 11px;");

        Button updateBtn = new Button("⬇  Download & Install Update");
        updateBtn.getStyleClass().setAll("btn-primary");
        updateBtn.setStyle(
                "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 10 20 10 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-pref-width: 380px; -fx-cursor: hand;");

        Button skipBtn = new Button("Skip This Update");
        skipBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #8b949e; -fx-font-size: 12px;" +
                        "-fx-cursor: hand; -fx-border-width: 0;");

        updateBtn.setOnAction(e -> {
            updateBtn.setDisable(true);
            skipBtn.setDisable(true);
            progressBar.setVisible(true);
            statusLabel.setText("Downloading update...");

            // Run download on background thread
            Thread downloadThread = new Thread(() -> {
                try {
                    File newJar = UpdateDownloader.download(
                            info.downloadUrl, progressBar);

                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText(
                                "Download complete. Applying update...");
                        progressBar.setProgress(1.0);
                    });

                    // Small pause so user sees 100%
                    Thread.sleep(800);

                    UpdateDownloader.applyUpdate(newJar);

                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText(
                                "Update failed: " + ex.getMessage());
                        statusLabel.setStyle(
                                "-fx-text-fill: #f85149; -fx-font-size: 11px;");
                        updateBtn.setDisable(false);
                        skipBtn.setDisable(false);
                        progressBar.setVisible(false);
                    });
                }
            });
            downloadThread.setDaemon(true);
            downloadThread.start();
        });

        skipBtn.setOnAction(e -> {
            dialog.close();
            if (onSkip != null) onSkip.run();
        });

        VBox content = new VBox(16,
                titleLabel, versionLabel,
                new Separator(),
                changelogTitle, changelogLabel,
                new Separator(),
                progressBar, statusLabel,
                updateBtn, skipBtn);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(32));
        content.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-width: 1;");

        Scene scene = new Scene(content, 440, 380);
        ThemeManager.apply(scene);
        dialog.setScene(scene);
        dialog.show();
    }
}