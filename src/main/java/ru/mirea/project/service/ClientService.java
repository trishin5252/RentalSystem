package ru.mirea.project.service;
import ru.mirea.project.exception.*;
import ru.mirea.project.model.Client;
import ru.mirea.project.repository.*;
import java.util.*;
public class ClientService {
    // Вызовы через интерфейс Repository демонстрируют полиморфизм.
    private final Repository<Client,Integer> repository = new ClientRepository();
    public int addClient(Client c) throws BusinessException { validate(c); return repository.save(c).getId(); }
    public List<Client> getAllClients() { return repository.findAll(); }
    public List<Client> searchClients(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream().filter(c -> c.getFullName().toLowerCase(Locale.ROOT).contains(q)
                || c.getPhone().contains(q) || c.getEmail().toLowerCase(Locale.ROOT).contains(q)).toList();
    }
    private void validate(Client c) throws BusinessException {
        if (c == null) throw new BusinessException("Клиент не указан");
        c.setFullName(Validation.text(c.getFullName(),"ФИО",150));
        c.setPhone(Validation.text(c.getPhone(),"Телефон",30));
        if (!c.getPhone().matches("[+0-9() -]{5,30}") || c.getPhone().replaceAll("[^0-9]","").length() < 5)
            throw new BusinessException("Телефон должен содержать не менее пяти цифр");
        c.setEmail(Validation.text(c.getEmail(),"Email",150).toLowerCase(Locale.ROOT));
        if (!c.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw new BusinessException("Некорректный email");
    }
}
