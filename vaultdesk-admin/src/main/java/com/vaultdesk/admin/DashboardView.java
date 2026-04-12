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

        // ── Sidebar header ───────────────────────────────
        Label sideTitle = new Label("VaultDesk");
        sideTitle.getStyleClass().add("sidebar-title");
        Label sideSub = new Label("IT Management");
        sideSub.getStyleClass().add("sidebar-subtitle");
        VBox sideHeader = new VBox(2, sideTitle, sideSub);
        sideHeader.getStyleClass().add("sidebar-header");

        // ── User card ────────────────────────────────────
        Label nameLabel = new Label(fullName);
        nameLabel.getStyleClass().add("sidebar-user-name");
        Label roleLabel = new Label(role);
        roleLabel.getStyleClass().add("sidebar-user-role");
        VBox userCard = new VBox(3, nameLabel, roleLabel);
        userCard.getStyleClass().add("sidebar-user-card");

        // ── Nav buttons ──────────────────────────────────
        Button btnDashboard   = sidebarBtn("  Dashboard");
        Button btnAssets      = sidebarBtn("  Assets");
        Button btnTickets     = sidebarBtn("  Tickets");
        Button btnEmployees   = sidebarBtn("  Employees");
        Button btnDepartments = sidebarBtn("  Departments");
        Button btnLicenses    = sidebarBtn("  Licenses");
        Button btnConsumables = sidebarBtn("  Consumables");
        Button btnMaintenance = sidebarBtn("  Maintenance");
        Button btnVendors     = sidebarBtn("  Vendors");
        Button btnReports     = sidebarBtn("  Reports");

        // ── Logout ───────────────────────────────────────
        Button btnLogout = new Button("  Logout");
        btnLogout.getStyleClass().add("sidebar-logout");
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // ── Sidebar VBox ─────────────────────────────────
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.getChildren().addAll(
                sideHeader, userCard,
                btnDashboard, btnAssets, btnTickets,
                btnEmployees, btnDepartments, btnLicenses,
                btnConsumables, btnMaintenance, btnVendors,
                btnReports,
                spacer, btnLogout
        );

        // ── Content area ─────────────────────────────────
        VBox contentArea = new VBox(10);
        contentArea.getStyleClass().add("content-area");
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        // ── Main layout ──────────────────────────────────
        HBox mainLayout = new HBox(sidebar, contentArea);

        // ── Default view ─────────────────────────────────
        setActive(btnDashboard);
        contentArea.getChildren().add(new DashboardStatsView().getView());

        // ── Nav actions ──────────────────────────────────
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
        btnLogout.setOnAction(e -> {
            stage.setScene(new LoginView().getScene(stage));
        });

        Scene scene = new Scene(mainLayout, 1200, 800);
        scene.getStylesheets().add(
                getClass().getResource("/styles.css").toExternalForm());
        return scene;
    }

    private Button sidebarBtn(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-btn");
        return btn;
    }

    private void setActive(Button btn) {
        if (activeBtn != null) {
            activeBtn.getStyleClass().setAll("sidebar-btn");
        }
        btn.getStyleClass().setAll("sidebar-btn-active");
        activeBtn = btn;
    }
}