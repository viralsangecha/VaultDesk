package com.vaultdesk.admin;

public class SessionManager {

    private static SessionManager instance;

    private int userId;
    private String fullName;
    private String role;

    private SessionManager() {}

    public static SessionManager get() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void login(int userId, String fullName, String role) {
        this.userId   = userId;
        this.fullName = fullName;
        this.role     = role;
    }

    public void logout() {
        userId   = 0;
        fullName = "";
        role     = "";
    }

    public int    getUserId()   { return userId; }
    public String getFullName() { return fullName; }
    public String getRole()     { return role; }

    public boolean isAdmin()    { return "ADMIN".equals(role); }
    public boolean isEngineer() { return "ENGINEER".equals(role) || isAdmin(); }
}