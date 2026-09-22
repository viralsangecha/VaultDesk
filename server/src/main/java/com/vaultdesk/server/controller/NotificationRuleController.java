package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.NotificationRuleDAO;
import com.vaultdesk.server.security.AuthContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification-rules")
public class NotificationRuleController {

    private final NotificationRuleDAO ruleDAO;

    public NotificationRuleController(NotificationRuleDAO ruleDAO) {
        this.ruleDAO = ruleDAO;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(ruleDAO.getAllRules());
    }

    @PutMapping("/{eventKey}")
    public ResponseEntity<?> update(@PathVariable String eventKey, @RequestBody Map<String, Object> body) {
        if (!isAdmin()) return ResponseEntity.status(403).body("Only admins can change notification rules.");
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        boolean notifyReporter = Boolean.TRUE.equals(body.get("notifyReporter"));
        ruleDAO.updateRule(eventKey, enabled, notifyReporter);
        return ResponseEntity.ok("Updated");
    }

    private boolean isAdmin() {
        return AuthContext.get() != null && "ADMIN".equals(AuthContext.get().role());
    }
}