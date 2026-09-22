package com.vaultdesk.admin;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressDialogUtil {

    public static class Handle {
        public final Stage stage;
        public final ProgressBar bar;
        public final Label countLabel;
        public final Label detailLabel;

        Handle(Stage stage, ProgressBar bar, Label countLabel, Label detailLabel) {
            this.stage = stage; this.bar = bar; this.countLabel = countLabel; this.detailLabel = detailLabel;
        }

        public void update(int current, int total, String detail) {
            Platform.runLater(() -> {
                bar.setProgress(total > 0 ? (double) current / total : 0);
                countLabel.setText(current + " / " + total);
                detailLabel.setText(detail != null ? detail : "");
            });
        }

        public void close() {
            Platform.runLater(stage::close);
        }
    }

    public static Handle show(String title) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);
        stage.setResizable(false);

        ProgressBar bar = new ProgressBar(0);
        bar.setPrefWidth(320);
        Label countLabel = new Label("0 / 0");
        countLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8b949e;");
        Label detailLabel = new Label("Starting...");
        detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #484f58;");
        detailLabel.setWrapText(true);
        detailLabel.setMaxWidth(320);

        VBox root = new VBox(10, bar, countLabel, detailLabel);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #161b22;");

        Scene scene = new Scene(root);
        ThemeManager.apply(scene);
        stage.setScene(scene);
        stage.show();

        return new Handle(stage, bar, countLabel, detailLabel);
    }
}