package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.UserDAO;
import com.vaultdesk.server.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserDAO userDAO;

    public UserController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userDAO.getAllUsers());
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsersAdmin() {
        return ResponseEntity.ok(userDAO.getAllUsersAdmin());
    }
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        String username  = body.get("username");
        String password  = body.get("password");
        String fullName  = body.get("fullName");
        String role      = body.get("role");
        int deptId       = body.get("deptId") != null
                ? Integer.parseInt(body.get("deptId")) : 0;
        String email     = body.get("email");

        if (username == null || password == null
                || fullName == null || role == null)
            return ResponseEntity.badRequest().body("Missing fields");

        String hash = sha256(password);
        userDAO.saveUser(username, hash, fullName, role, deptId,email);
        return ResponseEntity.status(201).body("User created");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable int id, @RequestBody Map<String, String> body) {
        String fullName = body.get("fullName");
        String username =body.get("username");
        String email    = body.get("email");
        String role     = body.get("role");
        int deptId      = Integer.parseInt(body.get("deptId"));

        int rows = userDAO.updateUser(id, fullName,username, email, role, deptId);

        if (rows == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok("User updated");
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> changePassword(@PathVariable int id,
                                            @RequestBody Map<String, String> body) {
        String currentPassword = body.get("currentPassword");
        String newPassword     = body.get("newPassword");

        if (currentPassword == null || newPassword == null)
            return ResponseEntity.badRequest().body("Missing fields");

        String currentHash = sha256(currentPassword);
        String newHash     = sha256(newPassword);

        boolean changed = userDAO.changePassword(id, currentHash, newHash);
        if (!changed)
            return ResponseEntity.status(401).body("Current password is incorrect");

        return ResponseEntity.ok("Password changed");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable int id) {
        int rows = userDAO.deactivateUser(id);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("User deactivated");
    }

    @PutMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable int id) {
        int rows = userDAO.reactivateUser(id);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("User reactivated");
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }
}