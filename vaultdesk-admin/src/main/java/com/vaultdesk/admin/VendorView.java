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

public class VendorView {
    public VBox getView()
    {
        // 1. Title label
        Label title = new Label("Vendors");

        // 2. Create the TableView
        TableView<Vendor> table = new TableView<>();

        // 3. Define columns
        TableColumn<Vendor, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Vendor, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        TableColumn<Vendor, String> contactPersonCol = new TableColumn<>("ContactPerson");
        contactPersonCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getContactPerson()));

        TableColumn<Vendor, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPhone()));

        TableColumn<Vendor, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEmail()));

        TableColumn<Vendor, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory()));

        // 4. Add columns to table
        table.getColumns().addAll(idCol,nameCol,contactPersonCol,phoneCol,emailCol,categoryCol);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/vendors"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());


            String body = response.body().trim();
            // strip outer [ and ]
            body = body.substring(1, body.length() - 1);

            if (body.isEmpty()) {
                System.out.println("No Vendors found.");
                // just leave the table empty and return
            } else {
                String[] objects = body.split("\\},\\{");
                // ... rest of parsing loop

                for (String obj : objects) {
                    // clean up leftover { or }
                    obj = obj.replace("{", "").replace("}", "");

                    int id = extractInt(obj, "id");
                    String name = extractValue(obj, "name");
                    String contactPerson = extractValue(obj, "contactPerson");
                    String phone = extractValue(obj, "phone");
                    String email = extractValue(obj, "email");
                    String category = extractValue(obj, "category");

                    table.getItems().add(new Vendor(id, name, contactPerson,phone,email, category));

                }            }
        } catch (Exception ex) {
            System.out.println("Error loading Vendor: " + ex.getMessage());
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
