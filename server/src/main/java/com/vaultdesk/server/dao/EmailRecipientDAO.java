package com.vaultdesk.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EmailRecipientDAO {
    private final JdbcTemplate jdbc;

    public EmailRecipientDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> getAllRecipients() {
        return jdbc.queryForList("SELECT * FROM email_recipients WHERE active = 1 ORDER BY email");
    }

    public List<Map<String, Object>> getActiveSubscribers(String eventKey) {
        return jdbc.queryForList(
                "SELECT * FROM email_recipients WHERE active = 1 AND (',' || subscribed_events || ',') LIKE ?",
                "%," + eventKey + ",%");
    }

    public void addRecipient(String email, String name) {
        jdbc.update("INSERT INTO email_recipients (email, name, active, subscribed_events) VALUES (?, ?, 1, '')", email, name);
    }

    public void updateSubscriptions(int recipientId, List<String> eventKeys) {
        jdbc.update("UPDATE email_recipients SET subscribed_events = ? WHERE id = ?",
                String.join(",", eventKeys), recipientId);
    }
    public void remove(int id) {
        jdbc.update("DELETE FROM email_recipients WHERE id = ?", id);
    }
}