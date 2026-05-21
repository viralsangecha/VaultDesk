package com.vaultdesk.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ComponentDAO {

    private final JdbcTemplate jdbc;

    public ComponentDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> getComponentsByAsset(
            int assetId) {
        return jdbc.queryForList(
                "SELECT * FROM components " +
                        "WHERE asset_id = ? ORDER BY component_type",
                assetId);
    }

    public void saveComponent(int assetId, String componentType,
                              String brand, String model,
                              String serialNumber, String specs,
                              String status, String purchaseDate,
                              String notes) {
        jdbc.update(
                "INSERT INTO components " +
                        "(asset_id, component_type, brand, model, " +
                        "serial_number, specs, status, " +
                        "purchase_date, notes, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'))",
                assetId, componentType, brand, model,
                serialNumber, specs, status,
                purchaseDate, notes);
    }

    public void updateComponent(int id, String brand,
                                String model, String serialNumber,
                                String specs, String status,
                                String notes) {
        jdbc.update(
                "UPDATE components SET brand = ?, model = ?, " +
                        "serial_number = ?, specs = ?, " +
                        "status = ?, notes = ? WHERE id = ?",
                brand, model, serialNumber,
                specs, status, notes, id);
    }

    public void deleteComponent(int id) {
        jdbc.update("DELETE FROM components WHERE id = ?", id);
    }
}