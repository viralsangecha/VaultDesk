package com.vaultdesk.employee;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;

public class MyTicketsView {

    private VBox detailPanel;
    private HBox mainLayout;

    public VBox getView() {
        Label title = new Label("My Tickets");
        title.getStyleClass().add("page-title");
        Label sub = new Label("All support requests you have raised.");
        sub.getStyleClass().add("page-subtitle");

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<String[], String> noCol = new TableColumn<>("Ticket No");
        noCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[0]));

        TableColumn<String[], String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[1]));

        TableColumn<String[], String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[2]));

        TableColumn<String[], String> priCol = new TableColumn<>("Priority");
        priCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[3]));
        priCol.setCellFactory(col -> new TableCell<>() {
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

        TableColumn<String[], String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[4]));
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

        TableColumn<String[], String> dateCol = new TableColumn<>("Created");
        dateCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[5]));

        table.getColumns().addAll(noCol, titleCol, catCol,
                priCol, statusCol, dateCol);

        // ── Double click → detail panel ───────────────────
        table.setRowFactory(tv -> {
            TableRow<String[]> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openDetail(row.getItem());
                }
            });
            return row;
        });

        Label hint = new Label(
                "Double-click a ticket to view details and comments");
        hint.setStyle("-fx-text-fill: #484f58; -fx-font-size: 11px;");

        loadTickets(table);

        // ── Detail panel ──────────────────────────────────
        detailPanel = new VBox();
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        detailPanel.setPrefWidth(380);
        detailPanel.setMinWidth(380);
        detailPanel.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-width: 0 0 0 1;");

        VBox tableBox = new VBox(8, hint, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(tableBox, Priority.ALWAYS);

        mainLayout = new HBox(tableBox, detailPanel);
        HBox.setHgrow(tableBox, Priority.ALWAYS);

        VBox root = new VBox(10, title, sub, mainLayout);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);
        return root;
    }

    private void loadTickets(TableView<String[]> table) {
        table.getItems().clear();
        try {
            int empId = SessionManager.get().getEmployeeId();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/employee/tickets/" + empId))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String created = extractValue(obj, "createdAt");
                    table.getItems().add(new String[]{
                            extractValue(obj, "ticketNo"),
                            extractValue(obj, "title"),
                            extractValue(obj, "category"),
                            extractValue(obj, "priority"),
                            extractValue(obj, "status"),
                            created.length() >= 10
                                    ? created.substring(0, 10) : created,
                            String.valueOf(extractInt(obj, "id")),
                            extractValue(obj, "description"),
                            extractValue(obj, "resolution")
                    });
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading tickets: " + ex.getMessage());
        }
    }

    private void openDetail(String[] ticket) {
        detailPanel.getChildren().clear();
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);

        // ── Header ────────────────────────────────────────
        Label ticketNoLabel = new Label(ticket[0]);
        ticketNoLabel.setStyle(
                "-fx-text-fill: #58a6ff; -fx-font-size: 12px;" +
                        "-fx-font-weight: bold;");
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #8b949e; -fx-font-size: 14px;" +
                        "-fx-cursor: hand; -fx-border-width: 0;");
        closeBtn.setOnAction(e -> {
            detailPanel.setVisible(false);
            detailPanel.setManaged(false);
        });
        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);
        HBox header = new HBox(ticketNoLabel, hSpacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 12, 8, 16));
        header.setStyle(
                "-fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");

        // ── Title + badges ────────────────────────────────
        Label titleLabel = new Label(ticket[1]);
        titleLabel.setStyle(
                "-fx-text-fill: #e6edf3; -fx-font-size: 14px;" +
                        "-fx-font-weight: bold;");
        titleLabel.setWrapText(true);

        Label statusBadge = new Label(ticket[4]);
        statusBadge.setPadding(new Insets(3, 10, 3, 10));
        statusBadge.setStyle(statusStyle(ticket[4]));

        Label priorityBadge = new Label(ticket[3]);
        priorityBadge.setPadding(new Insets(3, 10, 3, 10));
        priorityBadge.setStyle(priorityStyle(ticket[3]));

        HBox badges = new HBox(8, statusBadge, priorityBadge);

        // ── Info ──────────────────────────────────────────
        VBox infoBox = new VBox(6,
                infoRow("Category", ticket[2]),
                infoRow("Created",  ticket[5]));
        infoBox.setStyle(
                "-fx-background-color: #21262d;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 10;");

        // ── Description ───────────────────────────────────
        Label descTitle = new Label("Description");
        descTitle.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 11px;" +
                        "-fx-font-weight: bold;");
        Label descLabel = new Label(
                ticket.length > 7 && !ticket[7].isEmpty()
                        ? ticket[7] : "No description provided.");
        descLabel.setStyle(
                "-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");
        descLabel.setWrapText(true);

        // ── Resolution (if resolved) ──────────────────────
        VBox resolutionBox = new VBox();
        if (ticket.length > 8 && ticket[8] != null
                && !ticket[8].isEmpty()) {
            Label resTitle = new Label("Resolution");
            resTitle.setStyle(
                    "-fx-text-fill: #3fb950; -fx-font-size: 11px;" +
                            "-fx-font-weight: bold;");
            Label resLabel = new Label(ticket[8]);
            resLabel.setStyle(
                    "-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");
            resLabel.setWrapText(true);
            VBox resCard = new VBox(4, resTitle, resLabel);
            resCard.setStyle(
                    "-fx-background-color: #1b2d1f;" +
                            "-fx-background-radius: 6;" +
                            "-fx-padding: 10;");
            resolutionBox.getChildren().add(resCard);
        }

        // ── Comments ──────────────────────────────────────
        Separator sep = new Separator();
        Label commentsTitle = new Label("Comments");
        commentsTitle.setStyle(
                "-fx-text-fill: #e6edf3; -fx-font-size: 13px;" +
                        "-fx-font-weight: bold;");

        VBox commentsFeed = new VBox(8);
        ScrollPane commentsScroll = new ScrollPane(commentsFeed);
        commentsScroll.setFitToWidth(true);
        commentsScroll.setPrefHeight(180);
        commentsScroll.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;");

        int ticketId = ticket.length > 6
                ? Integer.parseInt(ticket[6]) : 0;
        loadComments(ticketId, commentsFeed);

        // ── Add comment ───────────────────────────────────
        TextArea commentInput = new TextArea();
        commentInput.setPromptText("Write a comment...");
        commentInput.setPrefRowCount(2);
        commentInput.setWrapText(true);
        commentInput.setStyle(
                "-fx-control-inner-background: #21262d;" +
                        "-fx-text-fill: #c9d1d9;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-radius: 6;");

        Button postBtn = new Button("Post Comment");
        postBtn.getStyleClass().setAll("btn-primary");
        postBtn.setStyle(
                "-fx-background-color: #238636; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 6 14 6 14;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");
        postBtn.setOnAction(e -> {
            String text = commentInput.getText().trim();
            if (text.isEmpty()) return;
            postComment(ticketId, text, commentsFeed);
            commentInput.clear();
        });

        VBox content = new VBox(12,
                titleLabel, badges, infoBox,
                descTitle, descLabel,
                resolutionBox,
                sep, commentsTitle,
                commentsScroll,
                commentInput, postBtn);
        content.setPadding(new Insets(12, 16, 12, 16));
        VBox.setVgrow(commentsScroll, Priority.ALWAYS);

        ScrollPane contentScroll = new ScrollPane(content);
        contentScroll.setFitToWidth(true);
        contentScroll.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-background: #161b22;");
        VBox.setVgrow(contentScroll, Priority.ALWAYS);

        detailPanel.getChildren().addAll(header, contentScroll);
        VBox.setVgrow(contentScroll, Priority.ALWAYS);
    }

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
                Label empty = new Label("No comments yet.");
                empty.setStyle(
                        "-fx-text-fill: #484f58; -fx-font-size: 12px;");
                feed.getChildren().add(empty);
                return;
            }

            for (String obj : body.split("\\},\\{")) {
                obj = obj.replace("{", "").replace("}", "");
                String name    = extractValue(obj, "addedByName");
                String comment = extractValue(obj, "comment");
                String time    = extractValue(obj, "addedAt");
                int addedBy    = extractInt(obj, "addedBy");

                // Employee comments shown differently
                // addedBy matches employeeId only if same person
                // For employee app — all comments shown same style
                feed.getChildren().add(
                        commentBubble(name, comment, time));
            }
        } catch (Exception ex) {
            Label err = new Label("Error loading comments.");
            err.setStyle("-fx-text-fill: #f85149; -fx-font-size: 11px;");
            feed.getChildren().add(err);
        }
    }

    private void postComment(int ticketId, String text, VBox feed) {
        try {
            String sanitized = text.replace("\\", "\\\\")
                    .replace("\"", "'")
                    .replace("\n", " ")
                    .replace("\r", "");
            // Use employeeId as addedBy
            String body = "{\"comment\":\"" + sanitized
                    + "\",\"addedBy\":\""
                    + SessionManager.get().getEmployeeId() + "\"}";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/tickets/" + ticketId + "/comments"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 201) {
                loadComments(ticketId, feed);
            }
        } catch (Exception ex) {
            System.out.println("Error posting comment: " + ex.getMessage());
        }
    }

    private VBox commentBubble(String name, String comment, String time) {
        Label nameLabel = new Label(
                name != null && !name.isEmpty() ? name : "Unknown");
        nameLabel.setStyle(
                "-fx-text-fill: #58a6ff; -fx-font-size: 11px;" +
                        "-fx-font-weight: bold;");
        Label commentLabel = new Label(comment);
        commentLabel.setStyle(
                "-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");
        commentLabel.setWrapText(true);
        Label timeLabel = new Label(
                time != null && time.length() >= 16
                        ? time.substring(0, 16) : time);
        timeLabel.setStyle(
                "-fx-text-fill: #484f58; -fx-font-size: 10px;");
        VBox bubble = new VBox(3, nameLabel, commentLabel, timeLabel);
        bubble.setPadding(new Insets(8, 10, 8, 10));
        bubble.setStyle(
                "-fx-background-color: #21262d;" +
                        "-fx-background-radius: 6;");
        return bubble;
    }

    private HBox infoRow(String label, String value) {
        Label k = new Label(label + ":");
        k.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 11px;" +
                        "-fx-min-width: 70;");
        Label v = new Label(value != null ? value : "-");
        v.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");
        return new HBox(8, k, v);
    }

    private String statusStyle(String status) {
        String bg = switch (status) {
            case "Open"        -> "#3d1f1e";
            case "In Progress" -> "#1a2840";
            case "Resolved"    -> "#1b2d1f";
            default            -> "#21262d";
        };
        String fg = switch (status) {
            case "Open"        -> "#f85149";
            case "In Progress" -> "#58a6ff";
            case "Resolved"    -> "#3fb950";
            default            -> "#8b949e";
        };
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + fg
                + "; -fx-background-radius: 10; -fx-font-size: 11px;"
                + " -fx-font-weight: bold;";
    }

    private String priorityStyle(String priority) {
        String bg = switch (priority.toUpperCase()) {
            case "CRITICAL" -> "#3d1f1e";
            case "HIGH"     -> "#2d2008";
            case "MEDIUM"   -> "#1a2840";
            default         -> "#21262d";
        };
        String fg = switch (priority.toUpperCase()) {
            case "CRITICAL" -> "#f85149";
            case "HIGH"     -> "#d29922";
            case "MEDIUM"   -> "#58a6ff";
            default         -> "#6e7681";
        };
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + fg
                + "; -fx-background-radius: 10; -fx-font-size: 11px;"
                + " -fx-font-weight: bold;";
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