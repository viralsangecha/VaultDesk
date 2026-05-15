package com.vaultdesk.employee;

public class SessionManager {

    private static SessionManager instance;

    private int employeeId;
    private String name;
    private String empCode;
    private String designation;
    private int departmentId;
    private String email;

    private SessionManager() {}

    public static SessionManager get() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void login(int employeeId, String name, String empCode,
                      String designation, int departmentId, String email) {
        this.employeeId   = employeeId;
        this.name         = name;
        this.empCode      = empCode;
        this.designation  = designation;
        this.departmentId = departmentId;
        this.email        = email;
    }

    public void logout() {
        employeeId   = 0;
        name         = "";
        empCode      = "";
        designation  = "";
        departmentId = 0;
        email        = "";
    }

    public int    getEmployeeId()   { return employeeId; }
    public String getName()         { return name; }
    public String getEmpCode()      { return empCode; }
    public String getDesignation()  { return designation; }
    public int    getDepartmentId() { return departmentId; }
    public String getEmail()        { return email; }

    public boolean isLoggedIn() { return employeeId > 0; }
}