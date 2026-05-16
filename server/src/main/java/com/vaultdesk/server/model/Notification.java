package com.vaultdesk.server.model;

public record Notification(
        int id,
        int userId,
        int employeeId,
        String message,
        String type,
        int referenceId,
        int isRead,
        String createdAt
) {}