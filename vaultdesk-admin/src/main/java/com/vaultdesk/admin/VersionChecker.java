package com.vaultdesk.admin;

import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.util.Properties;

public class VersionChecker {

    private static String currentVersion = null;

    // ── Load current version from bundled properties ──────
    public static String getCurrentVersion() {
        if (currentVersion != null) return currentVersion;
        try (InputStream is = VersionChecker.class
                .getResourceAsStream("/version.properties")) {
            Properties props = new Properties();
            props.load(is);
            currentVersion = props.getProperty("app.version", "1.0.0");
        } catch (Exception e) {
            currentVersion = "1.0.0";
        }
        return currentVersion;
    }

    // ── Check server for latest version ───────────────────
    public static UpdateInfo checkForUpdate() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getBaseUrl()
                            + "/api/version"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req,
                    HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) return null;

            String body = resp.body();
            String latestVersion = extractValue(body, "version");
            String downloadUrl   = extractValue(body, "downloadUrl");
            String changelog     = extractValue(body, "changelog");

            if (isNewerVersion(latestVersion, getCurrentVersion())) {
                return new UpdateInfo(latestVersion, downloadUrl, changelog);
            }
        } catch (Exception e) {
            System.out.println("Version check failed: " + e.getMessage());
        }
        return null; // null means no update available
    }

    // ── Compare versions: "1.1.0" > "1.0.0" ──────────────
    private static boolean isNewerVersion(String latest, String current) {
        try {
            String[] l = latest.split("\\.");
            String[] c = current.split("\\.");
            for (int i = 0; i < Math.min(l.length, c.length); i++) {
                int li = Integer.parseInt(l[i]);
                int ci = Integer.parseInt(c[i]);
                if (li > ci) return true;
                if (li < ci) return false;
            }
            return l.length > c.length;
        } catch (Exception e) {
            return false;
        }
    }

    private static String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }

    // ── Data class for update info ────────────────────────
    public static class UpdateInfo {
        public final String version;
        public final String downloadUrl;
        public final String changelog;

        public UpdateInfo(String version,
                          String downloadUrl,
                          String changelog) {
            this.version     = version;
            this.downloadUrl = downloadUrl;
            this.changelog   = changelog;
        }
    }
}