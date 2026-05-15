package com.vaultdesk.employee;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;
import java.util.function.Consumer;

public class RaiseTicketView {

    private final Consumer<Boolean> onSuccess;

    public RaiseTicketView(Consumer<Boolean> onSuccess) {
        this.onSuccess = onSuccess;
    }

    public VBox getView() {
        Label title = new Label("Raise a Ticket");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Submit a new IT support request.");
        sub.getStyleClass().add("page-subtitle");

        Label titleLbl = new Label("Title *");
        titleLbl.getStyleClass().add("login-label");
        TextField titleField = new TextField();
        titleField.setPromptText("Brief description of the issue");
        titleField.setMaxWidth(500);

        Label descLbl = new Label("Description *");
        descLbl.getStyleClass().add("login-label");
        TextArea descField = new TextArea();
        descField.setPromptText("Describe the issue in detail...");
        descField.setPrefRowCount(5);
        descField.setMaxWidth(500);
        descField.setWrapText(true);

        Label catLbl = new Label("Category *");
        catLbl.getStyleClass().add("login-label");
        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(
                "Hardware", "Software", "SAP",
                "Network", "General");
        categoryBox.setValue("Hardware");
        categoryBox.setMaxWidth(500);

        Label priLbl = new Label("Priority *");
        priLbl.getStyleClass().add("login-label");
        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("Low", "Medium", "High", "Critical");
        priorityBox.setValue("Medium");
        priorityBox.setMaxWidth(500);

        Label errorLabel = new Label("");
        errorLabel.setStyle(
                "-fx-text-fill: #f85149; -fx-font-size: 12px;");

        Button submitBtn = new Button("Submit Ticket");
        submitBtn.getStyleClass().setAll("btn-primary");
        submitBtn.setStyle(
                "-fx-background-color: #238636; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 10 24 10 24;" +
                        "-fx-font-size: 13px; -fx-font-weight: bold;" +
                        "-fx-cursor: hand;");
        submitBtn.setDisable(true);

        // ── Enable submit when title + desc filled ────────
        Runnable check = () -> submitBtn.setDisable(
                titleField.getText().trim().isEmpty()
                        || descField.getText().trim().isEmpty());
        titleField.textProperty().addListener((o, ov, nv) -> check.run());
        descField.textProperty().addListener((o, ov, nv) -> check.run());

        submitBtn.setOnAction(e -> {
            String t = titleField.getText().trim()
                    .replace("\\", "\\\\")
                    .replace("\"", "'")
                    .replace("\n", " ");
            String d = descField.getText().trim()
                    .replace("\\", "\\\\")
                    .replace("\"", "'")
                    .replace("\n", " ");

            String body = "{" +
                    "\"title\":\"" + t + "\"," +
                    "\"description\":\"" + d + "\"," +
                    "\"category\":\"" + categoryBox.getValue() + "\"," +
                    "\"priority\":\"" + priorityBox.getValue() + "\"," +
                    "\"reportedBy\":" + SessionManager.get().getEmployeeId()
                    + "}";
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ConfigManager.getBaseUrl()
                                + "/api/employee/tickets"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 201) {
                    showAlert("Success",
                            "Your ticket has been submitted successfully.\n" +
                                    "Our IT team will review it shortly.");
                    if (onSuccess != null) onSuccess.accept(true);
                } else {
                    errorLabel.setText(
                            "Server error: " + resp.statusCode());
                }
            } catch (Exception ex) {
                errorLabel.setText("Cannot connect to server.");
            }
        });

        VBox form = new VBox(12,
                titleLbl, titleField,
                descLbl, descField,
                catLbl, categoryBox,
                priLbl, priorityBox,
                errorLabel, submitBtn);
        form.setPadding(new Insets(16));
        form.setMaxWidth(540);
        form.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;");

        VBox root = new VBox(12, title, sub, form);
        return root;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}