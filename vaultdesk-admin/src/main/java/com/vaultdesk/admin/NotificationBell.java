package com.vaultdesk.admin;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
        try {
            int userId = SessionManager.get().getUserId();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/notifications/user/"
                            + userId + "/unread"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body();
            int count = extractInt(body, "count");

            javafx.application.Platform.runLater(() -> {
                unreadCount = count;
                if (count > 0) {
                    badgeLabel.setText(
                            count > 99 ? "99+" : String.valueOf(count));
                    badgeLabel.setVisible(true);
                    bellLabel.setStyle(
                            "-fx-font-size: 18px; -fx-cursor: hand;" +
                                    "-fx-effect: dropshadow(gaussian," +
                                    " #f85149, 8, 0, 0, 0);");
                    // ── Show popup toast for new notifications ──
                    if (count > 0) showToast(count);
                } else {
                    badgeLabel.setVisible(false);
                    bellLabel.setStyle(
                            "-fx-font-size: 18px; -fx-cursor: hand;");
                }
            });
        } catch (Exception ex) {
            System.out.println("Notification poll error: "
                    + ex.getMessage());
        }
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
            msg.setStyle(
                    "-fx-text-fill: #e6edf3; -fx-font-size: 13px;" +
                            "-fx-font-weight: bold;");

            Button viewBtn = new Button("View");
            viewBtn.setStyle(
                    "-fx-background-color: #1f6feb;" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 6;" +
                            "-fx-padding: 4 12 4 12;" +
                            "-fx-font-size: 11px;" +
                            "-fx-cursor: hand;");
            viewBtn.setOnAction(e -> {
                toast.close();
                toastShowing = false;
                showDropdown();
            });

            Button closeBtn = new Button("✕");
            closeBtn.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: #8b949e;" +
                            "-fx-font-size: 12px;" +
                            "-fx-cursor: hand;" +
                            "-fx-border-width: 0;");
            closeBtn.setOnAction(e -> {
                toast.close();
                toastShowing = false;
            });

            HBox content = new HBox(12, msg, viewBtn, closeBtn);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(12, 16, 12, 16));
            content.setStyle(
                    "-fx-background-color: #21262d;" +
                            "-fx-border-color: #58a6ff;" +
                            "-fx-border-width: 0 0 0 3;" +
                            "-fx-effect: dropshadow(gaussian," +
                            " rgba(0,0,0,0.5), 12, 0, 0, 4);");

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
        title.setStyle(
                "-fx-text-fill: #e6edf3; -fx-font-size: 14px;" +
                        "-fx-font-weight: bold;");

        Button markAllBtn = new Button("Mark all read");
        markAllBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #58a6ff;" +
                        "-fx-font-size: 11px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-width: 0;");

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #8b949e;" +
                        "-fx-font-size: 13px;" +
                        "-fx-cursor: hand;" +
                        "-fx-border-width: 0;");
        closeBtn.setOnAction(e -> dropdown.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, title, spacer, markAllBtn, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 12, 8, 16));
        header.setStyle(
                "-fx-border-color: #30363d;" +
                        "-fx-border-width: 0 0 1 0;");

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
        root.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-width: 1;" +
                        "-fx-effect: dropshadow(gaussian," +
                        " rgba(0,0,0,0.5), 16, 0, 0, 4);");

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
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/notifications/user/" + userId))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);

            if (body.isEmpty()) {
                Label empty = new Label("No notifications yet.");
                empty.setStyle(
                        "-fx-text-fill: #484f58;" +
                                "-fx-font-size: 12px;" +
                                "-fx-padding: 20;");
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
            err.setStyle(
                    "-fx-text-fill: #f85149; -fx-font-size: 11px;");
            feed.getChildren().add(err);
        }
    }

    private VBox notificationRow(int id, String message, String type,
                                 String time, int isRead, int refId,
                                 Stage dropdown) {
        String dot = isRead == 0 ? "🔵 " : "⚪ ";
        Label msgLabel = new Label(dot + message);
        msgLabel.setStyle(
                "-fx-text-fill: " + (isRead == 0
                        ? "#e6edf3" : "#8b949e") + ";" +
                        "-fx-font-size: 12px;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(320);

        Label timeLabel = new Label(
                time != null && time.length() >= 16
                        ? time.substring(0, 16) : time);
        timeLabel.setStyle(
                "-fx-text-fill: #484f58; -fx-font-size: 10px;");

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
        row.setStyle(
                "-fx-border-color: #21262d;" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-background-color: " + (isRead == 0
                        ? "#161b22" : "#0d1117") + ";" +
                        "-fx-cursor: hand;");

        row.setOnMouseEntered(e ->
                row.setStyle(
                        "-fx-border-color: #21262d;" +
                                "-fx-border-width: 0 0 1 0;" +
                                "-fx-background-color: #21262d;" +
                                "-fx-cursor: hand;"));
        row.setOnMouseExited(e ->
                row.setStyle(
                        "-fx-border-color: #21262d;" +
                                "-fx-border-width: 0 0 1 0;" +
                                "-fx-background-color: " + (isRead == 0
                                ? "#161b22" : "#0d1117") + ";" +
                                "-fx-cursor: hand;"));

        VBox wrapper = new VBox(row);
        row.setOnMouseClicked(e -> {
            markRead(id);
            dropdown.close();
        });

        return wrapper;
    }

    private void markRead(int id) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/notifications/" + id + "/read"))
                    .PUT(HttpRequest.BodyPublishers.noBody()).build();
            client.send(req, HttpResponse.BodyHandlers.ofString());
            fetchUnreadCount();
        } catch (Exception ex) {
            System.out.println("Mark read error: " + ex.getMessage());
        }
    }

    private void markAllRead() {
        try {
            int userId = SessionManager.get().getUserId();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/notifications/user/"
                            + userId + "/readall"))
                    .PUT(HttpRequest.BodyPublishers.noBody()).build();
            client.send(req, HttpResponse.BodyHandlers.ofString());
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