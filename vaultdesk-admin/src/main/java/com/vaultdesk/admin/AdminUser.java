package com.vaultdesk.admin;

public class AdminUser {
    private int id;
    private String username;
    private String fullName;
    private String role;
    private int active;
    private String createdAt;

    public AdminUser(int id, String username, String fullName,
                     String role, int active, String createdAt) {
        this.id        = id;
        this.username  = username;
        this.fullName  = fullName;
        this.role      = role;
        this.active    = active;
        this.createdAt = createdAt;
    }

    public int    getId()        { return id; }
    public String getUsername()  { return username; }
    public String getFullName()  { return fullName; }
    public String getRole()      { return role; }
    public boolean isActive()    { return active == 1; }
    public String getCreatedAt() { return createdAt; }
}