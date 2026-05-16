package com.vaultdesk.employee;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;

public class MyAssetsView {

    public VBox getView() {
        Label title = new Label("My Assets");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Assets currently assigned to you.");
        sub.getStyleClass().add("page-subtitle");

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<String[], String> tagCol = new TableColumn<>("Asset Tag");
        tagCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[0]));

        TableColumn<String[], String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[1]));

        TableColumn<String[], String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[2]));

        TableColumn<String[], String> brandCol = new TableColumn<>("Brand");
        brandCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[3]));

        TableColumn<String[], String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[4]));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "Active"    -> setStyle("-fx-text-fill: #3fb950; -fx-font-weight: bold;");
                    case "In Repair" -> setStyle("-fx-text-fill: #d29922; -fx-font-weight: bold;");
                    case "Retired"   -> setStyle("-fx-text-fill: #8b949e;");
                    default          -> setStyle("-fx-text-fill: #c9d1d9;");
                }
            }
        });

        TableColumn<String[], String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue()[5]));

        table.getColumns().addAll(
                tagCol, nameCol, catCol,
                brandCol, statusCol, locationCol);

        loadAssets(table);

        Label hint = new Label(
                "These are assets currently assigned to you by the IT team.");
        hint.setStyle("-fx-text-fill: #484f58; -fx-font-size: 11px;");

        VBox root = new VBox(10, title, sub, hint, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    private void loadAssets(TableView<String[]> table) {
        table.getItems().clear();
        LoadingUtil.setLoading(table, "Loading your assets...");
        try {
            int empId = SessionManager.get().getEmployeeId();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/employee/assets/" + empId))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    table.getItems().add(new String[]{
                            extractValue(obj, "assetTag"),
                            extractValue(obj, "name"),
                            extractValue(obj, "category"),
                            extractValue(obj, "brand"),
                            extractValue(obj, "status"),
                            extractValue(obj, "location")
                    });
                }
                if (table.getItems().isEmpty()) {
                    LoadingUtil.setEmpty(table, "▣",
                            "No assets assigned",
                            "Contact IT to assign assets to you.");
                }
            } else {
                LoadingUtil.setEmpty(table, "▣",
                        "No assets assigned",
                        "Contact IT to assign assets to you.");
            }
        } catch (Exception ex) {
            LoadingUtil.setEmpty(table, "⚠",
                    "Could not load assets",
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