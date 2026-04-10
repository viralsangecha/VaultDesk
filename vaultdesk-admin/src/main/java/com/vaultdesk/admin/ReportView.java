package com.vaultdesk.admin;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ReportView {
    public VBox getView()
    {
        Label title=new Label("Reports");

        Label totalassets=new Label("Total Assets");
        Label totaltickets=new Label("Total Tickets");
        Label totallicenses=new Label("Total Licenses");

        //Assets
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/reports/assets"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String statsBody = response.body().trim();

            totalassets.setText(" Total Assets: " +countItems(statsBody));
        }
        catch (Exception ex) {
            System.out.println("Error loading Assets: " + ex.getMessage());
        }

        //Ticket
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/reports/tickets"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String statsBody = response.body().trim();

            totaltickets.setText(" Total Tickets: " +countItems(statsBody));
        }
        catch (Exception ex) {
            System.out.println("Error loading Tickets: " + ex.getMessage());
        }

        //Licenses
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/reports/licenses"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String statsBody = response.body().trim();

            totallicenses.setText(" Total Licenses: " +countItems(statsBody));
        }
        catch (Exception ex) {
            System.out.println("Error loading Licenses: " + ex.getMessage());
        }




        VBox root = new VBox(10);
        root.getChildren().addAll(title,totalassets,totaltickets,totallicenses);
        return root;


    }
    private int countItems(String body) {
        body = body.substring(1, body.length() - 1).trim();
        if (body.isEmpty()) return 0;
        return body.split("\\},\\{").length;
    }
}
