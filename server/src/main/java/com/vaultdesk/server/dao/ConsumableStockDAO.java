package com.vaultdesk.server.dao;

import com.vaultdesk.server.model.ConsumableStock;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ConsumableStockDAO {
    private final JdbcTemplate jdbc;

    public ConsumableStockDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ConsumableStock> getAllConsumables() {
        try {
            List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM consumable_stock");
            List<ConsumableStock> list = new ArrayList<>();
            for (Map<String,Object> row : rows) {
                list.add(mapRow(row));
            }
            return list;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public ConsumableStock getConsumableById(int id) {
        try {
            Map<String,Object> row = jdbc.queryForMap("SELECT * FROM consumable_stock WHERE id=?", id);
            return mapRow(row);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<ConsumableStock> getLowStock() {
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT * FROM consumable_stock WHERE quantity_in_stock <= reorder_level");
        List<ConsumableStock> list = new ArrayList<>();
        for (Map<String,Object> row : rows) {
            list.add(mapRow(row));
        }
        return list;
    }

    public void saveConsumable(ConsumableStock c) {
        jdbc.update(
                "INSERT INTO consumable_stock (name, category, compatible_models, quantity_in_stock, " +
                        "reorder_level, unit, vendor_id, unit_cost, storage_location, notes) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                c.name(), c.category(), c.compatibleModels(), c.quantityInStock(),
                c.reorderLevel(), c.unit(), c.vendorId(), c.unitCost(),
                c.storageLocation(), c.notes()
        );
    }

    public int updateConsumable(ConsumableStock c) {
        return jdbc.update(
                "UPDATE consumable_stock SET name=?, category=?, compatible_models=?, " +
                        "reorder_level=?, unit=?, vendor_id=?, unit_cost=?, storage_location=?, notes=? " +
                        "WHERE id = ?",
                c.name(), c.category(), c.compatibleModels(),
                c.reorderLevel(), c.unit(), c.vendorId(),
                c.unitCost(), c.storageLocation(), c.notes(), c.id());
    }

    public int updateQuantity(int id, int newQuantity, String changeType, String notes, int changedBy) {
        ConsumableStock existing = getConsumableById(id);
        if (existing == null) return 0;
        int oldQuantity = existing.quantityInStock();

        int rows = jdbc.update(
                "UPDATE consumable_stock SET quantity_in_stock = ?, last_updated = datetime('now','+5 hours','+30 minutes') WHERE id = ?",
                newQuantity, id);

        if (rows > 0) {
            jdbc.update(
                    "INSERT INTO consumable_stock_history " +
                            "(consumable_id, old_quantity, new_quantity, change_amount, change_type, changed_by, notes) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    id, oldQuantity, newQuantity, newQuantity - oldQuantity, changeType, changedBy, notes);
        }
        return rows;
    }

    public List<Map<String, Object>> getStockHistory(int consumableId) {
        return jdbc.queryForList(
                "SELECT h.*, u.full_name as changed_by_name " +
                        "FROM consumable_stock_history h " +
                        "LEFT JOIN users u ON h.changed_by = u.id " +
                        "WHERE h.consumable_id = ? ORDER BY h.id DESC",
                consumableId);
    }

    private ConsumableStock mapRow(Map<String,Object> row) {
        return new ConsumableStock(
                ((Number) row.get("id")).intValue(),
                (String) row.get("name"),
                (String) row.get("category"),
                (String) row.get("compatible_models"),
                row.get("quantity_in_stock") != null ? ((Number) row.get("quantity_in_stock")).intValue() : 0,
                row.get("reorder_level") != null ? ((Number) row.get("reorder_level")).intValue() : 0,
                (String) row.get("unit"),
                row.get("vendor_id") != null ? ((Number) row.get("vendor_id")).intValue() : 0,
                row.get("unit_cost") != null ? ((Number) row.get("unit_cost")).doubleValue() : 0.0,
                (String) row.get("storage_location"),
                (String) row.get("notes"),
                (String) row.get("last_updated")
        );
    }
}