package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.AssetDAO;
import com.vaultdesk.server.dao.AssetHistoryDAO;
import com.vaultdesk.server.dao.AssetLinkDAO;
import com.vaultdesk.server.model.Asset;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetDAO assetDAO;
    private final AssetHistoryDAO historyDAO;
    private final JdbcTemplate jdbc;
    private final AssetLinkDAO assetLinkDAO;

    public AssetController(AssetDAO assetDAO,
                           AssetHistoryDAO historyDAO,
                           JdbcTemplate jdbc, AssetLinkDAO assetLinkDAO) {
        this.assetDAO   = assetDAO;
        this.historyDAO = historyDAO;
        this.jdbc       = jdbc;
        this.assetLinkDAO = assetLinkDAO;
    }

    @GetMapping
    public ResponseEntity<?> getallasset() {
        return ResponseEntity.ok(assetDAO.getAllAssets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getassetbyid(@PathVariable int id) {
        Asset asset = assetDAO.getAssetById(id);
        if (asset == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(asset);
    }

    @GetMapping("/department/{deptId}")
    public ResponseEntity<?> getassetbydeptid(
            @PathVariable int deptId) {
        List<Asset> assets =
                assetDAO.getAssetsByDepartment(deptId);
        if (assets == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getByEmployee(
            @PathVariable int employeeId) {
        return ResponseEntity.ok(
                assetDAO.getAssetsByEmployee(employeeId));
    }

    @PostMapping
    public ResponseEntity<?> saveassets(@RequestBody Asset asset) {
        assetDAO.saveAsset(asset);

        // ── Get new asset id ──────────────────────────────
        List<Asset> all = assetDAO.getAllAssets();
        int newId = all != null && !all.isEmpty()
                ? all.get(0).id() : 0;

        // ── Auto log history ──────────────────────────────
        historyDAO.logHistory(newId,
                "Asset created and added to inventory",
                0, asset.assignedTo(), "", 0);

        if (asset.assignedTo() > 0) {
            historyDAO.logHistory(newId,
                    "Asset assigned to employee on creation",
                    0, asset.assignedTo(), "", 0);
        }

        logActivity(0, "CREATE", "assets", newId,
                "Asset added: " + asset.name()
                        + " [" + asset.assetTag() + "]");

        return ResponseEntity.status(201).body("Assets added");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAsset(
            @PathVariable int id,
            @RequestBody Asset asset) {
        // Get old asset to compare assigned_to
        Asset old = assetDAO.getAssetById(id);
        int rows = assetDAO.updateAsset(asset);
        if (rows == 0)
            return ResponseEntity.notFound().build();

        // ── Auto log assignment change ────────────────────
        if (old != null
                && old.assignedTo() != asset.assignedTo()) {
            historyDAO.logHistory(id,
                    "Asset reassigned",
                    old.assignedTo(),
                    asset.assignedTo(),
                    "Assignment updated", 0);
        }

        // ── Auto log status change ────────────────────────
        if (old != null
                && !old.status().equals(asset.status())) {
            historyDAO.logHistory(id,
                    "Status changed to: " + asset.status(),
                    0, 0, "", 0);
        }

        logActivity(0, "UPDATE", "assets", id,
                "Asset updated: " + asset.name());

        return ResponseEntity.ok("Asset updated");
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateasset(
            @PathVariable int id,
            @RequestParam String status) {
        Asset old = assetDAO.getAssetById(id);
        int rows = assetDAO.updateAssetStatus(id, status);
        if (rows == 0)
            return ResponseEntity.notFound().build();

        // ── Auto log ──────────────────────────────────────
        historyDAO.logHistory(id,
                "Status changed to: " + status,
                0, 0, "", 0);

        logActivity(0, "UPDATE", "assets", id,
                "Asset #" + id
                        + " status changed to: " + status);

        return ResponseEntity.ok("Assets Status updated");
    }

    @GetMapping("/{id}/links")
    public ResponseEntity<?> getLinks(@PathVariable int id) {
        return ResponseEntity.ok(assetLinkDAO.getLinksForAsset(id));
    }

    @PostMapping("/{id}/links")
    public ResponseEntity<?> addLink(@PathVariable int id, @RequestBody Map<String, Object> body) {
        int linkedAssetId = ((Number) body.get("linkedAssetId")).intValue();
        String linkType = (String) body.getOrDefault("linkType", "");
        int newId = assetLinkDAO.addLink(id, linkedAssetId, linkType);
        if (newId == 0) {
            return ResponseEntity.badRequest().body("Already linked, or cannot link an asset to itself.");
        }
        return ResponseEntity.status(201).body(Map.of("id", newId));
    }

    @DeleteMapping("/links/{linkId}")
    public ResponseEntity<?> removeLink(@PathVariable int linkId) {
        int rows = assetLinkDAO.removeLink(linkId);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Link removed");
    }

    private void logActivity(int userId, String action,
                             String tableName, int recordId,
                             String details) {
        try {
            jdbc.update(
                    "INSERT INTO activity_log " +
                            "(user_id, action, table_name, " +
                            "record_id, details, logged_at) " +
                            "VALUES (?, ?, ?, ?, ?, datetime('now'))",
                    userId, action, tableName,
                    recordId, details);
        } catch (Exception e) {
            System.out.println("Activity log error: "
                    + e.getMessage());
        }
    }
}