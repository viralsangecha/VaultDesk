package com.vaultdesk.server.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class UserPermissionDAO {

    private final JdbcTemplate jdbc;

    public UserPermissionDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── Get all permissions for a user ────────────────────
    public List<String> getPermissions(int userId) {
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT permission FROM user_permissions " +
                        "WHERE user_id = ? AND granted = 1", userId);
        List<String> perms = new ArrayList<>();
        for (Map<String,Object> row : rows) {
            perms.add((String) row.get("permission"));
        }
        return perms;
    }

    // ── Check single permission ───────────────────────────
    public boolean hasPermission(int userId, String permission) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_permissions " +
                        "WHERE user_id = ? AND permission = ? AND granted = 1",
                Integer.class, userId, permission);
        return count != null && count > 0;
    }

    // ── Set permissions for a user (replaces all) ─────────
    public void setPermissions(int userId, List<String> permissions) {
        // Delete all existing
        jdbc.update("DELETE FROM user_permissions WHERE user_id = ?",
                userId);
        // Insert new ones
        for (String perm : permissions) {
            jdbc.update(
                    "INSERT INTO user_permissions " +
                            "(user_id, permission, granted) VALUES (?,?,1)",
                    userId, perm);
        }
    }

    // ── Get all defined permissions ───────────────────────
    public List<String> getAllDefinedPermissions() {
        return java.util.List.of(
                "VIEW_ALL_TICKETS", "VIEW_ASSIGNED_TICKETS",
                "UPDATE_TICKET_STATUS", "ASSIGN_TICKET",
                "DELETE_TICKET", "VIEW_ALL_ASSETS",
                "VIEW_DEPT_ASSETS", "ADD_ASSET", "EDIT_ASSET",
                "IMPORT_ASSETS", "VIEW_EMPLOYEES", "ADD_EMPLOYEE",
                "EDIT_EMPLOYEE", "SET_LOGIN", "VIEW_DEPARTMENTS",
                "ADD_DEPARTMENT", "VIEW_REPORTS", "VIEW_LICENSES",
                "ADD_LICENSE", "VIEW_CONSUMABLES", "ADD_CONSUMABLE",
                "VIEW_MAINTENANCE", "ADD_MAINTENANCE",
                "VIEW_VENDORS", "ADD_VENDOR",
                "MANAGE_USERS", "MANAGE_SETTINGS"
        );
    }
}