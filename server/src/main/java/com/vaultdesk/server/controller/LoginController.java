package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.UserDAO;
import com.vaultdesk.server.dao.UserPermissionDAO;
import com.vaultdesk.server.model.User;
import com.vaultdesk.server.security.TokenStore;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final UserDAO userDAO;
    private final UserPermissionDAO permissionDAO;
    private final JdbcTemplate jdbc;
    private final TokenStore tokenStore;


    public LoginController(UserDAO userDAO,
                           UserPermissionDAO permissionDAO,
                           JdbcTemplate jdbc, TokenStore tokenStore) {
        this.userDAO       = userDAO;
        this.permissionDAO = permissionDAO;
        this.jdbc          = jdbc;
        this.tokenStore = tokenStore;
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
            try {
                jdbc.update(
                        "INSERT INTO activity_log " +
                                "(user_id, action, table_name, " +
                                "record_id, details, logged_at) " +
                                "VALUES (?, 'LOGIN', 'users', ?, ?, datetime('now'))",
                        user.id(), user.id(),
                        "Login: " + user.username());
            } catch (Exception ignored) {}
            logActivity(user.id(), "LOGIN", "users", user.id(),
                    "User logged in: " + user.username());
            List<String> permissions =
                    permissionDAO.getPermissions(user.id());
            String token = tokenStore.issue(user.id(), "ADMIN", user.role());
            return ResponseEntity.ok(Map.of(
                    "success",     true,
                    "message",     "Login successful",
                    "role",        user.role(),
                    "fullName",    user.fullName(),
                    "userId",      user.id(),
                    "deptId",      user.deptId(),
                    "permissions", permissions,
                    "token",       token
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
            String token = tokenStore.issue(user.id(), "ADMIN", user.role());
            return ResponseEntity.ok(Map.of(
                    "success",     true,
                    "message",     "Login successful",
                    "role",        user.role(),
                    "fullName",    user.fullName(),
                    "userId",      user.id(),
                    "deptId",      user.deptId(),
                    "permissions", permissions,
                    "token",       token
            ));
        }
        return ResponseEntity.status(401).build();
    }
    private void logActivity(int userId, String action,
                             String tableName, int recordId,
                             String details) {
        try {
            // Use a direct JDBC call
            // LoginController doesn't have JdbcTemplate
            // so we skip logging here and rely on other controllers
        } catch (Exception e) { }
    }
}