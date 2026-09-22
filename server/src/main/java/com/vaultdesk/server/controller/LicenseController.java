package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.LicenseAssignmentDAO;
import com.vaultdesk.server.dao.LicenseDAO;
import com.vaultdesk.server.model.License;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {
    private final LicenseDAO licenseDAO;
    private final LicenseAssignmentDAO assignmentDAO;


    public LicenseController(LicenseDAO licenseDAO, LicenseAssignmentDAO assignmentDAO)
    {
        this.licenseDAO=licenseDAO;
        this.assignmentDAO = assignmentDAO;
    }

    @GetMapping
    public ResponseEntity<?> getalllicence()
    {
        return ResponseEntity.ok(licenseDAO.getAllLicenses());
    }

    @GetMapping("/expiring")
    public ResponseEntity<?> getExpiringLicenses(@RequestParam int days)
    {
        return ResponseEntity.ok(licenseDAO.getExpiringLicenses(days));
    }

    @PostMapping
    public ResponseEntity<?> saveLicense(@RequestBody License license)
    {
        licenseDAO.saveLicense(license);
        return ResponseEntity.status(201).body("Licence added");
    }

    @PutMapping("/{id}/seats")
    public  ResponseEntity<?> updateSeatsUsed(@PathVariable int id,@RequestParam int used)
    {
        int row=licenseDAO.updateSeatsUsed(id,used);
        if (row == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Licence updated");

    }
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLicense(
            @PathVariable int id,
            @RequestBody License license) {
        int rows = licenseDAO.updateLicense(license);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("License updated");
    }

    @GetMapping("/{id}/assignments")
    public ResponseEntity<?> getAssignments(@PathVariable int id) {
        return ResponseEntity.ok(assignmentDAO.getAssignments(id));
    }

    @PostMapping("/{id}/assignments")
    public ResponseEntity<?> assignLicense(@PathVariable int id, @RequestBody Map<String, Object> body) {
        int employeeId = ((Number) body.get("employeeId")).intValue();
        String notes = (String) body.getOrDefault("notes", "");
        License license = licenseDAO.getAllLicenses().stream()
                .filter(l -> l.id() == id).findFirst().orElse(null);
        int seatsTotal = license != null ? license.seatsTotal() : 0;

        int result = assignmentDAO.assign(id, employeeId, notes, seatsTotal);
        if (result == 0) return ResponseEntity.badRequest().body("This employee is already assigned this license.");
        if (result == -1) return ResponseEntity.badRequest().body("No seats remaining — all licensed seats are in use.");
        return ResponseEntity.status(201).body(Map.of("id", result));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<?> unassignLicense(@PathVariable int assignmentId, @RequestParam int licenseId) {
        int rows = assignmentDAO.unassign(assignmentId, licenseId);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Unassigned");
    }
}
