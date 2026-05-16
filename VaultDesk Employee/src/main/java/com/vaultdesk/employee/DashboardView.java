package com.vaultdesk.employee;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class DashboardView {

    private Button activeBtn = null;
    private VBox contentArea;
    private Button btnMyTickets;
    private Button btnRaiseTicket;
    private Button btnMyAssets;
    private Button btnSettings;

    public Scene getScene(Stage stage) {

        // ── Sidebar header ────────────────────────────────
        Label sideTitle = new Label("VaultDesk");
        sideTitle.getStyleClass().add("sidebar-title");
        Label sideSub = new Label("Employee Portal");
        sideSub.getStyleClass().add("sidebar-subtitle");
        VBox sideHeader = new VBox(2, sideTitle, sideSub);
        sideHeader.getStyleClass().add("sidebar-header");

        // ── User card ─────────────────────────────────────
        Label nameLabel = new Label(SessionManager.get().getName());
        nameLabel.getStyleClass().add("sidebar-user-name");
        Label roleLabel = new Label(
                SessionManager.get().getDesignation());
        roleLabel.getStyleClass().add("sidebar-user-role");
        Label empCodeLabel = new Label(
                SessionManager.get().getEmpCode());
        empCodeLabel.setStyle(
                "-fx-text-fill: #484f58; -fx-font-size: 10px;");
        VBox userCard = new VBox(3, nameLabel, roleLabel, empCodeLabel);
        userCard.getStyleClass().add("sidebar-user-card");

        // ── Nav buttons ───────────────────────────────────
        Button btnDashboard  = sidebarBtn("⊞  Dashboard");
        btnMyTickets         = sidebarBtn("✉  My Tickets");
        btnRaiseTicket       = sidebarBtn("＋  Raise Ticket");
        btnMyAssets          = sidebarBtn("▣  My Assets");
        btnSettings          = sidebarBtn("⚙  Settings");

        Button btnTheme = sidebarBtn("☀  Light Mode");
        Button btnLogout = new Button("→  Logout");
        btnLogout.getStyleClass().add("sidebar-logout");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox sidebar = new VBox(
                sideHeader, userCard,
                btnDashboard, btnMyTickets,
                btnRaiseTicket, btnMyAssets,
                spacer, btnSettings,btnTheme, btnLogout);
        sidebar.getStyleClass().add("sidebar");

        // ── Top bar ───────────────────────────────────────────
        Label topTitle = new Label("VaultDesk Employee Portal");
        topTitle.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        Label topUser = new Label("👤  " + SessionManager.get().getName());
        topUser.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 12px;");

        NotificationBell bell = new NotificationBell();
        StackPane bellView = bell.getView();

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topBar = new HBox(topTitle, topSpacer, topUser, bellView);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        stage.setOnCloseRequest(e -> bell.stopPolling());

        // ── Content area ──────────────────────────────────
        contentArea = new VBox(10);
        contentArea.getStyleClass().add("content-area");

        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("content-scroll");
        scrollPane.setStyle(
                "-fx-background-color: #0d1117; -fx-background: #0d1117;");

        VBox rightSide = new VBox(topBar, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        HBox mainLayout = new HBox(sidebar, rightSide);
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        // ── Default view ──────────────────────────────────
        setActive(btnDashboard);
        showHome();

        // ── Nav actions ───────────────────────────────────
        btnDashboard.setOnAction(e -> {
            setActive(btnDashboard);
            showHome();
        });
        btnMyTickets.setOnAction(e -> {
            setActive(btnMyTickets);
            contentArea.getChildren().setAll(
                    new MyTicketsView().getView());
        });
        btnRaiseTicket.setOnAction(e -> {
            setActive(btnRaiseTicket);
            contentArea.getChildren().setAll(
                    new RaiseTicketView(result -> {
                        setActive(btnMyTickets);
                        contentArea.getChildren().setAll(
                                new MyTicketsView().getView());
                    }).getView());
        });
        btnMyAssets.setOnAction(e -> {
            setActive(btnMyAssets);
            contentArea.getChildren().setAll(
                    new MyAssetsView().getView());
        });
        btnSettings.setOnAction(e -> {
            setActive(btnSettings);
            contentArea.getChildren().setAll(
                    new SettingsView().getView());
        });
        btnTheme.setOnAction(e -> {
            ThemeManager.toggle();
            ThemeManager.apply(stage.getScene());
            btnTheme.setText(ThemeManager.getCurrent() ==
                    ThemeManager.Theme.DARK ? "☀  Light Mode" : "🌙  Dark Mode");
        });

        btnLogout.setOnAction(e -> {
            SessionStore.clear();
            SessionManager.get().logout();
            Scene loginScene = new LoginView().getScene(stage);
            ThemeManager.apply(loginScene);
            stage.setScene(loginScene);
        });

        Scene scene = new Scene(mainLayout, 1000, 700);
        ThemeManager.apply(scene);
        return scene;
    }

    // ── Home dashboard ────────────────────────────────────
    private void showHome() {
        Label pageTitle = new Label("Welcome, "
                + SessionManager.get().getName() + " 👋");
        pageTitle.getStyleClass().add("page-title");
        Label pageSub = new Label(
                SessionManager.get().getDesignation()
                        + "  •  " + SessionManager.get().getEmpCode());
        pageSub.getStyleClass().add("page-subtitle");

        // ── Stat cards ────────────────────────────────────
        VBox cardTickets = homeCard("✉", "My Tickets",
                "View and track your support requests",
                "#58a6ff", "#1a2840");
        VBox cardRaise   = homeCard("＋", "Raise Ticket",
                "Submit a new support request",
                "#3fb950", "#1b2d1f");
        VBox cardAssets  = homeCard("▣", "My Assets",
                "View assets assigned to you",
                "#d29922", "#2d2008");

        cardTickets.setOnMouseClicked(e -> {
            setActive(btnMyTickets);
            contentArea.getChildren().setAll(
                    new MyTicketsView().getView());
        });
        cardRaise.setOnMouseClicked(e -> {
            setActive(btnRaiseTicket);
            contentArea.getChildren().setAll(
                    new RaiseTicketView(result -> {
                        setActive(btnMyTickets);
                        contentArea.getChildren().setAll(
                                new MyTicketsView().getView());
                    }).getView());
        });
        cardAssets.setOnMouseClicked(e -> {
            setActive(btnMyAssets);
            contentArea.getChildren().setAll(
                    new MyAssetsView().getView());
        });

        HBox cardsRow = new HBox(16, cardTickets, cardRaise, cardAssets);
        cardsRow.setPadding(new Insets(8, 0, 8, 0));

        // ── Quick info ────────────────────────────────────
        Label infoTitle = new Label("Your Information");
        infoTitle.getStyleClass().add("section-title");

        VBox infoCard = new VBox(10,
                infoRow("Name",        SessionManager.get().getName()),
                infoRow("Employee Code", SessionManager.get().getEmpCode()),
                infoRow("Designation", SessionManager.get().getDesignation()),
                infoRow("Email",       SessionManager.get().getEmail()));
        infoCard.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: #30363d;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 16;");

        contentArea.getChildren().setAll(
                pageTitle, pageSub, cardsRow, infoTitle, infoCard);
    }

    // ── Home card builder ─────────────────────────────────
    private VBox homeCard(String icon, String title,
                          String subtitle, String color, String bg) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
                "-fx-text-fill: " + color + "; -fx-font-size: 24px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: #e6edf3; -fx-font-size: 15px;" +
                        "-fx-font-weight: bold;");

        Label subLabel = new Label(subtitle);
        subLabel.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        subLabel.setWrapText(true);

        VBox card = new VBox(8, iconLabel, titleLabel, subLabel);
        card.setStyle(
                "-fx-background-color: #161b22;" +
                        "-fx-border-color: " + color + ";" +
                        "-fx-border-width: 0 0 0 3;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 20;" +
                        "-fx-cursor: hand;");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    // ── Info row ──────────────────────────────────────────
    private HBox infoRow(String label, String value) {
        Label k = new Label(label + ":");
        k.setStyle(
                "-fx-text-fill: #8b949e; -fx-font-size: 12px;" +
                        "-fx-min-width: 120;");
        Label v = new Label(value != null && !value.isEmpty()
                ? value : "-");
        v.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 13px;");
        return new HBox(8, k, v);
    }

    // ── Sidebar button factory ────────────────────────────
    private Button sidebarBtn(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-btn");
        return btn;
    }

    // ── Active state ──────────────────────────────────────
    private void setActive(Button btn) {
        if (activeBtn != null)
            activeBtn.getStyleClass().setAll("sidebar-btn");
        btn.getStyleClass().setAll("sidebar-btn-active");
        activeBtn = btn;
    }
}