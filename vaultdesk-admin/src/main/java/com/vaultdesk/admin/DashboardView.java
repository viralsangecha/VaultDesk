package com.vaultdesk.admin;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DashboardView {
    private String fullName;
    private String role;

    public DashboardView(String fullName, String role) {
        this.fullName = fullName;
        this.role = role;
    }

    public Scene getScene(Stage stage) {
        Label welcomeLabel = new Label("Welcome, " + fullName);
        Label roleLabel = new Label("Role: " + role);

        VBox layout = new VBox(10);
        layout.getChildren().addAll(welcomeLabel, roleLabel);

        return new Scene(layout, 400, 300);
    }
}