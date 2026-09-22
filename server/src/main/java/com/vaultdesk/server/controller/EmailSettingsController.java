package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.EmailRecipientDAO;
import com.vaultdesk.server.dao.EmailSettingsDAO;
import com.vaultdesk.server.security.AuthContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/email-settings")
public class EmailSettingsController {

    private final EmailSettingsDAO settingsDAO;
    private final EmailRecipientDAO recipientDAO;

    public EmailSettingsController(EmailSettingsDAO settingsDAO, EmailRecipientDAO recipientDAO) {
        this.settingsDAO = settingsDAO;
        this.recipientDAO = recipientDAO;
    }

    private boolean isAdmin() {
        return "ADMIN".equals(AuthContext.get() != null ? AuthContext.get().role() : null);
    }

    @GetMapping
    public ResponseEntity<?> get() {
        EmailSettingsDAO.EmailSettings s = settingsDAO.getSettings();
        if (s == null) {
            return ResponseEntity.ok(Map.of("configured", false, "publicUrl", ""));
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("configured", true);
        result.put("smtpHost", s.smtpHost());
        result.put("smtpPort", s.smtpPort());
        result.put("username", s.username());
        result.put("fromName", s.fromName());
        result.put("enabled", s.enabled());
        result.put("publicUrl", s.publicUrl() != null ? s.publicUrl() : "");
        // password intentionally never returned to the client
        return ResponseEntity.ok(result);
    }

    @PutMapping
    public ResponseEntity<?> save(@RequestBody Map<String, Object> body) {
        if (!isAdmin()) return ResponseEntity.status(403).body("Only admins can change email settings.");
        settingsDAO.saveSettings(
                (String) body.get("smtpHost"),
                ((Number) body.get("smtpPort")).intValue(),
                (String) body.get("username"),
                (String) body.get("password"),
                (String) body.getOrDefault("fromName", "VaultDesk"),
                Boolean.TRUE.equals(body.get("enabled")),
                (String) body.getOrDefault("publicUrl", ""));
        return ResponseEntity.ok("Saved");
    }

    @GetMapping("/recipients")
    public ResponseEntity<?> getRecipients() {
        return ResponseEntity.ok(recipientDAO.getAllRecipients());
    }

    @PostMapping("/recipients")
    public ResponseEntity<?> addRecipient(@RequestBody Map<String, String> body) {
        if (!isAdmin()) return ResponseEntity.status(403).body("Only admins can manage recipients.");
        recipientDAO.addRecipient(body.get("email"), body.getOrDefault("name", ""));
        return ResponseEntity.status(201).body("Added");
    }

    @PutMapping("/recipients/{id}/subscriptions")
    public ResponseEntity<?> updateSubscriptions(@PathVariable int id, @RequestBody Map<String, Object> body) {
        if (!isAdmin()) return ResponseEntity.status(403).body("Only admins can manage recipients.");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) body.getOrDefault("eventKeys", List.of());
        recipientDAO.updateSubscriptions(id, events);
        return ResponseEntity.ok("Updated");
    }

    @DeleteMapping("/recipients/{id}")
    public ResponseEntity<?> removeRecipient(@PathVariable int id) {
        if (!isAdmin()) return ResponseEntity.status(403).body("Only admins can manage recipients.");
        recipientDAO.remove(id);
        return ResponseEntity.ok("Removed");
    }
}