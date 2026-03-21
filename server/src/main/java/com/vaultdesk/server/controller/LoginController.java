package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.UserDAO;
import com.vaultdesk.server.model.LoginRequest;
import com.vaultdesk.server.model.LoginResponse;
import com.vaultdesk.server.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
    private final UserDAO userDAO;  // field

    public LoginController(UserDAO userDAO) {  // constructor injection
        this.userDAO = userDAO;
    }

    @PostMapping("/login")
    public ResponseEntity<?> LoginRequest(@RequestBody LoginRequest request)
    {
        String hased=sha256(request.password());
        if (userDAO.validateLogin(request.username(),hased))
        {
            User user=userDAO.getUserByUsername(request.username());
            return ResponseEntity.ok(new LoginResponse(true,"Login successful",user.role(),user.fullName()));
        }
        return ResponseEntity.status(401).body(new LoginResponse(false, "Invalid credentials", "", ""));
    }
}
