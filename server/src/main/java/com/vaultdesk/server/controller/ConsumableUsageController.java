package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.ConsumableUsageDAO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/consumables")
public class ConsumableUsageController {

    private final ConsumableUsageDAO usageDAO;

    public ConsumableUsageController(
            ConsumableUsageDAO usageDAO) {
        this.usageDAO = usageDAO;
    }

    @GetMapping("/{id}/usage")
    public ResponseEntity<?> getUsage(@PathVariable int id) {
        return ResponseEntity.ok(
                usageDAO.getUsageByConsumable(id));
    }

    @GetMapping("/usage/all")
    public ResponseEntity<?> getAllUsage() {
        return ResponseEntity.ok(usageDAO.getAllUsage());
    }

    @PostMapping("/{id}/usage")
    public ResponseEntity<?> logUsage(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {
        int qty = body.get("quantityUsed") != null
                ? Integer.parseInt(body.get("quantityUsed")) : 1;
        int assetId = body.get("assetId") != null
                ? Integer.parseInt(body.get("assetId")) : 0;
        int employeeId = body.get("employeeId") != null
                ? Integer.parseInt(body.get("employeeId")) : 0;
        int usedBy = body.get("usedBy") != null
                ? Integer.parseInt(body.get("usedBy")) : 0;
        String notes = body.getOrDefault("notes", "");

        boolean success = usageDAO.logUsage(
                id, assetId, employeeId, qty, usedBy, notes);

        if (!success)
            return ResponseEntity.badRequest()
                    .body("Insufficient stock");
        return ResponseEntity.status(201).body("Usage logged");
    }
}