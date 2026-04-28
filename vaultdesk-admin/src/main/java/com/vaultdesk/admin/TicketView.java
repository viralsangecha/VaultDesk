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
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Ticket, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getId()).asObject());

        TableColumn<Ticket, String> ticketNoCol = new TableColumn<>("Ticket No");
        ticketNoCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTicketNo()));

        TableColumn<Ticket, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTitle()));

        // ── Category column with colored text ─────────────────
        TableColumn<Ticket, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategory()));  // need category in Ticket
        categoryCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item.toUpperCase()) {
                        case "SAP"      -> setStyle("-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
                        case "HARDWARE" -> setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold;");
                        case "NETWORK"  -> setStyle("-fx-text-fill: #39d353; -fx-font-weight: bold;");
                        case "SOFTWARE" -> setStyle("-fx-text-fill: #a371f7; -fx-font-weight: bold;");
                        default         -> setStyle("-fx-text-fill: #c9d1d9; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // ── Priority column with colored text ──────────────────
        TableColumn<Ticket, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPriority()));
        priorityCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item.toUpperCase()) {
                        case "CRITICAL" -> setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
                        case "HIGH"     -> setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
                        case "MEDIUM"   -> setStyle("-fx-text-fill: #8b949e;");
                        case "LOW"      -> setStyle("-fx-text-fill: #6e7681;");
                        default         -> setStyle("-fx-text-fill: #c9d1d9;");
                    }
                }
            }
        });

        // ── Status column with colored text ────────────────────
        TableColumn<Ticket, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Open"        -> setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
                        case "In Progress" -> setStyle("-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
                        case "Resolved"    -> setStyle("-fx-text-fill: #3fb950; -fx-font-weight: bold;");
                        case "Closed"      -> setStyle("-fx-text-fill: #8b949e;");
                        default            -> setStyle("-fx-text-fill: #c9d1d9;");
                    }
                }
            }
        });

        TableColumn<Ticket, Integer> assignedToCol = new TableColumn<>("Assigned To");
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
                updateBtn.getStyleClass().setAll("btn-warning");
                updateBtn.setStyle("-fx-background-color: #b45309; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-font-weight: bold;");
                assignBtn.getStyleClass().setAll("btn-warning");
                assignBtn.setStyle("-fx-background-color: #b45309; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 6 14 6 14; -fx-font-weight: bold;");
                updateBtn.setOnAction(e -> {
                    Ticket ticket = getTableView().getItems().get(getIndex());
                    showUpdateStatusDialog(ticket, getTableView());
                });
                assignBtn.setOnAction(e -> {
                    Ticket ticket = getTableView().getItems().get(getIndex());
                    showAssignDialog(ticket, getTableView());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, updateBtn, assignBtn));
            }
        });

        table.getColumns().addAll(idCol, ticketNoCol, titleCol, categoryCol,
                priorityCol, statusCol, assignedToCol, createdCol, actionCol);

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
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    table.getItems().add(new Ticket(
                            extractInt(obj, "id"),
                            extractValue(obj, "ticketNo"),
                            extractValue(obj, "title"),
                            extractValue(obj, "category"),   // ← added
                            extractValue(obj, "priority"),
                            extractValue(obj, "status"),
                            extractInt(obj, "assignedTo"),
                            extractValue(obj, "createdAt")
                    ));
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
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Status:"),     0, 0); grid.add(statusBox,       1, 0);
        grid.add(new Label("Resolution:"), 0, 1); grid.add(resolutionField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (updateTicketStatus(ticket.getId(),
                    statusBox.getValue(), resolutionField.getText()))
                loadTickets(table);
        }
    }

    private void showAssignDialog(Ticket ticket, TableView<Ticket> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Assign Ticket");
        dialog.setHeaderText("Ticket: " + ticket.getTicketNo());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField userIdField = new TextField();
        userIdField.setPromptText("Enter User ID...");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Assign to User ID:"), 0, 0);
        grid.add(userIdField, 1, 0);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String txt = userIdField.getText().trim();
            if (!txt.isEmpty()) {
                if (assignTicket(ticket.getId(), Integer.parseInt(txt)))
                    loadTickets(table);
            }
        }
    }

    private boolean updateTicketStatus(int id, String status, String resolution) {
        try {
            String url = "http://localhost:8080/api/tickets/" + id
                    + "/status?status=" + URLEncoder.encode(status, StandardCharsets.UTF_8)
                    + "&resolution=" + URLEncoder.encode(resolution, StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                showAlert("Success", "Status updated to: " + status);
                return true;
            }
            showAlert("Error", "Server returned: " + response.statusCode());
            return false;
        } catch (Exception ex) {
            showAlert("Error", "Cannot connect: " + ex.getMessage());
            return false;
        }
    }

    private boolean assignTicket(int ticketId, int userId) {
        try {
            String url = "http://localhost:8080/api/tickets/"
                    + ticketId + "/assign?userId=" + userId;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                showAlert("Success", "Ticket assigned to user ID: " + userId);
                return true;
            }
            showAlert("Error", "Server returned: " + response.statusCode());
            return false;
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
        return json.substring(start, json.indexOf("\"", start));
    }

    private int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.length();
        try {
            return Integer.parseInt(
                    json.substring(start, end).trim().replace("}", ""));
        } catch (NumberFormatException e) { return 0; }
    }
}