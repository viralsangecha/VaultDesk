package com.vaultdesk.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class DashboardView {
    private String fullName;
    private String role;
    private Button activeBtn = null;
    private VBox contentArea;
    private String currentView = "dashboard";

    private Button btnAssets;
    private Button btnTickets;
    private Button btnLicenses;
    private Button btnEmployees;
    private Button btnDashboard;

    private AssetView currentAssetView       = null;
    private TicketView currentTicketView     = null;
    private EmployeeView currentEmployeeView = null;

    private TextField searchField;

    public DashboardView(String fullName, String role) {
        this.fullName = fullName;
        this.role     = role;
    }

    public Scene getScene(Stage stage) {

        // ── Sidebar header ────────────────────────────────
        Label sideTitle = new Label("VaultDesk");
        sideTitle.getStyleClass().add("sidebar-title");
        Label sideSub = new Label("IT Management");
        sideSub.getStyleClass().add("sidebar-subtitle");
        Label versionLabel = new Label(
                "v" + VersionChecker.getCurrentVersion());
        versionLabel.setStyle(
                "-fx-text-fill: #484f58; -fx-font-size: 10px;");
        VBox sideHeader = new VBox(2, sideTitle, sideSub, versionLabel);
        sideHeader.getStyleClass().add("sidebar-header");

        // ── User card ─────────────────────────────────────
        Label nameLabel = new Label(fullName);
        nameLabel.getStyleClass().add("sidebar-user-name");
        Label roleLabel = new Label(role);
        roleLabel.getStyleClass().add("sidebar-user-role");
        VBox userCard = new VBox(3, nameLabel, roleLabel);
        userCard.getStyleClass().add("sidebar-user-card");

        // ── Nav buttons ───────────────────────────────────
        btnDashboard         = sidebarBtn("⊞  Dashboard");
        Button btnTickets2   = sidebarBtn("✉  Tickets");
        btnTickets           = btnTickets2;
        Button btnAssets2    = sidebarBtn("▣  Assets");
        btnAssets            = btnAssets2;
        Button btnEmployees2 = sidebarBtn("👤  Employees");
        btnEmployees         = btnEmployees2;
        Button btnDepartments = sidebarBtn("🏢  Departments");
        btnLicenses          = sidebarBtn("🔑  Licenses");
        Button btnConsumables = sidebarBtn("📦  Consumables");
        Button btnMaintenance = sidebarBtn("🔧  Maintenance");
        Button btnVendors     = sidebarBtn("🤝  Vendors");
        Button btnReports     = sidebarBtn("📊  Reports");
        Button btnUsers       = sidebarBtn("👥  Users");
        Button btnSettings    = sidebarBtn("⚙  Settings");
        Button btnSupport     = sidebarBtn("？  Support");
        Button btnTheme       = sidebarBtn("☀  Light Mode");
        Button btnLogout = new Button("→  Logout");
        btnLogout.getStyleClass().add("sidebar-logout");

        Button btnNewAsset = new Button("＋  New Asset");
        btnNewAsset.getStyleClass().setAll("sidebar-new-asset");
        btnNewAsset.setStyle(
                "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-font-size: 13px;" +
                        "-fx-background-radius: 6; -fx-padding: 10 16 10 16;" +
                        "-fx-pref-width: 188px; -fx-cursor: hand;");
        VBox newAssetBox = new VBox(btnNewAsset);
        newAssetBox.setPadding(new Insets(16));

        // ── Sidebar ───────────────────────────────────────
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.getChildren().addAll(sideHeader, userCard, btnDashboard);

        if (SessionManager.get().isAdmin()
                || SessionManager.get().isEngineer()) {
            sidebar.getChildren().add(btnTickets);
        }

        sidebar.getChildren().add(btnAssets);
        sidebar.getChildren().add(btnEmployees);

        if (SessionManager.get().isAdmin()) {
            sidebar.getChildren().add(btnDepartments);
        }

        sidebar.getChildren().addAll(
                btnLicenses, btnConsumables,
                btnMaintenance, btnVendors);

        if (SessionManager.get().isAdmin()
                || SessionManager.get().isDeptHod()) {
            sidebar.getChildren().add(btnReports);
        }

        if (SessionManager.get().isAdmin()) {
            sidebar.getChildren().add(btnUsers);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(spacer, newAssetBox);

        if (SessionManager.get().isAdmin()) {
            sidebar.getChildren().addAll(btnSettings, btnSupport);
        }

        sidebar.getChildren().addAll(btnTheme, btnLogout);

        // ── Top bar ───────────────────────────────────────
        searchField = new TextField();
        searchField.setPromptText(
                "🔍  Search assets, tickets, or employees...");
        searchField.getStyleClass().add("search-bar");

        NotificationBell bell = new NotificationBell();
        StackPane bellView = bell.getView();

        HBox topBar = new HBox(searchField, bellView);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        stage.setOnCloseRequest(e -> bell.stopPolling());

        // ── Search listener ───────────────────────────────
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String keyword = newVal.trim().toLowerCase();
            switch (currentView) {
                case "assets"    -> searchField.setPromptText(
                        "🔍  Search by name, tag, category, brand...");
                case "tickets"   -> searchField.setPromptText(
                        "🔍  Search by title, ticket no, status...");
                case "employees" -> searchField.setPromptText(
                        "🔍  Search by name, code, email...");
                default          -> searchField.setPromptText(
                        "🔍  Go to Assets, Tickets or Employees to search");
            }
            switch (currentView) {
                case "assets" -> {
                    if (currentAssetView != null)
                        currentAssetView.search(keyword);
                }
                case "tickets" -> {
                    if (currentTicketView != null)
                        currentTicketView.search(keyword);
                }
                case "employees" -> {
                    if (currentEmployeeView != null)
                        currentEmployeeView.search(keyword);
                }
            }
        });

        // ── Content area ──────────────────────────────────
        contentArea = new VBox(10);
        contentArea.getStyleClass().add("content-area");

        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("content-scroll");
        scrollPane.setStyle(
                "-fx-background-color: #0d1117; -fx-background: #0d1117;");
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        VBox rightSide = new VBox(topBar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        HBox mainLayout = new HBox(sidebar, rightSide);
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        // ── Default view ──────────────────────────────────
        setActive(btnDashboard);
        showDashboard();

        // ── Nav actions ───────────────────────────────────
        btnDashboard.setOnAction(e -> {
            searchField.clear();
            currentView = "dashboard";
            currentAssetView = null;
            currentTicketView = null;
            currentEmployeeView = null;
            setActive(btnDashboard);
            showDashboard();
        });

        btnAssets.setOnAction(e -> {
            searchField.clear();
            setActive(btnAssets);
            currentView = "assets";
            currentTicketView = null;
            currentEmployeeView = null;
            currentAssetView = new AssetView();
            contentArea.getChildren().setAll(currentAssetView.getView());
        });

        btnTickets.setOnAction(e -> {
            searchField.clear();
            setActive(btnTickets);
            currentView = "tickets";
            currentAssetView = null;
            currentEmployeeView = null;
            currentTicketView = new TicketView();
            contentArea.getChildren().setAll(currentTicketView.getView());
        });

        btnEmployees.setOnAction(e -> {
            searchField.clear();
            setActive(btnEmployees);
            currentView = "employees";
            currentAssetView = null;
            currentTicketView = null;
            currentEmployeeView = new EmployeeView();
            contentArea.getChildren().setAll(
                    currentEmployeeView.getView());
        });

        btnDepartments.setOnAction(e -> {
            searchField.clear();
            currentView = "departments";
            currentAssetView = null;
            currentTicketView = null;
            currentEmployeeView = null;
            setActive(btnDepartments);
            contentArea.getChildren().setAll(
                    new DepartmentView().getView());
        });

        btnLicenses.setOnAction(e -> {
            searchField.clear();
            currentView = "licenses";
            currentAssetView = null;
            currentTicketView = null;
            currentEmployeeView = null;
            setActive(btnLicenses);
            contentArea.getChildren().setAll(
                    new LicenseView().getView());
        });

        btnConsumables.setOnAction(e -> {
            searchField.clear();
            currentView = "consumables";
            currentAssetView = null;
            currentTicketView = null;
            currentEmployeeView = null;
            setActive(btnConsumables);
            contentArea.getChildren().setAll(
                    new ConsumableView().getView());
        });

        btnMaintenance.setOnAction(e -> {
            searchField.clear();
            currentView = "maintenance";
            currentAssetView = null;
            currentTicketView = null;
            currentEmployeeView = null;
            setActive(btnMaintenance);
            contentArea.getChildren().setAll(
                    new MaintenanceView().getView());
        });

        btnVendors.setOnAction(e -> {
            searchField.clear();
            currentView = "vendors";
            currentAssetView = null;
            currentTicketView = null;
            currentEmployeeView = null;
            setActive(btnVendors);
            contentArea.getChildren().setAll(
                    new VendorView().getView());
        });

        btnReports.setOnAction(e -> {
            searchField.clear();
            currentView = "reports";
            currentAssetView = null;
            currentTicketView = null;
            currentEmployeeView = null;
            setActive(btnReports);
            contentArea.getChildren().setAll(
                    new ReportView().getView());
        });

        btnUsers.setOnAction(e -> {
            searchField.clear();
            currentView = "users";
            currentAssetView = null;
            currentTicketView = null;
            currentEmployeeView = null;
            setActive(btnUsers);
            contentArea.getChildren().setAll(
                    new UserManagementView().getView());
        });

        btnSettings.setOnAction(e -> {
            searchField.clear();
            currentView = "settings";
            currentAssetView = null;
            currentTicketView = null;
            currentEmployeeView = null;
            setActive(btnSettings);
            contentArea.getChildren().setAll(
                    new SettingsView().getView());
        });

        btnSupport.setOnAction(e -> {
            searchField.clear();
            currentView = "support";
            currentAssetView = null;
            currentTicketView = null;
            currentEmployeeView = null;
            setActive(btnSupport);
            contentArea.getChildren().setAll(buildSupportView());
        });

        btnNewAsset.setOnAction(e -> {
            searchField.clear();
            setActive(btnAssets);
            currentView = "assets";
            currentTicketView = null;
            currentEmployeeView = null;
            currentAssetView = new AssetView();
            contentArea.getChildren().setAll(currentAssetView.getView());
        });

        btnTheme.setOnAction(e -> {
            ThemeManager.toggle();
            Scene s = stage.getScene();
            ThemeManager.apply(s);
            btnTheme.setText(ThemeManager.getCurrent() ==
                    ThemeManager.Theme.DARK
                    ? "☀  Light Mode" : "🌙  Dark Mode");
        });

        btnLogout.setOnAction(e -> {
            bell.stopPolling();
            SessionStore.clear();
            SessionManager.get().logout();
            Scene loginScene = new LoginView().getScene(stage);
            ThemeManager.apply(loginScene);
            stage.setScene(loginScene);
        });

        Scene scene = new Scene(mainLayout, 1200, 800);
        ThemeManager.apply(scene);
        return scene;
    }

    // ── Show dashboard with navigation callback ───────────
    private void showDashboard() {
        DashboardStatsView dashView = new DashboardStatsView();
        dashView.setOnNavigate(view -> {
            searchField.clear();
            switch (view) {
                case "assets" -> {
                    setActive(btnAssets);
                    currentView = "assets";
                    currentAssetView = new AssetView();
                    currentTicketView = null;
                    currentEmployeeView = null;
                    contentArea.getChildren().setAll(
                            currentAssetView.getView());
                }
                case "tickets" -> {
                    setActive(btnTickets);
                    currentView = "tickets";
                    currentTicketView = new TicketView();
                    currentAssetView = null;
                    currentEmployeeView = null;
                    contentArea.getChildren().setAll(
                            currentTicketView.getView());
                }
                case "licenses" -> {
                    setActive(btnLicenses);
                    currentView = "licenses";
                    currentAssetView = null;
                    currentTicketView = null;
                    currentEmployeeView = null;
                    contentArea.getChildren().setAll(
                            new LicenseView().getView());
                }
                case "employees" -> {
                    setActive(btnEmployees);
                    currentView = "employees";
                    currentEmployeeView = new EmployeeView();
                    currentAssetView = null;
                    currentTicketView = null;
                    contentArea.getChildren().setAll(
                            currentEmployeeView.getView());
                }
            }
        });
        contentArea.getChildren().setAll(dashView.getView());
    }

    private Button sidebarBtn(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-btn");
        return btn;
    }

    private void setActive(Button btn) {
        if (activeBtn != null)
            activeBtn.getStyleClass().setAll("sidebar-btn");
        btn.getStyleClass().setAll("sidebar-btn-active");
        activeBtn = btn;
    }

    private VBox buildSupportView() {
        Label title = new Label("Support");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Need help? Here are your options.");
        sub.getStyleClass().add("page-subtitle");

        VBox card1 = supportCard("📧  Email Support",
                "IT Department internal support",
                "it-support@saurashtracement.com", "#58a6ff");
        VBox card2 = supportCard("📞  Phone Support",
                "Call the IT helpdesk directly",
                "Ext. 2100  |  Mon–Sat 9AM–6PM", "#3fb950");
        VBox card3 = supportCard("📋  Raise a Ticket",
                "Create a formal support ticket",
                "Go to Tickets → New Ticket", "#d29922");
        VBox card4 = supportCard("ℹ  About VaultDesk",
                "Version 1.0.0  |  Built by Viral Sangecha",
                "IT Helpdesk & Asset Management Platform", "#a371f7");

        HBox row1 = new HBox(16, card1, card2);
        HBox row2 = new HBox(16, card3, card4);
        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        return new VBox(16, title, sub, row1, row2);
    }

    private VBox supportCard(String heading, String desc,
                             String detail, String color) {
        Label h = new Label(heading);
        h.setStyle("-fx-text-fill: " + color + ";" +
                "-fx-font-size: 14px; -fx-font-weight: bold;");
        Label d = new Label(desc);
        d.getStyleClass().add("page-subtitle");
        Label dt = new Label(detail);
        dt.getStyleClass().add("section-title");
        dt.setWrapText(true);
        VBox card = new VBox(8, h, d, dt);
        card.getStyleClass().add("settings-card");
        card.setStyle("-fx-border-color: " + color + ";" +
                "-fx-border-width: 0 0 0 3;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 20;");
        return card;
    }
}