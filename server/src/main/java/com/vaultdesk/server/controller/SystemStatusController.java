package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.SystemStatusDAO;
import com.vaultdesk.server.security.AuthContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/system-status")
public class SystemStatusController {

    private final SystemStatusDAO statusDAO;

    public SystemStatusController(SystemStatusDAO statusDAO) {
        this.statusDAO = statusDAO;
    }

    @GetMapping
    public ResponseEntity<?> get() {
        SystemStatusDAO.Status s = statusDAO.getStatus();
        Long minutesRemaining = null;
        if (s.scheduledAt() != null && !s.scheduledAt().isBlank()) {
            try {
                LocalDateTime scheduled = LocalDateTime.parse(s.scheduledAt());
                minutesRemaining = Duration.between(LocalDateTime.now(), scheduled).toMinutes();
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(Map.of(
                "scheduledAt", s.scheduledAt() == null ? "" : s.scheduledAt(),
                "message", s.message(),
                "minutesRemaining", minutesRemaining == null ? -1 : minutesRemaining
        ));
    }

    @PutMapping("/schedule")
    public ResponseEntity<?> schedule(@RequestBody Map<String, String> body) {
        if (!isAdmin()) return ResponseEntity.status(403).body("Only admins can schedule maintenance.");
        statusDAO.schedule(body.get("scheduledAt"), body.getOrDefault("message",
                "VaultDesk will be briefly unavailable for scheduled maintenance."));
        return ResponseEntity.ok("Scheduled");
    }

    @DeleteMapping("/schedule")
    public ResponseEntity<?> cancel() {
        if (!isAdmin()) return ResponseEntity.status(403).body("Only admins can cancel maintenance.");
        statusDAO.clearSchedule();
        return ResponseEntity.ok("Cancelled");
    }

    private boolean isAdmin() {
        return AuthContext.get() != null && "ADMIN".equals(AuthContext.get().role());
    }
}