package com.vaultdesk.server.model;

public record Ticket(int id,String ticketNo,String title,String description,String category,String priority,String status,int reportedBy,int assetId,int assignedTo,String createdAt,String updatedAt,String resolvedAt,String resolution) {
}
