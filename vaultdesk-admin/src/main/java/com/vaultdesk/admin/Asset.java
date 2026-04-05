package com.vaultdesk.admin;

public class Asset {
    private int id;
    private String assetTag;
    private String name;
    private String category;
    private String brand;
    private String status;
    private String location;

    public Asset(int id,String assetTag,String name,String category, String brand,String status,String location)
    {
        this.id=id;
        this.assetTag=assetTag;
        this.name=name;
        this.category=category;
        this.brand=brand;
        this.status=status;
        this.location=location;
    }

    public int getId() {
        return id;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public String getStatus() {
        return status;
    }

    public String getLocation() {
        return location;
    }
}
