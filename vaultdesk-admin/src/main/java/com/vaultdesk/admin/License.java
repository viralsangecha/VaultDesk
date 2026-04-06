package com.vaultdesk.admin;

public class License {
    private int id;
    private String softwareName;
    private String licenseType;
    private String vendor;
    private String expiryDate;
    private int seatsTotal;
    private int seatsUsed;

    public License(int id, String softwareName, String licenseType, String vendor, String expiryDate, int seatsTotal, int seatsUsed) {
        this.id = id;
        this.softwareName = softwareName;
        this.licenseType = licenseType;
        this.vendor = vendor;
        this.expiryDate = expiryDate;
        this.seatsTotal = seatsTotal;
        this.seatsUsed = seatsUsed;
    }

    public int getId() {
        return id;
    }

    public String getSoftwareName() {
        return softwareName;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public String getVendor() {
        return vendor;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public int getSeatsTotal() {
        return seatsTotal;
    }

    public int getSeatsUsed() {
        return seatsUsed;
    }
}
