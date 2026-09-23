package ru.mirea.project.service;
import ru.mirea.project.exception.*;
import ru.mirea.project.model.Equipment;
import ru.mirea.project.repository.*;
import ru.mirea.project.util.DatabaseConnection;
import java.util.List;
public class EquipmentService {
    private final EquipmentRepository repository = new EquipmentRepository();
    public int addEquipment(Equipment e) throws BusinessException { validate(e); return repository.save(e).getId(); }
    public List<Equipment> getAllEquipment() { return repository.findAll(); }
    public Equipment getEquipmentById(int id) throws EntityNotFoundException { return repository.findById(id); }
    public boolean updateEquipment(Equipment e) throws BusinessException,EntityNotFoundException {
        validate(e);
        return DatabaseConnection.transaction(c -> {
            EquipmentRepository equipment = new EquipmentRepository(c);
            equipment.findForUpdate(e.getId());
            if (e.isAvailable() && new RentalRequestRepository(c).hasOpenRental(e.getId()))
                throw new BusinessException("Оборудование занято заявкой. Сначала завершите или отмените её");
            equipment.update(e); return true;
        });
    }
    public boolean deleteEquipment(int id) throws BusinessException,EntityNotFoundException {
        repository.findById(id);
        if (new RentalRequestRepository().findAll().stream().anyMatch(r -> r.getEquipmentId() == id))
            throw new BusinessException("Нельзя удалить оборудование, пока с ним связаны заявки");
        repository.delete(id); return true;
    }
    public List<Equipment> searchEquipment(String query) { return repository.search(query); }
    public List<Equipment> filterByCategory(String category) { return repository.filterByCategory(category); }
    public List<Equipment> filterByAvailability(boolean available) { return repository.filterByAvailability(available); }
    public List<Equipment> sortByPrice(boolean ascending) { return repository.sortByPrice(ascending); }
    public List<String> getCategories() { return repository.getCategories(); }
    private void validate(Equipment e) throws BusinessException {
        if (e == null) throw new BusinessException("Оборудование не указано");
        e.setName(Validation.text(e.getName(),"Название",150));
        e.setCategory(Validation.text(e.getCategory(),"Категория",80));
        Validation.money(e.getPricePerDay(),"Цена за день");
    }
}
