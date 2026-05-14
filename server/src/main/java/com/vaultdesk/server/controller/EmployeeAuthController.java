package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.EmployeeDAO;
import com.vaultdesk.server.model.Employee;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/employee/auth")
public class EmployeeAuthController {

    private final EmployeeDAO employeeDAO;

    public EmployeeAuthController(EmployeeDAO employeeDAO) {
        this.employeeDAO = employeeDAO;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null)
            return ResponseEntity.badRequest().body("Missing fields");

        String hash = sha256(password);

        if (employeeDAO.validateEmployeeLogin(username, hash)) {
            Employee emp = employeeDAO.getEmployeeByUsername(username);
            return ResponseEntity.ok(Map.of(
                    "success",      true,
                    "message",      "Login successful",
                    "employeeId",   emp.id(),
                    "name",         emp.name(),
                    "empCode",      emp.empCode() != null ? emp.empCode() : "",
                    "designation",  emp.designation() != null ? emp.designation() : "",
                    "departmentId", emp.departmentId(),
                    "email",        emp.email() != null ? emp.email() : ""
            ));
        }
        return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Invalid username or password"
        ));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody Map<String, String> body) {
        String username     = body.get("username");
        String passwordHash = body.get("passwordHash");

        if (username == null || passwordHash == null)
            return ResponseEntity.badRequest().build();

        if (employeeDAO.validateEmployeeLogin(username, passwordHash)) {
            Employee emp = employeeDAO.getEmployeeByUsername(username);
            return ResponseEntity.ok(Map.of(
                    "success",      true,
                    "employeeId",   emp.id(),
                    "name",         emp.name(),
                    "empCode",      emp.empCode() != null ? emp.empCode() : "",
                    "designation",  emp.designation() != null ? emp.designation() : "",
                    "departmentId", emp.departmentId(),
                    "email",        emp.email() != null ? emp.email() : ""
            ));
        }
        return ResponseEntity.status(401).build();
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