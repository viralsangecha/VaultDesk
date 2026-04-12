package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class TicketView {

    public VBox getView() {
        Label title = new Label("Tickets");
        title.getStyleClass().add("section-title");
        TableView<Ticket> table = new TableView<>();

        TableColumn<Ticket, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Ticket, String> ticketNoCol = new TableColumn<>("Ticket No");
        ticketNoCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTicketNo()));

        TableColumn<Ticket, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTitle()));

        TableColumn<Ticket, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPriority()));

        TableColumn<Ticket, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));

        TableColumn<Ticket, Integer> assignedToCol = new TableColumn<>("Assigned To (ID)");
        assignedToCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getAssignedTo()).asObject());

        TableColumn<Ticket, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCreatedAt()));

        TableColumn<Ticket, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button updateBtn = new Button("Update Status");
            private final Button assignBtn = new Button("Assign");

            {
                updateBtn.getStyleClass().add("btn-warning");
                updateBtn.setOnAction(e -> {
                    Ticket ticket = getTableView().getItems().get(getIndex());
                    showUpdateStatusDialog(ticket, getTableView());
                });
                assignBtn.getStyleClass().add("btn-warning");
                assignBtn.setOnAction(e -> {
                    Ticket ticket = getTableView().getItems().get(getIndex());
                    showAssignDialog(ticket, getTableView());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(5, updateBtn, assignBtn));
                }
            }
        });

        table.getColumns().addAll(idCol, ticketNoCol, titleCol, priorityCol,
                statusCol, assignedToCol, createdCol, actionCol);

        loadTickets(table);

        VBox root = new VBox(10);
        root.getChildren().addAll(title, table);
        return root;
    }

    private void loadTickets(TableView<Ticket> table) {
        table.getItems().clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/tickets"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);

            if (!body.isEmpty()) {
                String[] objects = body.split("\\},\\{");
                for (String obj : objects) {
                    obj = obj.replace("{", "").replace("}", "");
                    int id         = extractInt(obj, "id");
                    String ticketNo    = extractValue(obj, "ticketNo");
                    String ticketTitle = extractValue(obj, "title");
                    String priority    = extractValue(obj, "priority");
                    String status      = extractValue(obj, "status");
                    int assignedTo     = extractInt(obj, "assignedTo");
                    String createdAt   = extractValue(obj, "createdAt");
                    table.getItems().add(new Ticket(id, ticketNo, ticketTitle, priority, status, assignedTo, createdAt));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading tickets: " + ex.getMessage());
        }
    }

    private void showUpdateStatusDialog(Ticket ticket, TableView<Ticket> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Ticket Status");
        dialog.setHeaderText("Ticket: " + ticket.getTicketNo());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll("Open", "In Progress", "Resolved", "Closed");
        statusBox.setValue(ticket.getStatus());

        TextField resolutionField = new TextField();
        resolutionField.setPromptText("Resolution notes...");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Status:"), 0, 0);
        grid.add(statusBox, 1, 0);
        grid.add(new Label("Resolution:"), 0, 1);
        grid.add(resolutionField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String newStatus = statusBox.getValue();
            String resolution = resolutionField.getText();
            boolean success = updateTicketStatus(ticket.getId(), newStatus, resolution);
            if (success) loadTickets(table);  // refresh table after update
        }
    }

    private void showAssignDialog(Ticket ticket, TableView<Ticket> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Assign Ticket");
        dialog.setHeaderText("Ticket: " + ticket.getTicketNo());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField userIdField = new TextField();
        userIdField.setPromptText("Enter User ID to assign...");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Assign to User ID:"), 0, 0);
        grid.add(userIdField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String userIdText = userIdField.getText().trim();
            if (!userIdText.isEmpty()) {
                boolean success = assignTicket(ticket.getId(), Integer.parseInt(userIdText));
                if (success) loadTickets(table);  // refresh table after assign
            }
        }
    }

    private boolean updateTicketStatus(int id, String status, String resolution) {
        try {
            String encodedStatus = URLEncoder.encode(status, StandardCharsets.UTF_8);
            String encodedResolution = URLEncoder.encode(resolution, StandardCharsets.UTF_8);
            String url = "http://localhost:8080/api/tickets/" + id
                    + "/status?status=" + encodedStatus
                    + "&resolution=" + encodedResolution;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                showAlert("Success", "Status updated to: " + status);
                return true;
            } else {
                showAlert("Error", "Server returned: " + response.statusCode());
                return false;
            }
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private boolean assignTicket(int ticketId, int userId) {
        try {
            String url = "http://localhost:8080/api/tickets/" + ticketId + "/assign?userId=" + userId;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                showAlert("Success", "Ticket assigned to user ID: " + userId);
                return true;
            } else {
                showAlert("Error", "Server returned: " + response.statusCode());
                return false;
            }
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
        String value = json.substring(start, end).trim().replace("}", "");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}