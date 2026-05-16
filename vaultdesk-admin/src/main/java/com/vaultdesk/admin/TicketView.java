package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class TicketView {

    private final Map<Integer, String> userMap = new LinkedHashMap<>();
    // ── Main layout — list on left, detail on right ───────
    private HBox mainLayout;
    private VBox detailPanel;
    private TableView<Ticket> table;

    public VBox getView() {
        loadUsers();

        Label title = new Label("Tickets");
        title.getStyleClass().add("section-title");

        // ── Table ─────────────────────────────────────────
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Ticket, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        idCol.setMaxWidth(50);

        TableColumn<Ticket, String> ticketNoCol = new TableColumn<>("Ticket No");
        ticketNoCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTicketNo()));

        TableColumn<Ticket, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTitle()));

        TableColumn<Ticket, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCategory()));
        categoryCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item.toUpperCase()) {
                    case "SAP"      -> setStyle("-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
                    case "HARDWARE" -> setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold;");
                    case "NETWORK"  -> setStyle("-fx-text-fill: #39d353; -fx-font-weight: bold;");
                    case "SOFTWARE" -> setStyle("-fx-text-fill: #a371f7; -fx-font-weight: bold;");
                    default         -> setStyle("-fx-text-fill: #c9d1d9; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<Ticket, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPriority()));
        priorityCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item.toUpperCase()) {
                    case "CRITICAL" -> setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
                    case "HIGH"     -> setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
                    case "MEDIUM"   -> setStyle("-fx-text-fill: #8b949e;");
                    case "LOW"      -> setStyle("-fx-text-fill: #6e7681;");
                    default         -> setStyle("-fx-text-fill: #c9d1d9;");
                }
            }
        });

        TableColumn<Ticket, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "Open"        -> setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
                    case "In Progress" -> setStyle("-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
                    case "Resolved"    -> setStyle("-fx-text-fill: #3fb950; -fx-font-weight: bold;");
                    case "Closed"      -> setStyle("-fx-text-fill: #8b949e;");
                    default            -> setStyle("-fx-text-fill: #c9d1d9;");
                }
            }
        });

        TableColumn<Ticket, String> assignedToCol = new TableColumn<>("Assigned To");
        assignedToCol.setCellValueFactory(d -> {
            int uid = d.getValue().getAssignedTo();
            String name = uid == 0 ? "Unassigned"
                    : userMap.getOrDefault(uid, "User #" + uid);
            return new SimpleStringProperty(name);
        });

        TableColumn<Ticket, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(d -> {
            String ca = d.getValue().getCreatedAt();
            return new SimpleStringProperty(
                    ca != null && ca.length() >= 10 ? ca.substring(0, 10) : ca);
        });

        // ── Time open + SLA color ─────────────────────────────
        TableColumn<Ticket, String> slaCol = new TableColumn<>("Time Open");
        slaCol.setCellValueFactory(d -> {
            String created = d.getValue().getCreatedAt();
            String status  = d.getValue().getStatus();
            if (created == null || "Resolved".equals(status)
                    || "Closed".equals(status))
                return new SimpleStringProperty("—");
            try {
                java.time.LocalDateTime createdTime =
                        java.time.LocalDateTime.parse(
                                created.replace(" ", "T"));
                long hours = java.time.Duration.between(
                        createdTime,
                        java.time.LocalDateTime.now()).toHours();
                if (hours < 1) return new SimpleStringProperty("< 1h");
                if (hours < 24) return new SimpleStringProperty(hours + "h");
                return new SimpleStringProperty((hours / 24) + "d "
                        + (hours % 24) + "h");
            } catch (Exception e) {
                return new SimpleStringProperty("—");
            }
        });
        slaCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "—".equals(item)) {
                    setText(item);
                    setStyle("-fx-text-fill: #484f58;");
                    return;
                }
                setText(item);
                // Get priority from same row
                Ticket t = getTableView().getItems().get(getIndex());
                long hours = 0;
                try {
                    if (t.getCreatedAt() != null) {
                        java.time.LocalDateTime ct =
                                java.time.LocalDateTime.parse(
                                        t.getCreatedAt().replace(" ", "T"));
                        hours = java.time.Duration.between(ct,
                                java.time.LocalDateTime.now()).toHours();
                    }
                } catch (Exception ignored) {}

                long slaHours = switch (t.getPriority().toUpperCase()) {
                    case "CRITICAL" -> 4;
                    case "HIGH"     -> 8;
                    case "MEDIUM"   -> 24;
                    default         -> 48;
                };

                if (hours > slaHours)
                    setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
                else if (hours > slaHours * 0.75)
                    setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
                else
                    setStyle("-fx-text-fill: #3fb950;");
            }
        });

        TableColumn<Ticket, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button updateBtn = new Button("Update");
            private final Button assignBtn = new Button("Assign");
            {
                updateBtn.getStyleClass().setAll("btn-warning");
                updateBtn.setStyle(
                        "-fx-background-color: #b45309; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 5 10 5 10;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;");
                assignBtn.getStyleClass().setAll("btn-primary");
                assignBtn.setStyle(
                        "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 5 10 5 10;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;");
                updateBtn.setOnAction(e -> {
                    Ticket t = getTableView().getItems().get(getIndex());
                    showUpdateStatusDialog(t, getTableView());
                });
                assignBtn.setOnAction(e -> {
                    Ticket t = getTableView().getItems().get(getIndex());
                    showAssignDialog(t, getTableView());
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, updateBtn, assignBtn));
            }
        });

        table.getColumns().addAll(idCol, ticketNoCol, titleCol,
                categoryCol, priorityCol, statusCol,
                assignedToCol, createdCol,slaCol, actionCol);

        // ── Click row to open detail panel ────────────────
        table.setRowFactory(tv -> {
            TableRow<Ticket> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openDetailPanel(row.getItem());
                }
            });
            return row;
        });

        // ── Add ticket button ─────────────────────────────
        Button addBtn = new Button("＋ New Ticket");
        addBtn.getStyleClass().setAll("btn-primary");
        addBtn.setStyle(
                "-fx-background-color: #238636; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 8 16 8 16;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showAddTicketDialog(table));

        Label hint = new Label("Double-click a row to view details and comments");
        hint.setStyle("-fx-text-fill: #484f58; -fx-font-size: 11px;");

        HBox topBar = new HBox(10, addBtn, hint);
        topBar.setAlignment(Pos.CENTER_LEFT);

        loadTickets(table);

        // ── Detail panel (hidden initially) ──────────────
        detailPanel = new VBox();
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        detailPanel.setPrefWidth(400);
        detailPanel.setMinWidth(400);
        detailPanel.setMaxWidth(400);
        detailPanel.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-width: 0 0 0 1;");

        // ── Main layout: table left, detail right ─────────
        VBox tableBox = new VBox(10, topBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(tableBox, Priority.ALWAYS);

        mainLayout = new HBox(tableBox, detailPanel);
        HBox.setHgrow(tableBox, Priority.ALWAYS);

        VBox root = new VBox(10, title, mainLayout);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);
        return root;
    }

    // ── Open detail panel ─────────────────────────────────
    private void openDetailPanel(Ticket ticket) {
        detailPanel.getChildren().clear();
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);

        // ── Header ────────────────────────────────────────
        Label ticketNoLabel = new Label(ticket.getTicketNo());
        ticketNoLabel.setStyle(
                "-fx-text-fill: #58a6ff; -fx-font-size: 12px; -fx-font-weight: bold;");

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #8b949e;" +
                        "-fx-font-size: 14px; -fx-cursor: hand; -fx-border-width: 0;");
        closeBtn.setOnAction(e -> {
            detailPanel.setVisible(false);
            detailPanel.setManaged(false);
        });

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(ticketNoLabel, headerSpacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 12, 8, 16));
        header.setStyle("-fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");

        // ── Title ─────────────────────────────────────────
        Label titleLabel = new Label(ticket.getTitle());
        titleLabel.setStyle(
                "-fx-text-fill: #e6edf3; -fx-font-size: 15px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);

        // ── Status + Priority badges ──────────────────────
        Label statusBadge = new Label(ticket.getStatus());
        statusBadge.setPadding(new Insets(3, 10, 3, 10));
        statusBadge.setStyle(statusBadgeStyle(ticket.getStatus()));

        Label priorityBadge = new Label(ticket.getPriority());
        priorityBadge.setPadding(new Insets(3, 10, 3, 10));
        priorityBadge.setStyle(priorityBadgeStyle(ticket.getPriority()));

        HBox badges = new HBox(8, statusBadge, priorityBadge);

        // ── Info grid ─────────────────────────────────────
        String reporterName = userMap.getOrDefault(
                ticket.getAssignedTo(), "Employee #" + ticket.getAssignedTo());
        String assigneeName = ticket.getAssignedTo() == 0 ? "Unassigned"
                : userMap.getOrDefault(ticket.getAssignedTo(),
                "User #" + ticket.getAssignedTo());

        VBox infoBox = new VBox(6,
                infoRow("Reported By", userMap.getOrDefault(
                        ticket.getReportedBy(),                    // ← fix
                        "Employee #" + ticket.getReportedBy())),   // ← fix
                infoRow("Assigned To", ticket.getAssignedTo() == 0
                        ? "Unassigned"
                        : userMap.getOrDefault(ticket.getAssignedTo(),
                        "User #" + ticket.getAssignedTo())),
                infoRow("Category", ticket.getCategory()),
                infoRow("Created", ticket.getCreatedAt() != null
                        && ticket.getCreatedAt().length() >= 10
                        ? ticket.getCreatedAt().substring(0, 10) : ticket.getCreatedAt()),
                infoRow("Updated", ticket.getUpdatedAt() != null
                        && ticket.getUpdatedAt().length() >= 10
                        ? ticket.getUpdatedAt().substring(0, 10) : "-")
        );
        infoBox.setStyle(
                "-fx-background-color: #21262d; -fx-background-radius: 6;");
        infoBox.setPadding(new Insets(10));

        // ── Description ───────────────────────────────────
        Label descTitle = new Label("Description");
        descTitle.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label descLabel = new Label(
                ticket.getTitle()); // using title as placeholder — description not in client model
        descLabel.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");
        descLabel.setWrapText(true);

        // ── Separator ─────────────────────────────────────
        Separator sep = new Separator();

        // ── Comments section ──────────────────────────────
        Label commentsTitle = new Label("Comments");
        commentsTitle.setStyle(
                "-fx-text-fill: #e6edf3; -fx-font-size: 13px; -fx-font-weight: bold;");

        VBox commentsFeed = new VBox(8);
        ScrollPane commentsScroll = new ScrollPane(commentsFeed);
        commentsScroll.setFitToWidth(true);
        commentsScroll.setPrefHeight(200);
        commentsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(commentsScroll, Priority.ALWAYS);

        // ── Load comments ─────────────────────────────────
        loadComments(ticket.getId(), commentsFeed);

        // ── Add comment ───────────────────────────────────
        TextArea commentInput = new TextArea();
        commentInput.setPromptText("Write a comment...");
        commentInput.setPrefRowCount(2);
        commentInput.setWrapText(true);
        commentInput.setStyle(
                "-fx-control-inner-background: #21262d; -fx-text-fill: #c9d1d9;" +
                        "-fx-border-color: #30363d; -fx-border-radius: 6;");

        Button postBtn = new Button("Post Comment");
        postBtn.getStyleClass().setAll("btn-primary");
        postBtn.setStyle(
                "-fx-background-color: #238636; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 6 14 6 14;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");
        postBtn.setOnAction(e -> {
            String text = commentInput.getText().trim();
            if (text.isEmpty()) return;
            postComment(ticket.getId(), text, commentsFeed);
            commentInput.clear();
        });

        VBox commentInputBox = new VBox(6, commentInput, postBtn);
        commentInputBox.setPadding(new Insets(0, 0, 8, 0));

        // ── Assemble detail panel ─────────────────────────
        VBox content = new VBox(12,
                titleLabel, badges, infoBox,
                sep, commentsTitle, commentsScroll, commentInputBox);
        content.setPadding(new Insets(12, 16, 12, 16));
        VBox.setVgrow(commentsScroll, Priority.ALWAYS);

        ScrollPane contentScroll = new ScrollPane(content);
        contentScroll.setFitToWidth(true);
        contentScroll.setStyle(
                "-fx-background-color: #161b22; -fx-background: #161b22;");
        VBox.setVgrow(contentScroll, Priority.ALWAYS);

        detailPanel.getChildren().addAll(header, contentScroll);
        VBox.setVgrow(contentScroll, Priority.ALWAYS);
    }

    // ── Load comments from API ────────────────────────────
    private void loadComments(int ticketId, VBox feed) {
        feed.getChildren().clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/tickets/" + ticketId + "/comments"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);

            if (body.isEmpty()) {
                Label empty = new Label("No comments yet. Be the first to comment.");
                empty.setStyle("-fx-text-fill: #484f58; -fx-font-size: 12px;");
                feed.getChildren().add(empty);
                return;
            }

            for (String obj : body.split("\\},\\{")) {
                obj = obj.replace("{", "").replace("}", "");
                String name    = extractValue(obj, "addedByName");
                String comment = extractValue(obj, "comment");
                String time    = extractValue(obj, "addedAt");
                int addedBy    = extractInt(obj, "addedBy");

                boolean isMe = addedBy == SessionManager.get().getUserId();
                feed.getChildren().add(commentBubble(name, comment, time, isMe));
            }
        } catch (Exception ex) {
            Label err = new Label("Error loading comments.");
            err.setStyle("-fx-text-fill: #f85149; -fx-font-size: 11px;");
            feed.getChildren().add(err);
        }
    }

    // ── Post comment ──────────────────────────────────────
    private void postComment(int ticketId, String text, VBox feed) {
        try {
            String sanitized = text.replace("\\", "\\\\")
                    .replace("\"", "'")
                    .replace("\n", " ")
                    .replace("\r", "");
            String body = "{" +
                    "\"comment\":\"" + sanitized + "\"," +
                    "\"addedBy\":\"" + SessionManager.get().getUserId() + "\"" +
                    "}";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/tickets/" + ticketId + "/comments"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 201) {
                loadComments(ticketId, feed);
            }
        } catch (Exception ex) {
            System.out.println("Error posting comment: " + ex.getMessage());
        }
    }

    // ── Comment bubble ────────────────────────────────────
    private VBox commentBubble(String name, String comment,
                               String time, boolean isMe) {
        Label nameLabel = new Label(name != null && !name.isEmpty()
                ? name : "Unknown");
        nameLabel.setStyle(
                "-fx-text-fill: " + (isMe ? "#3fb950" : "#58a6ff") + ";" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;");

        Label commentLabel = new Label(comment);
        commentLabel.setStyle(
                "-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");
        commentLabel.setWrapText(true);

        Label timeLabel = new Label(time != null && time.length() >= 16
                ? time.substring(0, 16) : time);
        timeLabel.setStyle("-fx-text-fill: #484f58; -fx-font-size: 10px;");

        VBox bubble = new VBox(3, nameLabel, commentLabel, timeLabel);
        bubble.setPadding(new Insets(8, 10, 8, 10));
        bubble.setStyle(
                "-fx-background-color: " + (isMe ? "#1b2d1f" : "#21262d") + ";" +
                        "-fx-background-radius: 6;");
        bubble.setMaxWidth(340);

        if (isMe) {
            bubble.setAlignment(Pos.CENTER_RIGHT);
        }
        return bubble;
    }

    // ── Info row helper ───────────────────────────────────
    private HBox infoRow(String label, String value) {
        Label k = new Label(label + ":");
        k.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px; -fx-min-width: 80;");
        Label v = new Label(value != null ? value : "-");
        v.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");
        return new HBox(8, k, v);
    }

    // ── Badge styles ──────────────────────────────────────
    private String statusBadgeStyle(String status) {
        String bg = switch (status) {
            case "Open"        -> "#3d1f1e";
            case "In Progress" -> "#1a2840";
            case "Resolved"    -> "#1b2d1f";
            case "Closed"      -> "#21262d";
            default            -> "#21262d";
        };
        String fg = switch (status) {
            case "Open"        -> "#f85149";
            case "In Progress" -> "#58a6ff";
            case "Resolved"    -> "#3fb950";
            case "Closed"      -> "#8b949e";
            default            -> "#c9d1d9";
        };
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
                "-fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;";
    }

    private String priorityBadgeStyle(String priority) {
        String bg = switch (priority.toUpperCase()) {
            case "CRITICAL" -> "#3d1f1e";
            case "HIGH"     -> "#2d2008";
            case "MEDIUM"   -> "#1a2840";
            case "LOW"      -> "#21262d";
            default         -> "#21262d";
        };
        String fg = switch (priority.toUpperCase()) {
            case "CRITICAL" -> "#f85149";
            case "HIGH"     -> "#d29922";
            case "MEDIUM"   -> "#58a6ff";
            case "LOW"      -> "#6e7681";
            default         -> "#c9d1d9";
        };
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
                "-fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;";
    }

    // ── Load users into map ───────────────────────────────
    private void loadUsers() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl() + "/api/users"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    userMap.put(extractInt(obj, "id"), extractValue(obj, "fullName"));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading users: " + ex.getMessage());
        }
    }

    private void loadTickets(TableView<Ticket> table) {
        table.getItems().clear();
        try {
            String url = SessionManager.get().isDeptHod()
                    ? ConfigManager.getBaseUrl() + "/api/tickets/department/"
                    + SessionManager.get().getDeptId()
                    : ConfigManager.getBaseUrl() + "/api/tickets";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url)).GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    table.getItems().add(new Ticket(
                            extractInt(obj, "id"),
                            extractValue(obj, "ticketNo"),
                            extractValue(obj, "title"),
                            extractValue(obj, "category"),
                            extractValue(obj, "priority"),
                            extractValue(obj, "status"),
                            extractInt(obj, "assignedTo"),
                            extractInt(obj, "reportedBy"),
                            extractValue(obj, "createdAt"),
                            extractValue(obj, "updatedAt")
                    ));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading tickets: " + ex.getMessage());
        }
    }

    private void showAddTicketDialog(TableView<Ticket> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New Ticket");
        dialog.setHeaderText("Create a new support ticket");
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("Brief description of the issue");
        TextArea descField = new TextArea();
        descField.setPromptText("Detailed description...");
        descField.setPrefRowCount(3);

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(
                "Hardware", "Software", "SAP", "Network", "General");
        categoryBox.setValue("Hardware");

        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("Low", "Medium", "High", "Critical");
        priorityBox.setValue("Medium");

        Label reportedByLabel = new Label(
                "Reported by: " + SessionManager.get().getFullName());
        reportedByLabel.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 11px;");

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Title *:"),     0, 0); grid.add(titleField,  1, 0);
        grid.add(new Label("Description:"), 0, 1); grid.add(descField,   1, 1);
        grid.add(new Label("Category:"),    0, 2); grid.add(categoryBox, 1, 2);
        grid.add(new Label("Priority:"),    0, 3); grid.add(priorityBox, 1, 3);
        grid.add(reportedByLabel,           1, 4);
        grid.add(errorLabel,                1, 5);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        titleField.textProperty().addListener((o, ov, nv) ->
                okButton.setDisable(nv.trim().isEmpty()));

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (titleField.getText().trim().isEmpty()) {
                errorLabel.setText("Title is required.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int reportedBy = SessionManager.get().getUserId();
                String t = titleField.getText().trim()
                        .replace("\\", "\\\\").replace("\"", "'")
                        .replace("\n", " ").replace("\r", "");
                String d = descField.getText().trim()
                        .replace("\\", "\\\\").replace("\"", "'")
                        .replace("\n", " ").replace("\r", "");
                String body = "{\"title\":\"" + t + "\",\"description\":\"" + d
                        + "\",\"category\":\"" + categoryBox.getValue()
                        + "\",\"priority\":\"" + priorityBox.getValue()
                        + "\",\"reportedBy\":" + reportedBy + "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigManager.getBaseUrl() + "/api/tickets"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 201) {
                    showAlert("Success", "Ticket created.");
                    loadTickets(table);
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private void showUpdateStatusDialog(Ticket ticket, TableView<Ticket> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Ticket Status");
        dialog.setHeaderText("Ticket: " + ticket.getTicketNo());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Open", "In Progress", "Resolved", "Closed");
        statusBox.setValue(ticket.getStatus());

        TextField resolutionField = new TextField();
        resolutionField.setPromptText("Resolution notes...");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Status:"),     0, 0); grid.add(statusBox,       1, 0);
        grid.add(new Label("Resolution:"), 0, 1); grid.add(resolutionField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (updateTicketStatus(ticket.getId(),
                    statusBox.getValue(), resolutionField.getText()))
                loadTickets(table);
        }
    }

    private void showAssignDialog(Ticket ticket, TableView<Ticket> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Assign Ticket");
        dialog.setHeaderText("Ticket: " + ticket.getTicketNo());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> userBox = new ComboBox<>();
        ObservableList<String> userNames = FXCollections.observableArrayList();
        Map<String, Integer> nameToId = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : userMap.entrySet()) {
            String display = entry.getValue() + " (#" + entry.getKey() + ")";
            userNames.add(display);
            nameToId.put(display, entry.getKey());
        }
        userBox.setItems(userNames);
        if (!userNames.isEmpty()) userBox.setValue(userNames.get(0));
        if (ticket.getAssignedTo() != 0) {
            String current = userMap.get(ticket.getAssignedTo());
            if (current != null)
                userBox.setValue(current + " (#" + ticket.getAssignedTo() + ")");
        }

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Assign to:"), 0, 0);
        grid.add(userBox, 1, 0);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String selected = userBox.getValue();
            if (selected != null) {
                int userId = nameToId.get(selected);
                if (assignTicket(ticket.getId(), userId)) loadTickets(table);
            }
        }
    }

    private boolean updateTicketStatus(int id, String status, String resolution) {
        try {
            String url = ConfigManager.getBaseUrl() + "/api/tickets/" + id
                    + "/status?status="
                    + URLEncoder.encode(status, StandardCharsets.UTF_8)
                    + "&resolution="
                    + URLEncoder.encode(resolution, StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                showAlert("Success", "Status updated to: " + status);
                return true;
            }
            showAlert("Error", "Server returned: " + resp.statusCode());
            return false;
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private boolean assignTicket(int ticketId, int userId) {
        try {
            String url = ConfigManager.getBaseUrl() + "/api/tickets/"
                    + ticketId + "/assign?userId=" + userId;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                showAlert("Success", "Ticket assigned to: "
                        + userMap.getOrDefault(userId, "User #" + userId));
                return true;
            }
            showAlert("Error", "Server returned: " + resp.statusCode());
            return false;
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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