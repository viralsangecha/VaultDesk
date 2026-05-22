package com.vaultdesk.employee;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class UIComponents {

    // ── Glassmorphism card ────────────────────────────────
    public static VBox glassCard(javafx.scene.Node... children) {
        VBox card = new VBox(12, children);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: rgba(22,27,34,0.85);" +
                        "-fx-border-color: rgba(48,54,61,0.6);" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian," +
                        " rgba(0,0,0,0.4), 20, 0, 0, 8);");
        return card;
    }

    // ── Gradient button ───────────────────────────────────
    public static Button gradientBtn(String text,
                                     String color1,
                                     String color2) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: linear-gradient(" +
                        "to right, " + color1 + ", " + color2 + ");" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 10 20 10 20;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian," +
                        " rgba(0,0,0,0.3), 8, 0, 0, 3);");
        AnimationUtil.addHoverScale(btn);
        return btn;
    }

    // ── Status pill badge ─────────────────────────────────
    public static Label statusPill(String text,
                                   String bg, String fg) {
        Label pill = new Label(text);
        pill.setPadding(new Insets(3, 12, 3, 12));
        pill.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + fg + ";" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;");
        return pill;
    }

    // ── Avatar circle with initials ───────────────────────
    public static StackPane avatar(String name,
                                   String color) {
        String initials = getInitials(name);
        Label label = new Label(initials);
        label.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;");

        Circle circle = new Circle(20);
        circle.setFill(Color.web(color));

        StackPane pane = new StackPane(circle, label);
        pane.setMinSize(40, 40);
        pane.setMaxSize(40, 40);
        return pane;
    }

    private static String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split(" ");
        if (parts.length == 1)
            return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1)
                + parts[parts.length - 1]
                .substring(0, 1)).toUpperCase();
    }

    // ── Info card with icon ───────────────────────────────
    public static VBox infoCard(String icon,
                                String title,
                                String value,
                                String accentColor) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
                "-fx-font-size: 20px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: #8b949e;" +
                        "-fx-font-size: 11px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle(
                "-fx-text-fill: #e6edf3;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;");

        VBox card = new VBox(4,
                iconLabel, valueLabel, titleLabel);
        card.setPadding(new Insets(16));
        card.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: " + accentColor + ";" +
                        "-fx-border-width: 0 0 0 3;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;");
        AnimationUtil.addHoverScale(card);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // ── Search bar with icon ──────────────────────────────
    public static HBox searchBar(TextField field) {
        Label icon = new Label("🔍");
        icon.setStyle(
                "-fx-text-fill: #484f58;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 0 4 0 8;");

        field.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #e6edf3;" +
                        "-fx-prompt-text-fill: #484f58;" +
                        "-fx-border-width: 0;" +
                        "-fx-padding: 8 12 8 4;");

        HBox box = new HBox(icon, field);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle(
                "-fx-background-color: #21262d;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;");
        HBox.setHgrow(field, Priority.ALWAYS);

        // Focus glow
        field.focusedProperty().addListener(
                (obs, ov, nv) -> {
                    if (nv) {
                        box.setStyle(
                                "-fx-background-color: #21262d;" +
                                        "-fx-border-color: #58a6ff;" +
                                        "-fx-border-width: 1;" +
                                        "-fx-border-radius: 8;" +
                                        "-fx-background-radius: 8;" +
                                        "-fx-effect: dropshadow(gaussian," +
                                        " rgba(88,166,255,0.2)," +
                                        " 8, 0, 0, 0);");
                    } else {
                        box.setStyle(
                                "-fx-background-color: #21262d;" +
                                        "-fx-border-color: #30363d;" +
                                        "-fx-border-width: 1;" +
                                        "-fx-border-radius: 8;" +
                                        "-fx-background-radius: 8;");
                    }
                });

        return box;
    }

    // ── Skeleton loading placeholder ──────────────────────
    public static VBox skeleton(int rows) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(16));

        for (int i = 0; i < rows; i++) {
            double width = 60 + (i % 3) * 15;
            HBox row = new HBox(8);

            Region r1 = skeletonRect(40, 12);
            Region r2 = skeletonRect(width, 12);
            Region r3 = skeletonRect(50, 12);

            row.getChildren().addAll(r1, r2, r3);
            box.getChildren().add(row);

            // Animate each row
            animateSkeleton(r1, i * 100);
            animateSkeleton(r2, i * 100 + 50);
            animateSkeleton(r3, i * 100 + 100);
        }
        return box;
    }

    private static Region skeletonRect(double width,
                                       double height) {
        Region r = new Region();
        r.setPrefSize(width + "%".length(), height);
        r.setMinHeight(height);
        r.setMaxHeight(height);
        r.setPrefWidth(width);
        r.setStyle(
                "-fx-background-color: #21262d;" +
                        "-fx-background-radius: 4;");
        return r;
    }

    private static void animateSkeleton(Region r,
                                        int delay) {
        javafx.animation.Timeline tl =
                new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(
                                javafx.util.Duration.millis(0),
                                e -> r.setStyle(
                                        "-fx-background-color: #21262d;" +
                                                "-fx-background-radius: 4;")),
                        new javafx.animation.KeyFrame(
                                javafx.util.Duration.millis(600),
                                e -> r.setStyle(
                                        "-fx-background-color: #30363d;" +
                                                "-fx-background-radius: 4;")),
                        new javafx.animation.KeyFrame(
                                javafx.util.Duration.millis(1200),
                                e -> r.setStyle(
                                        "-fx-background-color: #21262d;" +
                                                "-fx-background-radius: 4;")));
        tl.setDelay(
                javafx.util.Duration.millis(delay));
        tl.setCycleCount(
                javafx.animation.Timeline.INDEFINITE);
        tl.play();
    }

    // ── Neon glow label ───────────────────────────────────
    public static Label neonLabel(String text,
                                  String color) {
        Label label = new Label(text);
        label.setStyle(
                "-fx-text-fill: " + color + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(gaussian," +
                        color + ", 12, 0.5, 0, 0);");
        return label;
    }

    // ── Progress ring ─────────────────────────────────────
    public static StackPane progressRing(
            double percent, String color) {
        javafx.scene.shape.Arc bg =
                new javafx.scene.shape.Arc(
                        24, 24, 20, 20, 90, -360);
        bg.setType(
                javafx.scene.shape.ArcType.OPEN);
        bg.setFill(Color.TRANSPARENT);
        bg.setStroke(Color.web(
                "#21262d"));
        bg.setStrokeWidth(4);

        javafx.scene.shape.Arc fill =
                new javafx.scene.shape.Arc(
                        24, 24, 20, 20, 90,
                        -(360 * percent / 100));
        fill.setType(
                javafx.scene.shape.ArcType.OPEN);
        fill.setFill(Color.TRANSPARENT);
        fill.setStroke(Color.web(color));
        fill.setStrokeWidth(4);
        fill.setStrokeLineCap(
                javafx.scene.shape.StrokeLineCap.ROUND);

        // Animate fill
        javafx.animation.Timeline tl =
                new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(
                                javafx.util.Duration.millis(0),
                                new javafx.animation.KeyValue(
                                        fill.lengthProperty(), 0)),
                        new javafx.animation.KeyFrame(
                                javafx.util.Duration.millis(800),
                                new javafx.animation.KeyValue(
                                        fill.lengthProperty(),
                                        -(360 * percent / 100),
                                        javafx.animation.Interpolator
                                                .EASE_OUT)));
        tl.play();

        Label pctLabel = new Label(
                (int) percent + "%");
        pctLabel.setStyle(
                "-fx-text-fill: " + color + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;");

        StackPane pane = new StackPane(bg, fill, pctLabel);
        pane.setMinSize(48, 48);
        pane.setMaxSize(48, 48);
        return pane;
    }
}