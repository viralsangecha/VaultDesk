package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.*;
import com.vaultdesk.server.model.Employee;
import com.vaultdesk.server.model.User;
import com.vaultdesk.server.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/api")
public class PasswordResetController {

    private final UserDAO userDAO;
    private final EmployeeDAO employeeDAO;
    private final PasswordResetDAO resetDAO;
    private final EmailService emailService;
    private final EmailSettingsDAO emailSettingsDAO;

    public PasswordResetController(UserDAO userDAO, EmployeeDAO employeeDAO,
                                   PasswordResetDAO resetDAO, EmailService emailService,
                                   EmailSettingsDAO emailSettingsDAO) {
        this.userDAO = userDAO;
        this.employeeDAO = employeeDAO;
        this.resetDAO = resetDAO;
        this.emailService = emailService;
        this.emailSettingsDAO = emailSettingsDAO;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String userType = body.getOrDefault("userType", "EMPLOYEE").toUpperCase();

        if ("ADMIN".equals(userType)) {
            User u = userDAO.getUserByUsername(username);
            if (u == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No account found with that username."));
            }
            if (u.email() == null || u.email().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "This account has no email on file. Contact your administrator."));
            }
            String token = resetDAO.createToken("ADMIN", u.id());
            String link = emailSettingsDAO.getPublicUrl() + "/portal/reset-password.html?token=" + token;
            emailService.sendPasswordResetLink(u.email(), u.fullName(), link);
            emailService.sendPasswordResetRequestedToAdmins(username, "ADMIN");
            return ResponseEntity.ok(Map.of("success", true, "message", "A reset link has been sent to your registered email."));
        } else {
            Employee e = employeeDAO.getEmployeeByUsername(username);
            if (e == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No account found with that username."));
            }
            if (e.email() == null || e.email().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "This account has no email on file. Contact your IT department."));
            }
            String token = resetDAO.createToken("EMPLOYEE", e.id());
            String link = emailSettingsDAO.getPublicUrl() + "/portal/reset-password.html?token=" + token;
            emailService.sendPasswordResetLink(e.email(), e.name(), link);
            emailService.sendPasswordResetRequestedToAdmins(username, "EMPLOYEE");
            return ResponseEntity.ok(Map.of("success", true, "message", "A reset link has been sent to your registered email."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Password must be at least 6 characters."));
        }

        PasswordResetDAO.TokenInfo info = resetDAO.validateToken(token);
        if (info == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "This reset link is invalid or has expired."));
        }

        String newHash = sha256(newPassword);
        if ("ADMIN".equals(info.subjectType())) {
            userDAO.forceUpdatePassword(info.subjectId(), newHash);
            User u = userDAO.getUserById(info.subjectId());
            if (u != null) emailService.notifyAllAboutSuccessfulReset(u.email(), u.username());
        } else {
            employeeDAO.forceUpdatePassword(info.subjectId(), newHash);
            Employee e = employeeDAO.getEmployeeById(info.subjectId());
            if (e != null) emailService.notifyAllAboutSuccessfulReset(e.email(),e.name());
        }
        resetDAO.markUsed(token);
        return ResponseEntity.ok(Map.of("success", true, "message", "Password reset successfully. You can now log in."));
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }
}