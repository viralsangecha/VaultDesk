package com.vaultdesk.admin;

public class Ticket {
    private int id;
    private String ticketNo;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private int assignedTo;
    private int reportedBy;
    private int assetId;
    private String createdAt;
    private String updatedAt;
    private String department;
    private String reason;
    private String requestedAt;



    public Ticket(int id, String ticketNo, String title, String description, String category,
                  String priority, String status, int assignedTo,
                  int reportedBy, int assetId, String createdAt, String updatedAt, String department, String reason,String requestedAt) {
        this.id = id;
        this.ticketNo = ticketNo;
        this.title = title;
        this.description=description;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.assignedTo = assignedTo;
        this.reportedBy = reportedBy;
        this.assetId=assetId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.department=department;
        this.reason=reason;
        this.requestedAt=requestedAt;

    }

    public int getId()           { return id; }
    public String getTicketNo()  { return ticketNo; }
    public String getTitle()     { return title; }
    public String getCategory()  { return category; }
    public String getPriority()  { return priority; }
    public String getStatus()    { return status; }
    public int getAssignedTo()   { return assignedTo; }
    public int getAssetId() {
        return assetId;
    }
    public int getReportedBy()   { return reportedBy; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getDescrption(){ return description; }
    public String getDescription() {
        return description;
    }
    public String getDepartment() {
        return department;
    }
    public String getReason() { return reason; }

    public String getRequestedAt() {
        return requestedAt;
    }
}