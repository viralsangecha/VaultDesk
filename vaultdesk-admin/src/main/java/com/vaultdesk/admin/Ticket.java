package com.vaultdesk.admin;

public class Ticket {
    private int id;
    private String ticketNo;
    private String title;
    private String priority;
    private String status;
    private int assignedTo;
    private String createdAt;

    public Ticket(int id, String ticketNo, String title, String priority, String status, int assignedTo, String createdAt) {
        this.id = id;
        this.ticketNo = ticketNo;
        this.title = title;
        this.priority = priority;
        this.status = status;
        this.assignedTo = assignedTo;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getTicketNo() { return ticketNo; }
    public String getTitle() { return title; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public int getAssignedTo() { return assignedTo; }
    public String getCreatedAt() { return createdAt; }
}