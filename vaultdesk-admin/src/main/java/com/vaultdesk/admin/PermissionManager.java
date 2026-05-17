package com.vaultdesk.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PermissionManager {

    private static final Set<String> permissions = new HashSet<>();

    // ── Load permissions on login ─────────────────────────
    public static void load(List<String> perms) {
        permissions.clear();
        if (perms != null) permissions.addAll(perms);
    }

    public static void clear() {
        permissions.clear();
    }

    // ── Check permission ──────────────────────────────────
    public static boolean has(String permission) {
        // ADMIN always has everything
        if ("ADMIN".equals(SessionManager.get().getRole()))
            return true;
        return permissions.contains(permission);
    }

    // ── Convenience methods ───────────────────────────────
    public static boolean canViewAllTickets() {
        return has("VIEW_ALL_TICKETS");
    }

    public static boolean canViewAssignedTickets() {
        return has("VIEW_ASSIGNED_TICKETS");
    }

    public static boolean canUpdateTicketStatus() {
        return has("UPDATE_TICKET_STATUS");
    }

    public static boolean canAssignTicket() {
        return has("ASSIGN_TICKET");
    }

    public static boolean canViewAllAssets() {
        return has("VIEW_ALL_ASSETS");
    }

    public static boolean canAddAsset() {
        return has("ADD_ASSET");
    }

    public static boolean canEditAsset() {
        return has("EDIT_ASSET");
    }

    public static boolean canImportAssets() {
        return has("IMPORT_ASSETS");
    }

    public static boolean canViewEmployees() {
        return has("VIEW_EMPLOYEES");
    }

    public static boolean canAddEmployee() {
        return has("ADD_EMPLOYEE");
    }

    public static boolean canEditEmployee() {
        return has("EDIT_EMPLOYEE");
    }

    public static boolean canSetLogin() {
        return has("SET_LOGIN");
    }

    public static boolean canViewDepartments() {
        return has("VIEW_DEPARTMENTS");
    }

    public static boolean canViewReports() {
        return has("VIEW_REPORTS");
    }

    public static boolean canViewLicenses() {
        return has("VIEW_LICENSES");
    }

    public static boolean canAddLicense() {
        return has("ADD_LICENSE");
    }

    public static boolean canViewConsumables() {
        return has("VIEW_CONSUMABLES");
    }

    public static boolean canViewMaintenance() {
        return has("VIEW_MAINTENANCE");
    }

    public static boolean canViewVendors() {
        return has("VIEW_VENDORS");
    }

    public static boolean canManageUsers() {
        return has("MANAGE_USERS");
    }

    public static boolean canManageSettings() {
        return has("MANAGE_SETTINGS");
    }

    public static Set<String> getAll() {
        return java.util.Collections.unmodifiableSet(permissions);
    }
}