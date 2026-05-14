package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.AssetDAO;
import com.vaultdesk.server.dao.EmployeeDAO;
import com.vaultdesk.server.dao.TicketDAO;
import com.vaultdesk.server.model.TicketRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
public class EmployeePortalController {

    private final TicketDAO ticketDAO;
    private final AssetDAO assetDAO;
    private final EmployeeDAO employeeDAO;

    public EmployeePortalController(TicketDAO ticketDAO,
                                    AssetDAO assetDAO,
                                    EmployeeDAO employeeDAO) {
        this.ticketDAO   = ticketDAO;
        this.assetDAO    = assetDAO;
        this.employeeDAO = employeeDAO;
    }

    // ── My Tickets ────────────────────────────────────────
    @GetMapping("/tickets/{employeeId}")
    public ResponseEntity<?> getMyTickets(
            @PathVariable int employeeId) {
        return ResponseEntity.ok(
                ticketDAO.getTicketsByReporter(employeeId));
    }

    // ── Raise Ticket ──────────────────────────────────────
    @PostMapping("/tickets")
    public ResponseEntity<?> raiseTicket(
            @RequestBody TicketRequest request) {
        ticketDAO.saveTicket(
                request.title(), request.description(),
                request.category(), request.priority(),
                request.reportedBy());
        return ResponseEntity.status(201).body("Ticket raised");
    }

    // ── My Assets ─────────────────────────────────────────
    @GetMapping("/assets/{employeeId}")
    public ResponseEntity<?> getMyAssets(
            @PathVariable int employeeId) {
        return ResponseEntity.ok(
                assetDAO.getAssetsByEmployee(employeeId));
    }

    // ── Profile ───────────────────────────────────────────
    @GetMapping("/profile/{employeeId}")
    public ResponseEntity<?> getProfile(
            @PathVariable int employeeId) {
        var emp = employeeDAO.getEmployeeById(employeeId);
        if (emp == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(emp);
    }

    // ── Change Password ───────────────────────────────────
    @PutMapping("/auth/password/{employeeId}")
    public ResponseEntity<?> changePassword(
            @PathVariable int employeeId,
            @RequestBody Map<String, String> body) {
        String current = body.get("currentPassword");
        String newPwd  = body.get("newPassword");
        if (current == null || newPwd == null)
            return ResponseEntity.badRequest().body("Missing fields");
        int rows = employeeDAO.changeEmployeePassword(
                employeeId, sha256(current), sha256(newPwd));
        if (rows == 0)
            return ResponseEntity.status(401)
                    .body("Current password incorrect");
        return ResponseEntity.ok("Password changed");
    }

    @PutMapping("/credentials/{employeeId}")
    public ResponseEntity<?> setCredentials(
            @PathVariable int employeeId,
            @RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null)
            return ResponseEntity.badRequest().body("Missing fields");
        int rows = employeeDAO.setEmployeeCredentials(
                employeeId, username, sha256(password));
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Credentials set");
    }

    // ── Employee app version ──────────────────────────────
    private static final String EMPLOYEE_LATEST_VERSION = "1.0.0";
    private static final String EMPLOYEE_CHANGELOG =
            "Initial release of VaultDesk Employee";

    @GetMapping("/version")
    public ResponseEntity<?> getEmployeeVersion() {
        return ResponseEntity.ok(Map.of(
                "version",     EMPLOYEE_LATEST_VERSION,
                "downloadUrl", "/api/employee/version/download",
                "changelog",   EMPLOYEE_CHANGELOG
        ));
    }

    @GetMapping("/version/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadEmployeeJar() {
        java.io.File jar = new java.io.File("vaultdesk-employee-fat.jar");
        if (!jar.exists()) return ResponseEntity.notFound().build();
        org.springframework.core.io.Resource resource =
                new org.springframework.core.io.FileSystemResource(jar);
        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=vaultdesk-employee-fat.jar")
                .header("Content-Type", "application/java-archive")
                .body(resource);
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