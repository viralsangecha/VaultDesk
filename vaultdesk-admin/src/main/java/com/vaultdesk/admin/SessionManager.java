package com.vaultdesk.admin;

import java.util.List;

public class SessionManager {

    private static SessionManager instance;

    private int userId;
    private String fullName;
    private String role;
    private int deptId;

    private SessionManager() {}

    public static SessionManager get() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void login(int userId, String fullName,
                      String role, List<String> permissions) {
        this.userId   = userId;
        this.fullName = fullName;
        this.role     = role;
        this.deptId   = 0;
        PermissionManager.load(permissions);
    }

    // ── Keep old signature for compatibility ──────────────
    public void login(int userId, String fullName, String role) {
        this.userId   = userId;
        this.fullName = fullName;
        this.role     = role;
        this.deptId   = 0;
    }

    public void setDeptId(int deptId) { this.deptId = deptId; }

    public void logout() {
        userId   = 0;
        fullName = "";
        role     = "";
        deptId   = 0;
        PermissionManager.clear();
    }

    public int    getUserId()   { return userId; }
    public String getFullName() { return fullName; }
    public String getRole()     { return role; }
    public int    getDeptId()   { return deptId; }

    public boolean isAdmin()    { return "ADMIN".equals(role); }
    public boolean isDeptHod()  { return "DEPT_HOD".equals(role); }
    public boolean isEngineer() { return "ENGINEER".equals(role); }

    public boolean canManageUsers()   { return isAdmin(); }
    public boolean canSeeAllDepts()   { return isAdmin(); }
    public boolean canDeleteRecords() {
        return isAdmin() || isDeptHod();
    }
    public boolean canAddAssets() {
        return PermissionManager.canAddAsset();
    }
    public boolean canViewReports() {
        return PermissionManager.canViewReports();
    }
}