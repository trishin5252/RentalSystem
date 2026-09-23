package ru.mirea.project.model;
public enum RentalStatus {
    CREATED("Создана"), ACTIVE("Активна"), COMPLETED("Завершена"), CANCELLED("Отменена"), OVERDUE("Просрочена");
    private final String displayName;
    RentalStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    public boolean reservesEquipment() { return this == CREATED || this == ACTIVE || this == OVERDUE; }
    public boolean canTransitionTo(RentalStatus next) {
        if (next == null) return false;
        return switch(this) {
            case CREATED -> next == ACTIVE || next == CANCELLED;
            case ACTIVE -> next == COMPLETED || next == OVERDUE;
            case OVERDUE -> next == COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
    }
    public static RentalStatus fromString(String text) {
        for (RentalStatus s : values())
            if (s.name().equalsIgnoreCase(text) || s.displayName.equalsIgnoreCase(text)) return s;
        throw new IllegalArgumentException("Неизвестный статус: " + text);
    }
}
