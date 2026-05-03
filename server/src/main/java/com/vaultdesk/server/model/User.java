package com.vaultdesk.server.model;

public record User(
        int id,
        String username,
        String passwordHash,
        String fullName,
        String role,
        int active,
        String createdAt,
        String lastLogin,
        int deptId
) {}