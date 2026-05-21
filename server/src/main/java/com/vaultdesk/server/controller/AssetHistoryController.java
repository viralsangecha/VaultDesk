package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.AssetHistoryDAO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class AssetHistoryController {

    private final AssetHistoryDAO historyDAO;

    public AssetHistoryController(AssetHistoryDAO historyDAO) {
        this.historyDAO = historyDAO;
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getHistory(@PathVariable int id) {
        return ResponseEntity.ok(
                historyDAO.getHistoryByAsset(id));
    }

    @PostMapping("/{id}/history")
    public ResponseEntity<?> addHistory(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {
        historyDAO.logHistory(
                id,
                body.getOrDefault("action", ""),
                body.get("fromEmployee") != null
                        ? Integer.parseInt(
                        body.get("fromEmployee")) : 0,
                body.get("toEmployee") != null
                        ? Integer.parseInt(
                        body.get("toEmployee")) : 0,
                body.getOrDefault("notes", ""),
                body.get("doneBy") != null
                        ? Integer.parseInt(
                        body.get("doneBy")) : 0
        );
        return ResponseEntity.status(201).body("History logged");
    }
}