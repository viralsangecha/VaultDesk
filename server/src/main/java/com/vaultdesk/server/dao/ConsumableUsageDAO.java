package com.vaultdesk.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ConsumableUsageDAO {

    private final JdbcTemplate jdbc;

    public ConsumableUsageDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> getUsageByConsumable(
            int consumableId) {
        return jdbc.queryForList(
                "SELECT cul.*, " +
                        "a.name as asset_name, " +
                        "a.asset_tag, " +
                        "e.name as employee_name, " +
                        "u.full_name as used_by_name " +
                        "FROM consumable_usage_log cul " +
                        "LEFT JOIN assets a ON cul.asset_id = a.id " +
                        "LEFT JOIN employees e " +
                        "  ON cul.employee_id = e.id " +
                        "LEFT JOIN users u ON cul.used_by = u.id " +
                        "WHERE cul.consumable_id = ? " +
                        "ORDER BY cul.usage_date DESC",
                consumableId);
    }

    public List<Map<String, Object>> getAllUsage() {
        return jdbc.queryForList(
                "SELECT cul.*, " +
                        "cs.name as consumable_name, " +
                        "a.name as asset_name, " +
                        "a.asset_tag, " +
                        "e.name as employee_name, " +
                        "u.full_name as used_by_name " +
                        "FROM consumable_usage_log cul " +
                        "LEFT JOIN consumable_stock cs " +
                        "  ON cul.consumable_id = cs.id " +
                        "LEFT JOIN assets a ON cul.asset_id = a.id " +
                        "LEFT JOIN employees e " +
                        "  ON cul.employee_id = e.id " +
                        "LEFT JOIN users u ON cul.used_by = u.id " +
                        "ORDER BY cul.usage_date DESC " +
                        "LIMIT 200");
    }

    public boolean logUsage(int consumableId, int assetId,
                            int employeeId, int quantityUsed,
                            int usedBy, String notes) {
        // Check sufficient stock
        Integer current = jdbc.queryForObject(
                "SELECT quantity_in_stock FROM consumable_stock " +
                        "WHERE id = ?", Integer.class, consumableId);
        if (current == null || current < quantityUsed) return false;

        // Log usage
        jdbc.update(
                "INSERT INTO consumable_usage_log " +
                        "(consumable_id, asset_id, employee_id, " +
                        "quantity_used, used_by, usage_date, notes) " +
                        "VALUES (?, ?, ?, ?, ?, datetime('now'), ?)",
                consumableId,
                assetId == 0 ? null : assetId,
                employeeId == 0 ? null : employeeId,
                quantityUsed, usedBy == 0 ? null : usedBy,
                notes);

        // Auto-deduct from stock
        jdbc.update(
                "UPDATE consumable_stock " +
                        "SET quantity_in_stock = quantity_in_stock - ?, " +
                        "last_updated = datetime('now') WHERE id = ?",
                quantityUsed, consumableId);

        return true;
    }
}