package com.vaultdesk.admin;

public class Vendor {
    private int id;
    private String name;
    private String contactPerson;
    private String phone;
    private String email;
    private String category;

    public Vendor(int id, String name, String contactPerson, String phone, String email, String category) {
        this.id = id;
        this.name = name;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.email = email;
        this.category = category;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getCategory() {
        return category;
    }
}
