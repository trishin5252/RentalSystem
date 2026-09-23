package ru.mirea.project.exception;

// Исключение, когда сущность не найдена в базе данных
public class EntityNotFoundException extends Exception {
    public EntityNotFoundException(String message) {
        super(message);
    }
}