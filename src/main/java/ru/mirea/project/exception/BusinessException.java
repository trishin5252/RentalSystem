package ru.mirea.project.exception;

// Исключение для нарушений бизнес-правил
public class BusinessException extends Exception {
    public BusinessException(String message) {
        super(message);
    }
}