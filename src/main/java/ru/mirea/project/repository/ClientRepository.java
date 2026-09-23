package ru.mirea.project.repository;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.Client;
import java.sql.*;
import java.util.List;
public class ClientRepository extends JdbcRepository<Client> implements Repository<Client, Integer> {
    public ClientRepository() { }
    public ClientRepository(Connection c) { super(c); }
    public Client findById(Integer id) throws EntityNotFoundException {
        List<Client> rows = query("SELECT * FROM clients WHERE id=?", id);
        if (rows.isEmpty()) throw new EntityNotFoundException("Клиент с ID " + id + " не найден");
        return rows.get(0);
    }
    public List<Client> findAll() { return query("SELECT * FROM clients ORDER BY id"); }
    public Client save(Client c) {
        c.setId(insert("INSERT INTO clients(full_name,phone,email) VALUES (?,?,?)", c.getFullName(),c.getPhone(),c.getEmail()));
        return c;
    }
    public void update(Client c) throws EntityNotFoundException {
        if (execute("UPDATE clients SET full_name=?,phone=?,email=? WHERE id=?", c.getFullName(),c.getPhone(),c.getEmail(),c.getId()) == 0)
            throw new EntityNotFoundException("Клиент с ID " + c.getId() + " не найден");
    }
    public void delete(Integer id) throws EntityNotFoundException {
        if (execute("DELETE FROM clients WHERE id=?",id) == 0) throw new EntityNotFoundException("Клиент с ID " + id + " не найден");
    }
    public List<Client> search(String text) {
        String p = "%"+text+"%";
        return query("SELECT * FROM clients WHERE full_name ILIKE ? OR phone ILIKE ? OR email ILIKE ? ORDER BY id",p,p,p);
    }
    protected Client map(ResultSet rs) throws SQLException {
        Client c = new Client(rs.getInt("id"),rs.getString("full_name"),rs.getString("phone"),rs.getString("email"));
        Timestamp time = rs.getTimestamp("created_at");
        if (time != null) c.setCreatedAt(time.toLocalDateTime());
        return c;
    }
}
