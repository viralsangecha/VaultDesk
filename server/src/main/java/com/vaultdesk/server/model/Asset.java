package com.vaultdesk.server.model;

public record Asset(
        int id,
        String assetTag,
        String name,
        String category,
        String brand,
        String model,
        String serialNumber,
        int departmentId,
        String location,
        String status,
        int assignedTo,
        String assignedDate,
        String purchaseDate,
        String warrantyExpiry,
        int vendorId,          // ← was vendorName String, now FK int
        double purchaseCost,
        String notes,
        String createdAt,
        String updatedAt
) {}