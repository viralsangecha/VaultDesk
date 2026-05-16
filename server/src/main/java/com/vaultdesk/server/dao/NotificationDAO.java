package com.vaultdesk.server.dao;

import com.vaultdesk.server.model.Notification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class NotificationDAO {

    private final JdbcTemplate jdbc;

    public NotificationDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ── Create notification for a user (admin/engineer) ──
    public void notifyUser(int userId, String message,
                           String type, int referenceId) {
        jdbc.update(
                "INSERT INTO notifications " +
                        "(user_id, message, type, reference_id, is_read, created_at) " +
                        "VALUES (?, ?, ?, ?, 0, datetime('now'))",
                userId, message, type, referenceId);
    }

    // ── Create notification for an employee ───────────────
    public void notifyEmployee(int employeeId, String message,
                               String type, int referenceId) {
        jdbc.update(
                "INSERT INTO notifications " +
                        "(employee_id, message, type, reference_id, is_read, created_at) " +
                        "VALUES (?, ?, ?, ?, 0, datetime('now'))",
                employeeId, message, type, referenceId);
    }

    // ── Notify all admin users ────────────────────────────
    public void notifyAllAdmins(JdbcTemplate jdbc, String message,
                                String type, int referenceId) {
        List<Map<String, Object>> admins = jdbc.queryForList(
                "SELECT id FROM users WHERE role = 'ADMIN' AND active = 1");
        for (Map<String, Object> admin : admins) {
            int adminId = ((Number) admin.get("id")).intValue();
            notifyUser(adminId, message, type, referenceId);
        }
    }

    // ── Get unread notifications for user ─────────────────
    public List<Notification> getUnreadForUser(int userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM notifications " +
                        "WHERE user_id = ? AND is_read = 0 " +
                        "ORDER BY created_at DESC LIMIT 20", userId);
        return mapRows(rows);
    }

    // ── Get all notifications for user (read + unread) ────
    public List<Notification> getAllForUser(int userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM notifications " +
                        "WHERE user_id = ? " +
                        "ORDER BY created_at DESC LIMIT 20", userId);
        return mapRows(rows);
    }

    // ── Get unread for employee ───────────────────────────
    public List<Notification> getUnreadForEmployee(int employeeId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM notifications " +
                        "WHERE employee_id = ? AND is_read = 0 " +
                        "ORDER BY created_at DESC LIMIT 20", employeeId);
        return mapRows(rows);
    }

    // ── Get all for employee ──────────────────────────────
    public List<Notification> getAllForEmployee(int employeeId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM notifications " +
                        "WHERE employee_id = ? " +
                        "ORDER BY created_at DESC LIMIT 20", employeeId);
        return mapRows(rows);
    }

    // ── Unread count for user ─────────────────────────────
    public int getUnreadCountForUser(int userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications " +
                        "WHERE user_id = ? AND is_read = 0", Integer.class, userId);
        return count != null ? count : 0;
    }

    // ── Unread count for employee ─────────────────────────
    public int getUnreadCountForEmployee(int employeeId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications " +
                        "WHERE employee_id = ? AND is_read = 0",
                Integer.class, employeeId);
        return count != null ? count : 0;
    }

    // ── Mark single notification as read ──────────────────
    public void markRead(int id) {
        jdbc.update("UPDATE notifications SET is_read = 1 WHERE id = ?", id);
    }

    // ── Mark all read for user ────────────────────────────
    public void markAllReadForUser(int userId) {
        jdbc.update(
                "UPDATE notifications SET is_read = 1 WHERE user_id = ?",
                userId);
    }

    // ── Mark all read for employee ────────────────────────
    public void markAllReadForEmployee(int employeeId) {
        jdbc.update(
                "UPDATE notifications SET is_read = 1 WHERE employee_id = ?",
                employeeId);
    }

    private List<Notification> mapRows(List<Map<String, Object>> rows) {
        List<Notification> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            list.add(new Notification(
                    ((Number) row.get("id")).intValue(),
                    row.get("user_id") != null
                            ? ((Number) row.get("user_id")).intValue() : 0,
                    row.get("employee_id") != null
                            ? ((Number) row.get("employee_id")).intValue() : 0,
                    (String) row.get("message"),
                    (String) row.get("type"),
                    row.get("reference_id") != null
                            ? ((Number) row.get("reference_id")).intValue() : 0,
                    row.get("is_read") != null
                            ? ((Number) row.get("is_read")).intValue() : 0,
                    (String) row.get("created_at")
            ));
        }
        return list;
    }
}