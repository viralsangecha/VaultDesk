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

        // ── Nav buttons — shown based on role ────────────────
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
        btnNewAsset.setOnAction(e -> {
            setActive(btnAssets);
            contentArea.getChildren().setAll(new AssetView().getView());
        });

        // ── Theme toggle in sidebar ───────────────────────────
        btnTheme.getStyleClass().add("sidebar-btn");
        btnTheme.setOnAction(e -> {
            ThemeManager.toggle();
            Scene s = stage.getScene();
            ThemeManager.apply(s);
            btnTheme.setText(ThemeManager.getCurrent() ==
                    ThemeManager.Theme.DARK ? "☀  Light Mode" : "🌙  Dark Mode");
        });

        // ── Logout ────────────────────────────────────────
        Button btnLogout = new Button("⏻  Logout");
        btnLogout.getStyleClass().add("sidebar-logout");
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // ── Sidebar VBox ──────────────────────────────────
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.getChildren().addAll(
                sideHeader, userCard,
                btnDashboard, btnAssets, btnTickets,
                btnEmployees, btnDepartments, btnLicenses,
                btnConsumables, btnMaintenance, btnVendors,
                btnReports,
                spacer,
                newAssetBox,
                btnSettings, btnSupport,btnTheme,
                btnLogout
        );

        // Always visible
        sidebar.getChildren().addAll(
                sideHeader, userCard,
                btnDashboard, btnTickets);

// Assets — all roles see it but ENGINEER is read-only
        sidebar.getChildren().add(btnAssets);

// ADMIN + DEPT_HOD see these
        if (SessionManager.get().isAdmin()
                || SessionManager.get().isDeptHod()) {
            sidebar.getChildren().addAll(
                    btnEmployees, btnDepartments,
                    btnLicenses, btnConsumables,
                    btnMaintenance, btnVendors);
        }

// Reports — ADMIN + DEPT_HOD
        if (SessionManager.get().canViewReports()) {
            sidebar.getChildren().add(btnReports);
        }

// Users — ADMIN only
        if (SessionManager.get().canManageUsers()) {
            sidebar.getChildren().add(btnUsers);
        }

        VBox.setVgrow(spacer, Priority.ALWAYS);

        sidebar.getChildren().addAll(
                spacer, newAssetBox,
                btnSettings, btnSupport,
                btnTheme, btnLogout);

        // ── Top bar with search ───────────────────────────
        TextField searchField = new TextField();
        searchField.setPromptText("🔍  Search assets, tickets, or serial numbers...");
        searchField.getStyleClass().add("search-bar");
        HBox topBar = new HBox(searchField);
        topBar.getStyleClass().add("top-bar");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // ── Content area ──────────────────────────────────
        contentArea = new VBox(10);
        contentArea.getStyleClass().add("content-area");
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("content-scroll");
        scrollPane.setStyle("-fx-background-color: #0d1117; -fx-background: #0d1117;");
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        // ── Right side = topbar + content ─────────────────
        VBox rightSide = new VBox(topBar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // ── Main layout ───────────────────────────────────
        HBox mainLayout = new HBox(sidebar, rightSide);
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        // ── Default view ──────────────────────────────────
        setActive(btnDashboard);
        contentArea.getChildren().add(new DashboardStatsView().getView());

        // ── Nav actions ───────────────────────────────────
        btnDashboard.setOnAction(e -> {
            setActive(btnDashboard);
            contentArea.getChildren().setAll(new DashboardStatsView().getView());
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
            contentArea.getChildren().setAll(new EmployeeView().getView());
        });
        btnDepartments.setOnAction(e -> {
            setActive(btnDepartments);
            contentArea.getChildren().setAll(new DepartmentView().getView());
        });
        btnLicenses.setOnAction(e -> {
            setActive(btnLicenses);
            contentArea.getChildren().setAll(new LicenseView().getView());
        });
        btnConsumables.setOnAction(e -> {
            setActive(btnConsumables);
            contentArea.getChildren().setAll(new ConsumableView().getView());
        });
        btnMaintenance.setOnAction(e -> {
            setActive(btnMaintenance);
            contentArea.getChildren().setAll(new MaintenanceView().getView());
        });
        btnVendors.setOnAction(e -> {
            setActive(btnVendors);
            contentArea.getChildren().setAll(new VendorView().getView());
        });
        btnReports.setOnAction(e -> {
            setActive(btnReports);
            contentArea.getChildren().setAll(new ReportView().getView());
        });

        btnSettings.setOnAction(e -> {
            setActive(btnSettings);
            contentArea.getChildren().setAll(new SettingsView().getView());
        });
        // ── Users button — ADMIN only ─────────────────────────

// Add to sidebar only if admin
        if ("ADMIN".equals(role)) {
            sidebar.getChildren().add(
                    sidebar.getChildren().indexOf(btnReports) + 1, btnUsers);
        }

        btnUsers.setOnAction(e -> {
            setActive(btnUsers);
            contentArea.getChildren().setAll(new UserManagementView().getView());
        });
        btnLogout.setOnAction(e ->
                stage.setScene(new LoginView().getScene(stage)));

        Scene scene = new Scene(mainLayout, 1200, 800);
        ThemeManager.apply(scene);
        return scene;
    }

    private VBox contentArea;

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
}