package com.vaultdesk.server.model;

public record DashboardStats(
        int totalAssets,
        int openTickets,
        int generalTickets,
        int sapTickets,
        int expiringLicenses,
        int totalEmployees,
        int totalDepartments
) {}