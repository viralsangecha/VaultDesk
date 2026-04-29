package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class TicketView {

    // ── User cache: id → fullName ─────────────────────────
    private final Map<Integer, String> userMap = new LinkedHashMap<>();

    public VBox getView() {
        loadUsers();

        Label title = new Label("Tickets");
        title.getStyleClass().add("section-title");

        TableView<Ticket> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Ticket, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        idCol.setMaxWidth(50);

        TableColumn<Ticket, String> ticketNoCol = new TableColumn<>("Ticket No");
        ticketNoCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTicketNo()));

        TableColumn<Ticket, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTitle()));

        // Category
        TableColumn<Ticket, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCategory()));
        categoryCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item.toUpperCase()) {
                    case "SAP"      -> setStyle("-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
                    case "HARDWARE" -> setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold;");
                    case "NETWORK"  -> setStyle("-fx-text-fill: #39d353; -fx-font-weight: bold;");
                    case "SOFTWARE" -> setStyle("-fx-text-fill: #a371f7; -fx-font-weight: bold;");
                    default         -> setStyle("-fx-text-fill: #c9d1d9; -fx-font-weight: bold;");
                }
            }
        });

        // Priority
        TableColumn<Ticket, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPriority()));
        priorityCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item.toUpperCase()) {
                    case "CRITICAL" -> setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
                    case "HIGH"     -> setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
                    case "MEDIUM"   -> setStyle("-fx-text-fill: #8b949e;");
                    case "LOW"      -> setStyle("-fx-text-fill: #6e7681;");
                    default         -> setStyle("-fx-text-fill: #c9d1d9;");
                }
            }
        });

        // Status
        TableColumn<Ticket, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "Open"        -> setStyle("-fx-text-fill: #f85149; -fx-font-weight: bold;");
                    case "In Progress" -> setStyle("-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
                    case "Resolved"    -> setStyle("-fx-text-fill: #3fb950; -fx-font-weight: bold;");
                    case "Closed"      -> setStyle("-fx-text-fill: #8b949e;");
                    default            -> setStyle("-fx-text-fill: #c9d1d9;");
                }
            }
        });

        // Assigned To — show name not ID
        TableColumn<Ticket, String> assignedToCol = new TableColumn<>("Assigned To");
        assignedToCol.setCellValueFactory(d -> {
            int uid = d.getValue().getAssignedTo();
            String name = uid == 0 ? "Unassigned"
                    : userMap.getOrDefault(uid, "User #" + uid);
            return new SimpleStringProperty(name);
        });

        TableColumn<Ticket, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getCreatedAt() != null &&
                                d.getValue().getCreatedAt().length() >= 10
                                ? d.getValue().getCreatedAt().substring(0, 10)
                                : d.getValue().getCreatedAt()));

        // Actions
        TableColumn<Ticket, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button updateBtn = new Button("Update");
            private final Button assignBtn = new Button("Assign");
            {
                updateBtn.getStyleClass().setAll("btn-warning");
                updateBtn.setStyle(
                        "-fx-background-color: #b45309; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 5 10 5 10;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;");
                assignBtn.getStyleClass().setAll("btn-primary");
                assignBtn.setStyle(
                        "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-padding: 5 10 5 10;" +
                                "-fx-font-size: 11px; -fx-font-weight: bold;");
                updateBtn.setOnAction(e -> {
                    Ticket t = getTableView().getItems().get(getIndex());
                    showUpdateStatusDialog(t, getTableView());
                });
                assignBtn.setOnAction(e -> {
                    Ticket t = getTableView().getItems().get(getIndex());
                    showAssignDialog(t, getTableView());
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, updateBtn, assignBtn));
            }
        });

        table.getColumns().addAll(idCol, ticketNoCol, titleCol,
                categoryCol, priorityCol, statusCol,
                assignedToCol, createdCol, actionCol);

        // ── Add ticket button ─────────────────────────────
        Button addBtn = new Button("＋ New Ticket");
        addBtn.getStyleClass().setAll("btn-primary");
        addBtn.setStyle(
                "-fx-background-color: #238636; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 8 16 8 16;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showAddTicketDialog(table));

        HBox topBar = new HBox(10, addBtn);

        loadTickets(table);

        VBox root = new VBox(10, title, topBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    // ── Load users into map ───────────────────────────────
    private void loadUsers() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/users"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    int id       = extractInt(obj, "id");
                    String name  = extractValue(obj, "fullName");
                    userMap.put(id, name);
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading users: " + ex.getMessage());
        }
    }

    private void loadTickets(TableView<Ticket> table) {
        table.getItems().clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/tickets"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    table.getItems().add(new Ticket(
                            extractInt(obj, "id"),
                            extractValue(obj, "ticketNo"),
                            extractValue(obj, "title"),
                            extractValue(obj, "category"),
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

    // ── Add ticket dialog ─────────────────────────────────
    private void showAddTicketDialog(TableView<Ticket> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New Ticket");
        dialog.setHeaderText("Create a new support ticket");
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("Brief description of the issue");
        TextArea descField = new TextArea();
        descField.setPromptText("Detailed description...");
        descField.setPrefRowCount(3);

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(
                "Hardware", "Software", "SAP", "Network", "General");
        categoryBox.setValue("Hardware");

        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("Low", "Medium", "High", "Critical");
        priorityBox.setValue("Medium");

        // Reported by — current logged-in user's id
        Label reportedByLabel = new Label(
                "Reported by: " + SessionManager.get().getFullName());
        reportedByLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px;");

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Title *:"),       0, 0); grid.add(titleField,      1, 0);
        grid.add(new Label("Description:"),   0, 1); grid.add(descField,       1, 1);
        grid.add(new Label("Category:"),      0, 2); grid.add(categoryBox,     1, 2);
        grid.add(new Label("Priority:"),      0, 3); grid.add(priorityBox,     1, 3);
        grid.add(reportedByLabel,             1, 4);
        grid.add(errorLabel,                  1, 5);
        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        titleField.textProperty().addListener((o, ov, nv) ->
                okButton.setDisable(nv.trim().isEmpty()));

        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (titleField.getText().trim().isEmpty()) {
                errorLabel.setText("Title is required.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int reportedBy = SessionManager.get().getUserId();
                String body = "{" +
                        "\"title\":\"" + titleField.getText() + "\"," +
                        "\"description\":\"" + descField.getText() + "\"," +
                        "\"category\":\"" + categoryBox.getValue() + "\"," +
                        "\"priority\":\"" + priorityBox.getValue() + "\"," +
                        "\"reportedBy\":" + reportedBy +
                        "}";
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/tickets"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                HttpResponse<String> resp = client.send(req,
                        HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 201) {
                    showAlert("Success", "Ticket created.");
                    loadTickets(table);
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    // ── Update status dialog ──────────────────────────────
    private void showUpdateStatusDialog(Ticket ticket,
                                        TableView<Ticket> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Ticket Status");
        dialog.setHeaderText("Ticket: " + ticket.getTicketNo());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll(
                "Open", "In Progress", "Resolved", "Closed");
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

    // ── Assign dialog — dropdown of users by name ─────────
    private void showAssignDialog(Ticket ticket, TableView<Ticket> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Assign Ticket");
        dialog.setHeaderText("Ticket: " + ticket.getTicketNo());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> userBox = new ComboBox<>();
        ObservableList<String> userNames = FXCollections.observableArrayList();
        Map<String, Integer> nameToId = new LinkedHashMap<>();

        for (Map.Entry<Integer, String> entry : userMap.entrySet()) {
            String display = entry.getValue() + " (#" + entry.getKey() + ")";
            userNames.add(display);
            nameToId.put(display, entry.getKey());
        }
        userBox.setItems(userNames);
        if (!userNames.isEmpty()) userBox.setValue(userNames.get(0));

        // Pre-select current assignee if set
        if (ticket.getAssignedTo() != 0) {
            String current = userMap.get(ticket.getAssignedTo());
            if (current != null) {
                String display = current + " (#" + ticket.getAssignedTo() + ")";
                userBox.setValue(display);
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("Assign to:"), 0, 0);
        grid.add(userBox, 1, 0);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String selected = userBox.getValue();
            if (selected != null) {
                int userId = nameToId.get(selected);
                if (assignTicket(ticket.getId(), userId))
                    loadTickets(table);
            }
        }
    }

    private boolean updateTicketStatus(int id, String status,
                                       String resolution) {
        try {
            String url = "http://localhost:8080/api/tickets/" + id
                    + "/status?status="
                    + URLEncoder.encode(status, StandardCharsets.UTF_8)
                    + "&resolution="
                    + URLEncoder.encode(resolution, StandardCharsets.UTF_8);
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                showAlert("Success", "Status updated to: " + status);
                return true;
            }
            showAlert("Error", "Server returned: " + resp.statusCode());
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
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .PUT(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                showAlert("Success", "Ticket assigned to: "
                        + userMap.getOrDefault(userId, "User #" + userId));
                return true;
            }
            showAlert("Error", "Server returned: " + resp.statusCode());
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