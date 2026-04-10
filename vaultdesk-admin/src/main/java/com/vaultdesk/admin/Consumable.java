package com.vaultdesk.admin;

public class Consumable {
    private int id;
    private String name;
    private String category;
    private String unit;
    private int quantityInStock;
    private int reorderLevel;

    public Consumable(int id, String name, String category, String unit, int quantityInStock, int reorderLevel) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.quantityInStock = quantityInStock;
        this.reorderLevel = reorderLevel;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getUnit() {
        return unit;
    }

    public int getQuantityInStock() {
        return quantityInStock;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }
}
