package ru.mirea.project.util;
import ru.mirea.project.exception.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

public final class DatabaseConnection {
    private DatabaseConnection() { }
    public static Connection getConnection() throws SQLException {
        Properties p = new Properties();
        Path config = Path.of(System.getProperty("db.config", "db.properties"));
        if (Files.exists(config)) {
            try (Reader reader = Files.newBufferedReader(config, StandardCharsets.UTF_8)) { p.load(reader); }
            catch (IOException e) { throw new SQLException("Не удалось прочитать db.properties", "08001", e); }
        }
        Properties auth = new Properties();
        auth.setProperty("user", setting("db.user", "DB_USER", p, "postgres"));
        auth.setProperty("password", setting("db.password", "DB_PASSWORD", p, ""));
        auth.setProperty("connectTimeout", "5");
        auth.setProperty("socketTimeout", "15");
        return DriverManager.getConnection(setting("db.url", "DB_URL", p, "jdbc:postgresql://localhost:5432/rental_system"), auth);
    }
    private static String setting(String key, String env, Properties p, String fallback) {
        String v = System.getProperty(key);
        if (v == null) v = System.getenv(env);
        return v != null ? v : p.getProperty(key, fallback);
    }
    @FunctionalInterface
    public interface Transaction<T> {
        T execute(Connection c) throws SQLException, BusinessException, EntityNotFoundException;
    }
    /** Заявка и оборудование изменяются вместе; при ошибке вся операция откатывается. */
    public static <T> T transaction(Transaction<T> action) throws BusinessException, EntityNotFoundException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = action.execute(c);
                c.commit();
                return result;
            } catch (SQLException | BusinessException | EntityNotFoundException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        } catch (SQLException e) { throw new DataAccessException(e); }
    }
}
