package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.ComponentDAO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class ComponentController {

    private final ComponentDAO componentDAO;

    public ComponentController(ComponentDAO componentDAO) {
        this.componentDAO = componentDAO;
    }

    @GetMapping("/{id}/components")
    public ResponseEntity<?> getComponents(@PathVariable int id) {
        return ResponseEntity.ok(
                componentDAO.getComponentsByAsset(id));
    }

    @PostMapping("/{id}/components")
    public ResponseEntity<?> addComponent(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {
        componentDAO.saveComponent(
                id,
                body.getOrDefault("componentType", ""),
                body.getOrDefault("brand", ""),
                body.getOrDefault("model", ""),
                body.getOrDefault("serialNumber", ""),
                body.getOrDefault("specs", ""),
                body.getOrDefault("status", "In Use"),
                body.getOrDefault("purchaseDate", ""),
                body.getOrDefault("notes", "")
        );
        return ResponseEntity.status(201).body("Component added");
    }

    @PutMapping("/components/{componentId}")
    public ResponseEntity<?> updateComponent(
            @PathVariable int componentId,
            @RequestBody Map<String, String> body) {
        componentDAO.updateComponent(
                componentId,
                body.getOrDefault("brand", ""),
                body.getOrDefault("model", ""),
                body.getOrDefault("serialNumber", ""),
                body.getOrDefault("specs", ""),
                body.getOrDefault("status", "In Use"),
                body.getOrDefault("notes", "")
        );
        return ResponseEntity.ok("Component updated");
    }

    @DeleteMapping("/components/{componentId}")
    public ResponseEntity<?> deleteComponent(
            @PathVariable int componentId) {
        componentDAO.deleteComponent(componentId);
        return ResponseEntity.ok("Component deleted");
    }
}