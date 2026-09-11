package com.adnaan525.medme.model;

public class Inventory {
    private int quantityRemaining;
    private int lowStockThreshold;
    private boolean lowStockNotified;

    public Inventory() {
    }

    public Inventory(int quantityRemaining, int lowStockThreshold) {
        this.quantityRemaining = quantityRemaining;
        this.lowStockThreshold = lowStockThreshold;
        this.lowStockNotified = false;
    }

    public int getQuantityRemaining() {
        return quantityRemaining;
    }

    public void setQuantityRemaining(int quantityRemaining) {
        this.quantityRemaining = quantityRemaining;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(int lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public boolean isLowStockNotified() {
        return lowStockNotified;
    }

    public void setLowStockNotified(boolean lowStockNotified) {
        this.lowStockNotified = lowStockNotified;
    }

    public boolean isLowStock() {
        return quantityRemaining <= lowStockThreshold;
    }
}
