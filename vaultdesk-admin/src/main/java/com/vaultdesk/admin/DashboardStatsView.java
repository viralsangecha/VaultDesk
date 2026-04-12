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
        VBox cardAssets    = statCard("0", "Total Assets",         "stat-card-blue");
        VBox cardTickets   = statCard("0", "Open Tickets",         "stat-card-red");
        VBox cardLicenses  = statCard("0", "Expiring Licenses",    "stat-card-orange");
        VBox cardEmployees = statCard("0", "Active Employees",     "stat-card-green");

        HBox statsRow = new HBox(16,
                cardAssets, cardTickets, cardLicenses, cardEmployees);
        statsRow.setPadding(new Insets(8, 0, 8, 0));

        // ── Recent tickets ───────────────────────────────
        Label recentLabel = new Label("Recent Support Tickets");
        recentLabel.getStyleClass().add("section-title");

        TableView<Ticket> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        javafx.scene.control.TableColumn<Ticket, String> ticketNoCol =
                new javafx.scene.control.TableColumn<>("Ticket No");
        ticketNoCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTicketNo()));

        javafx.scene.control.TableColumn<Ticket, String> titleCol =
                new javafx.scene.control.TableColumn<>("Title");
        titleCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTitle()));

        javafx.scene.control.TableColumn<Ticket, String> priorityCol =
                new javafx.scene.control.TableColumn<>("Priority");
        priorityCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getPriority()));

        javafx.scene.control.TableColumn<Ticket, String> statusCol =
                new javafx.scene.control.TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStatus()));

        javafx.scene.control.TableColumn<Ticket, String> createdCol =
                new javafx.scene.control.TableColumn<>("Created");
        createdCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getCreatedAt()));

        table.getColumns().addAll(ticketNoCol, titleCol,
                priorityCol, statusCol, createdCol);

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

        VBox root = new VBox(12,
                pageTitle, pageSub, statsRow, recentLabel, table);
        return root;
    }

    private VBox statCard(String number, String label, String colorClass) {
        Label numLabel = new Label(number);
        numLabel.getStyleClass().add("stat-number");

        Label txtLabel = new Label(label);
        txtLabel.getStyleClass().add("stat-label");

        VBox card = new VBox(6, numLabel, txtLabel);
        card.getStyleClass().addAll("stat-card", colorClass);
        return card;
    }

    private void setStatNumber(VBox card, int value) {
        Label numLabel = (Label) card.getChildren().get(0);
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