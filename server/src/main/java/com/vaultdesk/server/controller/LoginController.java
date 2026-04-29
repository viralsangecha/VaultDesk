package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.UserDAO;
import com.vaultdesk.server.model.LoginRequest;
import com.vaultdesk.server.model.LoginResponse;
import com.vaultdesk.server.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final UserDAO userDAO;

    public LoginController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String hashed = sha256(request.password());
        if (userDAO.validateLogin(request.username(), hashed)) {
            User user = userDAO.getUserByUsername(request.username());
            // Record login time
            userDAO.updateLastLogin(user.id());
            return ResponseEntity.ok(new LoginResponse(
                    true, "Login successful",
                    user.role(), user.fullName(), user.id()
            ));
        }
        return ResponseEntity.status(401).body(
                new LoginResponse(false, "Invalid credentials", "", "", 0));
    }
}