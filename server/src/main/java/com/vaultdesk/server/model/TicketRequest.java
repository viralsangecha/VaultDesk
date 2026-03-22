package com.vaultdesk.server.model;

public record TicketRequest(String title,String description,String category,String priority,int reportedBy) {
}
