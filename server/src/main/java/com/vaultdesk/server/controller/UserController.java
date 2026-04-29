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

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        String username  = body.get("username");
        String password  = body.get("password");
        String fullName  = body.get("fullName");
        String role      = body.get("role");

        if (username == null || password == null
                || fullName == null || role == null)
            return ResponseEntity.badRequest().body("Missing fields");

        String hash = sha256(password);
        userDAO.saveUser(username, hash, fullName, role);
        return ResponseEntity.status(201).body("User created");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable int id,
                                        @RequestBody Map<String, String> body) {
        String fullName = body.get("fullName");
        String role     = body.get("role");
        int rows = userDAO.updateUser(id, fullName, role);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("User updated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable int id) {
        int rows = userDAO.deactivateUser(id);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("User deactivated");
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