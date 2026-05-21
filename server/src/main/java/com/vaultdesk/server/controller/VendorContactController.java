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
