package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.MaintenanceLogDAO;
import com.vaultdesk.server.model.MaintenanceLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceLogController {
    private final MaintenanceLogDAO maintenanceLogDAO;

    public MaintenanceLogController(MaintenanceLogDAO maintenanceLogDAO) {
        this.maintenanceLogDAO = maintenanceLogDAO;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(maintenanceLogDAO.getAllLogs());
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<?> getByAsset(@PathVariable int assetId) {
        return ResponseEntity.ok(maintenanceLogDAO.getLogsByAsset(assetId));
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody MaintenanceLog log) {
        maintenanceLogDAO.saveLog(log);
        return ResponseEntity.status(201).body("Maintenance log added");
    }
}