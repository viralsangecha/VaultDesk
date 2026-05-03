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

        // ── Stat cards — real data loaded below ───────────
        VBox cardAssets    = statCard("⊞", "0", "TOTAL ASSETS",
                "Loading...", "+12.4%", "stat-badge-green",
                "stat-card-blue", "stat-icon-box-blue", "#58a6ff");
        VBox cardTickets   = statCard("✉", "0", "OPEN TICKETS",
                "Loading...", "Urgent", "stat-badge-red",
                "stat-card-red", "stat-icon-box-red", "#f85149");
        VBox cardLicenses  = statCard("🔑", "0", "EXPIRING LICENSES",
                "Within 30 days", "30 Days", "stat-badge-orange",
                "stat-card-orange", "stat-icon-box-orange", "#d29922");
        VBox cardEmployees = statCard("👤", "0", "ACTIVE EMPLOYEES",
                "Loading...", "Global", "stat-badge-blue",
                "stat-card-green", "stat-icon-box-green", "#3fb950");

        HBox statsRow = new HBox(16,
                cardAssets, cardTickets, cardLicenses, cardEmployees);
        statsRow.setPadding(new Insets(8, 0, 8, 0));

        // ── Tickets at a Glance ───────────────────────────
        Label recentLabel = new Label("Tickets at a Glance");
        recentLabel.getStyleClass().add("section-title");
        Label recentSub = new Label(
                "Monitoring operational health and resolution speed");
        recentSub.getStyleClass().add("page-subtitle");

        // ── Table ─────────────────────────────────────────
        TableView<Ticket> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(380);

        TableColumn<Ticket, String> ticketNoCol = new TableColumn<>("TICKET ID");
        ticketNoCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTicketNo()));

        TableColumn<Ticket, String> titleCol = new TableColumn<>("REQUEST DETAIL");
        titleCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTitle()));

        TableColumn<Ticket, String> categoryCol = new TableColumn<>("SYSTEM");
        categoryCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCategory()));
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

        TableColumn<Ticket, String> priorityCol = new TableColumn<>("PRIORITY");
        priorityCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPriority()));
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

        TableColumn<Ticket, String> statusCol = new TableColumn<>("STATUS");
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus()));
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
        realtimeLabel.setStyle(
                "-fx-text-fill: #484f58; -fx-font-size: 10px; -fx-font-weight: bold;");
        Region actSpacer = new Region();
        HBox.setHgrow(actSpacer, Priority.ALWAYS);
        HBox activityHeader = new HBox(activityTitle, actSpacer, realtimeLabel);
        activityHeader.setAlignment(Pos.CENTER_LEFT);

        VBox activityFeed = new VBox(0);
        VBox activityPanel = new VBox(10,
                activityHeader, new Separator(), activityFeed);
        activityPanel.getStyleClass().add("activity-panel");

        // ── Main content row ──────────────────────────────
        VBox tableBox = new VBox(8, recentLabel, recentSub, table);
        HBox.setHgrow(tableBox, Priority.ALWAYS);
        HBox mainRow = new HBox(16, tableBox, activityPanel);

        // ── Load stats ────────────────────────────────────
        try {
            // Replace the stats loading URL
            String statsUrl = SessionManager.get().isDeptHod()
                    ? ConfigManager.getBaseUrl() + "/api/dashboard/stats/department/"
                    + SessionManager.get().getDeptId()
                    : ConfigManager.getBaseUrl() + "/api/dashboard/stats";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(statsUrl))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();

            int totalAssets    = extractInt(body, "totalAssets");
            int openTickets    = extractInt(body, "openTickets");
            int generalTickets = extractInt(body, "generalTickets");
            int sapTickets     = extractInt(body, "sapTickets");
            int expiringLic    = extractInt(body, "expiringLicenses");
            int totalEmp       = extractInt(body, "totalEmployees");
            int totalDepts     = extractInt(body, "totalDepartments");

            setStatNumber(cardAssets,    totalAssets);
            setStatNumber(cardTickets,   openTickets);
            setStatNumber(cardLicenses,  expiringLic);
            setStatNumber(cardEmployees, totalEmp);

            // ── Real sublabels ─────────────────────────────
            setStatSublabel(cardAssets,
                    generalTickets + " General / " + sapTickets + " SAP tickets");
            setStatSublabel(cardTickets,
                    openTickets > 0 ? "Needs attention" : "All clear");
            setStatSublabel(cardLicenses,
                    expiringLic > 0 ? "Action required" : "All valid");
            setStatSublabel(cardEmployees,
                    totalDepts + " department(s)");

        } catch (Exception ex) {
            System.out.println("Error loading stats: " + ex.getMessage());
        }

        // ── Load recent tickets ───────────────────────────
        try {
            String activityUrl = SessionManager.get().isDeptHod()
                    ? ConfigManager.getBaseUrl() + "/api/tickets/department/"
                    + SessionManager.get().getDeptId()
                    : ConfigManager.getBaseUrl() + "/api/dashboard/recent-activity";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(activityUrl))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);

            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String priority = extractValue(obj, "priority");
                    String title    = extractValue(obj, "title");
                    String status   = extractValue(obj, "status");
                    String time     = extractValue(obj, "createdAt");

                    table.getItems().add(new Ticket(
                            extractInt(obj, "id"),
                            extractValue(obj, "ticketNo"),
                            title,
                            extractValue(obj, "category"),
                            priority,
                            status,
                            extractInt(obj, "assignedTo"),
                            time
                    ));

                    activityFeed.getChildren().add(
                            activityEntry(dotClass(priority), title, status, time));
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
                          String cardClass, String iconBoxClass,
                          String iconColor) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
                "-fx-text-fill: " + iconColor + "; -fx-font-size: 18px;");
        StackPane iconBox = new StackPane(iconLabel);
        iconBox.getStyleClass().add(iconBoxClass);
        iconBox.setMinSize(40, 40);
        iconBox.setMaxSize(40, 40);

        Label badgeLabel = new Label(badge);
        badgeLabel.getStyleClass().add(badgeClass);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(iconBox, spacer, badgeLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label numLabel = new Label(number);
        numLabel.getStyleClass().add("stat-number");
        numLabel.setId("stat-num");

        Label txtLabel = new Label(label);
        txtLabel.getStyleClass().add("stat-label");

        Label subLabel = new Label(sublabel);
        subLabel.getStyleClass().add("stat-sublabel");
        subLabel.setId("stat-sub");

        VBox card = new VBox(8, topRow, numLabel, txtLabel, subLabel);
        card.getStyleClass().addAll("stat-card", cardClass);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private void setStatNumber(VBox card, int value) {
        card.getChildren().stream()
                .filter(n -> n instanceof Label
                        && "stat-num".equals(((Label) n).getId()))
                .findFirst()
                .ifPresent(n -> ((Label) n).setText(String.valueOf(value)));
    }

    private void setStatSublabel(VBox card, String text) {
        card.getChildren().stream()
                .filter(n -> n instanceof Label
                        && "stat-sub".equals(((Label) n).getId()))
                .findFirst()
                .ifPresent(n -> ((Label) n).setText(text));
    }

    // ── Activity entry ────────────────────────────────────
    private VBox activityEntry(String dotClass, String title,
                               String status, String time) {
        Region dot = new Region();
        dot.getStyleClass().addAll("activity-dot", dotClass);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("activity-text");
        titleLabel.setMaxWidth(200);
        titleLabel.setWrapText(true);

        Label statusLabel = new Label(status);
        statusLabel.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 11px;");

        VBox textBox = new VBox(2, titleLabel, statusLabel);
        HBox row = new HBox(10, dot, textBox);
        row.setAlignment(Pos.TOP_LEFT);

        Label timeLabel = new Label(
                time != null && time.length() >= 10
                        ? time.substring(0, 10) : time);
        timeLabel.getStyleClass().add("activity-time");

        VBox entry = new VBox(4, row, timeLabel);
        entry.setStyle(
                "-fx-border-color: #21262d; -fx-border-width: 0 0 1 0;" +
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