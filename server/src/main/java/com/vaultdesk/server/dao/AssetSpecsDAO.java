package com.vaultdesk.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AssetSpecsDAO {

    private final JdbcTemplate jdbc;

    public AssetSpecsDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> getSpecsByAsset(int assetId) {
        return jdbc.queryForList(
                "SELECT * FROM asset_specs WHERE asset_id = ?",
                assetId);
    }

    public void saveSpec(int assetId, String key, String value) {
        jdbc.update(
                "INSERT OR REPLACE INTO asset_specs " +
                        "(asset_id, spec_key, spec_value) VALUES (?, ?, ?)",
                assetId, key, value);
    }

    public void updateSpec(int assetId, String key, String value) {
        int rows = jdbc.update(
                "UPDATE asset_specs SET spec_value = ? " +
                        "WHERE asset_id = ? AND spec_key = ?",
                value, assetId, key);
        if (rows == 0) saveSpec(assetId, key, value);
    }

    public void deleteSpec(int id) {
        jdbc.update("DELETE FROM asset_specs WHERE id = ?", id);
    }

    public void deleteAllSpecs(int assetId) {
        jdbc.update(
                "DELETE FROM asset_specs WHERE asset_id = ?",
                assetId);
    }

    public void saveAllSpecs(int assetId,
                             Map<String, String> specs) {
        deleteAllSpecs(assetId);
        for (Map.Entry<String, String> entry : specs.entrySet()) {
            if (entry.getValue() != null
                    && !entry.getValue().trim().isEmpty()) {
                saveSpec(assetId, entry.getKey(),
                        entry.getValue().trim());
            }
        }
    }
}