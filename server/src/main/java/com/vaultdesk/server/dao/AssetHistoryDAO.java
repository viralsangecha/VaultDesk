package com.vaultdesk.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AssetHistoryDAO {

    private final JdbcTemplate jdbc;

    public AssetHistoryDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> getHistoryByAsset(
            int assetId) {
        return jdbc.queryForList(
                "SELECT ah.*, " +
                        "e1.name as from_employee_name, " +
                        "e2.name as to_employee_name, " +
                        "u.full_name as done_by_name " +
                        "FROM asset_history ah " +
                        "LEFT JOIN employees e1 " +
                        "  ON ah.from_employee = e1.id " +
                        "LEFT JOIN employees e2 " +
                        "  ON ah.to_employee = e2.id " +
                        "LEFT JOIN users u ON ah.done_by = u.id " +
                        "WHERE ah.asset_id = ? " +
                        "ORDER BY ah.action_date DESC",
                assetId);
    }

    public void logHistory(int assetId, String action,
                           int fromEmployee, int toEmployee,
                           String notes, int doneBy) {
        jdbc.update(
                "INSERT INTO asset_history " +
                        "(asset_id, action, from_employee, " +
                        "to_employee, action_date, notes, done_by) " +
                        "VALUES (?, ?, ?, ?, datetime('now'), ?, ?)",
                assetId, action,
                fromEmployee == 0 ? null : fromEmployee,
                toEmployee == 0 ? null : toEmployee,
                notes, doneBy == 0 ? null : doneBy);
    }
}