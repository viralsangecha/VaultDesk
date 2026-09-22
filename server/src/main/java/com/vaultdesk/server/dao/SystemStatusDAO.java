package com.vaultdesk.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SystemStatusDAO {
    private final JdbcTemplate jdbc;

    public SystemStatusDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Status(String scheduledAt, String message) {}

    public Status getStatus() {
        try {
            return jdbc.queryForObject(
                    "SELECT scheduled_at, maintenance_message FROM system_status WHERE id = 1",
                    (rs, rowNum) -> new Status(rs.getString("scheduled_at"), rs.getString("maintenance_message")));
        } catch (Exception e) {
            return new Status(null, "VaultDesk will be briefly unavailable for scheduled maintenance.");
        }
    }

    public void schedule(String isoDateTime, String message) {
        jdbc.update("UPDATE system_status SET scheduled_at = ?, maintenance_message = ? WHERE id = 1",
                isoDateTime, message);
    }

    public void clearSchedule() {
        jdbc.update("UPDATE system_status SET scheduled_at = NULL WHERE id = 1");
    }
}