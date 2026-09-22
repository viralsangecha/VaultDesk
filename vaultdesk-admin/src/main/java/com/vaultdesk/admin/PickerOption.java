package com.vaultdesk.admin;

public class PickerOption {
    public final int id;
    public final String name;
    public final int extraId; // e.g. departmentId for an employee option; 0 if unused

    public PickerOption(int id, String name) {
        this(id, name, 0);
    }

    public PickerOption(int id, String name, int extraId) {
        this.id = id;
        this.name = name;
        this.extraId = extraId;
    }

    @Override
    public String toString() {
        return name;
    }
}