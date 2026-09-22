package com.vaultdesk.admin;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.http.*;
import java.util.*;
import java.util.prefs.Preferences;

public class SettingsView {

    private static final Preferences prefs =
            Preferences.userNodeForPackage(SettingsView.class);

    public VBox getView() {

        Label bcRoot = new Label("SYSTEM");
        bcRoot.getStyleClass().add("breadcrumb-root");
        Label bcSep = new Label("  /  ");
        bcSep.getStyleClass().add("breadcrumb-sep");
        Label bcCurrent = new Label("SETTINGS");
        bcCurrent.getStyleClass().add("breadcrumb-current");
        HBox breadcrumb = new HBox(bcRoot, bcSep, bcCurrent);

        Label pageTitle = new Label("Settings");
        pageTitle.getStyleClass().add("page-title");
        Label pageSub = new Label("Configure application preferences.");
        pageSub.getStyleClass().add("page-subtitle");

        // ── Left nav ──────────────────────────────────────
        VBox nav = new VBox(2);
        nav.getStyleClass().add("table-wrapper");
        nav.setPadding(new Insets(8));
        nav.setPrefWidth(200);
        nav.setMinWidth(200);

        // ── Right content, swapped on nav click ────────────
        VBox contentHolder = new VBox(20);
        ScrollPane contentScroll = new ScrollPane(contentHolder);
        contentScroll.setFitToWidth(true);
        contentScroll.getStyleClass().add("content-scroll");
        contentScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        HBox.setHgrow(contentScroll, Priority.ALWAYS);

        Map<String, java.util.function.Supplier<javafx.scene.Node>> categories = new LinkedHashMap<>();
        categories.put("🎨  General", this::buildGeneralPanel);
        categories.put("🖥  Server", this::buildServerPanel);
        categories.put("📧  Email & Notifications", this::buildEmailPanel);
        categories.put("🔒  Security", this::buildSecurityPanel);
        categories.put("💾  Data & Backup", this::buildDataBackupPanel);
        categories.put("ℹ  About", this::buildAboutPanel);

        List<Button> navButtons = new ArrayList<>();
        for (Map.Entry<String, java.util.function.Supplier<javafx.scene.Node>> entry : categories.entrySet()) {
            Button navBtn = new Button(entry.getKey());
            navBtn.getStyleClass().add("sidebar-btn");
            navBtn.setMaxWidth(Double.MAX_VALUE);
            navBtn.setAlignment(Pos.CENTER_LEFT);
            navBtn.setOnAction(e -> {
                contentHolder.getChildren().setAll(entry.getValue().get()); // rebuilt fresh from the server every click
                navButtons.forEach(b -> b.getStyleClass().remove("sidebar-btn-active"));
                navBtn.getStyleClass().add("sidebar-btn-active");
            });
            navButtons.add(navBtn);
            nav.getChildren().add(navBtn);
        }

        if (!navButtons.isEmpty()) {
            navButtons.get(0).getStyleClass().add("sidebar-btn-active");
            contentHolder.getChildren().setAll(categories.values().iterator().next().get());
        }

        HBox body = new HBox(20, nav, contentScroll);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox root = new VBox(16, breadcrumb, pageTitle, pageSub, body);
        VBox.setVgrow(root, Priority.ALWAYS);
        return root;
    }

// ── Category panels — group existing sections, unchanged internally ──

    private VBox buildGeneralPanel() {
        return new VBox(16, panelHeader("Appearance"), buildAppearanceSection());
    }

    private VBox buildServerPanel() {
        return new VBox(24,
                panelHeader("Server Configuration"), buildServerSection(),
                new Separator(),
                panelHeader("Maintenance Mode"), buildMaintenanceSection());
    }

    private VBox buildEmailPanel() {
        return new VBox(24,
                panelHeader("Email Server (SMTP)"), buildEmailSection(),
                new Separator(),
                panelHeader("Notification Rules"),
                subLabel("Configure exactly who gets notified for each event below."),
                buildNotificationRulesSection());
    }

    private VBox buildSecurityPanel() {
        return new VBox(16, panelHeader("Change Password"), buildPasswordSection());
    }

    private VBox buildDataBackupPanel() {
        return new VBox(24,
                panelHeader("Database Backup"), buildBackupSection(),
                new Separator(),
                panelHeader("Data & Import"), buildDataSection());
    }

    private VBox buildAboutPanel() {
        return new VBox(16, panelHeader("About VaultDesk"), buildAboutSection());
    }

    private Label panelHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        return l;
    }

    private Label subLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("text-muted");
        l.setStyle("-fx-font-size: 12px;");
        l.setWrapText(true);
        return l;
    }

    private ColumnConstraints[] labelValueCols(double minWidth) {
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(minWidth);
        ColumnConstraints valueCol = new ColumnConstraints();
        valueCol.setHgrow(Priority.ALWAYS);
        return new ColumnConstraints[]{labelCol, valueCol};
    }

    // ── Server section ────────────────────────────────────
    private VBox buildServerSection() {
        Label hostLabel = new Label("Server Host");
        hostLabel.getStyleClass().add("login-label");

        TextField hostField = new TextField(
                prefs.get("server.host", "localhost"));
        hostField.setPromptText("e.g. localhost or 192.168.1.100");
        hostField.setPrefWidth(320);

        Label portLabel = new Label("Server Port");
        portLabel.getStyleClass().add("login-label");

        NumberField portField = new NumberField();
        portField.setText(prefs.get("server.port", "2008"));
        portField.setPromptText("e.g. 2008");
        portField.setPrefWidth(120);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px;");

        Button testBtn = new Button("Test Connection");
        testBtn.getStyleClass().add("btn-primary");
        AnimationUtil.addHoverScale(testBtn);
        testBtn.setOnAction(e -> {
            String url = "http://" + hostField.getText().trim()
                    + ":" + portField.getText().trim()
                    + "/api/health";
            try {
                HttpResponse<String> resp = ApiClient.get(url);
                if (resp.statusCode() == 200) {
                    statusLabel.setText("✔ Connected successfully.");
                    statusLabel.setStyle(
                            "-fx-text-fill: #3fb950; -fx-font-size: 12px;");
                } else {
                    statusLabel.setText("⚠ Server returned: "
                            + resp.statusCode());
                    statusLabel.setStyle(
                            "-fx-text-fill: #d29922; -fx-font-size: 12px;");
                }
            } catch (Exception ex) {
                statusLabel.setText("✘ Cannot connect: " + ex.getMessage());
                statusLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 12px;");
            }
        });

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("btn-primary");
        AnimationUtil.addHoverScale(saveBtn);
        saveBtn.setOnAction(e -> {
            ConfigManager.setHost(hostField.getText().trim());
            ConfigManager.setPort(portField.getText().trim());
            ConfigManager.save();
            statusLabel.setText("✔ Server settings saved.");
            statusLabel.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 12px;");
        });

        Label note = new Label(
                "Changes take effect after restarting the application.");
        note.getStyleClass().add("text-muted");
        note.setStyle("-fx-font-size: 11px;");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.getColumnConstraints().addAll(labelValueCols(120));
        grid.add(hostLabel,  0, 0); grid.add(hostField, 1, 0);
        grid.add(portLabel,  0, 1); grid.add(portField, 1, 1);
        grid.add(note,       1, 2);
        grid.add(statusLabel,1, 3);

        HBox btnRow = new HBox(10, testBtn, saveBtn);
        return new VBox(12, grid, btnRow);
    }

    private VBox buildEmailSection() {
        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px;");

        TextField hostField = new TextField();
        hostField.setPromptText("e.g. smtp.zoho.in");
        hostField.setPrefWidth(280);

        NumberField portField = new NumberField();
        portField.setText("587");
        portField.setPrefWidth(100);

        TextField usernameField = new TextField();
        usernameField.setPromptText("SMTP login email");
        usernameField.setPrefWidth(280);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("SMTP password / app password");
        passwordField.setPrefWidth(280);

        TextField fromNameField = new TextField("VaultDesk");
        fromNameField.setPrefWidth(280);

        TextField publicUrlField = new TextField();
        publicUrlField.setPromptText("e.g. http://192.168.1.50:2008");
        publicUrlField.setPrefWidth(280);

        CheckBox enabledBox = new CheckBox("Enable email sending");

        Button suggestBtn = new Button("Suggest this machine's LAN address");
        suggestBtn.getStyleClass().add("btn-link-blue");
        suggestBtn.setOnAction(e -> {
            try {
                String ip = java.net.InetAddress.getLocalHost().getHostAddress();
                publicUrlField.setText("http://" + ip + ":2008");
            } catch (Exception ex) {
                statusLabel.setText("Could not detect local IP.");
                statusLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");
            }
        });

        try {
            HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/email-settings");
            String body = resp.body();
            if (extractValue(body, "configured").equals("true") || body.contains("\"configured\":true")) {
                hostField.setText(extractValue(body, "smtpHost"));
                String port = extractValue(body, "smtpPort");
                if (!port.isEmpty()) portField.setText(port);
                usernameField.setText(extractValue(body, "username"));
                String fromName = extractValue(body, "fromName");
                if (!fromName.isEmpty()) fromNameField.setText(fromName);
                publicUrlField.setText(extractValue(body, "publicUrl"));
                enabledBox.setSelected(body.contains("\"enabled\":true"));
            }
        } catch (Exception ex) {
            statusLabel.setText("Could not load current email settings.");
            statusLabel.setStyle("-fx-text-fill: #d29922; -fx-font-size: 12px;");
        }

        Button saveBtn = new Button("Save Email Settings");
        saveBtn.getStyleClass().add("btn-primary");
        AnimationUtil.addHoverScale(saveBtn);
        saveBtn.setOnAction(e -> {
            try {
                String body = "{" +
                        "\"smtpHost\":\"" + escapeJson(hostField.getText().trim()) + "\"," +
                        "\"smtpPort\":" + portField.getIntValue() + "," +
                        "\"username\":\"" + escapeJson(usernameField.getText().trim()) + "\"," +
                        "\"password\":\"" + jsonEscape(passwordField.getText()) + "\"," +
                        "\"fromName\":\"" + escapeJson(fromNameField.getText().trim()) + "\"," +
                        "\"enabled\":" + enabledBox.isSelected() + "," +
                        "\"publicUrl\":\"" + escapeJson(publicUrlField.getText().trim()) + "\"" +
                        "}";
                HttpResponse<String> resp = ApiClient.put(ConfigManager.getBaseUrl() + "/api/email-settings", body);
                if (resp.statusCode() == 200) {
                    statusLabel.setText("✔ Email settings saved.");
                    statusLabel.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 12px;");
                    passwordField.clear();
                } else {
                    statusLabel.setText("Error: " + resp.statusCode());
                    statusLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");
                }
            } catch (Exception ex) {
                statusLabel.setText("Cannot connect: " + ex.getMessage());
                statusLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");
            }
        });

        Label note = new Label(
                "Public URL is where reset-password links point — since this app runs on your " +
                        "company's private network only, use this machine's LAN address (e.g. http://192.168.x.x:2008), not localhost.");
        note.getStyleClass().add("text-muted");
        note.setStyle("-fx-font-size: 11px;");
        note.setWrapText(true);
        note.setMaxWidth(400);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.getColumnConstraints().addAll(labelValueCols(120));
        grid.add(new Label("SMTP Host:"), 0, 0);     grid.add(hostField, 1, 0);
        grid.add(new Label("SMTP Port:"), 0, 1);     grid.add(portField, 1, 1);
        grid.add(new Label("Username:"), 0, 2);      grid.add(usernameField, 1, 2);
        grid.add(new Label("Password:"), 0, 3);      grid.add(passwordField, 1, 3);
        grid.add(new Label("From Name:"), 0, 4);     grid.add(fromNameField, 1, 4);
        grid.add(new Label("Public URL:"), 0, 5);    grid.add(publicUrlField, 1, 5);
        grid.add(suggestBtn, 1, 6);
        grid.add(note, 1, 7);
        grid.add(enabledBox, 1, 8);
        grid.add(statusLabel, 1, 9);

        VBox recipientsBox = buildRecipientsSubsection();

        return new VBox(14, grid, saveBtn, new Separator(), recipientsBox);
    }

    private VBox buildRecipientsSubsection() {
        Label heading = new Label("People Who Get Notified");
        heading.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label sub = new Label("Add anyone who should receive certain alerts, then click Edit next to their name to choose which events they hear about.");
        sub.getStyleClass().add("text-muted");
        sub.setStyle("-fx-font-size: 11px;");
        sub.setWrapText(true);

        VBox peopleList = new VBox(6);
        Label loading = new Label("Loading...");
        loading.getStyleClass().add("text-muted");
        peopleList.getChildren().add(loading);

        Map<String, String> eventLabels = ticketEventLabels(); // shared helper, see below

        TextField emailField = new TextField();
        emailField.setPromptText("email@company.com");
        emailField.setPrefWidth(200);
        TextField nameField = new TextField();
        nameField.setPromptText("Display name (optional)");
        nameField.setPrefWidth(160);
        Label addError = new Label("");
        addError.setStyle("-fx-text-fill: #f85149; -fx-font-size: 11px;");
        Button addBtn = new Button("+ Add Person");
        addBtn.getStyleClass().add("btn-primary");
        AnimationUtil.addHoverScale(addBtn);

        Runnable[] reload = new Runnable[1];
        reload[0] = () -> {
            peopleList.getChildren().clear();
            try {
                HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/email-settings/recipients");
                String body = resp.body().trim();
                if (body.length() < 2 || body.equals("[]")) {
                    Label none = new Label("No one added yet.");
                    none.getStyleClass().add("text-muted");
                    peopleList.getChildren().add(none);
                    return;
                }
                body = body.substring(1, body.length() - 1).trim();
                for (String obj : body.split("\\},\\{")) {
                    String cleaned = obj.replace("{", "").replace("}", "");
                    int id = extractIntVal(cleaned, "id");
                    String email = extractValue(cleaned, "email");
                    String name = extractValue(cleaned, "name");
                    String subsRaw = extractValue(cleaned, "subscribed_events");
                    List<String> subs = subsRaw.isEmpty() ? List.of() : Arrays.asList(subsRaw.split(","));

                    Label personLabel = new Label(name.isEmpty() ? email : name + " — " + email);
                    personLabel.setStyle("-fx-font-size: 12px;");
                    Label countLabel = new Label(subs.size() + " event" + (subs.size() == 1 ? "" : "s"));
                    countLabel.getStyleClass().add("text-muted");
                    countLabel.setStyle("-fx-font-size: 11px;");

                    Button editBtn = new Button("Edit");
                    editBtn.getStyleClass().add("btn-link-blue");
                    editBtn.setOnAction(e -> showEditSubscriptionsDialog(id, email, subs, eventLabels, reload[0]));

                    Button removeBtn = new Button("✕");
                    removeBtn.getStyleClass().add("btn-link-red");
                    removeBtn.setOnAction(e -> {
                        try {
                            ApiClient.delete(ConfigManager.getBaseUrl() + "/api/email-settings/recipients/" + id);
                            reload[0].run();
                        } catch (Exception ignored) {}
                    });

                    HBox row = new HBox(12, personLabel, countLabel, editBtn, removeBtn);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("surface-card");
                    row.setStyle("-fx-padding: 8 12; -fx-background-radius: 6;");
                    peopleList.getChildren().add(row);
                }
            } catch (Exception ex) {
                peopleList.getChildren().add(new Label("Could not load people."));
            }
        };
        reload[0].run();

        addBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            if (!ValidationUtil.isValidEmail(email)) {
                addError.setText("Enter a valid email address.");
                return;
            }
            addError.setText("");
            try {
                String body = "{\"email\":\"" + escapeJson(email) + "\",\"name\":\"" + escapeJson(nameField.getText().trim()) + "\"}";
                ApiClient.post(ConfigManager.getBaseUrl() + "/api/email-settings/recipients", body);
                emailField.clear();
                nameField.clear();
                reload[0].run();
            } catch (Exception ignored) {}
        });

        HBox addRow = new HBox(8, emailField, nameField, addBtn);
        return new VBox(8, heading, sub, peopleList, addRow, addError);
    }

    private Map<String, String> ticketEventLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("TICKET_CREATED", "Ticket Created");
        labels.put("TICKET_ASSIGNED", "Ticket Assigned to Engineer");
        labels.put("TICKET_STATUS_CHANGED", "Ticket Status Changed");
        labels.put("TICKET_AUTO_CLOSED", "Ticket Auto-Closed (3-Day)");
        labels.put("PASSWORD_RESET_REQUESTED", "Password Reset Requested");
        labels.put("PASSWORD_RESET_COMPLETED", "Password Reset Completed");
        return labels;
    }

    private void showEditSubscriptionsDialog(int recipientId, String email, List<String> currentSubs,
                                             Map<String, String> eventLabels, Runnable reload) {
        Dialog<ButtonType> dialog = new Dialog<>();
        ThemeManager.applyToDialog(dialog);
        dialog.setTitle("Subscriptions — " + email);
        dialog.setHeaderText("Which events should this person hear about?");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox checks = new VBox(8);
        Map<String, CheckBox> boxes = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : eventLabels.entrySet()) {
            CheckBox cb = new CheckBox(entry.getValue());
            cb.setSelected(currentSubs.contains(entry.getKey()));
            boxes.put(entry.getKey(), cb);
            checks.getChildren().add(cb);
        }
        checks.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(checks);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            List<String> selected = new ArrayList<>();
            for (Map.Entry<String, CheckBox> b : boxes.entrySet()) {
                if (b.getValue().isSelected()) selected.add(b.getKey());
            }
            StringBuilder json = new StringBuilder("{\"eventKeys\":[");
            for (int i = 0; i < selected.size(); i++) {
                json.append("\"").append(selected.get(i)).append("\"");
                if (i < selected.size() - 1) json.append(",");
            }
            json.append("]}");
            try {
                HttpResponse<String> resp = ApiClient.put(
                        ConfigManager.getBaseUrl() + "/api/email-settings/recipients/" + recipientId + "/subscriptions",
                        json.toString());
                if (resp.statusCode() == 200) {
                    ToastUtil.success("Subscriptions updated for " + email);
                    reload.run();
                } else {
                    showAlert("Error", "Server returned " + resp.statusCode() + ":\n" + resp.body());
                }
            } catch (Exception ex) {
                showAlert("Error", "Cannot connect: " + ex.getMessage());
            }
        }
    }

    private int extractIntVal(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return 0;
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.length();
        try { return Integer.parseInt(json.substring(start, end).trim().replace("}", "")); }
        catch (Exception e) { return 0; }
    }

    private VBox buildNotificationRulesSection() {
        VBox container = new VBox(10);
        Label loading = new Label("Loading...");
        loading.getStyleClass().add("text-muted");
        container.getChildren().add(loading);

        Map<String, String> eventLabels = new LinkedHashMap<>();
        eventLabels.put("TICKET_CREATED", "Ticket Created");
        eventLabels.put("TICKET_ASSIGNED", "Ticket Assigned to Engineer");
        eventLabels.put("TICKET_STATUS_CHANGED", "Ticket Status Changed");
        eventLabels.put("TICKET_AUTO_CLOSED", "Ticket Auto-Closed (3-Day)");
        eventLabels.put("PASSWORD_RESET_REQUESTED", "Password Reset Requested");
        eventLabels.put("PASSWORD_RESET_COMPLETED", "Password Reset Completed");

        try {
            HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/notification-rules");
            String body = resp.body().trim();
            Map<String, boolean[]> rules = new HashMap<>(); // eventKey -> [enabled, notifyReporter]
            if (body.length() > 2) {
                body = body.substring(1, body.length() - 1).trim();
                for (String obj : body.split("\\},\\{")) {
                    String cleaned = obj.replace("{", "").replace("}", "");
                    String key = extractValue(cleaned, "eventKey");
                    rules.put(key, new boolean[]{cleaned.contains("\"enabled\":true"), cleaned.contains("\"notifyReporter\":true")});
                }
            }

            container.getChildren().clear();
            for (Map.Entry<String, String> entry : eventLabels.entrySet()) {
                boolean[] state = rules.getOrDefault(entry.getKey(), new boolean[]{true, true});
                container.getChildren().add(buildRuleRow(entry.getKey(), entry.getValue(), state[0], state[1]));
            }
        } catch (Exception ex) {
            container.getChildren().setAll(new Label("Could not load notification rules: " + ex.getMessage()));
        }

        return container;
    }

    private HBox buildRuleRow(String eventKey, String label, boolean enabled, boolean notifyReporter) {
        Label title = new Label(label);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        title.setPrefWidth(220);

        CheckBox enabledBox = new CheckBox("Send this email");
        enabledBox.setSelected(enabled);
        CheckBox reporterBox = new CheckBox("Notify the person it's about");
        reporterBox.setSelected(notifyReporter);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 11px;");

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("btn-primary");
        AnimationUtil.addHoverScale(saveBtn);
        saveBtn.setOnAction(e -> {
            String json = "{\"enabled\":" + enabledBox.isSelected() + ",\"notifyReporter\":" + reporterBox.isSelected() + "}";
            try {
                HttpResponse<String> resp = ApiClient.put(ConfigManager.getBaseUrl() + "/api/notification-rules/" + eventKey, json);
                statusLabel.setText(resp.statusCode() == 200 ? "✔ Saved" : "Error: " + resp.statusCode());
                statusLabel.setStyle("-fx-text-fill: " + (resp.statusCode() == 200 ? "#3fb950" : "#f85149") + ";");
            } catch (Exception ex) {
                statusLabel.setText("Cannot connect.");
                statusLabel.setStyle("-fx-text-fill: #f85149;");
            }
        });

        HBox row = new HBox(16, title, enabledBox, reporterBox, saveBtn, statusLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("surface-card");
        row.setStyle("-fx-padding: 10 14; -fx-background-radius: 8;");
        return row;
    }

    private VBox buildRuleRow(String eventKey, String label, boolean enabled, boolean notifyReporter,
                              Set<String> selectedTypes, Set<String> knownTypes) {
        Label title = new Label(label);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        CheckBox enabledBox = new CheckBox("Send this notification");
        enabledBox.setSelected(enabled);
        CheckBox reporterBox = new CheckBox("Notify the person this event is about (ticket reporter, or the user resetting their password)");
        reporterBox.setWrapText(true);
        reporterBox.setSelected(notifyReporter);

        HBox typeRow = new HBox(14);
        Map<String, CheckBox> typeBoxes = new LinkedHashMap<>();
        for (String type : knownTypes) {
            CheckBox tb = new CheckBox(type);
            tb.setSelected(selectedTypes.contains(type));
            typeBoxes.put(type, tb);
            typeRow.getChildren().add(tb);
        }

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 11px;");

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("btn-primary");
        AnimationUtil.addHoverScale(saveBtn);
        saveBtn.setOnAction(e -> {
            List<String> types = new ArrayList<>();
            for (Map.Entry<String, CheckBox> tb : typeBoxes.entrySet()) {
                if (tb.getValue().isSelected()) types.add(tb.getKey());
            }
            StringBuilder json = new StringBuilder("{\"enabled\":" + enabledBox.isSelected()
                    + ",\"notifyReporter\":" + reporterBox.isSelected() + ",\"notifyTypes\":[");
            for (int i = 0; i < types.size(); i++) {
                json.append("\"").append(types.get(i)).append("\"");
                if (i < types.size() - 1) json.append(",");
            }
            json.append("]}");
            try {
                HttpResponse<String> resp = ApiClient.put(
                        ConfigManager.getBaseUrl() + "/api/notification-rules/" + eventKey, json.toString());
                statusLabel.setText(resp.statusCode() == 200 ? "✔ Saved" : "Error: " + resp.statusCode());
                statusLabel.setStyle("-fx-text-fill: " + (resp.statusCode() == 200 ? "#3fb950" : "#f85149") + "; -fx-font-size: 11px;");
            } catch (Exception ex) {
                statusLabel.setText("Cannot connect: " + ex.getMessage());
                statusLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 11px;");
            }
        });

        HBox actionRow = new HBox(12, saveBtn, statusLabel);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Label alsoNotifyLabel = new Label("Also copy these groups:");
        alsoNotifyLabel.getStyleClass().add("text-muted");
        alsoNotifyLabel.setStyle("-fx-font-size: 11px;");
        VBox row = new VBox(6, title, enabledBox, reporterBox, alsoNotifyLabel, typeRow, actionRow);
        row.getStyleClass().add("surface-card");
        row.setStyle("-fx-padding: 12; -fx-background-radius: 6;");
        return row;
    }

    private String extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "0";
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) end = json.length();
        return json.substring(start, end).trim();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "'");
    }

    /** Real JSON-string escaping — preserves the exact characters (used for passwords, where substituting quotes would silently change the actual value being sent). */
    private String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ── Appearance section ────────────────────────────────
    private VBox buildAppearanceSection() {

        Label themeLabel = new Label("Theme");
        themeLabel.getStyleClass().add("login-label");

        ToggleGroup themeGroup = new ToggleGroup();
        RadioButton darkBtn  = new RadioButton("Dark Mode");
        RadioButton lightBtn = new RadioButton("Light Mode");
        darkBtn.setToggleGroup(themeGroup);
        lightBtn.setToggleGroup(themeGroup);

        if (ThemeManager.getCurrent() == ThemeManager.Theme.DARK)
            darkBtn.setSelected(true);
        else
            lightBtn.setSelected(true);

        Label themeStatus = new Label("");
        themeStatus.setStyle("-fx-font-size: 12px;");

        themeGroup.selectedToggleProperty().addListener((obs, ov, nv) -> {
            if (nv == darkBtn) {
                if (ThemeManager.getCurrent() != ThemeManager.Theme.DARK) {
                    ThemeManager.toggle();
                    applyThemeToAll();
                    themeStatus.setText("✔ Dark mode applied.");
                    themeStatus.setStyle(
                            "-fx-text-fill: #3fb950; -fx-font-size: 12px;");
                }
            } else {
                if (ThemeManager.getCurrent() != ThemeManager.Theme.LIGHT) {
                    ThemeManager.toggle();
                    applyThemeToAll();
                    themeStatus.setText("✔ Light mode applied.");
                    themeStatus.setStyle(
                            "-fx-text-fill: #3fb950; -fx-font-size: 12px;");
                }
            }
        });

        Label fontLabel = new Label("Font Size");
        fontLabel.getStyleClass().add("login-label");

        ComboBox<String> fontBox = new ComboBox<>();
        fontBox.getItems().addAll("Small (12px)", "Medium (13px)", "Large (15px)");
        fontBox.setValue(prefs.get("font.size", "Medium (13px)"));
        fontBox.setPrefWidth(200);

        Button saveFontBtn = new Button("Apply Font Size");
        saveFontBtn.getStyleClass().add("btn-primary");
        AnimationUtil.addHoverScale(saveFontBtn);
        saveFontBtn.setOnAction(e -> {
            prefs.put("font.size", fontBox.getValue());
            String size = fontBox.getValue().contains("12") ? "12px"
                    : fontBox.getValue().contains("15") ? "15px" : "13px";

            javafx.stage.Stage stage =
                    (javafx.stage.Stage) javafx.stage.Window.getWindows()
                            .stream()
                            .filter(w -> w instanceof javafx.stage.Stage)
                            .findFirst().orElse(null);
            if (stage != null && stage.getScene() != null) {
                stage.getScene().getRoot().setStyle(
                        "-fx-font-size: " + size + ";");
            }
            themeStatus.setText("✔ Font size applied: " + size);
            themeStatus.setStyle(
                    "-fx-text-fill: #3fb950; -fx-font-size: 12px;");
        });

        HBox themeRow = new HBox(16, darkBtn, lightBtn);
        themeRow.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.getColumnConstraints().addAll(labelValueCols(100));
        grid.add(themeLabel, 0, 0); grid.add(themeRow,   1, 0);
        grid.add(fontLabel,  0, 1); grid.add(fontBox,    1, 1);
        grid.add(themeStatus,1, 2);

        return new VBox(12, grid, saveFontBtn);
    }

    // ── Password section ──────────────────────────────────
    private VBox buildPasswordSection() {

        Label currentLabel = new Label("Current Password");
        currentLabel.getStyleClass().add("login-label");
        PasswordField currentField = new PasswordField();
        currentField.setPromptText("Enter current password");
        currentField.setPrefWidth(280);

        Label newLabel = new Label("New Password");
        newLabel.getStyleClass().add("login-label");
        PasswordField newField = new PasswordField();
        newField.setPromptText("Minimum 6 characters");
        newField.setPrefWidth(280);

        Label confirmLabel = new Label("Confirm New Password");
        confirmLabel.getStyleClass().add("login-label");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Re-enter new password");
        confirmField.setPrefWidth(280);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px;");

        Button changeBtn = new Button("Change Password");
        changeBtn.getStyleClass().add("btn-warning");
        AnimationUtil.addHoverScale(changeBtn);
        changeBtn.setOnAction(e -> {
            String current = currentField.getText();
            String newPwd  = newField.getText();
            String confirm = confirmField.getText();

            if (current.isEmpty() || newPwd.isEmpty()) {
                statusLabel.setText("All fields are required.");
                statusLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 12px;");
                return;
            }
            if (newPwd.length() < 6) {
                statusLabel.setText("New password must be at least 6 characters.");
                statusLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 12px;");
                return;
            }
            if (!newPwd.equals(confirm)) {
                statusLabel.setText("Passwords do not match.");
                statusLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 12px;");
                return;
            }

            try {
                int userId = SessionManager.get().getUserId();
                String body = "{" +
                        "\"currentPassword\":\"" + jsonEscape(current) + "\"," +
                        "\"newPassword\":\"" + jsonEscape(newPwd) + "\"" +
                        "}";
                HttpResponse<String> resp = ApiClient.put(
                        ConfigManager.getBaseUrl() + "/api/users/" + userId + "/password", body);
                if (resp.statusCode() == 200) {
                    statusLabel.setText("✔ Password changed successfully.");
                    statusLabel.setStyle(
                            "-fx-text-fill: #3fb950; -fx-font-size: 12px;");
                    currentField.clear();
                    newField.clear();
                    confirmField.clear();
                } else if (resp.statusCode() == 401) {
                    statusLabel.setText("✘ Current password is incorrect.");
                    statusLabel.setStyle(
                            "-fx-text-fill: #f85149; -fx-font-size: 12px;");
                } else {
                    statusLabel.setText("Error: " + resp.statusCode());
                    statusLabel.setStyle(
                            "-fx-text-fill: #f85149; -fx-font-size: 12px;");
                }
            } catch (Exception ex) {
                statusLabel.setText("Cannot connect: " + ex.getMessage());
                statusLabel.setStyle(
                        "-fx-text-fill: #f85149; -fx-font-size: 12px;");
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.getColumnConstraints().addAll(labelValueCols(160));
        grid.add(currentLabel, 0, 0); grid.add(currentField, 1, 0);
        grid.add(newLabel,     0, 1); grid.add(newField,     1, 1);
        grid.add(confirmLabel, 0, 2); grid.add(confirmField, 1, 2);
        grid.add(statusLabel,  1, 3);

        return new VBox(12, grid, changeBtn);
    }

    // ── Data section ──────────────────────────────────────
    private VBox buildDataSection() {

        Label infoLabel = new Label(
                "Use the Import CSV buttons in Employees and Departments views to bulk import data.\n" +
                        "Use the Export buttons in each screen to download Excel files.\n" +
                        "(Assets import/export is on hold pending final field list.)");
        infoLabel.getStyleClass().add("text-muted");
        infoLabel.setStyle("-fx-font-size: 12px;");
        infoLabel.setWrapText(true);

        Label formatTitle = new Label("CSV Format Reference");
        formatTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        TextArea formatArea = new TextArea(
                "EMPLOYEES (8 columns):\n" +
                        "name, empCode, departmentId, designation,\n" +
                        "email, phone, joinDate (dd-MM-yyyy), notes\n\n" +
                        "DEPARTMENTS (2 columns):\n" +
                        "name, location\n\n" +
                        "VENDORS (7 columns):\n" +
                        "name, contactPerson, phone, email,\n" +
                        "category, address, notes\n\n" +
                        "LICENSES (9 columns):\n" +
                        "softwareName, licenseType, licenseKey,\n" +
                        "seatsTotal, vendor, purchaseDate (dd-MM-yyyy),\n" +
                        "expiryDate (dd-MM-yyyy), cost, notes\n\n" +
                        "CONSUMABLES (8 columns):\n" +
                        "name, category, compatibleModels,\n" +
                        "quantityInStock, reorderLevel, unit,\n" +
                        "unitCost, storageLocation\n\n" +
                        "NOTES:\n" +
                        "- First row is header (skipped)\n" +
                        "- Wrap values containing commas in double quotes\n" +
                        "- Dates must be dd-MM-yyyy format (e.g. 18-07-2026)\n" +
                        "- IDs must match existing records in the database\n\n" +
                        "ASSETS: format not finalized yet — Assets import/export is currently on hold."
        );
        formatArea.setEditable(false);
        formatArea.setWrapText(false);
        formatArea.setPrefRowCount(16);
        formatArea.setPrefHeight(340);
        formatArea.setMinHeight(340);
        formatArea.setStyle(
                "-fx-font-family: monospace; -fx-font-size: 12px;");

        return new VBox(12, infoLabel, formatTitle, formatArea);
    }

    // ── About section ─────────────────────────────────────
    private VBox buildAboutSection() {

        Label appName = new Label("VaultDesk Admin");
        appName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label version = new Label("Version 1.0.0  —  Phase 10e");
        version.getStyleClass().add("text-muted");
        version.setStyle("-fx-font-size: 12px;");

        Label desc = new Label(
                "IT Helpdesk & Asset Management Platform\n" +
                        "Built for Saurashtra Cement Ltd IT Department\n" +
                        "Developed by Viral Sangecha");
        desc.setStyle("-fx-font-size: 13px;");
        desc.setWrapText(true);

        Separator sep = new Separator();

        Label techTitle = new Label("Technology Stack");
        techTitle.getStyleClass().add("text-muted");
        techTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        Label tech = new Label(
                "Backend  :  Java 21  •  Spring Boot 3.5  •  SQLite\n" +
                        "Frontend :  JavaFX 21  •  AtlantaFX PrimerDark\n" +
                        "Reports  :  Apache POI 5.2.3\n" +
                        "Styling  :  Custom CSS + Light/Dark theme");
        tech.getStyleClass().add("text-muted");
        tech.setStyle("-fx-font-size: 12px; -fx-font-family: monospace;");

        Separator sep2 = new Separator();

        Label loggedIn = new Label(
                "Logged in as: " + SessionManager.get().getFullName()
                        + "  |  Role: " + SessionManager.get().getRole()
                        + "  |  User ID: " + SessionManager.get().getUserId());
        loggedIn.getStyleClass().add("breadcrumb-current");
        loggedIn.setStyle("-fx-font-size: 11px;");

        return new VBox(10,
                appName, version, desc, sep,
                techTitle, tech, sep2, loggedIn);
    }

    private VBox buildMaintenanceSection() {
        Label warningLabel = new Label(
                "⚠ Stop/Resume only work when this Admin app is running on the actual VaultDesk server machine.");
        warningLabel.setStyle("-fx-text-fill: #d29922; -fx-font-size: 11px; -fx-font-weight: bold;");
        warningLabel.setWrapText(true);

        // ── Advance warning (banner shown to everyone, server stays up) ──
        Label scheduleLabel = new Label("Schedule advance warning:");
        scheduleLabel.getStyleClass().add("login-label");
        NumberField minutesField = new NumberField();
        minutesField.setPromptText("Minutes from now, e.g. 10");
        TextField messageField = new TextField("Scheduled maintenance is starting soon. Please save your work.");
        messageField.setPrefWidth(320);

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px;");

        Button scheduleBtn = new Button("Schedule Warning");
        scheduleBtn.getStyleClass().add("btn-primary");
        AnimationUtil.addHoverScale(scheduleBtn);
        scheduleBtn.setOnAction(e -> {
            try {
                int mins = minutesField.getIntValue();
                String scheduledAt = java.time.LocalDateTime.now().plusMinutes(mins).toString();
                String body = "{\"scheduledAt\":\"" + scheduledAt + "\",\"message\":\""
                        + messageField.getText().replace("\"", "'") + "\"}";
                HttpResponse<String> resp = ApiClient.put(ConfigManager.getBaseUrl() + "/api/system-status/schedule", body);
                statusLabel.setText(resp.statusCode() == 200
                        ? "✔ Warning scheduled — banner will show for everyone." : "Error: " + resp.statusCode());
                statusLabel.setStyle("-fx-text-fill: " + (resp.statusCode() == 200 ? "#3fb950" : "#f85149") + "; -fx-font-size: 12px;");
            } catch (Exception ex) {
                statusLabel.setText("Cannot connect: " + ex.getMessage());
                statusLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 12px;");
            }
        });

        Button cancelBtn = new Button("Cancel Warning");
        cancelBtn.getStyleClass().add("btn-link-red");
        cancelBtn.setOnAction(e -> {
            try {
                ApiClient.delete(ConfigManager.getBaseUrl() + "/api/system-status/schedule");
                statusLabel.setText("✔ Warning cancelled.");
                statusLabel.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 12px;");
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        });

        // ── Actual stop / resume (real OS action, this machine only) ──
        Button stopBtn = new Button("🛑 Start Maintenance Now (Stop Server)");
        stopBtn.getStyleClass().add("btn-danger");
        AnimationUtil.addHoverScale(stopBtn);
        stopBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            ThemeManager.applyToDialog(confirm);
            confirm.setContentText("This will stop the real VaultDesk server for everyone right now. Continue?");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    stopBtn.setDisable(true);
                    statusLabel.setText("Working — stopping server, please wait...");
                    statusLabel.setStyle("-fx-text-fill: #d29922; -fx-font-size: 12px;");
                    MaintenanceControlUtil.startMaintenance(
                            () -> javafx.application.Platform.runLater(() -> {
                                statusLabel.setText("✔ Maintenance started — server is stopped.");
                                statusLabel.setStyle("-fx-text-fill: #d29922; -fx-font-size: 12px;");
                                stopBtn.setDisable(false);
                            }),
                            err -> javafx.application.Platform.runLater(() -> {
                                stopBtn.setDisable(false);
                                showAlert("Error", "Could not start maintenance: " + err);
                            })
                    );
                }
            });
        });
        Button resumeBtn = new Button("✔ Resume Server");
        resumeBtn.getStyleClass().add("btn-primary");
        AnimationUtil.addHoverScale(resumeBtn);
        resumeBtn.setOnAction(e -> {
            resumeBtn.setDisable(true);
            statusLabel.setText("Working — resuming server, please wait...");
            statusLabel.setStyle("-fx-text-fill: #d29922; -fx-font-size: 12px;");
            MaintenanceControlUtil.resumeServer(
                    () -> javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("✔ Server resumed successfully.");
                        statusLabel.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 12px;");
                        resumeBtn.setDisable(false);
                    }),
                    err -> javafx.application.Platform.runLater(() -> {
                        resumeBtn.setDisable(false);
                        showAlert("Error", "Could not resume: " + err);
                    })
            );
        });

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.getColumnConstraints().addAll(labelValueCols(160));
        grid.add(scheduleLabel, 0, 0);   grid.add(minutesField, 1, 0);
        grid.add(new Label("Message:"), 0, 1); grid.add(messageField, 1, 1);

        HBox scheduleBtns = new HBox(10, scheduleBtn, cancelBtn);
        HBox controlBtns = new HBox(10, stopBtn, resumeBtn);

        return new VBox(12, warningLabel, grid, scheduleBtns, new Separator(), controlBtns, statusLabel);
    }

    private VBox buildBackupSection() {
        Label infoLabel = new Label("Loading database info...");
        infoLabel.getStyleClass().add("text-muted");
        infoLabel.setStyle("-fx-font-size: 12px;");

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px;");

        try {
            HttpResponse<String> resp = ApiClient.get(ConfigManager.getBaseUrl() + "/api/backup/info");
            if (resp.statusCode() == 200) {
                String body = resp.body();
                String size = extractValue(body, "sizeKb");
                String modified = extractValue(body, "lastModified");
                infoLabel.setText(
                        "Database size: " + size + " KB" +
                                "  •  Last modified: " + modified);
            }
        } catch (Exception ex) {
            infoLabel.setText("Could not load database info.");
        }

        Button backupBtn = new Button("⬇  Download Backup");
        backupBtn.getStyleClass().add("btn-primary");
        AnimationUtil.addHoverScale(backupBtn);

        backupBtn.setOnAction(e -> {
            LoadingUtil.setButtonLoading(backupBtn, "Downloading...");
            Thread t = new Thread(() -> {
                try {
                    HttpResponse<byte[]> resp = ApiClient.getBytes(ConfigManager.getBaseUrl() + "/api/backup/download");

                    if (resp.statusCode() == 200) {
                        String home = System.getProperty("user.home");
                        String timestamp = java.time.LocalDateTime
                                .now().format(java.time.format
                                        .DateTimeFormatter.ofPattern(
                                                "yyyyMMdd_HHmmss"));
                        java.io.File out = new java.io.File(
                                home + "\\Downloads\\vaultdesk_backup_"
                                        + timestamp + ".db");
                        java.nio.file.Files.write(
                                out.toPath(), resp.body());

                        javafx.application.Platform.runLater(() -> {
                            statusLabel.setText(
                                    "✔ Backup saved to Downloads: "
                                            + out.getName());
                            statusLabel.setStyle(
                                    "-fx-text-fill: #3fb950;" +
                                            "-fx-font-size: 12px;");
                            LoadingUtil.resetButton(
                                    backupBtn, "⬇  Download Backup");
                        });
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            statusLabel.setText(
                                    "✘ Backup failed: "
                                            + resp.statusCode());
                            statusLabel.setStyle(
                                    "-fx-text-fill: #f85149;" +
                                            "-fx-font-size: 12px;");
                            LoadingUtil.resetButton(
                                    backupBtn, "⬇  Download Backup");
                        });
                    }
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText(
                                "✘ Error: " + ex.getMessage());
                        statusLabel.setStyle(
                                "-fx-text-fill: #f85149;" +
                                        "-fx-font-size: 12px;");
                        LoadingUtil.resetButton(
                                backupBtn, "⬇  Download Backup");
                    });
                }
            });
            t.setDaemon(true);
            t.start();
        });

        Label noteLabel = new Label(
                "Backup is saved to your Downloads folder.\n" +
                        "Keep backups regularly — recommended weekly.");
        noteLabel.getStyleClass().add("text-muted");
        noteLabel.setStyle("-fx-font-size: 11px;");
        noteLabel.setWrapText(true);

        return new VBox(10, infoLabel, backupBtn,
                statusLabel, noteLabel);
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
            if (end == -1) end = json.indexOf("}", start);
            if (end == -1) end = json.length();
            return json.substring(start, end).trim();
        }
        start += search.length();
        return json.substring(start, json.indexOf("\"", start));
    }

    private VBox sectionCard(String heading, VBox content) {
        Label headLabel = new Label(heading);
        headLabel.getStyleClass().add("section-title");

        Separator sep = new Separator();

        VBox card = new VBox(12, headLabel, sep, content);
        card.getStyleClass().add("settings-card");
        return card;
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ThemeManager.applyToDialog(alert);
        alert.showAndWait();
    }

    private void applyThemeToAll() {
        javafx.stage.Stage stage =
                (javafx.stage.Stage) javafx.stage.Window.getWindows()
                        .stream()
                        .filter(w -> w instanceof javafx.stage.Stage)
                        .findFirst().orElse(null);
        if (stage != null && stage.getScene() != null)
            ThemeManager.apply(stage.getScene());
    }
}