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

    public DashboardView(String fullName, String role) {
        this.fullName = fullName;
        this.role = role;
    }

    public Scene getScene(Stage stage) {

        // ── Sidebar header ────────────────────────────────
        Label sideTitle = new Label("VaultDesk");
        sideTitle.getStyleClass().add("sidebar-title");
        Label sideSub = new Label("IT Management");
        sideSub.getStyleClass().add("sidebar-subtitle");
        VBox sideHeader = new VBox(2, sideTitle, sideSub);
        sideHeader.getStyleClass().add("sidebar-header");

        // ── User card ─────────────────────────────────────
        Label nameLabel = new Label(fullName);
        nameLabel.getStyleClass().add("sidebar-user-name");
        Label roleLabel = new Label(role);
        roleLabel.getStyleClass().add("sidebar-user-role");
        VBox userCard = new VBox(3, nameLabel, roleLabel);
        userCard.getStyleClass().add("sidebar-user-card");

        // ── All nav buttons ───────────────────────────────
        Button btnDashboard   = sidebarBtn("⊞  Dashboard");
        Button btnAssets      = sidebarBtn("▣  Assets");
        Button btnTickets     = sidebarBtn("✉  Tickets");
        Button btnEmployees   = sidebarBtn("👤  Employees");
        Button btnDepartments = sidebarBtn("🏢  Departments");
        Button btnLicenses    = sidebarBtn("🔑  Licenses");
        Button btnConsumables = sidebarBtn("📦  Consumables");
        Button btnMaintenance = sidebarBtn("🔧  Maintenance");
        Button btnVendors     = sidebarBtn("🤝  Vendors");
        Button btnReports     = sidebarBtn("📊  Reports");
        Button btnUsers       = sidebarBtn("👥  Users");
        Button btnSettings    = sidebarBtn("⚙  Settings");
        Button btnSupport     = sidebarBtn("？  Support");
        Button btnTheme       = sidebarBtn("☀  Light Mode");
        Button btnLogout      = new Button("⏻  Logout");
        btnLogout.getStyleClass().add("sidebar-logout");

        // ── New Asset button ──────────────────────────────
        Button btnNewAsset = new Button("＋  New Asset");
        btnNewAsset.getStyleClass().setAll("sidebar-new-asset");
        btnNewAsset.setStyle(
                "-fx-background-color: #1f6feb; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-font-size: 13px;" +
                        "-fx-background-radius: 6; -fx-padding: 10 16 10 16;" +
                        "-fx-pref-width: 188px; -fx-cursor: hand;");
        VBox newAssetBox = new VBox(btnNewAsset);
        newAssetBox.setPadding(new Insets(16));

        // ── Sidebar — built once, role-based ──────────────
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");

// Always visible for all roles
        sidebar.getChildren().addAll(
                sideHeader, userCard, btnDashboard);

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

        sidebar.getChildren().addAll(
                spacer, newAssetBox);

        if (SessionManager.get().isAdmin()) {
            sidebar.getChildren().addAll(btnSettings, btnSupport);
        }

        sidebar.getChildren().addAll(btnTheme, btnLogout);

        // ── Top bar ───────────────────────────────────────
        TextField searchField = new TextField();
        searchField.setPromptText(
                "🔍  Search assets, tickets, or serial numbers...");
        searchField.getStyleClass().add("search-bar");
        HBox topBar = new HBox(searchField);
        topBar.getStyleClass().add("top-bar");
        HBox.setHgrow(searchField, Priority.ALWAYS);

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

        // ── Default view — dashboard ──────────────────────
        setActive(btnDashboard);
        contentArea.getChildren().add(new DashboardStatsView().getView());

        // ── Nav actions ───────────────────────────────────
        btnDashboard.setOnAction(e -> {
            setActive(btnDashboard);
            contentArea.getChildren().setAll(
                    new DashboardStatsView().getView());
        });
        btnAssets.setOnAction(e -> {
            setActive(btnAssets);
            contentArea.getChildren().setAll(new AssetView().getView());
        });
        btnTickets.setOnAction(e -> {
            setActive(btnTickets);
            contentArea.getChildren().setAll(new TicketView().getView());
        });
        btnEmployees.setOnAction(e -> {
            setActive(btnEmployees);
            contentArea.getChildren().setAll(
                    new EmployeeView().getView());
        });
        btnDepartments.setOnAction(e -> {
            setActive(btnDepartments);
            contentArea.getChildren().setAll(
                    new DepartmentView().getView());
        });
        btnLicenses.setOnAction(e -> {
            setActive(btnLicenses);
            contentArea.getChildren().setAll(
                    new LicenseView().getView());
        });
        btnConsumables.setOnAction(e -> {
            setActive(btnConsumables);
            contentArea.getChildren().setAll(
                    new ConsumableView().getView());
        });
        btnMaintenance.setOnAction(e -> {
            setActive(btnMaintenance);
            contentArea.getChildren().setAll(
                    new MaintenanceView().getView());
        });
        btnVendors.setOnAction(e -> {
            setActive(btnVendors);
            contentArea.getChildren().setAll(new VendorView().getView());
        });
        btnReports.setOnAction(e -> {
            setActive(btnReports);
            contentArea.getChildren().setAll(
                    new ReportView().getView());
        });
        btnUsers.setOnAction(e -> {
            setActive(btnUsers);
            contentArea.getChildren().setAll(
                    new UserManagementView().getView());
        });
        btnSettings.setOnAction(e -> {
            setActive(btnSettings);
            contentArea.getChildren().setAll(
                    new SettingsView().getView());
        });
        btnSupport.setOnAction(e -> {
            setActive(btnSupport);
            contentArea.getChildren().setAll(buildSupportView());
        });
        btnNewAsset.setOnAction(e -> {
            setActive(btnAssets);
            contentArea.getChildren().setAll(new AssetView().getView());
        });

        // ── Theme toggle ──────────────────────────────────
        btnTheme.setOnAction(e -> {
            ThemeManager.toggle();
            Scene s = stage.getScene();
            ThemeManager.apply(s);
            btnTheme.setText(ThemeManager.getCurrent() ==
                    ThemeManager.Theme.DARK
                    ? "☀  Light Mode" : "🌙  Dark Mode");
        });

        // ── Logout ────────────────────────────────────────
        btnLogout.setOnAction(e -> {
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

    // ── Sidebar button factory ────────────────────────────
    private Button sidebarBtn(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-btn");
        return btn;
    }

    // ── Active state toggle ───────────────────────────────
    private void setActive(Button btn) {
        if (activeBtn != null)
            activeBtn.getStyleClass().setAll("sidebar-btn");
        btn.getStyleClass().setAll("sidebar-btn-active");
        activeBtn = btn;
    }

    // ── Support view ──────────────────────────────────────
    private VBox buildSupportView() {
        Label title = new Label("Support");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Need help? Here are your options.");
        sub.getStyleClass().add("page-subtitle");

        VBox card1 = supportCard("📧  Email Support",
                "IT Department internal support",
                "it-support@saurashtracement.com",
                "#58a6ff");
        VBox card2 = supportCard("📞  Phone Support",
                "Call the IT helpdesk directly",
                "Ext. 2100  |  Mon–Sat 9AM–6PM",
                "#3fb950");
        VBox card3 = supportCard("📋  Raise a Ticket",
                "Create a formal support ticket",
                "Go to Tickets → New Ticket",
                "#d29922");
        VBox card4 = supportCard("ℹ  About VaultDesk",
                "Version 1.0.0  |  Built by Viral Sangecha",
                "IT Helpdesk & Asset Management Platform",
                "#a371f7");

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
        d.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        Label dt = new Label(detail);
        dt.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 13px;" +
                "-fx-font-weight: bold;");
        dt.setWrapText(true);
        VBox card = new VBox(8, h, d, dt);
        card.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 0 0 0 3;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 20;");
        return card;
    }
}