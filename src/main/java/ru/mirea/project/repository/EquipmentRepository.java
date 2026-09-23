package ru.mirea.project.repository;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.Equipment;
import java.sql.*;
import java.util.List;
public class EquipmentRepository extends JdbcRepository<Equipment> implements Repository<Equipment,Integer> {
    public EquipmentRepository() { }
    public EquipmentRepository(Connection c) { super(c); }
    public Equipment findById(Integer id) throws EntityNotFoundException { return find(id,false); }
    public Equipment findForUpdate(int id) throws EntityNotFoundException { return find(id,true); }
    private Equipment find(int id,boolean lock) throws EntityNotFoundException {
        List<Equipment> rows = query("SELECT * FROM equipment WHERE id=?" + (lock ? " FOR UPDATE" : ""),id);
        if (rows.isEmpty()) throw new EntityNotFoundException("Оборудование с ID " + id + " не найдено");
        return rows.get(0);
    }
    public List<Equipment> findAll() { return query("SELECT * FROM equipment ORDER BY id"); }
    public Equipment save(Equipment e) {
        e.setId(insert("INSERT INTO equipment(name,category,price_per_day,is_available) VALUES (?,?,?,?)",
                e.getName(),e.getCategory(),e.getPricePerDay(),e.isAvailable()));
        return e;
    }
    public void update(Equipment e) throws EntityNotFoundException {
        if (execute("UPDATE equipment SET name=?,category=?,price_per_day=?,is_available=? WHERE id=?",
                e.getName(),e.getCategory(),e.getPricePerDay(),e.isAvailable(),e.getId()) == 0)
            throw new EntityNotFoundException("Оборудование с ID " + e.getId() + " не найдено");
    }
    public void delete(Integer id) throws EntityNotFoundException {
        if (execute("DELETE FROM equipment WHERE id=?",id) == 0) throw new EntityNotFoundException("Оборудование с ID " + id + " не найдено");
    }
    public List<Equipment> search(String text) {
        return query("SELECT * FROM equipment WHERE name ILIKE ? OR category ILIKE ? ORDER BY id","%"+text+"%","%"+text+"%");
    }
    public List<Equipment> filterByCategory(String category) {
        return query("SELECT * FROM equipment WHERE category=? ORDER BY price_per_day,id",category);
    }
    public List<Equipment> filterByAvailability(boolean available) {
        return query("SELECT * FROM equipment WHERE is_available=? ORDER BY name,id",available);
    }
    public List<Equipment> sortByPrice(boolean ascending) {
        return query("SELECT * FROM equipment ORDER BY price_per_day " + (ascending ? "ASC" : "DESC") + ",id");
    }
    public List<String> getCategories() { return findAll().stream().map(Equipment::getCategory).distinct().sorted().toList(); }
    protected Equipment map(ResultSet rs) throws SQLException {
        Equipment e = new Equipment(rs.getInt("id"),rs.getString("name"),rs.getString("category"),
                rs.getBigDecimal("price_per_day"),rs.getBoolean("is_available"));
        Timestamp time = rs.getTimestamp("created_at");
        if (time != null) e.setCreatedAt(time.toLocalDateTime());
        return e;
    }
}
