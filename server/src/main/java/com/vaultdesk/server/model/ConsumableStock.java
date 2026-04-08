package com.vaultdesk.server.model;

public record ConsumableStock(int id,String  name,String category,String  compatibleModels,int quantityInStock,int reorderLevel,String  unit,int vendorId,double unitCost,String storageLocation,String  notes,String  lastUpdated) {
}
