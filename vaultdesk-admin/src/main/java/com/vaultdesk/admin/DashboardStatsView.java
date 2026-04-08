package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DashboardStatsView {
    public VBox getView() {
        // 1. Title label
        Label title = new Label("Dashboard");

        Label totalAssets = new Label("Total Assets");
        Label openTickets = new Label("Open Tickets");
        Label expiringLicenses = new Label("Expiring Licenses");
        Label activeEmployees = new Label("Active Employees");


        Label recentTickets = new Label("Recent Tickets");

        HBox hBox=new HBox();
        hBox.getChildren().addAll(totalAssets,openTickets,expiringLicenses,activeEmployees);
        // 2. Create the TableView
        TableView<Ticket> table = new TableView<>();

        // 3. Define columns
        TableColumn<Ticket, String> ticketNoCol = new TableColumn<>("Ticket No");
        ticketNoCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTicketNo()));

        TableColumn<Ticket, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTitle()));

        TableColumn<Ticket, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPriority()));

        TableColumn<Ticket, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));

        TableColumn<Ticket, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCreatedAt()));

        // 4. Add columns to table
        table.getColumns().addAll(ticketNoCol, titleCol, priorityCol, statusCol, createdCol);

        try {
            HttpClient client2 = HttpClient.newHttpClient();
            HttpRequest statsRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/dashboard/stats"))
                    .GET()
                    .build();
            HttpResponse<String> statsResponse = client2.send(statsRequest, HttpResponse.BodyHandlers.ofString());

            String statsBody = statsResponse.body().trim();

            totalAssets.setText(" Total Assets: " + extractInt(statsBody, "totalAssets"));
            openTickets.setText(" Open Tickets: " + extractInt(statsBody, "openTickets"));
            expiringLicenses.setText(" Expiring Licenses: " + extractInt(statsBody, "expiringLicenses"));
            activeEmployees.setText(" Active Employees: " + extractInt(statsBody, "totalEmployees"));

        } catch (Exception ex) {
            System.out.println("Error loading stats: " + ex.getMessage());
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/dashboard/recent-activity"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


            String body = response.body().trim();
            // strip outer [ and ]
            body = body.substring(1, body.length() - 1);

            if (body.isEmpty()) {
                System.out.println("No tickets found.");
                // just leave the table empty and return
            }
            else
            {
            // split into individual objects
            String[] objects = body.split("\\},\\{");

            for (String obj : objects) {
                // clean up leftover { or }
                obj = obj.replace("{", "").replace("}", "");

                int id = extractInt(obj, "id");
                String ticketNo = extractValue(obj, "ticketNo");
                String ticketTitle = extractValue(obj, "title");
                String priority = extractValue(obj, "priority");
                String status = extractValue(obj, "status");
                String createdAt = extractValue(obj, "createdAt");

                table.getItems().add(new Ticket(id, ticketNo, ticketTitle, priority, status, createdAt));
            }
            }
        } catch (Exception ex) {
            System.out.println("Error loading tickets: " + ex.getMessage());
        }

        // 6. Build and return VBox
        VBox root = new VBox(10);
        root.getChildren().addAll(title,hBox,recentTickets, table);
        return root;


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
        return Integer.parseInt(json.substring(start, end).trim());
    }
}
