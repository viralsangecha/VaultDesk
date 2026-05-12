package com.vaultdesk.server.model;

public record TicketComment(
        int id,
        int ticketId,
        String comment,
        int addedBy,
        String addedByName,
        String addedAt
) {}