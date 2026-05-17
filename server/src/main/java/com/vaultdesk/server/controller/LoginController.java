package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.UserDAO;
import com.vaultdesk.server.dao.UserPermissionDAO;
import com.vaultdesk.server.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final UserDAO userDAO;
    private final UserPermissionDAO permissionDAO;

    public LoginController(UserDAO userDAO,
                           UserPermissionDAO permissionDAO) {
        this.userDAO       = userDAO;
        this.permissionDAO = permissionDAO;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md =
                    MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String hashed   = sha256(password);

        if (userDAO.validateLogin(username, hashed)) {
            User user = userDAO.getUserByUsername(username);
            userDAO.updateLastLogin(user.id());
            List<String> permissions =
                    permissionDAO.getPermissions(user.id());
            return ResponseEntity.ok(Map.of(
                    "success",     true,
                    "message",     "Login successful",
                    "role",        user.role(),
                    "fullName",    user.fullName(),
                    "userId",      user.id(),
                    "deptId",      user.deptId(),
                    "permissions", permissions
            ));
        }
        return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Invalid credentials"
        ));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(
            @RequestBody Map<String, String> body) {
        String username     = body.get("username");
        String passwordHash = body.get("passwordHash");

        if (username == null || passwordHash == null)
            return ResponseEntity.badRequest().build();

        if (userDAO.validateLogin(username, passwordHash)) {
            User user = userDAO.getUserByUsername(username);
            userDAO.updateLastLogin(user.id());
            List<String> permissions =
                    permissionDAO.getPermissions(user.id());
            return ResponseEntity.ok(Map.of(
                    "success",     true,
                    "message",     "Session valid",
                    "role",        user.role(),
                    "fullName",    user.fullName(),
                    "userId",      user.id(),
                    "deptId",      user.deptId(),
                    "permissions", permissions
            ));
        }
        return ResponseEntity.status(401).build();
    }
}