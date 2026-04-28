package com.vaultdesk.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;

public class DashboardStatsView {

    public VBox getView() {

        // ── Page header ───────────────────────────────────
        Label pageTitle = new Label("Dashboard Overview");
        pageTitle.getStyleClass().add("page-title");
        Label pageSub = new Label("Here's what's happening today.");
        pageSub.getStyleClass().add("page-subtitle");

        // ── Stat cards ────────────────────────────────────
        VBox cardAssets    = statCard("⊞", "0", "TOTAL ASSETS",
                "22 General / 12 SAP", "+12.4%", "stat-badge-green",
                "stat-card-blue", "stat-icon-box-blue", "#58a6ff");
        VBox cardTickets   = statCard("✉", "0", "OPEN TICKETS",
                "Needs attention", "Urgent", "stat-badge-red",
                "stat-card-red", "stat-icon-box-red", "#f85149");
        VBox cardLicenses  = statCard("🔑", "0", "EXPIRING LICENSES",
                "Within 30 days", "30 Days", "stat-badge-orange",
                "stat-card-orange", "stat-icon-box-orange", "#d29922");
        VBox cardEmployees = statCard("👤", "0", "ACTIVE EMPLOYEES",
                "All departments", "Global", "stat-badge-blue",
                "stat-card-green", "stat-icon-box-green", "#3fb950");

        HBox statsRow = new HBox(16,
                cardAssets, cardTickets, cardLicenses, cardEmployees);
        statsRow.setPadding(new Insets(8, 0, 8, 0));

        // ── Tickets at a Glance ───────────────────────────
        Label recentLabel = new Label("Tickets at a Glance");
        recentLabel.getStyleClass().add("section-title");
        Label recentSub = new Label("Monitoring operational health and resolution speed");
        recentSub.getStyleClass().add("page-subtitle");

        // ── Table ─────────────────────────────────────────
        TableView<Ticket> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(380);

        // Ticket No
        TableColumn<Ticket, String> ticketNoCol = new TableColumn<>("TICKET ID");
        ticketNoCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTicketNo()));
        ticketNoCol.setPrefWidth(160);

        // Title
        TableColumn<Ticket, String> titleCol = new TableColumn<>("REQUEST DETAIL");
        titleCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTitle()));
        titleCol.setPrefWidth(220);

        // Category — colored text
        TableColumn<Ticket, String> categoryCol = new TableColumn<>("SYSTEM");
        categoryCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCategory()));
        categoryCol.setPrefWidth(110);
        categoryCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
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

        // Priority — colored text
        TableColumn<Ticket, String> priorityCol = new TableColumn<>("PRIORITY");
        priorityCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPriority()));
        priorityCol.setPrefWidth(100);
        priorityCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
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

        // Status — colored text
        TableColumn<Ticket, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setPrefWidth(110);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
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

        table.getColumns().addAll(
                ticketNoCol, titleCol, categoryCol, priorityCol, statusCol);

        // ── Recent Activity panel ─────────────────────────
        Label activityTitle = new Label("Recent Activity");
        activityTitle.getStyleClass().add("activity-panel-title");

        Label realtimeLabel = new Label("REAL-TIME");
        realtimeLabel.setStyle("-fx-text-fill: #484f58; -fx-font-size: 10px; -fx-font-weight: bold;");

        HBox activityHeader = new HBox(activityTitle, new Region(), realtimeLabel);
        HBox.setHgrow(activityHeader.getChildren().get(1), Priority.ALWAYS);
        activityHeader.setAlignment(Pos.CENTER_LEFT);

        VBox activityFeed = new VBox(0);

        VBox activityPanel = new VBox(10, activityHeader, new Separator(), activityFeed);
        activityPanel.getStyleClass().add("activity-panel");

        // ── Main content row (table + activity) ───────────
        HBox mainRow = new HBox(16);
        VBox tableBox = new VBox(8, recentLabel, recentSub, table);
        HBox.setHgrow(tableBox, Priority.ALWAYS);
        mainRow.getChildren().addAll(tableBox, activityPanel);

        // ── Load stats ────────────────────────────────────
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/dashboard/stats"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();

            setStatNumber(cardAssets,    extractInt(body, "totalAssets"));
            setStatNumber(cardTickets,   extractInt(body, "openTickets"));
            setStatNumber(cardLicenses,  extractInt(body, "expiringLicenses"));
            setStatNumber(cardEmployees, extractInt(body, "totalEmployees"));

        } catch (Exception ex) {
            System.out.println("Error loading stats: " + ex.getMessage());
        }

        // ── Load recent tickets ───────────────────────────
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/dashboard/recent-activity"))
                    .GET().build();
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
                            extractValue(obj, "createdAt")
                    ));

                    // ── Add to activity feed ───────────────
                    String dotClass = dotClass(extractValue(obj, "priority"));
                    String title    = extractValue(obj, "title");
                    String status   = extractValue(obj, "status");
                    String time     = extractValue(obj, "createdAt");
                    activityFeed.getChildren().add(
                            activityEntry(dotClass, title, status, time));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading tickets: " + ex.getMessage());
        }

        VBox root = new VBox(16,
                pageTitle, pageSub, statsRow, mainRow);
        return root;
    }

    // ── Stat card builder ─────────────────────────────────
    private VBox statCard(String icon, String number, String label,
                          String sublabel, String badge, String badgeClass,
                          String cardClass, String iconBoxClass, String iconColor) {

        // Icon box
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-text-fill: " + iconColor + "; -fx-font-size: 18px;");
        StackPane iconBox = new StackPane(iconLabel);
        iconBox.getStyleClass().add(iconBoxClass);
        iconBox.setMinSize(40, 40);
        iconBox.setMaxSize(40, 40);

        // Badge
        Label badgeLabel = new Label(badge);
        badgeLabel.getStyleClass().add(badgeClass);

        // Top row: icon + badge
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(iconBox, spacer, badgeLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Number
        Label numLabel = new Label(number);
        numLabel.getStyleClass().add("stat-number");
        numLabel.setId("stat-num");

        // Labels
        Label txtLabel = new Label(label);
        txtLabel.getStyleClass().add("stat-label");

        Label subLabel = new Label(sublabel);
        subLabel.getStyleClass().add("stat-sublabel");

        VBox card = new VBox(8, topRow, numLabel, txtLabel, subLabel);
        card.getStyleClass().addAll("stat-card", cardClass);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void setStatNumber(VBox card, int value) {
        card.getChildren().stream()
                .filter(n -> n instanceof Label
                        && ((Label) n).getId() != null
                        && ((Label) n).getId().equals("stat-num"))
                .findFirst()
                .ifPresent(n -> ((Label) n).setText(String.valueOf(value)));
    }

    // ── Activity entry builder ────────────────────────────
    private VBox activityEntry(String dotClass, String title,
                               String status, String time) {
        Region dot = new Region();
        dot.getStyleClass().addAll("activity-dot", dotClass);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("activity-text");
        titleLabel.setMaxWidth(200);
        titleLabel.setWrapText(true);

        Label statusLabel = new Label(status);
        statusLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px;");

        VBox textBox = new VBox(2, titleLabel, statusLabel);
        HBox row = new HBox(10, dot, textBox);
        row.setAlignment(Pos.TOP_LEFT);

        Label timeLabel = new Label(time != null && time.length() >= 10
                ? time.substring(0, 10) : time);
        timeLabel.getStyleClass().add("activity-time");

        VBox entry = new VBox(4, row, timeLabel);
        entry.setStyle("-fx-border-color: #21262d; -fx-border-width: 0 0 1 0;" +
                "-fx-padding: 8 0 8 0;");
        return entry;
    }

    private String dotClass(String priority) {
        if (priority == null) return "activity-dot-grey";
        return switch (priority.toUpperCase()) {
            case "CRITICAL" -> "activity-dot-red";
            case "HIGH"     -> "activity-dot-orange";
            case "MEDIUM"   -> "activity-dot-blue";
            case "LOW"      -> "activity-dot-green";
            default         -> "activity-dot-grey";
        };
    }

    // ── Helpers ───────────────────────────────────────────
    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
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