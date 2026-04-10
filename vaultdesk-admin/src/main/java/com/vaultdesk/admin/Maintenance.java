package com.vaultdesk.admin;

public class Maintenance {
    private int id;
    private int assetId;
    private String maintenanceType;
    private String description;
    private double cost;
    private String maintenanceDate;
    private String status;

    public Maintenance(int id, int assetId, String maintenanceType, String description, double cost, String maintenanceDate, String status) {
        this.id = id;
        this.assetId = assetId;
        this.maintenanceType = maintenanceType;
        this.description = description;
        this.cost = cost;
        this.maintenanceDate = maintenanceDate;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getAssetId() {
        return assetId;
    }

    public String getMaintenanceType() {
        return maintenanceType;
    }

    public String getDescription() {
        return description;
    }

    public double getCost() {
        return cost;
    }

    public String getMaintenanceDate() {
        return maintenanceDate;
    }

    public String getStatus() {
        return status;
    }
}
