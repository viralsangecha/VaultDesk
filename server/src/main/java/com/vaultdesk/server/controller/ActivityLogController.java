package com.vaultdesk.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class ActivityLogController {

    private final JdbcTemplate jdbc;

    public ActivityLogController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ResponseEntity<?> getActivityLog(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String userId) {
        try {
            String sql;
            List<Map<String,Object>> rows;

            if (userId != null && !userId.isEmpty()) {
                sql = "SELECT al.*, u.full_name as user_name " +
                        "FROM activity_log al " +
                        "LEFT JOIN users u ON al.user_id = u.id " +
                        "WHERE al.user_id = ? " +
                        "ORDER BY al.logged_at DESC LIMIT ?";
                rows = jdbc.queryForList(sql,
                        Integer.parseInt(userId), limit);
            } else {
                sql = "SELECT al.*, u.full_name as user_name " +
                        "FROM activity_log al " +
                        "LEFT JOIN users u ON al.user_id = u.id " +
                        "ORDER BY al.logged_at DESC LIMIT ?";
                rows = jdbc.queryForList(sql, limit);
            }
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @PostMapping
    public ResponseEntity<?> logActivity(
            @RequestBody Map<String, String> body) {
        try {
            int userId = body.get("userId") != null
                    ? Integer.parseInt(body.get("userId")) : 0;
            String action    = body.get("action");
            String tableName = body.get("tableName");
            int recordId     = body.get("recordId") != null
                    ? Integer.parseInt(body.get("recordId")) : 0;
            String details   = body.get("details");

            jdbc.update(
                    "INSERT INTO activity_log " +
                            "(user_id, action, table_name, " +
                            "record_id, details, logged_at) " +
                            "VALUES (?, ?, ?, ?, ?, datetime('now'))",
                    userId, action, tableName,
                    recordId, details);
            return ResponseEntity.status(201).body("Logged");
        } catch (Exception e) {
            return ResponseEntity.ok("Logged");
        }
    }
}