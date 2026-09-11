package com.adnaan525.medme.model;

import java.util.ArrayList;
import java.util.List;

public class Medication {
    private String id;
    private String name;
    private DurationType durationType;
    private int totalDays;
    /** ISO-8601 "yyyy-MM-dd". */
    private String startDate;
    /** "HH:mm" strings, one per daily dose. */
    private List<String> doseTimes;
    private Inventory inventory;
    private boolean active;
    private List<DoseLog> doseLogs;

    public Medication() {
        this.doseTimes = new ArrayList<>();
        this.doseLogs = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DurationType getDurationType() {
        return durationType;
    }

    public void setDurationType(DurationType durationType) {
        this.durationType = durationType;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(int totalDays) {
        this.totalDays = totalDays;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public List<String> getDoseTimes() {
        return doseTimes;
    }

    public void setDoseTimes(List<String> doseTimes) {
        this.doseTimes = doseTimes;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<DoseLog> getDoseLogs() {
        return doseLogs;
    }

    public void setDoseLogs(List<DoseLog> doseLogs) {
        this.doseLogs = doseLogs;
    }
}
