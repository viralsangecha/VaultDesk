package com.vaultdesk.admin;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LoadingUtil {

    // ── Show spinner inside a table while loading ─────────
    public static void setLoading(TableView<?> table, String message) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(40, 40);
        spinner.setStyle("-fx-accent: #58a6ff;");
        Label msg = new Label(message);
        msg.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        VBox box = new VBox(8, spinner, msg);
        box.setAlignment(Pos.CENTER);
        table.setPlaceholder(box);
    }

    // ── Show empty state after loading ────────────────────
    public static void setEmpty(TableView<?> table,
                                String icon, String message,
                                String subtitle) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 32px;");
        Label msgLabel = new Label(message);
        msgLabel.setStyle(
                "-fx-text-fill: #e6edf3; -fx-font-size: 14px;" +
                        "-fx-font-weight: bold;");
        Label subLabel = new Label(subtitle);
        subLabel.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        VBox box = new VBox(6, iconLabel, msgLabel, subLabel);
        box.setAlignment(Pos.CENTER);
        table.setPlaceholder(box);
    }

    // ── Button loading state ──────────────────────────────
    public static void setButtonLoading(Button btn,
                                        String loadingText) {
        btn.setText(loadingText);
        btn.setDisable(true);
    }

    public static void resetButton(Button btn,
                                   String originalText) {
        btn.setText(originalText);
        btn.setDisable(false);
    }

    // ── General loading overlay for VBox content ──────────
    public static VBox loadingOverlay(String message) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        spinner.setStyle("-fx-accent: #58a6ff;");
        Label msg = new Label(message);
        msg.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 13px;");
        VBox box = new VBox(12, spinner, msg);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding: 40;");
        return box;
    }
}