package com.vaultdesk.server.controller;

import com.vaultdesk.server.dao.ConsumableStockDAO;
import com.vaultdesk.server.model.ConsumableStock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consumables")
public class ConsumableStockController {
    private final ConsumableStockDAO consumableStockDAO;

    public ConsumableStockController(ConsumableStockDAO consumableStockDAO) {
        this.consumableStockDAO = consumableStockDAO;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(consumableStockDAO.getAllConsumables());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable int id) {
        ConsumableStock c = consumableStockDAO.getConsumableById(id);
        if (c == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(c);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<?> getLowStock() {
        return ResponseEntity.ok(consumableStockDAO.getLowStock());
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody ConsumableStock c) {
        consumableStockDAO.saveConsumable(c);
        return ResponseEntity.status(201).body("Consumable added");
    }

    @PutMapping("/{id}/quantity")
    public ResponseEntity<?> updateQuantity(@PathVariable int id, @RequestParam int quantity) {
        int rows = consumableStockDAO.updateQuantity(id, quantity);
        if (rows == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Quantity updated");
    }
}