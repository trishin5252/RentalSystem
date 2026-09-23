package ru.mirea.project.repository;
import ru.mirea.project.exception.DataAccessException;
import ru.mirea.project.util.DatabaseConnection;
import java.sql.*;
import java.util.*;

/** Переданное соединение принадлежит транзакции; закрывается только собственное соединение. */
abstract class JdbcRepository<T> {
    private final Connection connection;
    protected JdbcRepository() { this(null); }
    protected JdbcRepository(Connection connection) { this.connection = connection; }
    @FunctionalInterface
    protected interface SqlAction<R> { R execute(Connection c) throws SQLException; }
    protected <R> R withConnection(SqlAction<R> action) {
        try {
            if (connection != null) return action.execute(connection);
            try (Connection c = DatabaseConnection.getConnection()) { return action.execute(c); }
        } catch (SQLException e) { throw new DataAccessException(e); }
    }
    protected abstract T map(ResultSet rs) throws SQLException;
    protected List<T> query(String sql, Object... args) {
        return withConnection(c -> {
            List<T> result = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                bind(ps, args);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(map(rs)); }
            }
            return result;
        });
    }
    protected int execute(String sql, Object... args) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql)) { bind(ps, args); return ps.executeUpdate(); }
        });
    }
    protected int insert(String sql, Object... args) {
        return withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, args);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                    throw new SQLException("База не вернула ID новой записи");
                }
            }
        });
    }
    private static void bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
    }
}
