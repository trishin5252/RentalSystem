package ru.mirea.project.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Класс заявки на аренду оборудования
public class RentalRequest {
    private int id;
    private int clientId;
    private String clientName;
    private int equipmentId;
    private String equipmentName;
    private LocalDate startDate;
    private LocalDate endDate;
    private RentalStatus status;
    private BigDecimal totalCost;
    private LocalDateTime createdAt;

    public RentalRequest() {
    }

    public RentalRequest(int id, int clientId, int equipmentId, LocalDate startDate,
                        LocalDate endDate, RentalStatus status, BigDecimal totalCost) {
        this.id = id;
        this.clientId = clientId;
        this.equipmentId = equipmentId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.totalCost = totalCost;
    }

    // Геттеры и сеттеры
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public int getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(int equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public RentalStatus getStatus() {
        return status;
    }

    public void setStatus(RentalStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getDaysCount() {
        if (startDate != null && endDate != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        }
        return 0;
    }

    @Override
    public String toString() {
        return String.format("%-4d | %-25s | %-25s | %-12s | %-12s | %-12s | %-10s",
                id, clientName != null ? clientName : "ID:" + clientId,
                equipmentName != null ? equipmentName : "ID:" + equipmentId,
                startDate, endDate, totalCost + " руб.", status.getDisplayName());
    }
}