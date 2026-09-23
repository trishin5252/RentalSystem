package ru.mirea.project.repository;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
public class RentalRequestRepository extends JdbcRepository<RentalRequest> implements Repository<RentalRequest,Integer> {
    private static final String SELECT = "SELECT r.*,c.full_name AS client_name,e.name AS equipment_name "
            + "FROM rental_requests r JOIN clients c ON c.id=r.client_id JOIN equipment e ON e.id=r.equipment_id ";
    public RentalRequestRepository() { }
    public RentalRequestRepository(Connection c) { super(c); }
    public RentalRequest findById(Integer id) throws EntityNotFoundException {
        List<RentalRequest> rows = query(SELECT + "WHERE r.id=?",id);
        if (rows.isEmpty()) throw new EntityNotFoundException("Заявка с ID " + id + " не найдена");
        return rows.get(0);
    }
    public List<RentalRequest> findAll() { return query(SELECT + "ORDER BY r.id"); }
    public RentalRequest save(RentalRequest r) {
        r.setId(insert("INSERT INTO rental_requests(client_id,equipment_id,start_date,end_date,status,total_cost) VALUES (?,?,?,?,?,?)",
                r.getClientId(),r.getEquipmentId(),r.getStartDate(),r.getEndDate(),r.getStatus().name(),r.getTotalCost()));
        return r;
    }
    public void update(RentalRequest r) throws EntityNotFoundException {
        if (execute("UPDATE rental_requests SET client_id=?,equipment_id=?,start_date=?,end_date=?,status=?,total_cost=? WHERE id=?",
                r.getClientId(),r.getEquipmentId(),r.getStartDate(),r.getEndDate(),r.getStatus().name(),r.getTotalCost(),r.getId()) == 0)
            throw new EntityNotFoundException("Заявка с ID " + r.getId() + " не найдена");
    }
    public void delete(Integer id) throws EntityNotFoundException {
        if (execute("DELETE FROM rental_requests WHERE id=?",id) == 0) throw new EntityNotFoundException("Заявка с ID " + id + " не найдена");
    }
    public List<RentalRequest> search(String text) {
        String p = "%"+text+"%";
        return query(SELECT + "WHERE c.full_name ILIKE ? OR e.name ILIKE ? OR r.status ILIKE ? ORDER BY r.id",p,p,p);
    }
    public List<RentalRequest> searchByClient(String name) { return query(SELECT + "WHERE c.full_name ILIKE ? ORDER BY r.id","%"+name+"%"); }
    public List<RentalRequest> searchByEquipment(String name) { return query(SELECT + "WHERE e.name ILIKE ? ORDER BY r.id","%"+name+"%"); }
    public List<RentalRequest> filterByStatus(RentalStatus status) { return query(SELECT + "WHERE r.status=? ORDER BY r.start_date,r.id",status.name()); }
    public List<RentalRequest> filterByDateRange(LocalDate from,LocalDate to) {
        return query(SELECT + "WHERE r.start_date BETWEEN ? AND ? ORDER BY r.start_date,r.id",from,to);
    }
    public boolean hasOpenRental(int equipmentId) {
        return !query(SELECT + "WHERE r.equipment_id=? AND r.status IN ('CREATED','ACTIVE','OVERDUE')",equipmentId).isEmpty();
    }
    protected RentalRequest map(ResultSet rs) throws SQLException {
        RentalRequest r = new RentalRequest(rs.getInt("id"),rs.getInt("client_id"),rs.getInt("equipment_id"),
                rs.getDate("start_date").toLocalDate(),rs.getDate("end_date").toLocalDate(),
                RentalStatus.valueOf(rs.getString("status")),rs.getBigDecimal("total_cost"));
        r.setClientName(rs.getString("client_name"));
        r.setEquipmentName(rs.getString("equipment_name"));
        Timestamp time = rs.getTimestamp("created_at");
        if (time != null) r.setCreatedAt(time.toLocalDateTime());
        return r;
    }
}
