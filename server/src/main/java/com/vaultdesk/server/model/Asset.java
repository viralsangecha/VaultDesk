package com.vaultdesk.server.model;

public record Asset(int id,String assetTag,String name,String category,String brand,String model,String serialNumber,String processor,int ramGb,int storageGb,String os,String ipAddress,String macAddress,int departmentId,String location,String purchaseDate,String warrantyExpiry,String vendorName,double purchaseCost,String status,int assignedTo,String assignedDate,String notes,String createdAt,String updatedAt) {
}
