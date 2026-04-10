package com.vaultdesk.admin;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MaintenanceView {
    public VBox getView()
    {
        // 1. Title label
        Label title = new Label("Maintenance");

        // 2. Create the TableView
        TableView<Maintenance> table = new TableView<>();

        // 3. Define columns
        TableColumn<Maintenance, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Maintenance, Integer> assetIdCol = new TableColumn<>("assetId");
        assetIdCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getAssetId()).asObject());

        TableColumn<Maintenance, String> maintenanceTypeCol = new TableColumn<>("MaintenanceType");
        maintenanceTypeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getMaintenanceType()));

        TableColumn<Maintenance, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDescription()));

        TableColumn<Maintenance, Double> costCol = new TableColumn<>("Cost");
        costCol.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getCost()).asObject());

        TableColumn<Maintenance, String> maintenanceDateCol = new TableColumn<>("MaintenanceDate");
        maintenanceDateCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getMaintenanceDate()));

        TableColumn<Maintenance, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));

        // 4. Add columns to table
        table.getColumns().addAll(idCol,assetIdCol,maintenanceTypeCol,descriptionCol,costCol,maintenanceDateCol,statusCol);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/maintenance"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


            String body = response.body().trim();
            // strip outer [ and ]
            body = body.substring(1, body.length() - 1);

            if (body.isEmpty()) {
                System.out.println("No Maintenance found.");
                // just leave the table empty and return
            } else {
                String[] objects = body.split("\\},\\{");
                // ... rest of parsing loop

                for (String obj : objects) {
                    // clean up leftover { or }
                    obj = obj.replace("{", "").replace("}", "");

                    int id = extractInt(obj, "id");
                    int assetId = extractInt(obj, "assetId");
                    String maintenanceType = extractValue(obj, "maintenanceType");
                    String description = extractValue(obj, "description");
                    double cost = extractDouble(obj, "cost");
                    String maintenanceDate = extractValue(obj, "maintenanceDate");
                    String status = extractValue(obj, "status");

                    table.getItems().add(new Maintenance(id,assetId,maintenanceType,description,cost,maintenanceDate,  status));

                }            }
        } catch (Exception ex) {
            System.out.println("Error loading Maintenance: " + ex.getMessage());
        }

        // 6. Build and return VBox
        VBox root = new VBox(10);
        root.getChildren().addAll(title, table);
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
    private double extractDouble(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.length();
        return Double.parseDouble(json.substring(start, end).trim());
    }
}

