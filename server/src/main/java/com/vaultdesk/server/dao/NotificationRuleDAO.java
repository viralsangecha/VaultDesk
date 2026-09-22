package com.vaultdesk.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class NotificationRuleDAO {
    private final JdbcTemplate jdbc;

    public NotificationRuleDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Rule(String eventKey, boolean enabled, boolean notifyReporter) {}

    private Rule mapRow(Map<String, Object> row) {
        return new Rule(
                (String) row.get("event_key"),
                ((Number) row.get("enabled")).intValue() == 1,
                ((Number) row.get("notify_reporter")).intValue() == 1
        );
    }

    public List<Rule> getAllRules() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM notification_rules");
        List<Rule> result = new ArrayList<>();
        for (Map<String, Object> row : rows) result.add(mapRow(row));
        return result;
    }

    public Rule getRule(String eventKey) {
        try {
            Map<String, Object> row = jdbc.queryForMap("SELECT * FROM notification_rules WHERE event_key = ?", eventKey);
            return mapRow(row);
        } catch (Exception e) {
            return new Rule(eventKey, true, true);
        }
    }

    public void updateRule(String eventKey, boolean enabled, boolean notifyReporter) {
        int rows = jdbc.update(
                "UPDATE notification_rules SET enabled = ?, notify_reporter = ? WHERE event_key = ?",
                enabled ? 1 : 0, notifyReporter ? 1 : 0, eventKey);
        if (rows == 0) {
            jdbc.update("INSERT INTO notification_rules (event_key, enabled, notify_reporter) VALUES (?, ?, ?)",
                    eventKey, enabled ? 1 : 0, notifyReporter ? 1 : 0);
        }
    }
}