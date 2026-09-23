package ru.mirea.project.exception;
import java.sql.SQLException;
public class DataAccessException extends RuntimeException {
    public DataAccessException(SQLException cause) { super(message(cause.getSQLState()), cause); }
    private static String message(String s) {
        if (s == null) return "Ошибка обращения к базе данных.";
        if (s.startsWith("08")) return "Нет соединения с PostgreSQL. Проверьте сервер и db.properties.";
        if (s.startsWith("28")) return "PostgreSQL отклонил вход. Проверьте пользователя и пароль.";
        return switch(s) {
            case "23505" -> "Запись с таким уникальным значением уже существует (email или занятое оборудование).";
            case "23503" -> "Нарушена связь записей: проверьте ID. Связанные записи удалять нельзя.";
            case "23514", "23502", "22001", "22003" -> "Данные не соответствуют ограничениям базы данных.";
            case "42P01", "3D000" -> "База или таблицы не созданы. Выполните database.sql по инструкции README.";
            default -> "Ошибка базы данных (SQLSTATE " + s + "). Операция не выполнена.";
        };
    }
}
