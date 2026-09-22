package com.vaultdesk.admin;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpConnectTimeoutException;

public class ApiClient {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static volatile boolean maintenanceDialogShowing = false;

    public static HttpResponse<String> get(String url) throws Exception {
        return checkAndReturn(() -> {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + SessionManager.get().getToken())
                    .GET().build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        });
    }

    public static HttpResponse<String> post(String url, String body) throws Exception {
        return checkAndReturn(() -> {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + SessionManager.get().getToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        });
    }

    public static HttpResponse<String> put(String url, String body) throws Exception {
        return checkAndReturn(() -> {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + SessionManager.get().getToken())
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body)).build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        });
    }

    public static HttpResponse<String> putNoBody(String url) throws Exception {
        return checkAndReturn(() -> {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + SessionManager.get().getToken())
                    .PUT(HttpRequest.BodyPublishers.noBody()).build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        });
    }

    public static HttpResponse<String> delete(String url) throws Exception {
        return checkAndReturn(() -> {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + SessionManager.get().getToken())
                    .DELETE().build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        });
    }

    public static HttpResponse<byte[]> getBytes(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + SessionManager.get().getToken())
                .GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofByteArray());
    }

    // ── Shared maintenance-detection wrapper ──────────────
    private interface Call<T> { T run() throws Exception; }

    private static HttpResponse<String> checkAndReturn(Call<HttpResponse<String>> call) throws Exception {
        try {
            HttpResponse<String> resp = call.run();
            if (resp.statusCode() == 503) {
                showMaintenanceDialog(extractMessage(resp.body()));
            }
            return resp;
        } catch (ConnectException | HttpConnectTimeoutException ex) {
            showMaintenanceDialog("Cannot reach the VaultDesk server right now. It may be restarting — please try again in a moment.");
            throw ex;
        }
    }

    private static String extractMessage(String body) {
        try {
            String search = "\"message\":\"";
            int start = body.indexOf(search);
            if (start == -1) return "VaultDesk is currently under maintenance.";
            start += search.length();
            int end = body.indexOf("\"", start);
            return body.substring(start, end);
        } catch (Exception e) {
            return "VaultDesk is currently under maintenance.";
        }
    }

    private static void showMaintenanceDialog(String message) {
        if (maintenanceDialogShowing) return; // don't stack a dialog per failed call — one is enough
        maintenanceDialogShowing = true;
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            ThemeManager.applyToDialog(alert);
            alert.setTitle("Under Maintenance");
            alert.setHeaderText("🛠️  VaultDesk is temporarily unavailable");
            alert.setContentText(message);
            alert.showAndWait();
            maintenanceDialogShowing = false;
        });
    }
}