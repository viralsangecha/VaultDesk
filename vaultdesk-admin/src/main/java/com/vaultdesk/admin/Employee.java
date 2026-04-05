package com.vaultdesk.admin;

public class Employee {
    private int id;
    private String name;
    private String empCode;
    private String designation;
    private String email;
    private String phone;

    public Employee(int id,String name,String empCode,String designation,String email,String phone)
    {
        this.id=id;
        this.name=name;
        this.empCode=empCode;
        this.designation=designation;
        this.email=email;
        this.phone=phone;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmpCode() {
        return empCode;
    }

    public String getDesignation() {
        return designation;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }
}
