package com.vaultdesk.admin;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.*;

public class LoginView {

    public Scene getScene(Stage stage)
    {
        Label userl = new Label("Username:");
        TextField usernameField = new TextField();

        Label passl = new Label("Password:");
        PasswordField passwordField = new PasswordField();

        Button loginButton = new Button("Login");

        Label statusLabel = new Label("");

        VBox layout = new VBox(10);
        layout.getChildren().addAll(userl,usernameField,passl,passwordField, loginButton,statusLabel);

        Scene scene = new Scene(layout, 400, 300);


        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();

            String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";

            // 1. Create the client
            HttpClient client = HttpClient.newHttpClient();

            // 2. Build the request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            // 3. Send and get response
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    statusLabel.setText("Login successful!");
                    String responseBody = response.body();
                    String fullName = extractValue(responseBody, "fullName");
                    String role = extractValue(responseBody, "role");
                    DashboardView dashboard = new DashboardView(fullName, role);
                    stage.setScene(dashboard.getScene(stage));
                } else {
                    statusLabel.setText("Invalid credentials");
                }
            } catch (Exception ex) {
                statusLabel.setText("Cannot connect to server"+ex.getMessage());
            }
            // 4. Read the body
            //String responseBody = response.body();
        });
        return scene;
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }


}
