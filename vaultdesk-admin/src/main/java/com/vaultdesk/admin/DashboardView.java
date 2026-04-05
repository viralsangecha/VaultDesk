package com.vaultdesk.admin;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
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
        Label co=new Label("select menu....");

        Button dashboard=new Button("Dashboard");
        Button tickets=new Button("Tickets");
        Button assets=new Button("Assets");
        Button employees=new Button("Employees");
        Button departments=new Button("Departments");
        Button licenses=new Button("Licenses");
        HBox mainlayout = new HBox();

        VBox sidebar = new VBox();
        sidebar.setPrefWidth(200);
        VBox contentArea=new VBox();




        sidebar.getChildren().addAll(dashboard,tickets,assets,employees,departments,licenses);

        HBox.setHgrow(contentArea, Priority.ALWAYS);
        contentArea.getChildren().addAll(welcomeLabel, roleLabel,co);

        mainlayout.getChildren().addAll(sidebar,contentArea);
        dashboard.setOnAction(e -> {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(new Label("Dashboard Content"));
        });

        tickets.setOnAction(e -> {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(new TicketView().getView());
        });

        assets.setOnAction(e -> {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(new AssetView().getView());
        });

        employees.setOnAction(e -> {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(new EmployeeView().getView());
        });

        departments.setOnAction(e -> {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(new Label("departments Content"));
        });

        licenses.setOnAction(e -> {
            contentArea.getChildren().clear();
            contentArea.getChildren().add(new Label("licenses Content"));
        });


        return new Scene(mainlayout, 1200, 800);
    }
}