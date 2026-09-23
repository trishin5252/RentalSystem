package ru.mirea.project.service;
import ru.mirea.project.exception.*;
import ru.mirea.project.model.*;
import ru.mirea.project.repository.*;
import ru.mirea.project.util.DatabaseConnection;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RentalRequestService {
    private final RentalRequestRepository repository = new RentalRequestRepository();
    private final Clock clock;
    public RentalRequestService() { this(Clock.systemDefaultZone()); }
    public RentalRequestService(Clock clock) { this.clock = Objects.requireNonNull(clock); }

    public int createRental(int clientId,int equipmentId,LocalDate start,LocalDate end)
            throws BusinessException,EntityNotFoundException {
        validateDates(start,end);
        return DatabaseConnection.transaction(c -> {
            new ClientRepository(c).findById(clientId);
            EquipmentRepository equipment = new EquipmentRepository(c);
            Equipment eq = equipment.findForUpdate(equipmentId);
            RentalRequestRepository rentals = new RentalRequestRepository(c);
            if (!eq.isAvailable() || rentals.hasOpenRental(equipmentId))
                throw new BusinessException("Оборудование недоступно или уже занято заявкой");
            RentalRequest r = new RentalRequest(0,clientId,equipmentId,start,end,RentalStatus.CREATED,cost(eq,start,end));
            rentals.save(r);
            eq.setAvailable(false);
            equipment.update(eq);
            return r.getId();
        });
    }
    private void validateDates(LocalDate start,LocalDate end) throws BusinessException {
        if (start == null || end == null) throw new BusinessException("Укажите обе даты аренды");
        if (!end.isAfter(start)) throw new BusinessException("Дата окончания должна быть позже начала: минимум один день");
        if (start.isBefore(LocalDate.now(clock))) throw new BusinessException("Дата начала не может быть в прошлом");
    }
    private BigDecimal cost(Equipment e,LocalDate start,LocalDate end) throws BusinessException {
        Validation.money(e.getPricePerDay(),"Цена за день");
        BigDecimal result = e.getPricePerDay().multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(start,end)));
        Validation.money(result,"Стоимость аренды");
        return result;
    }
    public List<RentalRequest> getAllRentals() { return repository.findAll(); }
    public RentalRequest getRentalById(int id) throws EntityNotFoundException { return repository.findById(id); }
    public boolean updateRental(RentalRequest requested) throws BusinessException,EntityNotFoundException {
        if (requested == null) throw new BusinessException("Заявка не указана");
        validateDates(requested.getStartDate(),requested.getEndDate());
        return DatabaseConnection.transaction(c -> {
            RentalRequestRepository rentals = new RentalRequestRepository(c);
            RentalRequest old = rentals.findForUpdate(requested.getId());
            if (old.getStatus() != RentalStatus.CREATED) throw new BusinessException("Редактировать даты можно только у созданной заявки");
            if (old.getClientId() != requested.getClientId() || old.getEquipmentId() != requested.getEquipmentId())
                throw new BusinessException("Клиента и оборудование изменить нельзя. Отмените заявку и создайте новую");
            if (requested.getStatus() != old.getStatus()) throw new BusinessException("Для изменения статуса используйте отдельную операцию");
            Equipment eq = new EquipmentRepository(c).findForUpdate(old.getEquipmentId());
            requested.setTotalCost(cost(eq,requested.getStartDate(),requested.getEndDate()));
            rentals.update(requested);
            return true;
        });
    }
    public boolean deleteRental(int id) throws BusinessException,EntityNotFoundException {
        return DatabaseConnection.transaction(c -> {
            RentalRequestRepository rentals = new RentalRequestRepository(c);
            RentalRequest r = rentals.findForUpdate(id);
            if (r.getStatus() == RentalStatus.ACTIVE || r.getStatus() == RentalStatus.OVERDUE)
                throw new BusinessException("Сначала оформите возврат оборудования (статус Завершена)");
            EquipmentRepository equipment = new EquipmentRepository(c);
            Equipment eq = equipment.findForUpdate(r.getEquipmentId());
            rentals.delete(id);
            if (r.getStatus().reservesEquipment()) { eq.setAvailable(true); equipment.update(eq); }
            return true;
        });
    }
    public boolean changeStatus(int id,RentalStatus next) throws BusinessException,EntityNotFoundException {
        return DatabaseConnection.transaction(c -> {
            RentalRequestRepository rentals = new RentalRequestRepository(c);
            RentalRequest r = rentals.findForUpdate(id);
            if (!r.getStatus().canTransitionTo(next)) throw new BusinessException("Запрещён переход: " + r.getStatus() + " -> " + next);
            LocalDate today = LocalDate.now(clock);
            if (next == RentalStatus.ACTIVE && r.getStartDate().isAfter(today))
                throw new BusinessException("Выдать оборудование можно не раньше даты начала аренды");
            if (next == RentalStatus.OVERDUE && !r.getEndDate().isBefore(today))
                throw new BusinessException("Срок аренды ещё не просрочен");
            EquipmentRepository equipment = new EquipmentRepository(c);
            Equipment eq = equipment.findForUpdate(r.getEquipmentId());
            r.setStatus(next);
            rentals.update(r);
            eq.setAvailable(!next.reservesEquipment());
            equipment.update(eq);
            return true;
        });
    }
    public List<RentalRequest> searchRentals(String query) { return repository.search(query); }
    public List<RentalRequest> searchByClient(String query) { return repository.searchByClient(query); }
    public List<RentalRequest> searchByEquipment(String query) { return repository.searchByEquipment(query); }
    public List<RentalRequest> filterByStatus(RentalStatus status) {
        if (status == null) throw new IllegalArgumentException("Укажите статус");
        return repository.filterByStatus(status);
    }
    public List<RentalRequest> filterByDateRange(LocalDate from,LocalDate to) throws BusinessException {
        if (from == null || to == null || from.isAfter(to)) throw new BusinessException("Неверный диапазон дат");
        return repository.filterByDateRange(from,to);
    }
    public List<RentalRequest> sortByDate(boolean ascending) {
        Comparator<RentalRequest> cmp = Comparator.comparing(RentalRequest::getStartDate);
        return repository.findAll().stream().sorted((ascending ? cmp : cmp.reversed()).thenComparingInt(RentalRequest::getId)).toList();
    }
    public List<RentalRequest> sortByCost(boolean ascending) {
        Comparator<RentalRequest> cmp = Comparator.comparing(RentalRequest::getTotalCost);
        return repository.findAll().stream().sorted((ascending ? cmp : cmp.reversed()).thenComparingInt(RentalRequest::getId)).toList();
    }
    public BigDecimal getTotalRevenue() {
        return repository.filterByStatus(RentalStatus.COMPLETED).stream().map(RentalRequest::getTotalCost).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    public int getCountByStatus(RentalStatus status) { return repository.filterByStatus(status).size(); }
    public int getTotalRentals() { return repository.findAll().size(); }
}
