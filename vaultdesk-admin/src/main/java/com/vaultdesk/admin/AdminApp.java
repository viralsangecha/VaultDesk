package com.vaultdesk.admin;

import javafx.application.Application;
import javafx.stage.Stage;

public class AdminApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        LoginView loginView=new LoginView();

        stage.setScene(loginView.getScene(stage));
        stage.setTitle("VaultDesk Admin");
        stage.setWidth(1200);
        stage.setHeight(800);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
