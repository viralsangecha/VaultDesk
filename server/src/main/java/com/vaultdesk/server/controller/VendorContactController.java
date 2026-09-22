package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.VendorContactDAO;
import com.vaultdesk.server.model.VendorContact;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendors")
public class VendorContactController
{
    private final VendorContactDAO vendorContactDAO;

    public VendorContactController(VendorContactDAO vendorContactDAO)
    {
        this.vendorContactDAO=vendorContactDAO;
    }

    @GetMapping
    public ResponseEntity<?> getAllVendors()
    {
        return ResponseEntity.ok(vendorContactDAO.getAllVendors());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVendorById(@PathVariable int id) {
        VendorContact v = vendorContactDAO.getVendorById(id);
        if (v == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(v);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllVendorsAdmin() {
        return ResponseEntity.ok(vendorContactDAO.getAllVendorsAdmin());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivate(@PathVariable int id) {
        int rows = vendorContactDAO.deactivateVendor(id);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Vendor deactivated");
    }

    @PutMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable int id) {
        int rows = vendorContactDAO.reactivateVendor(id);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Vendor reactivated");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVendor(
            @PathVariable int id,
            @RequestBody VendorContact vendor) {
        int rows = vendorContactDAO.updateVendor(vendor);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Vendor updated");
    }

    @PostMapping
    public ResponseEntity<?> saveVendor(@RequestBody VendorContact vendorContact) {
        vendorContactDAO.saveVendor(vendorContact);
        return ResponseEntity.status(201).body("Vendor added");
    }
}
