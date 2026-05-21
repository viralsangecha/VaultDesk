package com.vaultdesk.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;

public class ActivityLogView {

    public VBox getView() {
        Label title = new Label("Activity Log");
        title.getStyleClass().add("page-title");
        Label sub = new Label(
                "Audit trail — who did what and when.");
        sub.getStyleClass().add("page-subtitle");

        // ── Filter bar ────────────────────────────────────
        ComboBox<String> limitBox = new ComboBox<>();
        limitBox.getItems().addAll("50", "100", "200", "500");
        limitBox.setValue("50");
        limitBox.setStyle(
                "-fx-background-color: #21262d;" +
                        "-fx-text-fill: #c9d1d9;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-radius: 6;");

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().setAll("btn-primary");
        refreshBtn.setStyle(
                "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 6 14 6 14;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");

        Label limitLabel = new Label("Show last:");
        limitLabel.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 12px;");

        HBox filterBar = new HBox(10,
                limitLabel, limitBox, refreshBtn);
        filterBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("filter-bar");

        // ── Table ─────────────────────────────────────────
        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<String[], String> timeCol =
                new TableColumn<>("Time");
        timeCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[0]));
        timeCol.setPrefWidth(140);

        TableColumn<String[], String> userCol =
                new TableColumn<>("User");
        userCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[1]));

        TableColumn<String[], String> actionCol =
                new TableColumn<>("Action");
        actionCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[2]));
        actionCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle(""); return;
                }
                setText(item);
                switch (item.toUpperCase()) {
                    case "CREATE" -> setStyle(
                            "-fx-text-fill: #3fb950;" +
                                    "-fx-font-weight: bold;");
                    case "UPDATE" -> setStyle(
                            "-fx-text-fill: #58a6ff;" +
                                    "-fx-font-weight: bold;");
                    case "DELETE" -> setStyle(
                            "-fx-text-fill: #f85149;" +
                                    "-fx-font-weight: bold;");
                    case "LOGIN"  -> setStyle(
                            "-fx-text-fill: #d29922;" +
                                    "-fx-font-weight: bold;");
                    default       -> setStyle(
                            "-fx-text-fill: #c9d1d9;");
                }
            }
        });

        TableColumn<String[], String> tableCol =
                new TableColumn<>("Table");
        tableCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[3]));

        TableColumn<String[], String> detailsCol =
                new TableColumn<>("Details");
        detailsCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[4]));

        table.getColumns().addAll(
                timeCol, userCol, actionCol,
                tableCol, detailsCol);

        loadLogs(table, Integer.parseInt(limitBox.getValue()));

        refreshBtn.setOnAction(e ->
                loadLogs(table,
                        Integer.parseInt(limitBox.getValue())));
        limitBox.setOnAction(e ->
                loadLogs(table,
                        Integer.parseInt(limitBox.getValue())));

        VBox root = new VBox(12, title, sub, filterBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    private void loadLogs(TableView<String[]> table, int limit) {
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading activity log...");
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/activity?limit=" + limit))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);

            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String time     = extractValue(obj, "logged_at");
                    String userName = extractValue(obj, "user_name");
                    String action   = extractValue(obj, "action");
                    String tableName = extractValue(obj, "table_name");
                    String details  = extractValue(obj, "details");

                    table.getItems().add(new String[]{
                            time.length() >= 16
                                    ? time.substring(0, 16) : time,
                            userName.isEmpty() ? "System" : userName,
                            action,
                            tableName,
                            details
                    });
                }
                if (table.getItems().isEmpty()) {
                    LoadingUtil.setEmpty(table, "📋",
                            "No activity recorded yet",
                            "Actions will appear here as users " +
                                    "interact with the system.");
                }
            } else {
                LoadingUtil.setEmpty(table, "📋",
                        "No activity recorded yet",
                        "Actions will appear here as users " +
                                "interact with the system.");
            }
        } catch (Exception ex) {
            LoadingUtil.setEmpty(table, "⚠",
                    "Could not load activity log",
                    "Check server connection and try again.");
        }
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        return json.substring(start, json.indexOf("\"", start));
    }
}