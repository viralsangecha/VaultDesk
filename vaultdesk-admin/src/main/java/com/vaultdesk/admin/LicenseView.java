package com.vaultdesk.admin;

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

public class LicenseView {

    public VBox getView()
    {
        // 1. Title label
        Label title = new Label("License");

        // 2. Create the TableView
        TableView<License> table = new TableView<>();

        // 3. Define columns
        TableColumn<License, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<License, String> softwareNameCol = new TableColumn<>("softwareName");
        softwareNameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSoftwareName()));

        TableColumn<License, String> licenseTypeCol = new TableColumn<>("licenseType");
        licenseTypeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLicenseType()));

        TableColumn<License, String> vendorCol = new TableColumn<>("vendor");
        vendorCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getVendor()));

        TableColumn<License, String> expiryDateCol = new TableColumn<>("expiryDate");
        expiryDateCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getExpiryDate()));

        TableColumn<License, Integer> seatsTotalCol = new TableColumn<>("seatsTotal");
        seatsTotalCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getSeatsTotal()).asObject());  //

        TableColumn<License, Integer> seatsUsedCol = new TableColumn<>("seatsUsed");
        seatsUsedCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getSeatsUsed()).asObject());   //
        // 4. Add columns to table
        table.getColumns().addAll(idCol,softwareNameCol,licenseTypeCol,vendorCol,expiryDateCol,seatsTotalCol,seatsUsedCol);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/licenses"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


            String body = response.body().trim();
            // strip outer [ and ]
            body = body.substring(1, body.length() - 1);

            if (body.isEmpty()) {
                System.out.println("No Licenses found.");
                // just leave the table empty and return
            } else {
                String[] objects = body.split("\\},\\{");
                // ... rest of parsing loop

                for (String obj : objects) {
                    // clean up leftover { or }
                    obj = obj.replace("{", "").replace("}", "");

                    int id = extractInt(obj, "id");
                    String softwareName = extractValue(obj, "softwareName");
                    String licenseType = extractValue(obj, "licenseType");
                    String vendor = extractValue(obj, "vendor");
                    String expiryDate = extractValue(obj, "expiryDate");
                    int seatsTotal = extractInt(obj, "seatsTotal");
                    int seatsUsed = extractInt(obj, "seatsUsed");

                    table.getItems().add(new License(id,softwareName,licenseType,vendor,expiryDate,seatsTotal,seatsUsed));

                }            }
        } catch (Exception ex) {
            System.out.println("Error loading Assets: " + ex.getMessage());
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
}
