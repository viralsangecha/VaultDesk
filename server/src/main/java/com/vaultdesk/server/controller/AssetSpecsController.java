package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.AssetSpecsDAO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class AssetSpecsController {

    private final AssetSpecsDAO specsDAO;

    public AssetSpecsController(AssetSpecsDAO specsDAO) {
        this.specsDAO = specsDAO;
    }

    @GetMapping("/{id}/specs")
    public ResponseEntity<?> getSpecs(@PathVariable int id) {
        return ResponseEntity.ok(specsDAO.getSpecsByAsset(id));
    }

    @PutMapping("/{id}/specs")
    public ResponseEntity<?> saveSpecs(
            @PathVariable int id,
            @RequestBody Map<String, String> specs) {
        specsDAO.saveAllSpecs(id, specs);
        return ResponseEntity.ok("Specs saved");
    }

    @DeleteMapping("/{id}/specs")
    public ResponseEntity<?> deleteSpecs(@PathVariable int id) {
        specsDAO.deleteAllSpecs(id);
        return ResponseEntity.ok("Specs deleted");
    }
}