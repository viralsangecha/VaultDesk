package com.vaultdesk.admin;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.*;
import java.util.function.Consumer;

public class NotificationBell {

    private final Label bellLabel     = new Label("🔔");
    private final Label badgeLabel    = new Label("");
    private final StackPane bellPane  = new StackPane();
    private Timeline pollTimer;
    private Consumer<Integer> onNavigate; // referenceId → ticket id
    private int unreadCount = 0;

    public StackPane getView() {
        bellLabel.setStyle(
                "-fx-font-size: 18px; -fx-cursor: hand;");

        badgeLabel.setStyle(
                "-fx-background-color: #f85149;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 9px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 1 4 1 4;");
        badgeLabel.setVisible(false);

        StackPane.setAlignment(badgeLabel, Pos.TOP_RIGHT);
        bellPane.getChildren().addAll(bellLabel, badgeLabel);
        bellPane.setPadding(new Insets(0, 8, 0, 8));
        bellPane.setStyle("-fx-cursor: hand;");

        bellPane.setOnMouseClicked(e -> showDropdown());

        startPolling();
        return bellPane;
    }

    // ── Poll every 60 seconds ─────────────────────────────
    private void startPolling() {
        fetchUnreadCount();
        pollTimer = new Timeline(
                new KeyFrame(Duration.seconds(60),
                        e -> fetchUnreadCount()));
        pollTimer.setCycleCount(Timeline.INDEFINITE);
        pollTimer.play();
    }

    public void stopPolling() {
        if (pollTimer != null) pollTimer.stop();
    }

    private void fetchUnreadCount() {
        // 1. Background Task
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                int userId = SessionManager.get().getUserId();

                String url = ConfigManager.getBaseUrl() + "/api/notifications/user/" + userId + "/unread";
                HttpResponse<String> resp = ApiClient.get(url);
                String body = resp.body();

                return extractInt(body, "count");
            }
        };

        // 2. Success Callback (Runs on JavaFX Application Thread)
        task.setOnSucceeded(e -> {
            int count = task.getValue();
            unreadCount = count; // Assuming this is a class-level variable

            if (count > 0) {
                badgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
                badgeLabel.setVisible(true);

                // Fire animations
                AnimationUtil.pulse(badgeLabel);
                AnimationUtil.popIn(badgeLabel);

                // Highlight the bell icon
                bellLabel.setStyle("-fx-font-size: 18px; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, #f85149, 8, 0, 0, 0);");

                // ── Show popup toast for new notifications ──
                showToast(count);
            } else {
                badgeLabel.setVisible(false);
                bellLabel.setStyle("-fx-font-size: 18px; -fx-cursor: hand;");
            }
        });

        // 3. Failure Callback (Runs on JavaFX Application Thread)
        task.setOnFailed(e -> {
            System.out.println("Notification poll error: " + task.getException().getMessage());
        });

        // 4. Daemon Thread Execution
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ── Toast popup ───────────────────────────────────────
    private static boolean toastShowing = false;

    private void showToast(int count) {
        if (toastShowing) return;
        toastShowing = true;

        javafx.application.Platform.runLater(() -> {
            Stage toast = new Stage();
            toast.initStyle(StageStyle.UNDECORATED);
            toast.setAlwaysOnTop(true);
            toast.setResizable(false);

            Label msg = new Label(
                    "🔔  You have " + count
                            + " unread notification"
                            + (count > 1 ? "s" : ""));
            msg.getStyleClass().add("toast-msg");

            Button viewBtn = new Button("View");
            viewBtn.getStyleClass().add("toast-view-btn");
            viewBtn.setStyle("-fx-background-radius: 6; -fx-padding: 4 12 4 12;");
            viewBtn.setOnAction(e -> {
                toast.close();
                toastShowing = false;
                showDropdown();
            });

            Button closeBtn = new Button("✕");
            closeBtn.getStyleClass().add("toast-close-btn");
            closeBtn.setOnAction(e -> {
                toast.close();
                toastShowing = false;
            });

            HBox content = new HBox(12, msg, viewBtn, closeBtn);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(12, 16, 12, 16));
            content.getStyleClass().add("toast-panel");

            Scene scene = new Scene(content);
            ThemeManager.apply(scene);
            toast.setScene(scene);

            // ── Position bottom right ─────────────────────
            javafx.geometry.Rectangle2D screen =
                    javafx.stage.Screen.getPrimary().getVisualBounds();
            toast.setX(screen.getMaxX() - 420);
            toast.setY(screen.getMaxY() - 80);
            toast.show();

            // ── Auto dismiss after 5 seconds ──────────────
            Timeline dismiss = new Timeline(
                    new KeyFrame(Duration.seconds(5), ev -> {
                        toast.close();
                        toastShowing = false;
                    }));
            dismiss.play();
        });
    }

    // ── Notification dropdown ─────────────────────────────
    private void showDropdown() {
        Stage dropdown = new Stage();
        dropdown.initStyle(StageStyle.UNDECORATED);
        dropdown.setResizable(false);

        Label title = new Label("Notifications");
        title.getStyleClass().add("notif-title");

        Button markAllBtn = new Button("Mark all read");
        markAllBtn.getStyleClass().add("notif-link-btn");

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("notif-close-btn");
        closeBtn.setOnAction(e -> dropdown.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, title, spacer, markAllBtn, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 12, 8, 16));
        header.getStyleClass().add("notif-header");

        VBox feed = new VBox(0);
        ScrollPane scroll = new ScrollPane(feed);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(360);
        scroll.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;");

        loadNotifications(feed, dropdown);

        markAllBtn.setOnAction(e -> {
            markAllRead();
            dropdown.close();
        });

        VBox root = new VBox(header, scroll);
        root.setPrefWidth(360);
        root.getStyleClass().add("notif-panel");

        Scene scene = new Scene(root);
        ThemeManager.apply(scene);
        dropdown.setScene(scene);

        // ── Position near bell icon ───────────────────────
        javafx.geometry.Bounds bounds =
                bellPane.localToScreen(bellPane.getBoundsInLocal());
        if (bounds != null) {
            dropdown.setX(bounds.getMaxX() - 360);
            dropdown.setY(bounds.getMaxY() + 4);
        }

        // ── Close when clicking outside ───────────────────
        dropdown.focusedProperty().addListener((obs, ov, nv) -> {
            if (!nv) dropdown.close();
        });

        dropdown.show();
    }

    private void loadNotifications(VBox feed, Stage dropdown) {
        feed.getChildren().clear();
        try {
            int userId = SessionManager.get().getUserId();
            HttpResponse<String> resp = ApiClient.get(
                    ConfigManager.getBaseUrl() + "/api/notifications/user/" + userId);
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);

            if (body.isEmpty()) {
                Label empty = new Label("No notifications yet.");
                empty.getStyleClass().add("notif-empty");
                feed.getChildren().add(empty);
                return;
            }

            for (String obj : body.split("\\},\\{")) {
                obj = obj.replace("{", "").replace("}", "");
                int id        = extractInt(obj, "id");
                String msg    = extractValue(obj, "message");
                String type   = extractValue(obj, "type");
                String time   = extractValue(obj, "createdAt");
                int isRead    = extractInt(obj, "isRead");
                int refId     = extractInt(obj, "referenceId");

                feed.getChildren().add(
                        notificationRow(id, msg, type,
                                time, isRead, refId, dropdown));
            }
        } catch (Exception ex) {
            Label err = new Label("Error loading notifications.");
            err.getStyleClass().add("notif-error");
            feed.getChildren().add(err);
        }
    }

    private VBox notificationRow(int id, String message, String type,
                                 String time, int isRead, int refId,
                                 Stage dropdown) {
        String dot = isRead == 0 ? "🔵 " : "⚪ ";
        Label msgLabel = new Label(dot + message);
        msgLabel.getStyleClass().add(isRead == 0 ? "notif-msg-unread" : "notif-msg-read");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(320);

        Label timeLabel = new Label(DateTimeFormatUtil.toIndianDateTime(time));
        timeLabel.getStyleClass().add("notif-time");

        String iconStr = switch (type) {
            case "TICKET_CREATED"  -> "🎫";
            case "TICKET_ASSIGNED" -> "👤";
            case "STATUS_CHANGED"  -> "🔄";
            case "COMMENT_ADDED"   -> "💬";
            default                -> "📌";
        };
        Label typeIcon = new Label(iconStr);
        typeIcon.setStyle("-fx-font-size: 16px;");

        VBox textBox = new VBox(3, msgLabel, timeLabel);
        HBox row = new HBox(10, typeIcon, textBox);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.getStyleClass().add(isRead == 0 ? "notif-row-unread" : "notif-row-read");

        row.setOnMouseEntered(e -> {
            row.getStyleClass().removeAll("notif-row-unread", "notif-row-read");
            row.getStyleClass().add("notif-row-hover");
        });
        row.setOnMouseExited(e -> {
            row.getStyleClass().remove("notif-row-hover");
            row.getStyleClass().add(isRead == 0 ? "notif-row-unread" : "notif-row-read");
        });

        VBox wrapper = new VBox(row);
        row.setOnMouseClicked(e -> {
            markRead(id);
            dropdown.close();
        });

        return wrapper;
    }

    private void markRead(int id) {
        try {
            ApiClient.putNoBody(ConfigManager.getBaseUrl() + "/api/notifications/" + id + "/read");
            fetchUnreadCount();
        } catch (Exception ex) {
            System.out.println("Mark read error: " + ex.getMessage());
        }
    }

    private void markAllRead() {
        try {
            int userId = SessionManager.get().getUserId();
            ApiClient.putNoBody(ConfigManager.getBaseUrl() + "/api/notifications/user/" + userId + "/readall");
            fetchUnreadCount();
        } catch (Exception ex) {
            System.out.println("Mark all read error: " + ex.getMessage());
        }
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        return json.substring(start, json.indexOf("\"", start));
    }

    private int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.length();
        try {
            return Integer.parseInt(
                    json.substring(start, end).trim().replace("}", ""));
        } catch (NumberFormatException e) { return 0; }
    }
}