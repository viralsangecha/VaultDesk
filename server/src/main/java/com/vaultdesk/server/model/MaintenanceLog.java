package com.vaultdesk.server.model;

public record MaintenanceLog(int id,int assetId,String  maintenanceType,String  description,int doneByInternal,int doneByVendor,double cost,String  maintenanceDate,String  nextDueDate,String  status,String  notes,int loggedBy,String  createdAt) {
}
