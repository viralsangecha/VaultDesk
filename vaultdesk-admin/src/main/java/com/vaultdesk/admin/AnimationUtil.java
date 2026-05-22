package com.vaultdesk.admin;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class AnimationUtil {

    // ── Fade in ───────────────────────────────────────────
    public static void fadeIn(Node node) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(
                Duration.millis(300), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    // ── Slide in from right ───────────────────────────────
    public static void slideInRight(Node node) {
        node.setTranslateX(40);
        node.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(
                Duration.millis(280), node);
        tt.setFromX(40);
        tt.setToX(0);
        FadeTransition ft = new FadeTransition(
                Duration.millis(280), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ParallelTransition pt = new ParallelTransition(
                tt, ft);
        pt.play();
    }

    // ── Slide in from bottom ──────────────────────────────
    public static void slideInUp(Node node) {
        node.setTranslateY(20);
        node.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(
                Duration.millis(250), node);
        tt.setFromY(20);
        tt.setToY(0);
        FadeTransition ft = new FadeTransition(
                Duration.millis(250), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ParallelTransition pt = new ParallelTransition(
                tt, ft);
        pt.play();
    }

    // ── Scale in (pop) ────────────────────────────────────
    public static void popIn(Node node) {
        node.setScaleX(0.8);
        node.setScaleY(0.8);
        node.setOpacity(0);
        ScaleTransition st = new ScaleTransition(
                Duration.millis(200), node);
        st.setFromX(0.8);
        st.setToX(1.0);
        st.setFromY(0.8);
        st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition ft = new FadeTransition(
                Duration.millis(200), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ParallelTransition pt = new ParallelTransition(
                st, ft);
        pt.play();
    }

    // ── Shake (error) ─────────────────────────────────────
    public static void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(
                Duration.millis(60), node);
        tt.setFromX(0);
        tt.setByX(8);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    // ── Pulse (attention) ─────────────────────────────────
    public static void pulse(Node node) {
        ScaleTransition st = new ScaleTransition(
                Duration.millis(150), node);
        st.setFromX(1.0);
        st.setToX(1.08);
        st.setFromY(1.0);
        st.setToY(1.08);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }

    // ── Count up number ───────────────────────────────────
    public static void countUp(Label label,
                               int from, int to) {
        Timeline timeline = new Timeline();
        int duration = 800;
        int steps = Math.min(Math.abs(to - from), 30);
        if (steps == 0) {
            label.setText(String.valueOf(to));
            return;
        }
        for (int i = 0; i <= steps; i++) {
            final int value = from
                    + (int) ((to - from)
                    * (double) i / steps);
            KeyFrame kf = new KeyFrame(
                    Duration.millis(
                            (double) duration * i / steps),
                    e -> label.setText(
                            String.valueOf(value)));
            timeline.getKeyFrames().add(kf);
        }
        timeline.getKeyFrames().add(new KeyFrame(
                Duration.millis(duration),
                e -> label.setText(String.valueOf(to))));
        timeline.play();
    }

    // ── Hover scale ───────────────────────────────────────
    public static void addHoverScale(Node node) {
        node.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(
                    Duration.millis(120), node);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
        });
        node.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(
                    Duration.millis(120), node);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    // ── Stagger children ──────────────────────────────────
    public static void staggerFadeIn(VBox container) {
        int delay = 0;
        for (Node child : container.getChildren()) {
            child.setOpacity(0);
            FadeTransition ft = new FadeTransition(
                    Duration.millis(250), child);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(delay));
            ft.play();
            delay += 60;
        }
    }

    // ── Loading spinner ───────────────────────────────────
    public static javafx.scene.shape.Arc loadingSpinner() {
        javafx.scene.shape.Arc arc =
                new javafx.scene.shape.Arc(
                        20, 20, 16, 16, 0, 270);
        arc.setType(
                javafx.scene.shape.ArcType.OPEN);
        arc.setFill(javafx.scene.paint.Color.TRANSPARENT);
        arc.setStroke(javafx.scene.paint.Color.web(
                "#58a6ff"));
        arc.setStrokeWidth(3);
        arc.setStrokeLineCap(
                javafx.scene.shape.StrokeLineCap.ROUND);

        RotateTransition rt = new RotateTransition(
                Duration.millis(800), arc);
        rt.setByAngle(360);
        rt.setCycleCount(Timeline.INDEFINITE);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.play();

        return arc;
    }

    // ── Success checkmark flash ───────────────────────────
    public static void successFlash(Node node) {
        node.setStyle(node.getStyle()
                + "-fx-border-color: #3fb950;"
                + "-fx-border-width: 2;");
        Timeline tl = new Timeline(new KeyFrame(
                Duration.millis(1500),
                e -> node.setStyle(
                        node.getStyle()
                                .replace(
                                        "-fx-border-color: #3fb950;"
                                                + "-fx-border-width: 2;", ""))));
        tl.play();
    }
}