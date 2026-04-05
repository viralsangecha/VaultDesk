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

public class AssetView {
    public VBox getView()
    {
        // 1. Title label
        Label title = new Label("Assets");

        // 2. Create the TableView
        TableView<Asset> table = new TableView<>();

        // 3. Define columns
        TableColumn<Asset, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Asset, String> assetTagCol = new TableColumn<>("AssetTag");
        assetTagCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getAssetTag()));

        TableColumn<Asset, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Asset, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory()));

        TableColumn<Asset, String> brandCol = new TableColumn<>("Brand");
        brandCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getBrand()));

        TableColumn<Asset, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));

        TableColumn<Asset, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLocation()));

        // 4. Add columns to table
        table.getColumns().addAll(idCol,assetTagCol,nameCol,categoryCol,brandCol,statusCol,locationCol);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/assets"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


            String body = response.body().trim();
            // strip outer [ and ]
            body = body.substring(1, body.length() - 1);

            if (body.isEmpty()) {
                System.out.println("No Assets found.");
                // just leave the table empty and return
            } else {
                String[] objects = body.split("\\},\\{");
                // ... rest of parsing loop

            for (String obj : objects) {
                // clean up leftover { or }
                obj = obj.replace("{", "").replace("}", "");

                int id = extractInt(obj, "id");
                String assetTag = extractValue(obj, "assetTag");
                String name = extractValue(obj, "name");
                String category = extractValue(obj, "category");
                String brand = extractValue(obj, "brand");
                String status = extractValue(obj, "status");
                String location = extractValue(obj, "location");

                table.getItems().add(new Asset(id, assetTag, name, category, brand, status, location));

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
