package ru.mirea.project.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Класс оборудования для проката
public class Equipment {
    private int id;
    private String name;
    private String category;
    private BigDecimal pricePerDay;
    private boolean isAvailable;
    private LocalDateTime createdAt;

    public Equipment() {
    }

    public Equipment(int id, String name, String category, BigDecimal pricePerDay, boolean isAvailable) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.pricePerDay = pricePerDay;
        this.isAvailable = isAvailable;
    }

    public Equipment(String name, String category, BigDecimal pricePerDay, boolean isAvailable) {
        this.name = name;
        this.category = category;
        this.pricePerDay = pricePerDay;
        this.isAvailable = isAvailable;
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(BigDecimal pricePerDay) {
        this.pricePerDay = pricePerDay;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("%-4d | %-30s | %-20s | %-12s | %-10s",
                id, name, category, pricePerDay + " руб.", isAvailable ? "Доступно" : "Занято");
    }
}