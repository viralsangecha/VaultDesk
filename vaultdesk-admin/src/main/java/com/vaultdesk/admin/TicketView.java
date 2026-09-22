package com.vaultdesk.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

public class TicketView {

    private ObservableList<Ticket> allTickets =
            FXCollections.observableArrayList();

    private final Map<Integer, String> userMap = new LinkedHashMap<>();
    private final Map<Integer, String> empMap = new LinkedHashMap<>();
    private final Map<Integer, String> deptMap = new LinkedHashMap<>();
    private final Map<Integer, String> assetMap = new LinkedHashMap<>();
    private final Map<Integer, String> activeUserMap = new LinkedHashMap<>();
    private Integer selectedDeptId = 0;
    // ── Main layout — list on left, detail on right ───────
    private HBox mainLayout;
    private VBox detailPanel;
    private TableView<Ticket> table;
    Label openCount   = new Label("0");
    Label inProgCount = new Label("0");
    Label resolvedCount = new Label("0");


    public VBox getView() {
        loadUsers();
        loademp();
        loadAssets();

        Label title = new Label("Tickets");
        title.getStyleClass().add("section-title");

        Label mytitle = new Label("My Assigned Tickets");
        mytitle.getStyleClass().add("page-title");
        Label sub = new Label(
                "Tickets assigned to you — update status to resolve.");
        sub.getStyleClass().add("page-subtitle");

        // ── Stats row ─────────────────────────────────────────
        Label openCount   = new Label("0");
        Label inProgCount = new Label("0");
        Label resolvedCount = new Label("0");

        VBox openCard     = engineerStatCard("Open", openCount, "engineer-status-open");
        VBox inProgCard   = engineerStatCard("In Progress", inProgCount, "engineer-status-in-progress");
        VBox resolvedCard = engineerStatCard("Resolved", resolvedCount, "engineer-status-resolved");

        HBox statsRow = new HBox(12, openCard, inProgCard, resolvedCard);
        statsRow.setPadding(new javafx.geometry.Insets(8, 0, 8, 0));
        // ── Table ─────────────────────────────────────────
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Ticket, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getId()).asObject());
        idCol.setMaxWidth(50);

        TableColumn<Ticket, String> ticketNoCol = new TableColumn<>("Ticket No");
        ticketNoCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTicketNo()));
        ticketNoCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); getStyleClass().remove("data-mono"); return; }
                setText(item);
                if (!getStyleClass().contains("data-mono")) getStyleClass().add("data-mono");
            }
        });

        TableColumn<Ticket, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTitle()));

        TableColumn<Ticket, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCategory()));
        categoryCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
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

        TableColumn<Ticket, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPriority()));
        priorityCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
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

        TableColumn<Ticket, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
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

        TableColumn<Ticket, String> assignedToCol = new TableColumn<>("Assigned To");
        assignedToCol.setCellValueFactory(d -> {
            int uid = d.getValue().getAssignedTo();
            String name = uid == 0 ? "Unassigned"
                    : userMap.getOrDefault(uid, "User #" + uid);
            return new SimpleStringProperty(name);
        });

        TableColumn<Ticket, String> createdCol = new TableColumn<>("Created");
        createdCol.setCellValueFactory(d ->
                new SimpleStringProperty(DateTimeFormatUtil.toIndianDate(d.getValue().getCreatedAt())));

        // ── Time open + SLA color ─────────────────────────────
        TableColumn<Ticket, String> slaCol = new TableColumn<>("Time Open");
        slaCol.setCellValueFactory(d -> {
            String created = d.getValue().getCreatedAt();
            String status  = d.getValue().getStatus();
            String updated = d.getValue().getUpdatedAt();

            if (created == null) return new SimpleStringProperty("—");

            if ("Closed".equals(status)) {
                // Ticket is done — show the final total time it was open, same figure as the export column
                return new SimpleStringProperty(DateTimeFormatUtil.elapsedBetween(created, updated));
            }

            // Still active (Open, In Progress, Resolved-but-not-yet-closed) — show live-ticking elapsed time
            try {
                java.time.LocalDateTime createdTime =
                        java.time.LocalDateTime.parse(created.replace(" ", "T"));
                long hours = java.time.Duration.between(
                        createdTime,
                        java.time.LocalDateTime.now()).toHours();
                if (hours < 1) return new SimpleStringProperty("< 1h");
                if (hours < 24) return new SimpleStringProperty(hours + "h");
                return new SimpleStringProperty((hours / 24) + "d " + (hours % 24) + "h");
            } catch (Exception e) {
                return new SimpleStringProperty("—");
            }
        });

        /*TableColumn<Ticket, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button updateBtn = new Button("Update");
            private final Button assignBtn = new Button("Assign");
            {
                updateBtn.getStyleClass().add("btn-warning");
                updateBtn.setStyle("-fx-padding: 5 10 5 10; -fx-font-size: 11px;");
                assignBtn.getStyleClass().add("btn-primary");
                assignBtn.setStyle("-fx-padding: 5 10 5 10; -fx-font-size: 11px;");
                updateBtn.setOnAction(e -> {
                    Ticket t = getTableView().getItems().get(getIndex());
                    showUpdateStatusDialog(t, getTableView());
                });
                assignBtn.setOnAction(e -> {
                    Ticket t = getTableView().getItems().get(getIndex());
                    showAssignDialog(t, getTableView());
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(5);
                if (PermissionManager.canUpdateTicketStatus()) box.getChildren().add(updateBtn);
                if (PermissionManager.canAssignTicket()) box.getChildren().add(assignBtn);
                setGraphic(box.getChildren().isEmpty() ? null : box);
            }
        });**/

        table.getColumns().addAll(idCol, ticketNoCol, titleCol,
                categoryCol, priorityCol, statusCol,
                assignedToCol, createdCol,slaCol);

        // ── Bulk action bar ───────────────────────────────────
        Label bulkLabel = new Label("Bulk Actions:");
        bulkLabel.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 12px;");

        ComboBox<String> bulkStatusBox = new ComboBox<>();
        bulkStatusBox.getItems().addAll(
                "In Progress", "Resolved", "Closed");
        bulkStatusBox.setPromptText("Select status...");
        bulkStatusBox.setStyle(
                "-fx-background-color: #21262d;" +
                        "-fx-text-fill: #c9d1d9;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-radius: 6;");

        Button bulkApplyBtn = new Button("Apply to Selected");
        bulkApplyBtn.getStyleClass().setAll("btn-warning");
        bulkApplyBtn.setStyle(
                "-fx-background-color: #b45309; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-padding: 6 14 6 14;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");

        Label bulkResultLabel = new Label("");
        bulkResultLabel.setStyle(
                "-fx-text-fill: #3fb950; -fx-font-size: 11px;");

        HBox bulkBar = new HBox(10, bulkLabel, bulkStatusBox,
                bulkApplyBtn, bulkResultLabel);
        bulkBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bulkBar.getStyleClass().add("filter-bar");

        // ── Enable multi-select on table ──────────────────────
        table.getSelectionModel().setSelectionMode(
                SelectionMode.MULTIPLE);

        bulkApplyBtn.setOnAction(e -> {
            String status = bulkStatusBox.getValue();
            if (status == null || status.isEmpty()) {
                bulkResultLabel.setText("Select a status first.");
                bulkResultLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 11px;");
                return;
            }
            var selected = table.getSelectionModel()
                    .getSelectedItems();
            if (selected.isEmpty()) {
                bulkResultLabel.setText("Select tickets first.");
                bulkResultLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 11px;");
                return;
            }

            int success = 0;
            for (Ticket t : selected) {
                if (updateTicketStatus(t.getId(), status, "")) {
                    success++;
                }
            }

            int finalSuccess = success;
            bulkResultLabel.setText(
                    "✔ Updated " + finalSuccess + " tickets.");
            bulkResultLabel.setStyle(
                    "-fx-text-fill: #3fb950; -fx-font-size: 11px;");
            loadTickets(table,openCount,inProgCount,resolvedCount);
            table.getSelectionModel().clearSelection();
        });
        // ── Click row to open detail panel ────────────────
        // ── Click row to open detail panel & Right-Click Context Menu ────────────────
        table.setRowFactory(tv -> {
            TableRow<Ticket> row = new TableRow<>();

            // Create Context Menu for the row
            ContextMenu rowMenu = new ContextMenu();

            MenuItem updateItem = new MenuItem("✏ Update Status");
            updateItem.setOnAction(e -> showUpdateStatusDialog(row.getItem(), tv));

            MenuItem assignItem = new MenuItem("👤 Assign Ticket");
            assignItem.setOnAction(e -> showAssignDialog(row.getItem(), tv));

            // Add items only if user has permission
            if (PermissionManager.canUpdateTicketStatus()) {
                rowMenu.getItems().add(updateItem);
            }
            if (PermissionManager.canAssignTicket()) {
                rowMenu.getItems().add(assignItem);
            }

            // Bind the context menu to only show on non-empty rows
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu)
            );

            // Keep the existing double-click behavior
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    if (PermissionManager.canViewTicketDetails()) {
                        openDetailPanel(row.getItem());
                    } else {
                        showAlert("Access Denied",
                                "You don't have permission to view Ticket details.\nContact your administrator.");
                    }
                }
            });

            return row;
        });

        Label hint = new Label("Right-click a row for actions, or right-click anywhere for New Ticket / Export.");
        hint.getStyleClass().add("text-muted");
        hint.setStyle("-fx-font-size: 11px;");

        HBox topBar = new HBox(10, hint);
        topBar.setAlignment(Pos.CENTER_LEFT);

        // ── Department Filter Bar ────────────────────────────
        Label filterLabel = new Label("Department:");
        filterLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");

        ComboBox<String> deptFilterBox = new ComboBox<>();
        deptFilterBox.getStyleClass().add("filter-combo");

        loadDepartments(deptFilterBox);

        // Listener to trigger data reload when a department is selected
        deptFilterBox.setOnAction(e -> {
            String selected = deptFilterBox.getValue();
            if (selected != null) {
                // Find ID from the selected String name
                selectedDeptId = deptMap.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(selected))
                        .map(Map.Entry::getKey)
                        .findFirst().orElse(0);

                loadTickets(table,openCount,inProgCount,resolvedCount);
            }
        });

        HBox filterBar = new HBox(10, filterLabel, deptFilterBox);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("filter-bar");

        // ── Hide filter for HODs ────────────────────────
        if (SessionManager.get().isDeptHod()) {
            filterBar.setVisible(false);
            filterBar.setManaged(false);
            bulkBar.setVisible(false);
            filterBar.setManaged(false);
        }


        // ── Detail panel (hidden initially) ──────────────
        detailPanel = new VBox();
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        detailPanel.setPrefWidth(560);
        detailPanel.setMinWidth(560);
        detailPanel.setMaxWidth(600);
        detailPanel.setMaxHeight(Double.MAX_VALUE);
        detailPanel.getStyleClass().add("activity-panel");
        detailPanel.setStyle("-fx-border-width: 0 0 0 1; -fx-background-radius: 0; -fx-border-radius: 0;");

        // ── Main layout: table left, detail right ─────────
        VBox tableBox = new VBox(10, statsRow,topBar,filterBar,/*bulkBar,*/ table);
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(tableBox, Priority.ALWAYS);

        mainLayout = new HBox(tableBox, detailPanel);
        HBox.setHgrow(tableBox, Priority.ALWAYS);

        VBox root = new VBox(10, title, mainLayout);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);
        loadTickets(table, openCount, inProgCount, resolvedCount);
        // ── Screen Context Menu (Right Click anywhere to Export) ─────────────
        ContextMenu screenMenu = new ContextMenu();
        screenMenu.setAutoHide(true);

        MenuItem newTicketItem = new MenuItem("＋ New Ticket");
        newTicketItem.setOnAction(e -> showAddTicketDialog(table));
        screenMenu.getItems().add(newTicketItem);

        MenuItem exportItem = new MenuItem("⬇ Export Tickets");
        exportItem.setOnAction(e -> exportTickets());
        screenMenu.getItems().add(exportItem);

        // FIX: Force the context menu to hide if the user clicks anywhere with the left mouse button
        root.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                screenMenu.hide();
            }
        });

        root.setOnContextMenuRequested(e -> {
            // Check if we right-clicked on a table row. If so, let the row's menu handle it.
            boolean clickedOnRow = false;
            if (e.getTarget() instanceof javafx.scene.Node) {
                javafx.scene.Node current = (javafx.scene.Node) e.getTarget();
                while (current != null) {
                    if (current instanceof TableRow) {
                        TableRow<?> tr = (TableRow<?>) current;
                        if (!tr.isEmpty()) {
                            clickedOnRow = true;
                        }
                        break;
                    }
                    current = current.getParent();
                }
            }

            // Show the screen menu if we didn't click on a populated table row
            if (!clickedOnRow) {
                screenMenu.show(root, e.getScreenX(), e.getScreenY());
            } else {
                screenMenu.hide(); // Hide screen menu if we click a row to prevent overlaps
            }
        });
        return root;
    }
    public VBox getEngineerView(int userId) {
        loadUsers();
        loademp();
        loadAssets();

        Label title = new Label("My Assigned Tickets");
        title.getStyleClass().add("page-title");
        Label sub = new Label(
                "Tickets assigned to you — update status to resolve.");
        sub.getStyleClass().add("page-subtitle");

        // ── Stats row ─────────────────────────────────────────
        Label openCount   = new Label("0");
        Label inProgCount = new Label("0");
        Label resolvedCount = new Label("0");

        VBox openCard     = engineerStatCard("Open", openCount, "engineer-status-open");
        VBox inProgCard   = engineerStatCard("In Progress", inProgCount, "engineer-status-in-progress");
        VBox resolvedCard = engineerStatCard("Resolved", resolvedCount, "engineer-status-resolved");

        HBox statsRow = new HBox(12, openCard, inProgCard, resolvedCard);
        statsRow.setPadding(new javafx.geometry.Insets(8, 0, 8, 0));

        // ── Table ─────────────────────────────────────────────
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        LoadingUtil.setLoading(table, "Loading your tickets...");

        TableColumn<Ticket, String> ticketNoCol = new TableColumn<>("Ticket No");
        ticketNoCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTicketNo()));
        ticketNoCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); getStyleClass().remove("data-mono"); return; }
                setText(item);
                if (!getStyleClass().contains("data-mono")) getStyleClass().add("data-mono");
            }
        });

        TableColumn<Ticket, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTitle()));

        TableColumn<Ticket, String> categoryCol =
                new TableColumn<>("Category");
        categoryCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCategory()));
        categoryCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle(""); return;
                }
                setText(item);
                switch (item.toUpperCase()) {
                    case "SAP"      -> setStyle(
                            "-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
                    case "HARDWARE" -> setStyle(
                            "-fx-text-fill: #8b949e; -fx-font-weight: bold;");
                    case "NETWORK"  -> setStyle(
                            "-fx-text-fill: #39d353; -fx-font-weight: bold;");
                    case "SOFTWARE" -> setStyle(
                            "-fx-text-fill: #a371f7; -fx-font-weight: bold;");
                    default         -> setStyle(
                            "-fx-text-fill: #c9d1d9; -fx-font-weight: bold;");
                }
            }
        });

        TableColumn<Ticket, String> priorityCol =
                new TableColumn<>("Priority");
        priorityCol.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getPriority()));
        priorityCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle(""); return;
                }
                setText(item);
                switch (item.toUpperCase()) {
                    case "CRITICAL" -> setStyle(
                            "-fx-text-fill: #f85149; -fx-font-weight: bold;");
                    case "HIGH"     -> setStyle(
                            "-fx-text-fill: #d29922; -fx-font-weight: bold;");
                    case "MEDIUM"   -> setStyle(
                            "-fx-text-fill: #8b949e;");
                    case "LOW"      -> setStyle(
                            "-fx-text-fill: #6e7681;");
                    default         -> setStyle(
                            "-fx-text-fill: #c9d1d9;");
                }
            }
        });

        TableColumn<Ticket, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle(""); return;
                }
                setText(item);
                switch (item) {
                    case "Open"        -> setStyle(
                            "-fx-text-fill: #f85149; -fx-font-weight: bold;");
                    case "In Progress" -> setStyle(
                            "-fx-text-fill: #58a6ff; -fx-font-weight: bold;");
                    case "Resolved"    -> setStyle(
                            "-fx-text-fill: #3fb950; -fx-font-weight: bold;");
                    case "Closed"      -> setStyle(
                            "-fx-text-fill: #8b949e;");
                    default            -> setStyle(
                            "-fx-text-fill: #c9d1d9;");
                }
            }
        });

        // ── SLA column ────────────────────────────────────────
        TableColumn<Ticket, String> slaCol =
                new TableColumn<>("Time Open");
        slaCol.setCellValueFactory(d -> {
            String created = d.getValue().getCreatedAt();
            String status  = d.getValue().getStatus();
            if (created == null || "Resolved".equals(status)
                    || "Closed".equals(status))
                return new SimpleStringProperty("—");
            try {
                java.time.LocalDateTime ct =
                        java.time.LocalDateTime.parse(
                                created.replace(" ", "T"));
                long hours = java.time.Duration.between(
                        ct, java.time.LocalDateTime.now()).toHours();
                if (hours < 1)  return new SimpleStringProperty("< 1h");
                if (hours < 24) return new SimpleStringProperty(hours + "h");
                return new SimpleStringProperty(
                        (hours / 24) + "d " + (hours % 24) + "h");
            } catch (Exception e) {
                return new SimpleStringProperty("—");
            }
        });
        slaCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "—".equals(item)) {
                    setText(item);
                    setStyle("-fx-text-fill: #484f58;");
                    return;
                }
                setText(item);
                try {
                    Ticket t = getTableView().getItems().get(getIndex());
                    java.time.LocalDateTime ct =
                            java.time.LocalDateTime.parse(
                                    t.getCreatedAt().replace(" ", "T"));
                    long hours = java.time.Duration.between(
                            ct, java.time.LocalDateTime.now()).toHours();
                    long slaH = switch (t.getPriority().toUpperCase()) {
                        case "CRITICAL" -> 4L;
                        case "HIGH"     -> 8L;
                        case "MEDIUM"   -> 24L;
                        default         -> 48L;
                    };
                    if (hours > slaH)
                        setStyle("-fx-text-fill: #f85149;" +
                                " -fx-font-weight: bold;");
                    else if (hours > slaH * 0.75)
                        setStyle("-fx-text-fill: #d29922;" +
                                " -fx-font-weight: bold;");
                    else
                        setStyle("-fx-text-fill: #3fb950;");
                } catch (Exception ignored) {
                    setStyle("-fx-text-fill: #484f58;");
                }
            }
        });

        // ── Actions ───────────────────────────────────────────
        /*TableColumn<Ticket, Void> actionCol =
                new TableColumn<>("Actions");
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button updateBtn = new Button("Update Status");
            {
                updateBtn.getStyleClass().add("btn-warning");
                updateBtn.setStyle("-fx-padding: 5 10 5 10; -fx-font-size: 19px;");
                updateBtn.setOnAction(e -> {
                    Ticket t = getTableView().getItems().get(getIndex());
                    showUpdateStatusDialog(t, getTableView());
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || !PermissionManager.canUpdateTicketStatus() ? null : updateBtn);
            }
        });*/

        table.getColumns().addAll(ticketNoCol, titleCol,
                categoryCol, priorityCol, statusCol,
                slaCol/*, actionCol*/);

        // ── Double click → detail panel ───────────────────────
        table.setRowFactory(tv -> {
            TableRow<Ticket> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty())
                    if (PermissionManager.canViewTicketDetails())
                    {
                        openDetailPanel(row.getItem());
                    }
                    else {
                        showAlert("Access Denied",
                                "You don't have permission to view Ticket details.\nContact your administrator.");
                    }
            });
            return row;
        });

        // ── Detail panel ──────────────────────────────────────
        detailPanel = new VBox();
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
        detailPanel.setPrefWidth(500);
        detailPanel.setMinWidth(500);
        detailPanel.setMaxWidth(520);
        detailPanel.setMaxHeight(Double.MAX_VALUE);
        detailPanel.getStyleClass().add("activity-panel");
        detailPanel.setStyle("-fx-border-width: 0 0 0 1; -fx-background-radius: 0; -fx-border-radius: 0;");

        Label hint = new Label(
                "Double-click a ticket to view details and comments");
        hint.setStyle(
                "-fx-text-fill: #484f58; -fx-font-size: 11px;");

        VBox tableBox = new VBox(8, hint, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(tableBox, Priority.ALWAYS);

        HBox mainRow = new HBox(tableBox, detailPanel);
        HBox.setHgrow(tableBox, Priority.ALWAYS);

        // ── Department Filter Bar ────────────────────────────
        Label filterLabel = new Label("Department:");
        filterLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");

        ComboBox<String> deptFilterBox = new ComboBox<>();
        deptFilterBox.setStyle(
                "-fx-background-color: #21262d;" +
                        "-fx-text-fill: #c9d1d9;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-radius: 6;");

        loadDepartments(deptFilterBox);

        // Listener to trigger data reload when a department is selected
        deptFilterBox.setOnAction(e -> {
            String selected = deptFilterBox.getValue();
            if (selected != null) {
                // Find ID from the selected String name
                selectedDeptId = deptMap.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(selected))
                        .map(Map.Entry::getKey)
                        .findFirst().orElse(0);

                loadEngineerTickets(userId, table, openCount, inProgCount, resolvedCount);
            }
        });

        HBox filterBar = new HBox(10, filterLabel, deptFilterBox);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("filter-bar");

        // ── Load tickets ──────────────────────────────────────
        loadEngineerTickets(userId, table,
                openCount, inProgCount, resolvedCount);

        VBox root = new VBox(12, title, sub, statsRow,filterBar, mainRow);
        VBox.setVgrow(mainRow, Priority.ALWAYS);
        // ── Screen Context Menu (Right Click anywhere to Export) ─────────────
        ContextMenu screenMenu = new ContextMenu();
        screenMenu.setAutoHide(true); // Ensure standard JavaFX auto-hide behavior

        MenuItem exportItem = new MenuItem("⬇ Export Tickets");
        exportItem.setOnAction(e -> exportTickets());
        screenMenu.getItems().add(exportItem);

        // FIX: Force the context menu to hide if the user clicks anywhere with the left mouse button
        root.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                screenMenu.hide();
            }
        });

        root.setOnContextMenuRequested(e -> {
            // Check if we right-clicked on a table row. If so, let the row's menu handle it.
            boolean clickedOnRow = false;
            if (e.getTarget() instanceof javafx.scene.Node) {
                javafx.scene.Node current = (javafx.scene.Node) e.getTarget();
                while (current != null) {
                    if (current instanceof TableRow) {
                        TableRow<?> tr = (TableRow<?>) current;
                        if (!tr.isEmpty()) {
                            clickedOnRow = true;
                        }
                        break;
                    }
                    current = current.getParent();
                }
            }

            // Show the screen menu if we didn't click on a populated table row
            if (!clickedOnRow) {
                screenMenu.show(root, e.getScreenX(), e.getScreenY());
            } else {
                screenMenu.hide(); // Hide screen menu if we click a row to prevent overlaps
            }
        });
        return root;
    }

    // ── Engineer stat card ────────────────────────────────────
    private VBox engineerStatCard(String label, Label countLabel, String statusClass) {
        // 1. FORCE CLEAR any inline styles that were set outside this method
        countLabel.setStyle(null);

        // 2. Apply classes
        countLabel.getStyleClass().addAll("engineer-stat-count", statusClass);

        Label titleLabel = new Label(label);
        titleLabel.getStyleClass().add("engineer-stat-title");

        VBox card = new VBox(4, countLabel, titleLabel);
        card.getStyleClass().addAll("engineer-card", statusClass);
        card.setPrefWidth(160);

        return card;
    }

    private void exportTickets() {
        Dialog<ButtonType> rangeDialog = new Dialog<>();
        ThemeManager.applyToDialog(rangeDialog);
        rangeDialog.setTitle("Export Tickets");
        rangeDialog.setHeaderText("Select a date range (by ticket created date)");
        rangeDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField fromField = new TextField();
        TextField toField = new TextField();
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.add(new Label("From:"), 0, 0);
        grid.add(DatePickerUtil.dateField(fromField), 1, 0);
        grid.add(new Label("To:"), 0, 1);
        grid.add(DatePickerUtil.dateField(toField), 1, 1);
        rangeDialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = rangeDialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;
        String from = DatePickerUtil.toIso(fromField.getText().trim());
        String to = DatePickerUtil.toIso(toField.getText().trim());
        if (from.isEmpty() || to.isEmpty()) {
            showAlert("Error", "Please enter valid dates (dd-MM-yyyy).");
            return;
        }

        Task<List<List<String>>> task = new Task<>() {
            @Override
            protected List<List<String>> call() throws Exception {
                List<List<String>> rows = new ArrayList<>();
                HttpResponse<String> resp = ApiClient.get(
                        ConfigManager.getBaseUrl() + "/api/tickets/export?from=" + from + "&to=" + to);
                String body = resp.body().trim();
                if (body.length() < 2) return rows;
                body = body.substring(1, body.length() - 1).trim();
                if (body.isEmpty()) return rows;

                // Each top-level entry is {"ticket":{...},"cycles":[...]}
                for (String entry : splitTopLevelObjects(body)) {
                    String ticketBlock = extractNestedObject(entry, "ticket");
                    String cyclesBlock = extractNestedArray(entry, "cycles");

                    List<String> row = new ArrayList<>();
                    row.add(extractValue(ticketBlock, "ticketNo"));
                    row.add(extractValue(ticketBlock, "title"));
                    row.add(extractValue(ticketBlock, "category"));
                    row.add(extractValue(ticketBlock, "priority"));
                    row.add(extractValue(ticketBlock, "status"));
                    row.add(empMap.getOrDefault(extractInt(ticketBlock, "reportedBy"),
                            "Employee #" + extractInt(ticketBlock, "reportedBy")));
                    row.add(extractValue(ticketBlock, "department"));
                    row.add(DateTimeFormatUtil.toIndianDateTime(extractValue(ticketBlock, "createdAt")));
                    row.add(extractInt(ticketBlock, "assignedTo") == 0 ? "Unassigned"
                            : userMap.getOrDefault(extractInt(ticketBlock, "assignedTo"), "-"));

                    List<String[]> cycles = parseCycles(cyclesBlock);
                    for (int i = 0; i < 5; i++) {
                        if (i < cycles.size()) {
                            String[] c = cycles.get(i);
                            row.add(DateTimeFormatUtil.toIndianDateTime(c[0])); // Resolved N At
                            row.add(c[4].isEmpty() ? "-" : c[4]); // Resolution N Text (what the engineer wrote)
                            row.add(DateTimeFormatUtil.toIndianDateTime(c[1]) + " — " + c[2] // Closed/Denied N At + outcome
                                    + (c[2].equals("In Progress") && !c[3].isEmpty() ? " (Deny Reason: " + c[3] + ")" : ""));
                        } else {
                            row.add("-");
                            row.add("-");
                            row.add("-");
                        }
                    }
                    boolean isClosed = "Closed".equals(extractValue(ticketBlock, "status"));
                    String createdAtRaw = extractValue(ticketBlock, "createdAt");
                    String updatedAtRaw = extractValue(ticketBlock, "updatedAt");
                    row.add(isClosed ? DateTimeFormatUtil.toIndianDateTime(updatedAtRaw) : "-");
                    row.add(isClosed ? DateTimeFormatUtil.elapsedBetween(createdAtRaw, updatedAtRaw) : "-");
                    row.add(extractValue(ticketBlock, "resolution"));

                    boolean autoClosed = cycles.stream().anyMatch(c ->
                            "Closed".equals(c[2]) && "Auto-closed after 3 days".equals(c[3]));
                    row.add(autoClosed ? "Yes" : "No");

                    rows.add(row);
                }
                return rows;
            }
        };
        task.setOnSucceeded(e -> {
            List<String> headers = new ArrayList<>(List.of(
                    "Ticket No", "Title", "Category", "Priority", "Status", "Reporter",
                    "Department", "Created Date/Time", "Assigned To"));
            for (int i = 1; i <= 5; i++) {
                headers.add("Resolved " + i + " At");
                headers.add("Resolution " + i + " Text");
                headers.add("Closed/Denied " + i + " At");
            }
            headers.add("Final Closed Date/Time");
            headers.add("Total Time Open");
            headers.add("Resolution");
            headers.add("Auto-Closed");
            ExcelExporter.export("Tickets", headers, task.getValue());
        });
        task.setOnFailed(e -> showAlert("Error", "Export failed: " + task.getException().getMessage()));
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }


    // ── Small JSON helpers for the nested export payload ──────
    private List<String> splitTopLevelObjects(String arrayBody) {
        List<String> result = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < arrayBody.length(); i++) {
            char c = arrayBody.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0) result.add(arrayBody.substring(start, i + 1)); }
        }
        return result;
    }

    private String extractNestedObject(String json, String key) {
        String search = "\"" + key + "\":{";
        int start = json.indexOf(search);
        if (start == -1) return "{}";
        start += search.length() - 1;
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '{') depth++;
            else if (json.charAt(i) == '}') { depth--; if (depth == 0) return json.substring(start, i + 1); }
        }
        return "{}";
    }

    private String extractNestedArray(String json, String key) {
        String search = "\"" + key + "\":[";
        int start = json.indexOf(search);
        if (start == -1) return "[]";
        start += search.length() - 1;
        int depth = 0;
        for (int i = start; i < json.length(); i++) {
            if (json.charAt(i) == '[') depth++;
            else if (json.charAt(i) == ']') { depth--; if (depth == 0) return json.substring(start, i + 1); }
        }
        return "[]";
    }

    private List<String[]> parseCycles(String cyclesArrayJson) {
        List<String[]> result = new ArrayList<>();
        String body = cyclesArrayJson.trim();
        if (body.length() < 2) return result;
        body = body.substring(1, body.length() - 1).trim();
        if (body.isEmpty()) return result;
        for (String obj : body.split("\\},\\{")) {
            String cleaned = obj.replace("{", "").replace("}", "");
            result.add(new String[]{
                    extractValue(cleaned, "resolvedAt"),
                    extractValue(cleaned, "closedOrDeniedAt"),
                    extractValue(cleaned, "outcome"),
                    extractValue(cleaned, "reason"),
                    extractValue(cleaned, "resolutionText")
            });
        }
        return result;
    }

    // ── Load engineer tickets ─────────────────────────────────
    private void loadEngineerTickets(int userId,
                                     TableView<Ticket> table,
                                     Label openCount,
                                     Label inProgCount,
                                     Label resolvedCount) {
        // 1. Pre-Task UI Setup
        table.getItems().clear();
        allTickets.clear();
        LoadingUtil.setLoading(table, "Loading your tickets...");

        // 2. Background Task
        Task<List<Ticket>> task = new Task<>() {
            @Override
            protected List<Ticket> call() throws Exception {
                List<Ticket> result = new ArrayList<>();


                // Determine URL based on permissions
                String url;
                if (PermissionManager.canViewAllTickets()) {
                    if (selectedDeptId != null && selectedDeptId > 0) {
                        url = ConfigManager.getBaseUrl() + "/api/tickets/department/" + selectedDeptId;
                    } else {
                        url = ConfigManager.getBaseUrl() + "/api/tickets";
                    }
                } else {
                    if (selectedDeptId != null && selectedDeptId > 0) {
                        url = ConfigManager.getBaseUrl() + "/api/tickets/assignee/" + userId + "/department/" + selectedDeptId;
                    } else {
                        url = ConfigManager.getBaseUrl() + "/api/tickets/assignee/" + userId;
                    }
                }

                HttpResponse<String> resp = ApiClient.get(url);
                String body = resp.body().trim();

                // Handle empty arrays securely
                if (body.equals("[]") || body.isEmpty()) {
                    return result;
                }

                // Strip leading '[' and trailing ']'
                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    // Split JSON objects correctly and safely
                    String[] jsonObjects = body.split("\\},\\s*\\{");
                    for (String obj : jsonObjects) {
                        // Clean curly braces safely
                        String cleanedObj = obj.replace("{", "").replace("}", "");

                        Ticket t = new Ticket(
                                extractInt(cleanedObj, "id"),
                                extractValue(cleanedObj, "ticketNo"),
                                extractValue(cleanedObj, "title"),
                                extractValue(cleanedObj, "description"),
                                extractValue(cleanedObj, "category"),
                                extractValue(cleanedObj, "priority"),
                                extractValue(cleanedObj, "status"),
                                extractInt(cleanedObj, "assignedTo"),
                                extractInt(cleanedObj, "reportedBy"),
                                extractInt(cleanedObj, "assetId"),
                                extractValue(cleanedObj, "createdAt"),
                                extractValue(cleanedObj, "updatedAt"),
                                extractValue(cleanedObj, "department"),
                                extractValue(cleanedObj,"reason"),
                                extractValue(cleanedObj,"requestedAt")
                        );
                        result.add(t);
                    }
                }
                return result;
            }
        };

        // 3. Success Callback (Runs on JavaFX Application Thread)
        task.setOnSucceeded(e -> {
            List<Ticket> tickets = task.getValue();

            table.getItems().clear();
            allTickets.clear();
            table.getItems().addAll(tickets);
            allTickets.addAll(tickets);
            // Calculate and update stats
            int open = 0, inProg = 0, resolved = 0;
            for (Ticket t : tickets) {
                switch (t.getStatus()) {
                    case "Open"        -> open++;
                    case "In Progress" -> inProg++;
                    case "Resolved",
                         "Closed"      -> resolved++;
                }
            }

            openCount.setText(String.valueOf(open));
            inProgCount.setText(String.valueOf(inProg));
            resolvedCount.setText(String.valueOf(resolved));

            // Handle empty state
            if (tickets.isEmpty()) {
                LoadingUtil.setEmpty(table, "✉", "No tickets assigned", "You have no tickets assigned at the moment.");
            }
        });

        // 4. Failure Callback (Runs on JavaFX Application Thread)
        task.setOnFailed(e -> {
            LoadingUtil.setEmpty(table, "⚠", "Could not load tickets", "Check server connection and try again.");
            task.getException().printStackTrace(); // Helps with debugging
        });

        // 5. Daemon Thread Execution
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ── Open detail panel ─────────────────────────────────
    private void openDetailPanel(Ticket ticket) {
        detailPanel.getChildren().clear();
        detailPanel.setVisible(true);
        detailPanel.setManaged(true);
        AnimationUtil.slideInRight(detailPanel);


        // ── Header ────────────────────────────────────────
        Label ticketNoLabel = new Label(ticket.getTicketNo());
        ticketNoLabel.setStyle(
                "-fx-text-fill: #58a6ff; -fx-font-size: 12px; -fx-font-weight: bold;");
        ticketNoLabel.getStyleClass().add("data-mono");

        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #8b949e;" +
                        "-fx-font-size: 14px; -fx-cursor: hand; -fx-border-width: 0;");
        closeBtn.setOnAction(e -> {
            detailPanel.setVisible(false);
            detailPanel.setManaged(false);
        });

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        HBox header = new HBox(ticketNoLabel, headerSpacer);
        if (PermissionManager.canUpdateTicketStatus()) {
            Button editTicketBtn = new Button("✏ Edit");
            editTicketBtn.getStyleClass().add("btn-warning");
            editTicketBtn.setStyle("-fx-padding: 5 12 5 12;");
            editTicketBtn.setOnAction(e -> showEditTicketDialog(ticket, table));
            header.getChildren().add(editTicketBtn);
        }
        header.getChildren().add(closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 12, 8, 16));
        header.getStyleClass().add("top-bar");
        // ── Title ─────────────────────────────────────────
        Label titleLabel = new Label(ticket.getTitle());
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);

        // ── Status + Priority badges ──────────────────────
        Label statusBadge = new Label(ticket.getStatus());
        statusBadge.setPadding(new Insets(3, 10, 3, 10));
        statusBadge.setStyle(statusBadgeStyle(ticket.getStatus()));

        Label priorityBadge = new Label(ticket.getPriority());
        priorityBadge.setPadding(new Insets(3, 10, 3, 10));
        priorityBadge.setStyle(priorityBadgeStyle(ticket.getPriority()));

        HBox badges = new HBox(8, statusBadge, priorityBadge);

        // ── Info grid ─────────────────────────────────────
        String reporterName = empMap.getOrDefault(ticket.getReportedBy(),
                userMap.getOrDefault(ticket.getReportedBy(), "Unknown #" + ticket.getReportedBy()));

        System.out.println("deny:"+ticket.getReason());
        VBox infoBox = new VBox(6,
                infoRow("Reported By", reporterName),
                infoRow("Department",ticket.getDepartment()),
                infoRow("System no", ticket.getAssetId() == 0
                        ? "No Asset"
                        : assetMap.getOrDefault(ticket.getAssetId(), "Asset #" + ticket.getAssetId())),
                infoRow("Description",ticket.getDescrption()),
                infoRow("Assigned To", ticket.getAssignedTo() == 0
                        ? "Unassigned"
                        : userMap.getOrDefault(ticket.getAssignedTo(),
                        "User #" + ticket.getAssignedTo())),
                infoRow("Category", ticket.getCategory()),
                infoRow("Created", DateTimeFormatUtil.toIndianDateTime(ticket.getCreatedAt())),
                infoRow("Closed", "Closed".equals(ticket.getStatus())
                        ? DateTimeFormatUtil.toIndianDateTime(ticket.getUpdatedAt())
                        : "-"),
                infoRow("Total Time Open", "Closed".equals(ticket.getStatus())
                        ? DateTimeFormatUtil.elapsedBetween(ticket.getCreatedAt(), ticket.getUpdatedAt())
                        : "Still open"),
                infoRow("Requested at:",DateTimeFormatUtil.toIndianDateTime(ticket.getRequestedAt())),
                infoRow("Reason for Deny",ticket.getReason())
        );
        infoBox.getStyleClass().add("settings-card");
        infoBox.setStyle("-fx-padding: 10; -fx-background-radius: 6;");
        infoBox.setPadding(new Insets(10));

        // ── Description ───────────────────────────────────
        Label descTitle = new Label("Description");
        descTitle.getStyleClass().add("text-muted");
        descTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");
        Label descLabel = new Label(
                ticket.getTitle()); // using title as placeholder — description not in client model
        descLabel.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");
        descLabel.setWrapText(true);


        // ── Separator ─────────────────────────────────────
        Separator sep = new Separator();

        // ── Comments section ──────────────────────────────
        Label commentsTitle = new Label("Comments");
        commentsTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        final VBox commentsFeed = new VBox(8);
        ScrollPane commentsScroll = new ScrollPane(commentsFeed);
        commentsScroll.setFitToWidth(true);
        commentsScroll.setPrefHeight(200);
        commentsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(commentsScroll, Priority.ALWAYS);

        // ── Load comments ─────────────────────────────────
        loadComments(ticket.getId(), commentsFeed);

        // ── Add comment ───────────────────────────────────
        TextArea commentInput = new TextArea();
        commentInput.setPromptText("Write a comment...");
        commentInput.setPrefRowCount(2);
        commentInput.setWrapText(true);
        commentInput.setStyle("-fx-border-radius: 6;");

        Button postBtn = new Button("Post Comment");
        postBtn.getStyleClass().add("btn-primary");
        postBtn.setStyle("-fx-padding: 6 14 6 14;");
        postBtn.setOnAction(e -> {
            String text = commentInput.getText().trim();
            if (text.isEmpty()) return;
            postComment(ticket.getId(), text, commentsFeed);
            commentInput.clear();
        });

        VBox commentInputBox = new VBox(6, commentInput, postBtn);
        commentInputBox.setPadding(new Insets(0, 0, 8, 0));

        Label historyTitle = new Label("Status Cycle History");
        historyTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        VBox historyBox = new VBox(8);
        loadStatusCycles(ticket.getId(), historyBox);

        // ── Assemble detail panel ─────────────────────────
        VBox content = new VBox(12,
                titleLabel, badges, infoBox,
                historyTitle, historyBox,
                sep, commentsTitle, commentsScroll, commentInputBox);
        content.setPadding(new Insets(12, 16, 12, 16));
        VBox.setVgrow(commentsScroll, Priority.ALWAYS);

        ScrollPane contentScroll = new ScrollPane(content);
        contentScroll.setFitToWidth(true);
        contentScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(contentScroll, Priority.ALWAYS);

        detailPanel.getChildren().addAll(header, contentScroll);
        VBox.setVgrow(contentScroll, Priority.ALWAYS);

        // ── Auto-refresh comments every 10 seconds ────────────
        javafx.animation.Timeline commentRefresh =
                new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(
                                javafx.util.Duration.seconds(10),
                                ev -> loadComments(ticket.getId(),
                                        commentsFeed)));
        commentRefresh.setCycleCount(
                javafx.animation.Timeline.INDEFINITE);
        commentRefresh.play();

// Stop when panel closes
        closeBtn.setOnAction(e -> {
            commentRefresh.stop();
            detailPanel.setVisible(false);
            detailPanel.setManaged(false);
        });

        // ── Resolution (shown prominently when resolved) ──────
        if ("Resolved".equals(ticket.getStatus())
                || "Closed".equals(ticket.getStatus())) {
            // Load resolution from server
            String resolution = loadResolution(ticket.getId());
            if (resolution != null && !resolution.isEmpty()) {
                Label resTitle = new Label("✅ Resolution");
                resTitle.setStyle(
                        "-fx-text-fill: #3fb950;" +
                                "-fx-font-size: 13px;" +
                                "-fx-font-weight: bold;");
                Label resLabel = new Label(resolution);
                resLabel.setStyle("-fx-font-size: 13px;");
                resLabel.setWrapText(true);
                Label resDate = new Label(
                        "Resolved: " + DateTimeFormatUtil.toIndianDateTime(ticket.getUpdatedAt()));
                resDate.getStyleClass().add("text-muted");
                resDate.setStyle("-fx-font-size: 11px;");

                VBox resCard = new VBox(6,
                        resTitle, resLabel, resDate);
                resCard.getStyleClass().add("surface-card-success");
                resCard.setStyle("-fx-background-radius: 8; -fx-border-width: 0 0 0 3; -fx-border-radius: 8; -fx-padding: 12;");
                content.getChildren().add(3, resCard);
            }
        }
    }

    private void loadStatusCycles(int ticketId, VBox container) {
        container.getChildren().clear();
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                HttpResponse<String> resp = ApiClient.get(
                        ConfigManager.getBaseUrl() + "/api/tickets/" + ticketId + "/history");
                String body = resp.body();
                List<String[]> cycles = new ArrayList<>();
                int idx = body.indexOf("\"cycles\":[");
                if (idx == -1) return Map.of("cycles", cycles);
                String cyclesBlock = body.substring(idx);
                int end = cyclesBlock.lastIndexOf(']');
                cyclesBlock = cyclesBlock.substring(10, end); // strip "cycles":[ ... ]
                if (!cyclesBlock.trim().isEmpty()) {
                    for (String obj : cyclesBlock.split("\\},\\{")) {
                        String cleaned = obj.replace("{", "").replace("}", "");
                        cycles.add(new String[]{
                                extractValue(cleaned, "resolvedAt"),
                                extractValue(cleaned, "closedOrDeniedAt"),
                                extractValue(cleaned, "outcome"),
                                extractValue(cleaned, "reason"),
                                extractValue(cleaned, "resolutionText")
                        });
                    }
                }
                return Map.of("cycles", cycles);
            }
        };
        task.setOnSucceeded(e -> {
            @SuppressWarnings("unchecked")
            List<String[]> cycles = (List<String[]>) task.getValue().get("cycles");
            if (cycles.isEmpty()) {
                Label none = new Label("No resolve/deny cycles yet.");
                none.getStyleClass().add("text-muted");
                none.setStyle("-fx-font-size: 11px;");
                container.getChildren().add(none);
                return;
            }
            int cycleNum = 1;
            for (String[] c : cycles) {
                String outcome = c[2];
                String color = "Closed".equals(outcome) ? "#3fb950" : "#d29922";
                Label header = new Label("Cycle " + cycleNum + " — " + outcome);
                header.getStyleClass().add("top-bar");
                Label resolved = new Label("Resolved: " + DateTimeFormatUtil.toIndianDateTime(c[0]));
                resolved.setStyle("-fx-font-size: 11px;");
                Label resolutionText = new Label("Resolution: " + (c.length > 4 && !c[4].isEmpty() ? c[4] : "-"));
                resolutionText.setStyle("-fx-font-size: 11px;");
                resolutionText.setWrapText(true);
                Label closed = new Label(("Closed".equals(outcome) ? "Closed: " : "Denied/Reopened: ")
                        + DateTimeFormatUtil.toIndianDateTime(c[1])
                        + (c[3] != null && !c[3].isEmpty() ? " (Reason: " + c[3] + ")" : ""));
                closed.setStyle("-fx-font-size: 11px;");
                closed.setWrapText(true);
                VBox cycleBox = new VBox(2, header, resolved, resolutionText, closed);
                cycleBox.getStyleClass().add("surface-card");
                cycleBox.setStyle("-fx-background-radius: 6; -fx-padding: 8;");
                container.getChildren().add(cycleBox);
                cycleNum++;
            }
        });
        task.setOnFailed(e -> {
            Label err = new Label("Could not load status history.");
            err.setStyle("-fx-text-fill: #f85149; -fx-font-size: 11px;");
            container.getChildren().add(err);
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void showEditTicketDialog(Ticket ticket,
                                      TableView<Ticket> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Edit Ticket");
        dialog.setHeaderText(ticket.getTicketNo());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField titleField = new TextField(
                ticket.getTitle());

        ComboBox<String> categoryBox = new ComboBox<>();
        categoryBox.getItems().addAll(
                "Hardware", "Software", "SAP","CCTV","Printer","Drive","Zoho Mail","Adobe acrobat","IVMS",
                "Network", "General","other");
        categoryBox.setValue(ticket.getCategory());

        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll(
                "Low", "Medium", "High", "Critical");
        priorityBox.setValue(ticket.getPriority());

        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.getItems().addAll(
                "Open", "In Progress", "Resolved", "Closed");
        statusBox.setValue(ticket.getStatus());

        TextField resolutionField = new TextField();
        // Load resolution
        try {
            HttpResponse<String> resp = ApiClient.get(
                    ConfigManager.getBaseUrl() + "/api/tickets/" + ticket.getId());
            resolutionField.setText(
                    extractValue(resp.body(), "resolution"));
        } catch (Exception ignored) {}

        TextField assetIdField = new TextField();
        assetIdField.setPromptText("Asset ID (optional)");

        Label errorLabel = new Label("");
        errorLabel.setStyle(
                "-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        int r = 0;
        grid.add(new Label("Title *:"),      0, r);
        grid.add(titleField,                 1, r++);
        grid.add(new Label("Category:"),     0, r);
        grid.add(categoryBox,                1, r++);
        grid.add(new Label("Priority:"),     0, r);
        grid.add(priorityBox,                1, r++);
        grid.add(new Label("Status:"),       0, r);
        grid.add(statusBox,                  1, r++);
        grid.add(new Label("Resolution:"),   0, r);
        grid.add(resolutionField,            1, r++);
        grid.add(new Label("Asset ID:"),     0, r);
        grid.add(assetIdField,               1, r++);
        grid.add(errorLabel,                 1, r);
        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane()
                .lookupButton(ButtonType.OK);
        okBtn.setDisable(titleField.getText().trim().isEmpty());
        titleField.textProperty().addListener((o, ov, nv) ->
                okBtn.setDisable(nv.trim().isEmpty()));

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent()
                && result.get() == ButtonType.OK) {
            try {
                // Update status + resolution
                if (!statusBox.getValue()
                        .equals(ticket.getStatus())
                        || !resolutionField.getText().isEmpty()) {
                    updateTicketStatus(ticket.getId(),
                            statusBox.getValue(),
                            resolutionField.getText());
                }
                loadTickets(table,openCount,inProgCount,resolvedCount);
                // Close detail panel
                detailPanel.setVisible(false);
                detailPanel.setManaged(false);
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        }
    }

    private String loadResolution(int ticketId) {
        try {
            HttpResponse<String> resp = ApiClient.get(
                    ConfigManager.getBaseUrl() + "/api/tickets/" + ticketId);
            return extractValue(resp.body(), "resolution");
        } catch (Exception e) { return ""; }
    }

    // ── Load comments from API ────────────────────────────
    private void loadComments(int ticketId, VBox feed) {
        feed.getChildren().clear();
        try {
            HttpResponse<String> resp = ApiClient.get(
                    ConfigManager.getBaseUrl() + "/api/tickets/" + ticketId + "/comments");
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);

            if (body.isEmpty()) {
                Label empty = new Label("No comments yet. Be the first to comment.");
                empty.getStyleClass().add("text-muted");
                empty.setStyle("-fx-font-size: 12px;");
                feed.getChildren().add(empty);
                return;
            }

            for (String obj : body.split("\\},\\{")) {
                obj = obj.replace("{", "").replace("}", "");
                String name    = extractValue(obj, "addedByName");
                String comment = extractValue(obj, "comment");
                String time    = extractValue(obj, "addedAt");
                int addedBy    = extractInt(obj, "addedBy");

                boolean isMe = addedBy == SessionManager.get().getUserId();
                feed.getChildren().add(commentBubble(name, comment, time, isMe));
            }
        } catch (Exception ex) {
            Label err = new Label("Error loading comments.");
            err.setStyle("-fx-text-fill: #f85149; -fx-font-size: 11px;");
            feed.getChildren().add(err);
        }
    }

    // ── Post comment ──────────────────────────────────────
    private void postComment(int ticketId, String text, VBox feed) {
        try {
            String sanitized = text.replace("\\", "\\\\")
                    .replace("\"", "'")
                    .replace("\n", " ")
                    .replace("\r", "");
            String body = "{" +
                    "\"comment\":\"" + sanitized + "\"," +
                    "\"addedBy\":\"" + SessionManager.get().getUserId() + "\"" +
                    "}";
            HttpResponse<String> resp = ApiClient.post(
                    ConfigManager.getBaseUrl() + "/api/tickets/" + ticketId + "/comments", body);
            if (resp.statusCode() == 201) {
                loadComments(ticketId, feed);
            }
        } catch (Exception ex) {
            System.out.println("Error posting comment: " + ex.getMessage());
        }
    }

    // ── Comment bubble ────────────────────────────────────
    private VBox commentBubble(String name, String comment,
                               String time, boolean isMe) {
        Label nameLabel = new Label(name != null && !name.isEmpty()
                ? name : "Unknown");
        nameLabel.setStyle(
                "-fx-text-fill: " + (isMe ? "#3fb950" : "#58a6ff") + ";" +
                        "-fx-font-size: 11px; -fx-font-weight: bold;");

        Label commentLabel = new Label(comment);
        commentLabel.setStyle("-fx-font-size: 12px;");
        commentLabel.setWrapText(true);

        Label timeLabel = new Label(time != null && time.length() >= 16
                ? time.substring(0, 16) : time);
        timeLabel.getStyleClass().add("text-muted");
        timeLabel.setStyle("-fx-font-size: 10px;");

        VBox bubble = new VBox(3, nameLabel, commentLabel, timeLabel);
        bubble.setPadding(new Insets(8, 10, 8, 10));
        bubble.getStyleClass().add(isMe ? "bubble-mine" : "bubble-theirs");
        bubble.setStyle("-fx-background-radius: 6;");
        bubble.setMaxWidth(340);

        if (isMe) {
            bubble.setAlignment(Pos.CENTER_RIGHT);
        }
        return bubble;
    }

    // ── Info row helper ───────────────────────────────────
    private HBox infoRow(String label, String value) {
        Label k = new Label(label + ":");
        k.getStyleClass().add("text-muted");
        k.setStyle("-fx-font-size: 11px; -fx-min-width: 100;");
        Label v = new Label(value != null ? value : "-");
        v.setStyle("-fx-font-size: 12px;");
        v.setWrapText(true);
        v.setMaxWidth(360);
        return new HBox(8, k, v);
    }

    // ── Badge styles ──────────────────────────────────────
    private String statusBadgeStyle(String status) {
        boolean light = ThemeManager.getCurrent() == ThemeManager.Theme.LIGHT;
        String bg = light ? switch (status) {
            case "Open" -> "#fdecea";
            case "In Progress" -> "#eaf3fb";
            case "Resolved" -> "#eafaf1";
            case "Closed" -> "#eceff1";
            default -> "#eceff1";
        } : switch (status) {
            case "Open" -> "#3d1f1e";
            case "In Progress" -> "#1a2840";
            case "Resolved" -> "#1b2d1f";
            case "Closed" -> "#21262d";
            default -> "#21262d";
        };
        String fg = light ? switch (status) {
            case "Open" -> "#c0392b";
            case "In Progress" -> "#2980b9";
            case "Resolved" -> "#27ae60";
            case "Closed" -> "#5B6B7D";
            default -> "#5B6B7D";
        } : switch (status) {
            case "Open" -> "#f85149";
            case "In Progress" -> "#58a6ff";
            case "Resolved" -> "#3fb950";
            case "Closed" -> "#8b949e";
            default -> "#c9d1d9";
        };
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
                "-fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;";
    }

    private String priorityBadgeStyle(String priority) {
        boolean light = ThemeManager.getCurrent() == ThemeManager.Theme.LIGHT;
        String bg = light ? switch (priority.toUpperCase()) {
            case "CRITICAL" -> "#fdecea";
            case "HIGH" -> "#fdf2e3";
            case "MEDIUM" -> "#eaf3fb";
            case "LOW" -> "#eceff1";
            default -> "#eceff1";
        } : switch (priority.toUpperCase()) {
            case "CRITICAL" -> "#3d1f1e";
            case "HIGH" -> "#2d2008";
            case "MEDIUM" -> "#1a2840";
            case "LOW" -> "#21262d";
            default -> "#21262d";
        };
        String fg = light ? switch (priority.toUpperCase()) {
            case "CRITICAL" -> "#c0392b";
            case "HIGH" -> "#b3650f";
            case "MEDIUM" -> "#2980b9";
            case "LOW" -> "#5B6B7D";
            default -> "#5B6B7D";
        } : switch (priority.toUpperCase()) {
            case "CRITICAL" -> "#f85149";
            case "HIGH" -> "#d29922";
            case "MEDIUM" -> "#58a6ff";
            case "LOW" -> "#6e7681";
            default -> "#c9d1d9";
        };
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
                "-fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;";
    }

    // ── Load users into map ───────────────────────────────
    private void loadUsers() {
        try {
            HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/users/all");
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    int id = extractInt(obj, "id");
                    String name = extractValue(obj, "fullName");
                    boolean active = extractInt(obj, "active") != 0;
                    userMap.put(id, active ? name : name + " (Inactive)");
                    if (active) activeUserMap.put(id, name);
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading users: " + ex.getMessage());
        }
    }
    // ── Load employees into map ───────────────────────────────
    private void loademp() {
        try {
            HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/employees");
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    empMap.put(extractInt(obj, "id"), extractValue(obj, "name"));
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading users: " + ex.getMessage());
        }
    }
    private void loadAssets() {
        try {
            HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/assets");
            String body = resp.body().trim();
            body = body.substring(1, body.length() - 1);
            if (!body.isEmpty()) {
                for (String obj : body.split("\\},\\{")) {
                    obj = obj.replace("{", "").replace("}", "");
                    String tag = extractValue(obj, "assetTag");
                    String name = extractValue(obj, "name");
                    assetMap.put(extractInt(obj, "id"), tag + " — " + name);
                }
            }
        } catch (Exception ex) {
            System.out.println("Error loading assets: " + ex.getMessage());
        }
    }
    // ── Load departments into map and update ComboBox ────────────────────
    private void loadDepartments(ComboBox<String> deptFilterBox) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/departments");
                String body = resp.body().trim();

                deptMap.clear();
                deptMap.put(0, "All Departments"); // Default fallback option

                if (body.equals("[]") || body.isEmpty()) return null;

                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    for (String obj : body.split("\\},\\s*\\{")) {
                        obj = obj.replace("{", "").replace("}", "");
                        int id = extractInt(obj, "id");
                        // Assuming your department JSON uses "name" or "departmentName"
                        String name = extractValue(obj, "name");
                        if (name.isEmpty()) name = extractValue(obj, "departmentName");
                        deptMap.put(id, name);
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            deptFilterBox.getItems().clear();
            deptFilterBox.getItems().addAll(deptMap.values());
            deptFilterBox.getSelectionModel().select("All Departments");
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void loadTickets(TableView<Ticket> table,Label openCount,
                             Label inProgCount,
                             Label resolvedCount) {
        table.getItems().clear();
        allTickets.clear();
        LoadingUtil.setLoading(table, "Loading tickets...");
        Task<List<Ticket>> task = new Task<>() {
            @Override
            protected List<Ticket> call() throws Exception {
                List<Ticket> result = new ArrayList<>();

                String url;
                if (SessionManager.get().isEngineer() && !PermissionManager.canViewAllTickets()) {
                    if (selectedDeptId != null && selectedDeptId > 0) {
                        url = ConfigManager.getBaseUrl() + "/api/tickets/assignee/" + SessionManager.get().getUserId() + "/department/" + selectedDeptId;
                    } else {
                        url = ConfigManager.getBaseUrl() + "/api/tickets/assignee/" + SessionManager.get().getUserId();
                    }
                } else if (SessionManager.get().isDeptHod()) {
                    url = ConfigManager.getBaseUrl() + "/api/tickets/department/" + SessionManager.get().getDeptId();
                } else {
                    if (selectedDeptId != null && selectedDeptId > 0) {
                        url = ConfigManager.getBaseUrl() + "/api/tickets/department/" + selectedDeptId;
                    } else {
                        url = ConfigManager.getBaseUrl() + "/api/tickets";
                    }
                }

                HttpResponse<String> resp = ApiClient.get(url);
                String body = resp.body().trim();

                // Handle empty arrays securely
                if (body.equals("[]") || body.isEmpty()) {
                    return result;
                }

                // Strip leading '[' and trailing ']'
                if (body.startsWith("[")) body = body.substring(1);
                if (body.endsWith("]")) body = body.substring(0, body.length() - 1);
                body = body.trim();

                if (!body.isEmpty()) {
                    // Split JSON objects correctly
                    String[] jsonObjects = body.split("\\},\\s*\\{");
                    for (String obj : jsonObjects) {
                        // Clean curly braces safely
                        String cleanedObj = obj.replace("{", "").replace("}", "");

                        Ticket t = new Ticket(
                                extractInt(cleanedObj, "id"),
                                extractValue(cleanedObj, "ticketNo"),
                                extractValue(cleanedObj, "title"),
                                extractValue(cleanedObj, "description"),
                                extractValue(cleanedObj, "category"),
                                extractValue(cleanedObj, "priority"),
                                extractValue(cleanedObj, "status"),
                                extractInt(cleanedObj, "assignedTo"),
                                extractInt(cleanedObj, "reportedBy"),
                                extractInt(cleanedObj, "assetId"),
                                extractValue(cleanedObj, "createdAt"),
                                //DateTimeFormatUtil.toIndianDateTime(extractValue(cleanedObj,"createdAt")),
                                extractValue(cleanedObj, "updatedAt"),
                                extractValue(cleanedObj, "department"),
                                extractValue(cleanedObj,"reason"),
                                extractValue(cleanedObj,"requestedAt")
                        );
                        result.add(t);
                    }
                }
                return result;
            }
        };

        task.setOnSucceeded(e -> {
            List<Ticket> tickets = task.getValue();
            // Update UI collections on the JavaFX Application Thread

            table.getItems().clear();
            allTickets.clear();
            table.getItems().addAll(tickets);
            allTickets.addAll(tickets);

            // Calculate and update stats
            int open = 0, inProg = 0, resolved = 0;
            for (Ticket t : tickets) {
                switch (t.getStatus()) {
                    case "Open"        -> open++;
                    case "In Progress" -> inProg++;
                    case "Resolved",
                         "Closed"      -> resolved++;
                }
            }

            openCount.setText(String.valueOf(open));
            inProgCount.setText(String.valueOf(inProg));
            resolvedCount.setText(String.valueOf(resolved));

            // Remove loading state if needed
            if (tickets.isEmpty()) {
                LoadingUtil.setEmpty(table, "ℹ", "No tickets found", "There are no tickets assigned to you.");
            }
        });

        task.setOnFailed(e -> {
            // Task callbacks run automatically on the JavaFX Thread, Platform.runLater is redundant here
            LoadingUtil.setEmpty(table, "⚠", "Could not load tickets", "Check server connection and try again.");
            task.getException().printStackTrace(); // Helps with debugging
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public void search(String keyword) {
        if (keyword.isEmpty()) {
            table.getItems().setAll(allTickets);
            return;
        }
        table.getItems().setAll(allTickets.filtered(t ->
                t.getTitle().toLowerCase().contains(keyword)
                        || t.getTicketNo().toLowerCase().contains(keyword)
                        || t.getCategory().toLowerCase().contains(keyword)
                        || t.getPriority().toLowerCase().contains(keyword)
                        || t.getStatus().toLowerCase().contains(keyword)
        ));
        if (table.getItems().isEmpty()) {
            LoadingUtil.setEmpty(table, "🔍",
                    "No results found",
                    "No tickets match \"" + keyword + "\"");
        }
    }

    private void showAddTicketDialog(TableView<Ticket> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
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
                "Hardware", "Software", "SAP","CCTV","Printer","Drive","Zoho Mail","Adobe acrobat","IVMS",
                "Network", "General","other");
        categoryBox.setValue("Hardware");

        ComboBox<String> priorityBox = new ComboBox<>();
        priorityBox.getItems().addAll("Low", "Medium", "High", "Critical");
        priorityBox.setValue("Medium");

        Label reportedByLabel = new Label(
                "Reported by: " + SessionManager.get().getFullName());
        reportedByLabel.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 11px;");

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(150);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelCol, valueCol);
        grid.add(new Label("Title *:"),     0, 0); grid.add(titleField,  1, 0);
        grid.add(new Label("Description:"), 0, 1); grid.add(descField,   1, 1);
        grid.add(new Label("Category:"),    0, 2); grid.add(categoryBox, 1, 2);
        grid.add(new Label("Priority:"),    0, 3); grid.add(priorityBox, 1, 3);
        grid.add(reportedByLabel,           1, 4);
        grid.add(errorLabel,                1, 5);
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
                String t = titleField.getText().trim()
                        .replace("\\", "\\\\").replace("\"", "'")
                        .replace("\n", " ").replace("\r", "");
                String d = descField.getText().trim()
                        .replace("\\", "\\\\").replace("\"", "'")
                        .replace("\n", " ").replace("\r", "");
                String body = "{\"title\":\"" + t + "\",\"description\":\"" + d
                        + "\",\"category\":\"" + categoryBox.getValue()
                        + "\",\"priority\":\"" + priorityBox.getValue()
                        + "\",\"reportedBy\":" + reportedBy + "}";
                HttpResponse<String> resp = ApiClient.post(ConfigManager.getBaseUrl() + "/api/tickets", body);
                if (resp.statusCode() == 201) {
                    ToastUtil.success("Ticket created.");
                    loadTickets(table,openCount,inProgCount,resolvedCount);
                } else {
                    showAlert("Error", "Server returned: " + resp.statusCode());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private void showUpdateStatusDialog(Ticket ticket, TableView<Ticket> table) {
        if (ticket.getAssignedTo() <= 0) {
            showAlert("Not Assigned",
                    "This ticket must be assigned to an engineer before its status can be changed.\n"
                            + "Use the Assign action first.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Update Ticket Status");
        dialog.setHeaderText("Ticket: " + ticket.getTicketNo());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

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
                loadTickets(table,openCount,inProgCount,resolvedCount);
        }
    }

    private void showAssignDialog(Ticket ticket, TableView<Ticket> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Assign Ticket");
        dialog.setHeaderText("Ticket: " + ticket.getTicketNo());
        dialog.getDialogPane().getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> userBox = new ComboBox<>();
        ObservableList<String> userNames = FXCollections.observableArrayList();
        Map<String, Integer> nameToId = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : activeUserMap.entrySet()) {
            String display = entry.getValue() + " (#" + entry.getKey() + ")";
            userNames.add(display);
            nameToId.put(display, entry.getKey());
        }
        userBox.setItems(userNames);
        if (!userNames.isEmpty()) userBox.setValue(userNames.get(0));
        if (ticket.getAssignedTo() != 0) {
            String current = userMap.get(ticket.getAssignedTo());
            if (current != null)
                userBox.setValue(current + " (#" + ticket.getAssignedTo() + ")");
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
                if (assignTicket(ticket.getId(), userId)) loadTickets(table,openCount,inProgCount,resolvedCount);
            }
        }
    }

    private boolean updateTicketStatus(int id, String status, String resolution) {
        try {
            String url = ConfigManager.getBaseUrl() + "/api/tickets/" + id
                    + "/status?status="
                    + URLEncoder.encode(status, StandardCharsets.UTF_8)
                    + "&resolution="
                    + URLEncoder.encode(resolution, StandardCharsets.UTF_8);
            HttpResponse<String> resp = ApiClient.putNoBody(url);
            if (resp.statusCode() == 200) {
                ToastUtil.success("Status updated to: " + status);
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
            String url = ConfigManager.getBaseUrl() + "/api/tickets/"
                    + ticketId + "/assign?userId=" + userId;
            HttpResponse<String> resp = ApiClient.putNoBody(url);
            if (resp.statusCode() == 200) {
                ToastUtil.success("Ticket assigned to: "
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
        ThemeManager.applyToDialog(alert);
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