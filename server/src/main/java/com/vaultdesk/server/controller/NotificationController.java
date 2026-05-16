package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.NotificationDAO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationDAO notificationDAO;

    public NotificationController(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    // ── Admin/Engineer endpoints ──────────────────────────
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserNotifications(
            @PathVariable int userId) {
        return ResponseEntity.ok(
                notificationDAO.getAllForUser(userId));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<?> getUserUnread(
            @PathVariable int userId) {
        return ResponseEntity.ok(Map.of(
                "count", notificationDAO.getUnreadCountForUser(userId),
                "notifications", notificationDAO.getUnreadForUser(userId)
        ));
    }

    @PutMapping("/user/{userId}/readall")
    public ResponseEntity<?> markAllReadUser(
            @PathVariable int userId) {
        notificationDAO.markAllReadForUser(userId);
        return ResponseEntity.ok("All marked as read");
    }

    // ── Employee endpoints ────────────────────────────────
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getEmployeeNotifications(
            @PathVariable int employeeId) {
        return ResponseEntity.ok(
                notificationDAO.getAllForEmployee(employeeId));
    }

    @GetMapping("/employee/{employeeId}/unread")
    public ResponseEntity<?> getEmployeeUnread(
            @PathVariable int employeeId) {
        return ResponseEntity.ok(Map.of(
                "count", notificationDAO
                        .getUnreadCountForEmployee(employeeId),
                "notifications", notificationDAO
                        .getUnreadForEmployee(employeeId)
        ));
    }

    @PutMapping("/employee/{employeeId}/readall")
    public ResponseEntity<?> markAllReadEmployee(
            @PathVariable int employeeId) {
        notificationDAO.markAllReadForEmployee(employeeId);
        return ResponseEntity.ok("All marked as read");
    }

    // ── Mark single as read ───────────────────────────────
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable int id) {
        notificationDAO.markRead(id);
        return ResponseEntity.ok("Marked as read");
    }
}