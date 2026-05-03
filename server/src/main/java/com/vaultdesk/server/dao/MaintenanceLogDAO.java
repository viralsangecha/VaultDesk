package com.vaultdesk.server.dao;

import com.vaultdesk.server.model.MaintenanceLog;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MaintenanceLogDAO {
    private final JdbcTemplate jdbc;

    public MaintenanceLogDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<MaintenanceLog> getAllLogs() {
        try {
            List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM maintenance_log");
            List<MaintenanceLog> list = new ArrayList<>();
            for (Map<String,Object> row : rows) {
                list.add(mapRow(row));
            }
            return list;
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<MaintenanceLog> getLogsByAsset(int assetId) {
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT * FROM maintenance_log WHERE asset_id = ? ORDER BY maintenance_date DESC", assetId);
        List<MaintenanceLog> list = new ArrayList<>();
        for (Map<String,Object> row : rows) {
            list.add(mapRow(row));
        }
        return list;
    }

    public void saveLog(MaintenanceLog log) {
        jdbc.update(
                "INSERT INTO maintenance_log (asset_id, maintenance_type, description, " +
                        "done_by_internal, done_by_vendor, cost, maintenance_date, next_due_date, " +
                        "status, notes, logged_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                log.assetId(), log.maintenanceType(), log.description(),
                log.doneByInternal(), log.doneByVendor(), log.cost(),
                log.maintenanceDate(), log.nextDueDate(), log.status(),
                log.notes(), log.loggedBy()
        );
    }

    private MaintenanceLog mapRow(Map<String,Object> row) {
        return new MaintenanceLog(
                ((Number) row.get("id")).intValue(),
                row.get("asset_id") != null ? ((Number) row.get("asset_id")).intValue() : 0,
                (String) row.get("maintenance_type"),
                (String) row.get("description"),
                row.get("done_by_internal") != null ? ((Number) row.get("done_by_internal")).intValue() : 0,
                row.get("done_by_vendor") != null ? ((Number) row.get("done_by_vendor")).intValue() : 0,
                row.get("cost") != null ? ((Number) row.get("cost")).doubleValue() : 0.0,
                (String) row.get("maintenance_date"),
                (String) row.get("next_due_date"),
                (String) row.get("status"),
                (String) row.get("notes"),
                row.get("logged_by") != null ? ((Number) row.get("logged_by")).intValue() : 0,
                (String) row.get("created_at")
        );
    }

    public List<MaintenanceLog> getLogsByDept(int deptId) {
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT m.* FROM maintenance_log m " +
                        "JOIN assets a ON m.asset_id = a.id " +
                        "WHERE a.department_id = ?", deptId);
        List<MaintenanceLog> list = new ArrayList<>();
        for (Map<String,Object> row : rows) {
            list.add(mapRow(row));
        }
        return list;
    }
}