package com.vaultdesk.admin;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class ToastUtil {

    public static void success(String message) {
        show(message, "toast-panel-success", "✔");
    }

    public static void error(String message) {
        show(message, "toast-panel-error", "✘");
    }

    private static void show(String message, String accentClass, String icon) {
        javafx.application.Platform.runLater(() -> {
            Stage toast = new Stage();
            toast.initStyle(StageStyle.UNDECORATED);
            toast.setAlwaysOnTop(true);
            toast.setResizable(false);

            Label iconLabel = new Label(icon);
            iconLabel.getStyleClass().add("toast-icon-" + ("toast-panel-success".equals(accentClass) ? "success" : "error"));
            Label msg = new Label(message);
            msg.getStyleClass().add("toast-msg");
            msg.setWrapText(true);
            msg.setMaxWidth(280);

            HBox content = new HBox(12, iconLabel, msg);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(14, 20, 14, 18));
            content.getStyleClass().addAll("toast-panel", accentClass);

            Scene scene = new Scene(content);
            ThemeManager.apply(scene);
            toast.setScene(scene);

            javafx.geometry.Rectangle2D screen = javafx.stage.Screen.getPrimary().getVisualBounds();
            toast.setX(screen.getMaxX() - 400);
            toast.setY(screen.getMaxY() - 100);
            toast.show();
            content.setOpacity(0);
            AnimationUtil.fadeIn(content);

            Timeline dismiss = new Timeline(new KeyFrame(Duration.seconds(3.2), e -> toast.close()));
            dismiss.play();
        });
    }
}