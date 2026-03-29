package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.LicenseDAO;
import com.vaultdesk.server.model.License;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {
    private final LicenseDAO licenseDAO;

    public LicenseController(LicenseDAO licenseDAO)
    {
        this.licenseDAO=licenseDAO;
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
}
