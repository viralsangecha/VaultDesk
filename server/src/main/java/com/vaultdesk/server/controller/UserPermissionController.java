package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.UserPermissionDAO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserPermissionController {

    private final UserPermissionDAO permissionDAO;

    public UserPermissionController(UserPermissionDAO permissionDAO) {
        this.permissionDAO = permissionDAO;
    }

    // ── Get permissions for a user ────────────────────────
    @GetMapping("/{id}/permissions")
    public ResponseEntity<?> getPermissions(@PathVariable int id) {
        return ResponseEntity.ok(Map.of(
                "userId", id,
                "permissions", permissionDAO.getPermissions(id),
                "allPermissions",
                permissionDAO.getAllDefinedPermissions()
        ));
    }

    // ── Set permissions for a user ────────────────────────
    @PutMapping("/{id}/permissions")
    public ResponseEntity<?> setPermissions(
            @PathVariable int id,
            @RequestBody Map<String, Object> body) {
        Object permsObj = body.get("permissions");
        if (permsObj == null)
            return ResponseEntity.badRequest()
                    .body("permissions field required");

        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) permsObj;
        permissionDAO.setPermissions(id, permissions);
        return ResponseEntity.ok("Permissions updated");
    }
}