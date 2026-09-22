package com.vaultdesk.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmailSettingsDAO {
    private final JdbcTemplate jdbc;

    public EmailSettingsDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record EmailSettings(String smtpHost, int smtpPort, String username,
                                String password, String fromName, boolean enabled, String publicUrl) {}

    public EmailSettings getSettings() {
        try {
            Map<String, Object> row = jdbc.queryForMap("SELECT * FROM email_settings WHERE id = 1");
            return new EmailSettings(
                    (String) row.get("smtp_host"),
                    ((Number) row.get("smtp_port")).intValue(),
                    (String) row.get("username"),
                    (String) row.get("password"),
                    (String) row.get("from_name"),
                    ((Number) row.get("enabled")).intValue() == 1,
                    (String) row.get("public_url")
            );
        } catch (Exception e) {
            return null; // not configured yet
        }
    }

    public void saveSettings(String host, int port, String username, String password,
                             String fromName, boolean enabled, String publicUrl) {
        int rows = jdbc.update(
                "UPDATE email_settings SET smtp_host=?, smtp_port=?, username=?, password=?, " +
                        "from_name=?, enabled=?, public_url=? WHERE id = 1",
                host, port, username, password, fromName, enabled ? 1 : 0, publicUrl);
        if (rows == 0) {
            jdbc.update(
                    "INSERT INTO email_settings (id, smtp_host, smtp_port, username, password, from_name, enabled, public_url) " +
                            "VALUES (1, ?, ?, ?, ?, ?, ?, ?)",
                    host, port, username, password, fromName, enabled ? 1 : 0, publicUrl);
        }
    }

    public boolean isConfigured() {
        EmailSettings s = getSettings();
        return s != null && s.enabled() && s.smtpHost() != null && !s.smtpHost().isEmpty();
    }

    /** Falls back to localhost:2008 only if nothing has been configured yet. */
    public String getPublicUrl() {
        EmailSettings s = getSettings();
        if (s != null && s.publicUrl() != null && !s.publicUrl().trim().isEmpty()) {
            return s.publicUrl().trim();
        }
        return "http://localhost:2008";
    }
}