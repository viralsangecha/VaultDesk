package com.vaultdesk.admin;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginView {

    public Scene getScene(Stage stage)
    {
        Label userl = new Label("Username:");
        TextField usernameField = new TextField();

        Label passl = new Label("Password:");
        PasswordField passwordField = new PasswordField();

        Button loginButton = new Button("Login");

        VBox layout = new VBox(10);
        layout.getChildren().addAll(userl,usernameField,passl,passwordField, loginButton);

        Scene scene = new Scene(layout, 400, 300);


        loginButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();

            System.out.println("User = " + username);
            System.out.println("Password = " + password);
        });
        return scene;
    }



}
