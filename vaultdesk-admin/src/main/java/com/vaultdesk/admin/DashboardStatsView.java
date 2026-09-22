package com.vaultdesk.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DashboardStatsView {

    // ── Navigation callback ───────────────────────────────
    private Consumer<String> onNavigate;

    public void setOnNavigate(Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;
    }

    private void navigateTo(String view) {
        if (onNavigate != null) onNavigate.accept(view);
    }

    public VBox getView() {

        // ── Page header ───────────────────────────────────
        Label pageTitle = new Label("Dashboard Overview");
        pageTitle.getStyleClass().add("page-title");
        Label pageSub = new Label("Here's what's happening today.");
        pageSub.getStyleClass().add("page-subtitle");

        // ── Stat cards ────────────────────────────────────
        VBox cardAssets = statCard("⊞", "0", "TOTAL ASSETS", "Loading...",
                "REAL-TIME", "stat-badge-blue", "stat-card-blue", "stat-icon-box-blue", "#58a6ff");

        VBox cardTickets = statCard("✉", "0", "OPEN TICKETS", "Loading...",
                "REAL-TIME", "stat-badge-red", "stat-card-red", "stat-icon-box-red", "#f85149");

        VBox cardLicenses = statCard("🔑", "0", "EXPIRING LICENSES", "Loading...",
                "REAL-TIME", "stat-badge-orange", "stat-card-orange", "stat-icon-box-orange", "#d29922");

        VBox cardEmployees = statCard("👤", "0", "ACTIVE EMPLOYEES", "Loading...",
                "REAL-TIME", "stat-badge-green", "stat-card-green", "stat-icon-box-green", "#3fb950");

        // After statsRow is built:
        AnimationUtil.staggerFadeIn(
                new VBox(cardAssets, cardTickets,
                        cardLicenses, cardEmployees));

        // Add hover to each card:
        AnimationUtil.addHoverScale(cardAssets);
        AnimationUtil.addHoverScale(cardTickets);
        AnimationUtil.addHoverScale(cardLicenses);
        AnimationUtil.addHoverScale(cardEmployees);
        // ── Card click navigation ─────────────────────────
        if (PermissionManager.canViewAllAssets())
        {
            cardAssets.setOnMouseClicked(e -> navigateTo("assets"));
        }
        if (PermissionManager.canViewAllTickets() || PermissionManager.canViewAssignedTickets())
        {
            cardTickets.setOnMouseClicked(e -> navigateTo("tickets"));
        }
        if (PermissionManager.canViewLicenses())
        {
            cardLicenses.setOnMouseClicked(e -> navigateTo("licenses"));
        }
        if (PermissionManager.canViewEmployees())
        {
            cardEmployees.setOnMouseClicked(e -> navigateTo("employees"));
        }

        // ── Cursor hand on hover ──────────────────────────
        cardAssets.setStyle(cardAssets.getStyle() + "-fx-cursor: hand;");
        cardTickets.setStyle(cardTickets.getStyle() + "-fx-cursor: hand;");
        cardLicenses.setStyle(cardLicenses.getStyle() + "-fx-cursor: hand;");
        cardEmployees.setStyle(cardEmployees.getStyle() + "-fx-cursor: hand;");

        HBox statsRow = new HBox(16,
                cardAssets, cardTickets, cardLicenses, cardEmployees);
        statsRow.setPadding(new Insets(8, 0, 8, 0));
        HBox statusPillRow = new HBox(8);
        statusPillRow.setPadding(new Insets(0, 0, 4, 0));

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
        table.setPlaceholder(UIComponents.skeleton(6));

        // ── Recent Activity panel ─────────────────────────
        Label activityTitle = new Label("Recent Activity");
        activityTitle.getStyleClass().add("activity-panel-title");
        javafx.scene.shape.Circle pulseDot = AnimationUtil.livePulseDot("#3fb950");
        Label realtimeLabel = new Label("REAL-TIME");
        realtimeLabel.getStyleClass().add("activity-realtime-label");
        HBox realtimeGroup = new HBox(5, pulseDot, realtimeLabel);
        realtimeGroup.setAlignment(Pos.CENTER_LEFT);
        Region actSpacer = new Region();
        HBox.setHgrow(actSpacer, Priority.ALWAYS);
        HBox activityHeader = new HBox(activityTitle, actSpacer, realtimeGroup);
        activityHeader.setAlignment(Pos.CENTER_LEFT);

        VBox activityFeed = new VBox(0);
        activityFeed.getChildren().add(UIComponents.skeleton(5));
        VBox activityPanel = new VBox(10,
                activityHeader, new Separator(), activityFeed);
        activityPanel.getStyleClass().add("activity-panel");

        // ── Main content row ──────────────────────────────
        VBox tableBox = new VBox(10, recentLabel, recentSub, statusPillRow, table);
        tableBox.setPadding(new Insets(16));
        tableBox.getStyleClass().add("activity-panel"); // reuse the same panel-card look as Recent Activity, for visual symmetry
        HBox.setHgrow(tableBox, Priority.ALWAYS);
        HBox mainRow = new HBox(16, tableBox, activityPanel);

        // ── Load stats ────────────────────────────────────
        // A small class to hold the stats temporarily while passing them to the UI thread
        class DashboardStats {
            int totalAssets, openTickets, generalTickets, sapTickets, expiringLic, totalEmp, totalDepts;
        }

        Task<DashboardStats> statsTask = new Task<>() {
            @Override
            protected DashboardStats call() throws Exception {
                String statsUrl = SessionManager.get().isDeptHod()
                        ? ConfigManager.getBaseUrl() + "/api/dashboard/stats/department/"
                        + SessionManager.get().getDeptId()
                        : ConfigManager.getBaseUrl() + "/api/dashboard/stats";

                HttpResponse<String> resp = ApiClient.get(statsUrl);
                String body = resp.body().trim();

                DashboardStats stats = new DashboardStats();
                stats.totalAssets    = extractInt(body, "totalAssets");
                stats.openTickets    = extractInt(body, "openTickets");
                stats.generalTickets = extractInt(body, "generalTickets");
                stats.sapTickets     = extractInt(body, "sapTickets");
                stats.expiringLic    = extractInt(body, "expiringLicenses");
                stats.totalEmp       = extractInt(body, "totalEmployees");
                stats.totalDepts     = extractInt(body, "totalDepartments");

                return stats;
            }
        };

        statsTask.setOnSucceeded(e -> {
            DashboardStats stats = statsTask.getValue();

            setStatNumber(cardAssets,    stats.totalAssets);
            setStatNumber(cardTickets,   stats.openTickets);
            setStatNumber(cardLicenses,  stats.expiringLic);
            setStatNumber(cardEmployees, stats.totalEmp);

            setStatSublabel(cardTickets,    stats.generalTickets + " General / " + stats.sapTickets + " SAP tickets");
            setStatSublabel(cardAssets,   "Assets");
            setStatSublabel(cardLicenses,  stats.expiringLic > 0 ? "Action required" : "All valid");
            setStatSublabel(cardEmployees, stats.totalDepts + " department(s)");
        });

        statsTask.setOnFailed(e -> {
            System.out.println("Error loading stats: " + statsTask.getException().getMessage());
        });

        Thread statsThread = new Thread(statsTask);
        statsThread.setDaemon(true);
        statsThread.start();


// ── Load recent tickets ───────────────────────────
        Task<List<Ticket>> activityTask = new Task<>() {
            @Override
            protected List<Ticket> call() throws Exception {
                List<Ticket> result = new ArrayList<>();

                String activityUrl = SessionManager.get().isDeptHod()
                        ? ConfigManager.getBaseUrl() + "/api/tickets/department/"
                        + SessionManager.get().getDeptId()
                        : ConfigManager.getBaseUrl() + "/api/dashboard/recent-activity";

                HttpResponse<String> resp = ApiClient.get(activityUrl);
                String body = resp.body().trim();

                // Safe array extraction
                if (body.equals("[]") || body.isEmpty()) return result;
                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    String[] jsonObjects = body.split("\\},\\s*\\{");
                    for (String obj : jsonObjects) {
                        String cleanedObj = obj.replace("{", "").replace("}", "");
                        String priority = extractValue(cleanedObj, "priority");
                        String title    = extractValue(cleanedObj, "title");
                        String status   = extractValue(cleanedObj, "status");
                        String time     = extractValue(cleanedObj, "createdAt");

                        Ticket t = new Ticket(
                                extractInt(cleanedObj, "id"),
                                extractValue(cleanedObj, "ticketNo"),
                                title,
                                extractValue(cleanedObj,"description"),
                                extractValue(cleanedObj, "category"),
                                priority,
                                status,
                                extractInt(cleanedObj, "assignedTo"),
                                extractInt(cleanedObj, "reportedBy"),
                                extractInt(cleanedObj, "assetId"),
                                time,
                                "",
                                extractValue(cleanedObj,"department"),
                                extractValue(cleanedObj,"reason"),
                                extractValue(cleanedObj,"requestedAt")
                        );
                        result.add(t);
                    }
                }
                return result;
            }
        };

        activityTask.setOnSucceeded(e -> {
            List<Ticket> tickets = activityTask.getValue();

            activityFeed.getChildren().clear(); // remove the skeleton now that real data is here

            long openCount = tickets.stream().filter(t -> "Open".equals(t.getStatus())).count();
            long progressCount = tickets.stream().filter(t -> "In Progress".equals(t.getStatus())).count();
            long resolvedCount = tickets.stream().filter(t -> "Resolved".equals(t.getStatus()) || "Closed".equals(t.getStatus())).count();
            Label openPill = new Label(openCount + " Open");
            openPill.getStyleClass().add("stat-badge-red");
            Label progressPill = new Label(progressCount + " In Progress");
            progressPill.getStyleClass().add("stat-badge-blue");
            Label resolvedPill = new Label(resolvedCount + " Resolved");
            resolvedPill.getStyleClass().add("stat-badge-green");
            statusPillRow.getChildren().setAll(openPill, progressPill, resolvedPill);

            for (Ticket t : tickets) {
                table.getItems().add(t);
                activityFeed.getChildren().add(
                        activityEntry(dotClass(t.getPriority()), t.getTitle(), t.getStatus(), t.getCreatedAt())
                );
            }
        });

        activityTask.setOnFailed(e -> {
            System.out.println("Error loading tickets: " + activityTask.getException().getMessage());
        });

        Thread activityThread = new Thread(activityTask);
        activityThread.setDaemon(true);
        activityThread.start();

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

        javafx.scene.shape.Circle pulseDot = AnimationUtil.livePulseDot(iconColor);
        HBox badgeGroup = new HBox(5, pulseDot, badgeLabel);
        badgeGroup.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(iconBox, spacer, badgeGroup);
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
                .filter(n -> n != null && "stat-num".equals(n.getId()))
                .findFirst()
                .ifPresent(n -> AnimationUtil.countUp((Label) n, 0, value));
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
        statusLabel.getStyleClass().add("activity-status-label");

        VBox textBox = new VBox(2, titleLabel, statusLabel);
        HBox row = new HBox(10, dot, textBox);
        row.setAlignment(Pos.TOP_LEFT);

        Label timeLabel = new Label(DateTimeFormatUtil.toIndianDateTime(time));
        timeLabel.getStyleClass().add("activity-time");

        VBox entry = new VBox(4, row, timeLabel);
        entry.getStyleClass().add("activity-entry-card");
        VBox.setMargin(entry, new Insets(0, 0, 6, 0));
        AnimationUtil.addHoverScale(entry);
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