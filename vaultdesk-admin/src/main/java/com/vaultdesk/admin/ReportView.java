package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.http.*;
import java.util.*;

public class ReportView {

    public VBox getView() {
        Label title = new Label("Reports & Analytics");

        // ── Tab layout ──────────────────────────────────────
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabPane.getTabs().addAll(
                buildAssetsByCategoryTab(),
                buildTicketsByStatusTab(),
                buildExpiringLicensesTab(),
                buildLowStockTab(),
                buildMaintenanceCostTab(),
                buildDeptWiseAssetsTab(),
                buildTicketResolutionTab(),
                buildWarrantyTab()
        );

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.getChildren().addAll(title, tabPane);
        return root;
    }

    // ── TAB 1: Assets by Category ────────────────────────────
    private Tab buildAssetsByCategoryTab() {
        Tab tab = new Tab("Assets by Category");

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<String[], String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[0]));
        TableColumn<String[], String> countCol = new TableColumn<>("Count");
        countCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[1]));
        table.getColumns().addAll(catCol, countCol);

        Label summaryLabel = new Label();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/assets"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);

            Map<String, Integer> categoryCounts = new LinkedHashMap<>();
            int total = 0;

            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String category = extractValue(obj, "category");
                    if (category.isEmpty()) category = "Unknown";
                    categoryCounts.merge(category, 1, Integer::sum);
                    total++;
                }
            }

            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                table.getItems().add(new String[]{
                        entry.getKey(), String.valueOf(entry.getValue())
                });
            }
            summaryLabel.setText("Total Assets: " + total
                    + " across " + categoryCounts.size() + " categories");

        } catch (Exception ex) {
            summaryLabel.setText("Error loading data: " + ex.getMessage());
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                new Label("Asset Distribution by Category"),
                summaryLabel, table);
        tab.setContent(content);
        return tab;
    }

    // ── TAB 2: Tickets by Status ─────────────────────────────
    private Tab buildTicketsByStatusTab() {
        Tab tab = new Tab("Ticket Status");

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<String[], String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[0]));
        TableColumn<String[], String> countCol = new TableColumn<>("Count");
        countCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[1]));
        TableColumn<String[], String> pctCol = new TableColumn<>("% of Total");
        pctCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[2]));
        table.getColumns().addAll(statusCol, countCol, pctCol);

        // Resolution rate label
        Label resolutionLabel = new Label();
        Label avgAgeLabel = new Label();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/tickets"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);

            Map<String, Integer> statusCounts = new LinkedHashMap<>();
            int total = 0;

            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String status = extractValue(obj, "status");
                    if (status.isEmpty()) status = "Unknown";
                    statusCounts.merge(status, 1, Integer::sum);
                    total++;
                }
            }

            int finalTotal = total;
            for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
                double pct = finalTotal == 0 ? 0
                        : (entry.getValue() * 100.0 / finalTotal);
                table.getItems().add(new String[]{
                        entry.getKey(),
                        String.valueOf(entry.getValue()),
                        String.format("%.1f%%", pct)
                });
            }

            int resolved = statusCounts.getOrDefault("Resolved", 0)
                    + statusCounts.getOrDefault("Closed", 0);
            double resRate = total == 0 ? 0 : (resolved * 100.0 / total);
            resolutionLabel.setText(String.format(
                    "Resolution Rate: %.1f%%  (%d resolved out of %d total)",
                    resRate, resolved, total));

        } catch (Exception ex) {
            resolutionLabel.setText("Error: " + ex.getMessage());
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                new Label("Ticket Distribution by Status"),
                resolutionLabel, table);
        tab.setContent(content);
        return tab;
    }

    // ── TAB 3: Expiring Licenses ─────────────────────────────
    private Tab buildExpiringLicensesTab() {
        Tab tab = new Tab("Expiring Licenses");

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<String[], String> nameCol = new TableColumn<>("Software");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[0]));
        TableColumn<String[], String> vendorCol = new TableColumn<>("Vendor");
        vendorCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[1]));
        TableColumn<String[], String> expiryCol = new TableColumn<>("Expiry Date");
        expiryCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[2]));
        TableColumn<String[], String> seatsCol = new TableColumn<>("Seats");
        seatsCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[3]));
        table.getColumns().addAll(nameCol, vendorCol, expiryCol, seatsCol);

        Label countLabel = new Label();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/licenses/expiring?days=90"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);
            int count = 0;

            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String name = extractValue(obj, "softwareName");
                    String vendor = extractValue(obj, "vendor");
                    String expiry = extractValue(obj, "expiryDate");
                    int total = extractInt(obj, "seatsTotal");
                    int used = extractInt(obj, "seatsUsed");
                    table.getItems().add(new String[]{
                            name, vendor, expiry,
                            used + " / " + total
                    });
                    count++;
                }
            }
            countLabel.setText(count == 0
                    ? "No licenses expiring in next 90 days."
                    : "⚠ " + count + " license(s) expiring within 90 days — action required!");

        } catch (Exception ex) {
            countLabel.setText("Error: " + ex.getMessage());
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                new Label("Licenses Expiring in Next 90 Days"),
                countLabel, table);
        tab.setContent(content);
        return tab;
    }

    // ── TAB 4: Low Stock Consumables ─────────────────────────
    private Tab buildLowStockTab() {
        Tab tab = new Tab("Low Stock");

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<String[], String> nameCol = new TableColumn<>("Item");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[0]));
        TableColumn<String[], String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[1]));
        TableColumn<String[], String> stockCol = new TableColumn<>("In Stock");
        stockCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[2]));
        TableColumn<String[], String> reorderCol = new TableColumn<>("Reorder Level");
        reorderCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[3]));
        TableColumn<String[], String> urgencyCol = new TableColumn<>("Urgency");
        urgencyCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[4]));
        table.getColumns().addAll(nameCol, catCol, stockCol, reorderCol, urgencyCol);

        Label countLabel = new Label();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/consumables/low-stock"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);
            int count = 0;

            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String name = extractValue(obj, "name");
                    String category = extractValue(obj, "category");
                    int stock = extractInt(obj, "quantityInStock");
                    int reorder = extractInt(obj, "reorderLevel");
                    String urgency = stock == 0 ? "CRITICAL - OUT OF STOCK"
                            : stock <= reorder / 2 ? "HIGH"
                            : "MEDIUM";
                    table.getItems().add(new String[]{
                            name, category,
                            String.valueOf(stock),
                            String.valueOf(reorder),
                            urgency
                    });
                    count++;
                }
            }
            countLabel.setText(count == 0
                    ? "All consumables are adequately stocked."
                    : "⚠ " + count + " item(s) below reorder level — purchase order needed!");

        } catch (Exception ex) {
            countLabel.setText("Error: " + ex.getMessage());
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                new Label("Consumables Below Reorder Level"),
                countLabel, table);
        tab.setContent(content);
        return tab;
    }

    // ── TAB 5: Maintenance Cost Summary ─────────────────────
    private Tab buildMaintenanceCostTab() {
        Tab tab = new Tab("Maintenance Cost");

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<String[], String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[0]));
        TableColumn<String[], String> countCol = new TableColumn<>("Count");
        countCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[1]));
        TableColumn<String[], String> costCol = new TableColumn<>("Total Cost (₹)");
        costCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[2]));
        table.getColumns().addAll(typeCol, countCol, costCol);

        Label totalLabel = new Label();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/maintenance"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);

            Map<String, Integer> typeCounts = new LinkedHashMap<>();
            Map<String, Double> typeCosts = new LinkedHashMap<>();
            double grandTotal = 0;

            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String type = extractValue(obj, "maintenanceType");
                    if (type.isEmpty()) type = "Unknown";
                    double cost = extractDouble(obj, "cost");
                    typeCounts.merge(type, 1, Integer::sum);
                    typeCosts.merge(type, cost, Double::sum);
                    grandTotal += cost;
                }
            }

            for (String type : typeCounts.keySet()) {
                table.getItems().add(new String[]{
                        type,
                        String.valueOf(typeCounts.get(type)),
                        String.format("%.2f", typeCosts.get(type))
                });
            }
            totalLabel.setText(String.format(
                    "Total Maintenance Spend: ₹%.2f across %d log entries",
                    grandTotal, typeCounts.values().stream()
                            .mapToInt(Integer::intValue).sum()));

        } catch (Exception ex) {
            totalLabel.setText("Error: " + ex.getMessage());
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                new Label("Maintenance Cost by Type"),
                totalLabel, table);
        tab.setContent(content);
        return tab;
    }

    // ── TAB 6: Department-wise Asset Count ───────────────────
    private Tab buildDeptWiseAssetsTab() {
        Tab tab = new Tab("Dept Asset Load");

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<String[], String> deptCol = new TableColumn<>("Department ID");
        deptCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[0]));
        TableColumn<String[], String> countCol = new TableColumn<>("Asset Count");
        countCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[1]));
        TableColumn<String[], String> activeCol = new TableColumn<>("Active");
        activeCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[2]));
        TableColumn<String[], String> inRepairCol = new TableColumn<>("In Repair");
        inRepairCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[3]));
        table.getColumns().addAll(deptCol, countCol, activeCol, inRepairCol);

        Label noteLabel = new Label(
                "Tip: Department names visible in Departments tab. ID maps to name.");

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/assets"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);

            Map<String, Integer> deptTotal = new LinkedHashMap<>();
            Map<String, Integer> deptActive = new LinkedHashMap<>();
            Map<String, Integer> deptRepair = new LinkedHashMap<>();

            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String deptId = String.valueOf(extractInt(obj, "departmentId"));
                    String status = extractValue(obj, "status");
                    deptTotal.merge(deptId, 1, Integer::sum);
                    if ("Active".equals(status))
                        deptActive.merge(deptId, 1, Integer::sum);
                    if ("In Repair".equals(status))
                        deptRepair.merge(deptId, 1, Integer::sum);
                }
            }

            for (String deptId : deptTotal.keySet()) {
                table.getItems().add(new String[]{
                        "Dept #" + deptId,
                        String.valueOf(deptTotal.get(deptId)),
                        String.valueOf(deptActive.getOrDefault(deptId, 0)),
                        String.valueOf(deptRepair.getOrDefault(deptId, 0))
                });
            }

        } catch (Exception ex) {
            noteLabel.setText("Error: " + ex.getMessage());
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                new Label("Asset Load by Department"),
                noteLabel, table);
        tab.setContent(content);
        return tab;
    }

    // ── TAB 7: Ticket Resolution Performance ─────────────────
    private Tab buildTicketResolutionTab() {
        Tab tab = new Tab("Ticket Performance");

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<String[], String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[0]));
        TableColumn<String[], String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[1]));
        TableColumn<String[], String> openCol = new TableColumn<>("Open");
        openCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[2]));
        TableColumn<String[], String> resolvedCol = new TableColumn<>("Resolved");
        resolvedCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[3]));
        TableColumn<String[], String> rateCol = new TableColumn<>("Resolution Rate");
        rateCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[4]));
        table.getColumns().addAll(priorityCol, totalCol, openCol, resolvedCol, rateCol);

        Label summaryLabel = new Label();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/tickets"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);

            Map<String, Integer> byPriorityTotal = new LinkedHashMap<>();
            Map<String, Integer> byPriorityOpen = new LinkedHashMap<>();
            Map<String, Integer> byPriorityResolved = new LinkedHashMap<>();

            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String priority = extractValue(obj, "priority");
                    String status = extractValue(obj, "status");
                    if (priority.isEmpty()) priority = "Unknown";
                    byPriorityTotal.merge(priority, 1, Integer::sum);
                    if ("Open".equals(status) || "In Progress".equals(status))
                        byPriorityOpen.merge(priority, 1, Integer::sum);
                    if ("Resolved".equals(status) || "Closed".equals(status))
                        byPriorityResolved.merge(priority, 1, Integer::sum);
                }
            }

            int grandOpen = 0, grandResolved = 0;
            for (String priority : byPriorityTotal.keySet()) {
                int total = byPriorityTotal.get(priority);
                int open = byPriorityOpen.getOrDefault(priority, 0);
                int resolved = byPriorityResolved.getOrDefault(priority, 0);
                double rate = total == 0 ? 0 : (resolved * 100.0 / total);
                table.getItems().add(new String[]{
                        priority,
                        String.valueOf(total),
                        String.valueOf(open),
                        String.valueOf(resolved),
                        String.format("%.1f%%", rate)
                });
                grandOpen += open;
                grandResolved += resolved;
            }

            summaryLabel.setText(String.format(
                    "Overall — Open: %d  |  Resolved: %d  |  Needs attention: %s priority tickets",
                    grandOpen, grandResolved,
                    byPriorityOpen.getOrDefault("High", 0) > 0 ? "HIGH" : "none critical"));

        } catch (Exception ex) {
            summaryLabel.setText("Error: " + ex.getMessage());
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                new Label("Ticket Resolution Performance by Priority"),
                summaryLabel, table);
        tab.setContent(content);
        return tab;
    }

    // ── TAB 8: Warranty Status ───────────────────────────────
    private Tab buildWarrantyTab() {
        Tab tab = new Tab("Warranty Status");

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<String[], String> nameCol = new TableColumn<>("Asset");
        nameCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[0]));
        TableColumn<String[], String> tagCol = new TableColumn<>("Asset Tag");
        tagCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[1]));
        TableColumn<String[], String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[2]));
        TableColumn<String[], String> warrantyCol = new TableColumn<>("Warranty Expiry");
        warrantyCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[3]));
        TableColumn<String[], String> statusCol = new TableColumn<>("Warranty Status");
        statusCol.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue()[4]));
        table.getColumns().addAll(nameCol, tagCol, categoryCol, warrantyCol, statusCol);

        Label summaryLabel = new Label();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/assets"))
                    .GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String body = response.body().trim();
            body = body.substring(1, body.length() - 1);

            int underWarranty = 0, expired = 0, noData = 0;

            if (!body.isEmpty()) {
                String today = java.time.LocalDate.now().toString();
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String name = extractValue(obj, "name");
                    String tag = extractValue(obj, "assetTag");
                    String category = extractValue(obj, "category");
                    String warranty = extractValue(obj, "warrantyExpiry");

                    String warrantyStatus;
                    if (warranty == null || warranty.isEmpty()) {
                        warrantyStatus = "No Data";
                        noData++;
                    } else if (warranty.compareTo(today) < 0) {
                        warrantyStatus = "EXPIRED";
                        expired++;
                    } else if (warranty.compareTo(
                            java.time.LocalDate.now().plusDays(90).toString()) <= 0) {
                        warrantyStatus = "Expiring Soon (90 days)";
                        underWarranty++;
                    } else {
                        warrantyStatus = "Valid";
                        underWarranty++;
                    }
                    table.getItems().add(new String[]{
                            name, tag, category, warranty.isEmpty() ? "-" : warranty,
                            warrantyStatus
                    });
                }
            }

            summaryLabel.setText(String.format(
                    "Under Warranty: %d  |  Expired: %d  |  No Data: %d",
                    underWarranty, expired, noData));

        } catch (Exception ex) {
            summaryLabel.setText("Error: " + ex.getMessage());
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(
                new Label("Asset Warranty Status Overview"),
                summaryLabel, table);
        tab.setContent(content);
        return tab;
    }

    // ── Helpers ──────────────────────────────────────────────
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

    private double extractDouble(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.length();
        try {
            return Double.parseDouble(
                    json.substring(start, end).trim().replace("}", ""));
        } catch (NumberFormatException e) { return 0.0; }
    }
}