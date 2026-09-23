package ru.mirea.project.service;
import ru.mirea.project.exception.*;
import ru.mirea.project.model.Equipment;
import ru.mirea.project.repository.*;
import java.util.List;
public class EquipmentService {
    private final EquipmentRepository repository = new EquipmentRepository();
    public int addEquipment(Equipment e) throws BusinessException { validate(e); return repository.save(e).getId(); }
    public List<Equipment> getAllEquipment() { return repository.findAll(); }
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
