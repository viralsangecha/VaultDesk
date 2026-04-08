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

    public int updateQuantity(int id, int quantity) {
        return jdbc.update(
                "UPDATE consumable_stock SET quantity_in_stock = ?, last_updated = datetime('now') WHERE id = ?",
                quantity, id);
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