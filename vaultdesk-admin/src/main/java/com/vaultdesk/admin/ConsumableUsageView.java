package com.vaultdesk.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.http.*;
import java.util.ArrayList;
import java.util.List;

public class ConsumableUsageView {

    public VBox getView() {
        Label bcRoot = new Label("INVENTORY");
        bcRoot.getStyleClass().add("breadcrumb-root");
        Label bcSep = new Label("  /  ");
        bcSep.getStyleClass().add("breadcrumb-sep");
        Label bcCurrent = new Label("USAGE HISTORY");
        bcCurrent.getStyleClass().add("breadcrumb-current");
        HBox breadcrumb = new HBox(bcRoot, bcSep, bcCurrent);

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
        refreshBtn.getStyleClass().add("btn-primary");
        AnimationUtil.addHoverScale(refreshBtn);
        refreshBtn.setOnAction(e -> loadUsage(table));

        HBox topBar = new HBox(10, refreshBtn);
        topBar.setPadding(new Insets(0, 0, 4, 0));

        loadUsage(table);

        VBox tableWrapper = new VBox(table);
        tableWrapper.getStyleClass().add("table-wrapper");
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableWrapper, Priority.ALWAYS);

        VBox root = new VBox(12, breadcrumb,
                new VBox(4, title, sub), topBar, tableWrapper);
        return root;
    }

    private void loadUsage(TableView<String[]> table) {
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading usage log...");

        Task<List<String[]>> task = new Task<>() {
            @Override
            protected List<String[]> call() throws Exception {
                List<String[]> result = new ArrayList<>();

                String url = ConfigManager.getBaseUrl() + "/api/consumables/usage/all";
                HttpResponse<String> resp = ApiClient.get(url);
                String body = resp.body().trim();

                if (body.equals("[]") || body.isEmpty()) {
                    return result;
                }

                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    String[] jsonObjects = body.split("\\},\\s*\\{");
                    for (String obj : jsonObjects) {
                        String cleanedObj = obj.replace("{", "").replace("}", "");

                        String[] row = new String[]{
                                DateTimeFormatUtil.toIndianDateTime(extractValue(cleanedObj, "usage_date")),
                                extractValue(cleanedObj, "consumable_name"),
                                extractValue(cleanedObj, "quantity_used"),
                                extractValue(cleanedObj, "asset_name"),
                                extractValue(cleanedObj, "employee_name"),
                                extractValue(cleanedObj, "used_by_name"),
                                extractValue(cleanedObj, "notes")
                        };

                        result.add(row);
                    }
                }
                return result;
            }
        };

        task.setOnSucceeded(e -> {
            List<String[]> rows = task.getValue();
            table.getItems().addAll(rows);
            if (rows.isEmpty()) {
                LoadingUtil.setEmpty(table, "📦", "No usage recorded yet", "Use the Issue button in Consumables to record usage.");
            }
        });

        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠", "Could not load usage log", "Check server connection.");
            task.getException().printStackTrace();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
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