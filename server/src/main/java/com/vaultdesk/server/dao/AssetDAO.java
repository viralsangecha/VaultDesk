package com.vaultdesk.server.dao;

import com.vaultdesk.server.model.Asset;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AssetDAO {
    private final JdbcTemplate jdbc;  // field

    public AssetDAO(JdbcTemplate jdbc) {  // constructor injection
        this.jdbc =jdbc ;
    }

    public List<Asset> getAllAssets()
    {
        try {
            List<Map<String,Object>> rows = jdbc.queryForList(
                    "SELECT * FROM assets");

            List<Asset> assets = new ArrayList<>();

            for (Map<String,Object> row : rows) {
                assets.add(mapRowToAsset(row));
            }
            return assets;
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }
    }
    public Asset getAssetById(int id)
    {
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT * FROM assets where id=?", id);
            return mapRowToAsset(row);
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }
    }

    public List<Asset> getAssetsByDepartment(int departmentId)
    {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT * FROM assets where department_id=?", departmentId);
            List<Asset> assets = new ArrayList<>();
            for (Map<String,Object> row : rows) {
                assets.add(mapRowToAsset(row));  // we will fix duplication below
            }
            return assets;
        }
        catch (EmptyResultDataAccessException e)
        {
            return null;
        }
    }

    public void saveAsset(Asset asset)
    {
        jdbc.update(
                "INSERT INTO assets (asset_tag, name, category, brand, model, serial_number, " +
                        "department_id, location, status, assigned_to, assigned_date, " +
                        "purchase_date, warranty_expiry, vendor_id, purchase_cost, notes) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                asset.assetTag(), asset.name(), asset.category(), asset.brand(),
                asset.model(), asset.serialNumber(), asset.departmentId(), asset.location(),
                asset.status(), asset.assignedTo(), asset.assignedDate(),
                asset.purchaseDate(), asset.warrantyExpiry(), asset.vendorId(),
                asset.purchaseCost(), asset.notes()
        );
    }

    public int updateAssetStatus(int id, String status)
    {
        return jdbc.update(
                "UPDATE assets SET status = ?, updated_at = datetime('now') " +
                        "WHERE id = ?",status,id);
    }

    private Asset mapRowToAsset(Map<String,Object> row) {
        return new Asset(
                ((Number) row.get("id")).intValue(),
                (String) row.get("asset_tag"),
                (String) row.get("name"),
                (String) row.get("category"),
                (String) row.get("brand"),
                (String) row.get("model"),
                (String) row.get("serial_number"),
                row.get("department_id") != null ? ((Number) row.get("department_id")).intValue() : 0,
                (String) row.get("location"),
                (String) row.get("status"),
                row.get("assigned_to") != null ? ((Number) row.get("assigned_to")).intValue() : 0,
                (String) row.get("assigned_date"),
                (String) row.get("purchase_date"),
                (String) row.get("warranty_expiry"),
                row.get("vendor_id") != null ? ((Number) row.get("vendor_id")).intValue() : 0,
                row.get("purchase_cost") != null ? ((Number) row.get("purchase_cost")).doubleValue() : 0.0,
                (String) row.get("notes"),
                (String) row.get("created_at"),
                (String) row.get("updated_at")
        );
    }
}
