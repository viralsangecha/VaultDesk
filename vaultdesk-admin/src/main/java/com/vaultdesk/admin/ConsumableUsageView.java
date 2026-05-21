package com.vaultdesk.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;
import java.util.Optional;

public class ConsumableUsageView {

    public VBox getView() {
        Label title = new Label("Consumable Usage History");
        title.getStyleClass().add("page-title");
        Label sub = new Label(
                "Track who used what, where, and when.");
        sub.getStyleClass().add("page-subtitle");

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<String[], String> dateCol =
                new TableColumn<>("Date");
        dateCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[0]));

        TableColumn<String[], String> consumableCol =
                new TableColumn<>("Consumable");
        consumableCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[1]));

        TableColumn<String[], String> qtyCol =
                new TableColumn<>("Qty Used");
        qtyCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[2]));

        TableColumn<String[], String> assetCol =
                new TableColumn<>("Used In Asset");
        assetCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[3]));

        TableColumn<String[], String> empCol =
                new TableColumn<>("Given To Employee");
        empCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[4]));

        TableColumn<String[], String> usedByCol =
                new TableColumn<>("Issued By");
        usedByCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[5]));

        TableColumn<String[], String> notesCol =
                new TableColumn<>("Notes");
        notesCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[6]));

        table.getColumns().addAll(dateCol, consumableCol,
                qtyCol, assetCol, empCol,
                usedByCol, notesCol);

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().setAll("btn-primary");
        refreshBtn.setStyle(
                "-fx-background-color: #1f6feb;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 6 14 6 14;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;");
        refreshBtn.setOnAction(e -> loadUsage(table));

        HBox topBar = new HBox(10, refreshBtn);
        topBar.setPadding(new Insets(0, 0, 4, 0));

        loadUsage(table);

        VBox root = new VBox(10,
                title, sub, topBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    private void loadUsage(TableView<String[]> table) {
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading usage log...");
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/consumables/usage/all"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);

            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "")
                            .replace("}", "");
                    String date = extractValue(
                            obj, "usage_date");
                    table.getItems().add(new String[]{
                            date.length() >= 10
                                    ? date.substring(0, 10)
                                    : date,
                            extractValue(obj,
                                    "consumable_name"),
                            extractValue(obj,
                                    "quantity_used"),
                            extractValue(obj, "asset_name"),
                            extractValue(obj,
                                    "employee_name"),
                            extractValue(obj,
                                    "used_by_name"),
                            extractValue(obj, "notes")
                    });
                }
                if (table.getItems().isEmpty()) {
                    LoadingUtil.setEmpty(table, "📦",
                            "No usage recorded yet",
                            "Use the Issue button in " +
                                    "Consumables to record usage.");
                }
            } else {
                LoadingUtil.setEmpty(table, "📦",
                        "No usage recorded yet",
                        "Use the Issue button in " +
                                "Consumables to record usage.");
            }
        } catch (Exception ex) {
            LoadingUtil.setEmpty(table, "⚠",
                    "Could not load usage log",
                    "Check server connection.");
        }
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) {
            search = "\"" + key + "\":";
            start = json.indexOf(search);
            if (start == -1) return "";
            start += search.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.length();
            return json.substring(start, end)
                    .trim().replace("}", "");
        }
        start += search.length();
        return json.substring(start,
                json.indexOf("\"", start));
    }
}