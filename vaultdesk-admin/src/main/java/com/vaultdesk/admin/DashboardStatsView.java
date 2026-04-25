package com.vaultdesk.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;

public class DashboardStatsView {

    public VBox getView() {
        Label pageTitle = new Label("Dashboard Overview");
        pageTitle.getStyleClass().add("page-title");

        Label pageSub = new Label("Here's what's happening today.");
        pageSub.getStyleClass().add("page-subtitle");

        // ── Stat cards ───────────────────────────────────
        VBox cardAssets    = statCard("0", "Total Assets",      "22 General / 12 SAP", "stat-card-blue",   "stat-badge-blue",   "Global");
        VBox cardTickets   = statCard("0", "Open Tickets",      "Needs attention",      "stat-card-red",    "stat-badge-red",    "Urgent");
        VBox cardLicenses  = statCard("0", "Expiring Licenses", "Within 30 days",       "stat-card-orange", "stat-badge-orange", "30 Days");
        VBox cardEmployees = statCard("0", "Active Employees",  "All departments",      "stat-card-green",  "stat-badge-green",  "Active");

        HBox statsRow = new HBox(16,
                cardAssets, cardTickets, cardLicenses, cardEmployees);
        statsRow.setPadding(new Insets(8, 0, 8, 0));

        // ── Recent tickets table ─────────────────────────
        Label recentLabel = new Label("Recent Support Tickets");
        recentLabel.getStyleClass().add("section-title");

        TableView<Ticket> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Ticket, String> ticketNoCol = new TableColumn<>("Ticket No");
        ticketNoCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTicketNo()));

        TableColumn<Ticket, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTitle()));

        // Priority column — colored text
        TableColumn<Ticket, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getPriority()));
        priorityCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null); setText(null);
                } else {
                    Label lbl = new Label(item);
                    switch (item) {
                        case "Critical" -> lbl.setStyle(
                                "-fx-text-fill: #f85149; -fx-font-weight: bold;");
                        case "High" -> lbl.setStyle(
                                "-fx-text-fill: #d29922; -fx-font-weight: bold;");
                        case "Medium" -> lbl.setStyle(
                                "-fx-text-fill: #8b949e;");
                        default -> lbl.setStyle(
                                "-fx-text-fill: #6e7681;");
                    }
                    setGraphic(lbl); setText(null);
                }
            }
        });

        // Status column — pill labels
        TableColumn<Ticket, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Open"        -> setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
                        case "In Progress" -> setStyle("-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
                        case "Resolved"    -> setStyle("-fx-text-fill: #3fb950; -fx-font-weight: bold;");
                        default            -> setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold;");
                    }
                    setGraphic(null);
                }
            }
        });

        TableColumn<Ticket, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getCreatedAt()));

        table.getColumns().addAll(ticketNoCol, titleCol,
                priorityCol, statusCol, createdCol);

        // ── Left column ──────────────────────────────────
        VBox leftCol = new VBox(12,
                pageTitle, pageSub, statsRow, recentLabel, table);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        // ── Right column — Activity feed ─────────────────
        VBox activityPanel = buildActivityPanel();

        // ── Two-column layout ────────────────────────────
        HBox mainRow = new HBox(16, leftCol, activityPanel);
        VBox.setVgrow(mainRow, Priority.ALWAYS);

        // ── Load stats ───────────────────────────────────
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/dashboard/stats"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();

            setStatNumber(cardAssets,    extractInt(body, "totalAssets"));
            setStatNumber(cardTickets,   extractInt(body, "openTickets"));
            setStatNumber(cardLicenses,  extractInt(body, "expiringLicenses"));
            setStatNumber(cardEmployees, extractInt(body, "totalEmployees"));

        } catch (Exception ex) {
            System.out.println("Error loading stats: " + ex.getMessage());
        }

        // ── Load recent tickets ──────────────────────────
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/dashboard/recent-activity"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);

            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    table.getItems().add(new Ticket(
                            extractInt(obj, "id"),
                            extractValue(obj, "ticketNo"),
                            extractValue(obj, "title"),
                            extractValue(obj, "category"),   // ← added
                            extractValue(obj, "priority"),
                            extractValue(obj, "status"),
                            extractInt(obj, "assignedTo"),
                            extractValue(obj, "createdAt")
                    ));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading tickets: " + ex.getMessage());
        }

        VBox root = new VBox(12, mainRow);
        return root;
    }

    // ── Activity feed panel ──────────────────────────────
    private VBox buildActivityPanel() {
        VBox panel = new VBox(0);
        panel.getStyleClass().add("activity-panel");

        Label panelTitle = new Label("Recent Activity");
        panelTitle.getStyleClass().add("activity-panel-title");
        panel.getChildren().add(panelTitle);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/dashboard/recent-activity"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);

            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String title    = extractValue(obj, "title");
                    String status   = extractValue(obj, "status");
                    String priority = extractValue(obj, "priority");
                    String created  = extractValue(obj, "createdAt");

                    // shorten timestamp display
                    String timeShort = created.length() >= 16
                            ? created.substring(0, 16) : created;

                    // dot color based on priority
                    String dotClass = switch (priority) {
                        case "Critical" -> "activity-dot-red";
                        case "High"     -> "activity-dot-orange";
                        case "Medium"   -> "activity-dot-blue";
                        default         -> "activity-dot-green";
                    };

                    Region dot = new Region();
                    dot.getStyleClass().add(dotClass);

                    Label titleLbl = new Label(title);
                    titleLbl.getStyleClass().add("activity-text");
                    titleLbl.setWrapText(true);
                    titleLbl.setMaxWidth(220);

                    Label timeLbl = new Label(timeShort);
                    timeLbl.getStyleClass().add("activity-time");

                    VBox textBox = new VBox(2, titleLbl, timeLbl);

                    HBox entry = new HBox(8, dot, textBox);
                    entry.getStyleClass().add("activity-entry");
                    entry.setAlignment(Pos.TOP_LEFT);
                    entry.setPadding(new Insets(8, 0, 8, 0));

                    panel.getChildren().add(entry);
                }
            } else {
                Label empty = new Label("No recent activity.");
                empty.getStyleClass().add("activity-time");
                panel.getChildren().add(empty);
            }

        } catch (Exception ex) {
            Label err = new Label("Cannot load activity.");
            err.getStyleClass().add("activity-time");
            panel.getChildren().add(err);
        }

        return panel;
    }

    // ── Stat card builder ────────────────────────────────
    private VBox statCard(String number, String label,
                          String subLabel, String colorClass,
                          String badgeClass, String badgeText) {

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label numLabel = new Label(number);
        numLabel.getStyleClass().add("stat-number");

        Label badge = new Label(badgeText);
        badge.getStyleClass().add(badgeClass);

        topRow.getChildren().addAll(numLabel, badge);

        Label txtLabel = new Label(label);
        txtLabel.getStyleClass().add("stat-label");

        Label subLbl = new Label(subLabel);
        subLbl.getStyleClass().add("stat-sublabel");

        VBox card = new VBox(4, topRow, txtLabel, subLbl);
        card.getStyleClass().addAll("stat-card", colorClass);
        card.setPrefWidth(210);
        card.setPrefHeight(110);
        return card;
    }

    private void setStatNumber(VBox card, int value) {
        // topRow is child 0, numLabel is topRow child 0
        HBox topRow = (HBox) card.getChildren().get(0);
        Label numLabel = (Label) topRow.getChildren().get(0);
        numLabel.setText(String.valueOf(value));
    }

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