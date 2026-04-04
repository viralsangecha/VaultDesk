package com.vaultdesk.admin;

public class Ticket {

    private int id;
    private String ticketNo;
    private String title;
    private String priority;
    private String status;
    private String createdAt;

    // Constructor with all 6 fields
    public Ticket(int id, String ticketNo, String title, String priority, String status, String createdAt) {
        this.id = id;
        this.ticketNo = ticketNo;
        this.title = title;
        this.priority = priority;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters (required for TableView)
    public int getId() {
        return id;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public String getTitle() {
        return title;
    }

    public String getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}