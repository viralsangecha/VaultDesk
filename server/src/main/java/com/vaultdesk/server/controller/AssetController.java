package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.AssetDAO;
import com.vaultdesk.server.model.Asset;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetDAO assetDAO;

    public AssetController(AssetDAO assetDAO)
    {
        this.assetDAO=assetDAO;
    }
    @GetMapping
    public ResponseEntity<?> getallasset() {
        return ResponseEntity.ok(assetDAO.getAllAssets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getassetbyid(@PathVariable int id)
    {
        Asset  asset=assetDAO.getAssetById(id);
        if (asset == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(asset);
    }

    @GetMapping("/department/{deptId}")
    public ResponseEntity<?> getassetbydeptid(@PathVariable int deptId)
    {
        List<Asset> assets = assetDAO.getAssetsByDepartment(deptId);
        if (assets == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(assets);
    }

    @PostMapping
    public ResponseEntity<?> saveassets(@RequestBody Asset asset)
    {
        assetDAO.saveAsset(asset);
        return ResponseEntity.status(201).body("Assets added");
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateasset(@PathVariable int id,@RequestParam String status)
    {
        int rows = assetDAO.updateAssetStatus(id,status);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Assets Status updated");

    }
}
