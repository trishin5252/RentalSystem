package ru.mirea.project.view;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.*;
import ru.mirea.project.service.ClientService;
import ru.mirea.project.service.EquipmentService;
import ru.mirea.project.service.RentalRequestService;
import ru.mirea.project.util.DataExporter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;

// Класс главного меню системы проката оборудования
public class Menu {
    private final Scanner scanner = new Scanner(System.in);
    private final ClientService clientService = new ClientService();
    private final EquipmentService equipmentService = new EquipmentService();
    private final RentalRequestService rentalService = new RentalRequestService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Электроинструмент", "Строительное", "Сварочное", "Измерительное"
    );

    public void start() {
        try { runMenus(); }
        catch (InputEndedException e) { System.out.println("\nВвод завершён. До свидания!"); }
    }

    private static class InputEndedException extends RuntimeException { }
    private String readLine() {
        if (!scanner.hasNextLine()) throw new InputEndedException();
        return scanner.nextLine();
    }
    private int chooseListNumber(int size, String prompt) {
        if (size == 0) throw new IllegalArgumentException("Список пуст");
        System.out.print(prompt + " (0 — отмена): ");
        int number = Integer.parseInt(readLine().trim());
        if (number == 0) return -1;
        if (number < 1 || number > size) throw new IllegalArgumentException("Выберите номер от 1 до " + size);
        return number - 1;
    }
    private Client selectClient() {
        List<Client> clients = clientService.getAllClients();
        if (clients.isEmpty()) throw new IllegalArgumentException("Сначала создайте хотя бы одного клиента");
        System.out.println("\nДоступные клиенты:");
        for (int i = 0; i < clients.size(); i++) System.out.println((i + 1) + ". " + clients.get(i));
        int index = chooseListNumber(clients.size(), "Выберите клиента");
        return index < 0 ? null : clients.get(index);
    }
    private Equipment selectEquipment(List<Equipment> equipment, String title) {
        if (equipment.isEmpty()) throw new IllegalArgumentException("Нет доступного оборудования");
        System.out.println("\n" + title + ":");
        for (int i = 0; i < equipment.size(); i++) System.out.println((i + 1) + ". " + equipment.get(i));
        int index = chooseListNumber(equipment.size(), "Выберите оборудование");
        return index < 0 ? null : equipment.get(index);
    }
    private Equipment selectEquipment() {
        return selectEquipment(equipmentService.getAllEquipment(), "Оборудование");
    }
    private RentalRequest selectRental() {
        List<RentalRequest> rentals = rentalService.getAllRentals();
        if (rentals.isEmpty()) throw new IllegalArgumentException("Список заявок пуст");
        System.out.println("\nЗаявки на аренду:");
        for (int i = 0; i < rentals.size(); i++) System.out.println((i + 1) + ". " + rentals.get(i));
        int index = chooseListNumber(rentals.size(), "Выберите заявку");
        return index < 0 ? null : rentals.get(index);
    }
    private String selectCategory() {
        LinkedHashSet<String> allCategories = new LinkedHashSet<>(DEFAULT_CATEGORIES);
        allCategories.addAll(equipmentService.getCategories());
        List<String> categories = new ArrayList<>(allCategories);
        System.out.println("\nКатегории оборудования:");
        for (int i = 0; i < categories.size(); i++) System.out.println((i + 1) + ". " + categories.get(i));
        int index = chooseListNumber(categories.size(), "Выберите категорию");
        return index < 0 ? null : categories.get(index);
    }
    private Boolean selectAvailability() {
        System.out.println("\nСтатус оборудования:");
        System.out.println("1. Доступно");
        System.out.println("2. Занято / временно недоступно");
        int index = chooseListNumber(2, "Выберите статус");
        return index < 0 ? null : index == 0;
    }
    private boolean confirmAction(String action) {
        System.out.println("\nПодтверждение: " + action);
        System.out.println("1. Да");
        System.out.println("2. Нет");
        int index = chooseListNumber(2, "Выберите вариант");
        return index == 0;
    }
    private RentalStatus statusByNumber(int number) {
        if (number < 0 || number >= RentalStatus.values().length)
            throw new IllegalArgumentException("Номер статуса должен быть от 0 до " + (RentalStatus.values().length - 1));
        return RentalStatus.values()[number];
    }
    private String friendlyMessage(Exception e) {
        if (e instanceof NumberFormatException) return "Введите корректное число (десятичный разделитель — точка)";
        if (e instanceof DateTimeParseException) return "Введите существующую дату в формате гггг-мм-дд";
        return e.getMessage();
    }
    private void runMenus() {
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = readLine().trim();
            try {
                switch (choice) {
                    case "1" -> clientMenu();
                    case "2" -> equipmentMenu();
                    case "3" -> rentalMenu();
                    case "4" -> searchMenu();
                    case "5" -> filterMenu();
                    case "6" -> statisticsMenu();
                    case "7" -> exportMenu();
                    case "8" -> showDatabaseTables();
                    case "0" -> {
                        System.out.println("\nДо свидания!");
                        running = false;
                    }
                    default -> System.out.println("Неверный выбор! Попробуйте снова.");
                }
            } catch (Exception e) {
            if (e instanceof InputEndedException) throw (InputEndedException) e;
                System.err.println("Ошибка: " + friendlyMessage(e));
            }
        }
    }

    private void printMainMenu() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("              СИСТЕМА ПРОКАТА ОБОРУДОВАНИЯ");
        System.out.println("=".repeat(70));
        System.out.println("1. Клиенты");
        System.out.println("2. Оборудование");
        System.out.println("3. Заявки на аренду");
        System.out.println("4. Поиск");
        System.out.println("5. Фильтрация");
        System.out.println("6. Статистика");
        System.out.println("7. Экспорт данных");
        System.out.println("8. Вывести таблицы базы данных");
        System.out.println("0. Выход");
        System.out.println("-".repeat(70));
        System.out.print("Выберите действие: ");
    }

    // Меню клиентов
    private void clientMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- МЕНЮ: КЛИЕНТЫ ---");
            System.out.println("1. Создать клиента");
            System.out.println("2. Вывести всех клиентов");
            System.out.println("3. Поиск клиента");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");

            String choice = readLine().trim();
            try {
                switch (choice) {
                    case "1" -> createClient();
                    case "2" -> showAllClients();
                    case "3" -> searchClients();
                    case "0" -> back = true;
                    default -> System.out.println("Неверный выбор!");
                }
            } catch (Exception e) {
            if (e instanceof InputEndedException) throw (InputEndedException) e;
                System.err.println("Ошибка: " + friendlyMessage(e));
            }
        }
    }

    private void createClient() throws BusinessException {
        System.out.print("Введите ФИО: ");
        String name = readLine().trim();
        System.out.print("Введите телефон: ");
        String phone = readLine().trim();
        System.out.print("Введите email: ");
        String email = readLine().trim();

        Client client = new Client(name, phone, email);
        int id = clientService.addClient(client);
        System.out.println("Клиент успешно создан! ID: " + id);
    }

    private void showAllClients() {
        List<Client> clients = clientService.getAllClients();
        if (clients.isEmpty()) {
            System.out.println("Список клиентов пуст.");
            return;
        }
        System.out.println("\n" + "-".repeat(90));
        System.out.printf("%-4s | %-30s | %-20s | %-30s%n", "ID", "ФИО", "Телефон", "Email");
        System.out.println("-".repeat(90));
        for (Client c : clients) {
            System.out.println(c);
        }
        System.out.println("-".repeat(90));
        System.out.println("Всего клиентов: " + clients.size());
    }

    private void searchClients() {
        System.out.print("Введите часть ФИО, телефона или email: ");
        List<Client> result = clientService.searchClients(readLine().trim());
        System.out.println("Найдено: " + result.size());
        for (Client client : result) System.out.println(client);
    }
        // Меню оборудования
    private void equipmentMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- МЕНЮ: ОБОРУДОВАНИЕ ---");
            System.out.println("1. Добавить оборудование");
            System.out.println("2. Вывести всё оборудование");
            System.out.println("3. Сортировка по цене");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");

            String choice = readLine().trim();
            try {
                switch (choice) {
                    case "1" -> createEquipment();
                    case "2" -> showAllEquipment();
                    case "3" -> sortEquipmentMenu();
                    case "0" -> back = true;
                    default -> System.out.println("Неверный выбор!");
                }
            } catch (Exception e) {
            if (e instanceof InputEndedException) throw (InputEndedException) e;
                System.err.println("Ошибка: " + friendlyMessage(e));
            }
        }
    }

    private void createEquipment() throws BusinessException {
        System.out.print("Название: ");
        String name = readLine().trim();
        String category = selectCategory();
        if (category == null) return;
        System.out.print("Цена за день (руб.): ");
        BigDecimal price = new BigDecimal(readLine().trim());
        Boolean available = selectAvailability();
        if (available == null) return;

        Equipment eq = new Equipment(name, category, price, available);
        int id = equipmentService.addEquipment(eq);
        System.out.println("Оборудование добавлено! ID: " + id);
    }

    private void showAllEquipment() {
        List<Equipment> list = equipmentService.getAllEquipment();
        if (list.isEmpty()) {
            System.out.println("Список оборудования пуст.");
            return;
        }
        System.out.println("\n" + "-".repeat(90));
        System.out.printf("%-4s | %-30s | %-20s | %-12s | %-10s%n",
                "ID", "Название", "Категория", "Цена/день", "Статус");
        System.out.println("-".repeat(90));
        for (Equipment e : list) {
            System.out.println(e);
        }
        System.out.println("-".repeat(90));
        System.out.println("Всего единиц: " + list.size());
    }


    private void sortEquipmentMenu() {
        System.out.println("1. По возрастанию цены");
        System.out.println("2. По убыванию цены");
        System.out.print("Выбор: ");
        String choice = readLine().trim();
        if (!choice.equals("1") && !choice.equals("2")) throw new IllegalArgumentException("Выберите 1 или 2");
        boolean ascending = choice.equals("1");
        List<Equipment> list = equipmentService.sortByPrice(ascending);
        System.out.println("\n" + "-".repeat(90));
        System.out.printf("%-4s | %-30s | %-20s | %-12s | %-10s%n",
                "ID", "Название", "Категория", "Цена/день", "Статус");
        System.out.println("-".repeat(90));
        for (Equipment e : list) System.out.println(e);
    }
        // Меню заявок на аренду
    private void rentalMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- МЕНЮ: ЗАЯВКИ НА АРЕНДУ ---");
            System.out.println("1. Создать заявку");
            System.out.println("2. Вывести все заявки");
            System.out.println("3. Получить заявку по ID");
            System.out.println("4. Изменить заявку");
            System.out.println("5. Удалить заявку");
            System.out.println("6. Изменить статус заявки");
            System.out.println("7. Сортировка заявок");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");

            String choice = readLine().trim();
            try {
                switch (choice) {
                    case "1" -> createRental();
                    case "2" -> showAllRentals();
                    case "3" -> getRentalById();
                    case "4" -> updateRental();
                    case "5" -> deleteRental();
                    case "6" -> changeRentalStatus();
                    case "7" -> sortRentalsMenu();
                    case "0" -> back = true;
                    default -> System.out.println("Неверный выбор!");
                }
            } catch (Exception e) {
            if (e instanceof InputEndedException) throw (InputEndedException) e;
                System.err.println("Ошибка: " + friendlyMessage(e));
            }
        }
    }

    private void createRental() {
        try {
            Client client = selectClient();
            if (client == null) return;
            Equipment equipment = selectEquipment(equipmentService.filterByAvailability(true), "Доступное оборудование");
            if (equipment == null) return;

            System.out.print("Дата начала (гггг-мм-дд): ");
            LocalDate start = LocalDate.parse(readLine().trim(), dateFormatter);
            System.out.print("Дата окончания (гггг-мм-дд): ");
            LocalDate end = LocalDate.parse(readLine().trim(), dateFormatter);

            int id = rentalService.createRental(client.getId(), equipment.getId(), start, end);
            System.out.println("Заявка создана! ID: " + id);
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: ID должен быть целым числом.");
        } catch (DateTimeParseException e) {
            System.err.println("Ошибка: неверный формат даты. Используйте гггг-мм-дд");
        } catch (Exception e) {
            if (e instanceof InputEndedException) throw (InputEndedException) e;
            System.err.println("Ошибка: " + friendlyMessage(e));
        }
    }

    private void showAllRentals() {
        List<RentalRequest> list = rentalService.getAllRentals();
        if (list.isEmpty()) {
            System.out.println("Список заявок пуст.");
            return;
        }
        System.out.println("\n" + "-".repeat(110));
        System.out.printf("%-4s | %-25s | %-25s | %-12s | %-12s | %-12s | %-12s%n",
                "ID", "Клиент", "Оборудование", "Начало", "Окончание", "Стоимость", "Статус");
        System.out.println("-".repeat(110));
        for (RentalRequest r : list) System.out.println(r);
        System.out.println("-".repeat(110));
        System.out.println("Всего заявок: " + list.size());
    }

    private void getRentalById() {
        try {
            RentalRequest r = selectRental();
            if (r == null) return;
            System.out.println("\nID: " + r.getId());
            System.out.println("Клиент: " + r.getClientName());
            System.out.println("Оборудование: " + r.getEquipmentName());
            System.out.println("Период: " + r.getStartDate() + " - " + r.getEndDate());
            System.out.println("Дней: " + r.getDaysCount());
            System.out.println("Стоимость: " + r.getTotalCost() + " руб.");
            System.out.println("Статус: " + r.getStatus().getDisplayName());
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: ID должен быть целым числом.");
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    private void updateRental() {
        try {
            RentalRequest r = selectRental();
            if (r == null) return;

            System.out.println("Текущие даты: " + r.getStartDate() + " - " + r.getEndDate());
            System.out.print("Новая дата начала (Enter - оставить): ");
            String s = readLine().trim();
            if (!s.isEmpty()) r.setStartDate(LocalDate.parse(s, dateFormatter));

            System.out.print("Новая дата окончания (Enter - оставить): ");
            String e = readLine().trim();
            if (!e.isEmpty()) r.setEndDate(LocalDate.parse(e, dateFormatter));


            rentalService.updateRental(r);
            System.out.println("Заявка обновлена! Новая стоимость: " + r.getTotalCost() + " руб.");
        } catch (NumberFormatException ex) {
            System.err.println("Ошибка: ID должен быть целым числом.");
        } catch (DateTimeParseException ex) {
            System.err.println("Ошибка: неверный формат даты.");
        } catch (Exception ex) {
            if (ex instanceof InputEndedException) throw (InputEndedException) ex;
            System.err.println("Ошибка: " + friendlyMessage(ex));
        }
    }

    private void deleteRental() {
        try {
            RentalRequest r = selectRental();
            if (r == null) return;
            if (confirmAction("удалить заявку №" + r.getId())) {
                rentalService.deleteRental(r.getId());
                System.out.println("Заявка удалена!");
            }
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: ID должен быть целым числом.");
        } catch (Exception e) {
            if (e instanceof InputEndedException) throw (InputEndedException) e;
            System.err.println("Ошибка: " + friendlyMessage(e));
        }
    }

    private void changeRentalStatus() {
        try {
            RentalRequest rental = selectRental();
            if (rental == null) return;

            System.out.println("Доступные статусы:");
            for (RentalStatus s : RentalStatus.values()) {
                System.out.println("  " + s.ordinal() + ". " + s.getDisplayName());
            }
            System.out.print("Выберите статус (номер): ");
            int statusNum = Integer.parseInt(readLine().trim());
            RentalStatus newStatus = statusByNumber(statusNum);

            rentalService.changeStatus(rental.getId(), newStatus);
            System.out.println("Статус изменён на: " + newStatus.getDisplayName());
        } catch (NumberFormatException e) {
            System.err.println("Ошибка: введите число.");
        } catch (Exception e) {
            if (e instanceof InputEndedException) throw (InputEndedException) e;
            System.err.println("Ошибка: " + friendlyMessage(e));
        }
    }

    private void sortRentalsMenu() {
        System.out.println("1. По дате (возрастание)");
        System.out.println("2. По дате (убывание)");
        System.out.println("3. По стоимости (возрастание)");
        System.out.println("4. По стоимости (убывание)");
        System.out.print("Выбор: ");
        String choice = readLine().trim();

        List<RentalRequest> list = switch (choice) {
            case "1" -> rentalService.sortByDate(true);
            case "2" -> rentalService.sortByDate(false);
            case "3" -> rentalService.sortByCost(true);
            case "4" -> rentalService.sortByCost(false);
            default -> {
                System.out.println("Неверный выбор!");
                yield new ArrayList<>();
            }
        };

        if (!list.isEmpty()) {
            System.out.println("\n" + "-".repeat(110));
            System.out.printf("%-4s | %-25s | %-25s | %-12s | %-12s | %-12s | %-12s%n",
                    "ID", "Клиент", "Оборудование", "Начало", "Окончание", "Стоимость", "Статус");
            System.out.println("-".repeat(110));
            for (RentalRequest r : list) System.out.println(r);
        }
    }
        // Меню поиска
    private void searchMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- МЕНЮ: ПОИСК ---");
            System.out.println("1. Поиск клиентов");
            System.out.println("2. Поиск оборудования");
            System.out.println("3. Общий поиск заявок");
            System.out.println("4. Заявки выбранного клиента");
            System.out.println("5. Заявки выбранного оборудования");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");

            String choice = readLine().trim();
            try {
                switch (choice) {
                    case "1" -> {
                        System.out.print("Введите запрос: ");
                        String q = readLine().trim();
                        List<Client> results = clientService.searchClients(q);
                        System.out.println("Найдено: " + results.size());
                        for (Client c : results) System.out.println(c);
                    }
                    case "2" -> {
                        System.out.print("Введите запрос: ");
                        String q = readLine().trim();
                        List<Equipment> results = equipmentService.searchEquipment(q);
                        System.out.println("Найдено: " + results.size());
                        for (Equipment e : results) System.out.println(e);
                    }
                    case "3" -> {
                        System.out.print("Введите запрос: ");
                        String q = readLine().trim();
                        List<RentalRequest> results = rentalService.searchRentals(q);
                        System.out.println("Найдено: " + results.size());
                        for (RentalRequest r : results) System.out.println(r);
                    }
                    case "4" -> {
                        Client client = selectClient();
                        if (client == null) continue;
                        List<RentalRequest> results = rentalService.searchByClient(client.getFullName());
                        System.out.println("Найдено: " + results.size());
                        for (RentalRequest r : results) System.out.println(r);
                    }
                    case "5" -> {
                        Equipment equipment = selectEquipment();
                        if (equipment == null) continue;
                        List<RentalRequest> results = rentalService.searchByEquipment(equipment.getName());
                        System.out.println("Найдено: " + results.size());
                        for (RentalRequest r : results) System.out.println(r);
                    }
                    case "0" -> back = true;
                    default -> System.out.println("Неверный выбор!");
                }
            } catch (Exception e) {
            if (e instanceof InputEndedException) throw (InputEndedException) e;
                System.err.println("Ошибка: " + friendlyMessage(e));
            }
        }
    }

    // Меню фильтрации
    private void filterMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- МЕНЮ: ФИЛЬТРАЦИЯ ---");
            System.out.println("1. Оборудование по категории");
            System.out.println("2. Оборудование по доступности");
            System.out.println("3. Заявки по статусу");
            System.out.println("4. Заявки по диапазону дат");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");

            String choice = readLine().trim();
            try {
                switch (choice) {
                    case "1" -> {
                        List<String> cats = equipmentService.getCategories();
                        System.out.println("Доступные категории:");
                        for (int i = 0; i < cats.size(); i++) {
                            System.out.println((i + 1) + ". " + cats.get(i));
                        }
                        System.out.print("Выберите номер: ");
                        int num = Integer.parseInt(readLine().trim());
                        if (num < 1 || num > cats.size()) throw new IllegalArgumentException("Нет категории с таким номером");
                        List<Equipment> list = equipmentService.filterByCategory(cats.get(num - 1));
                        System.out.println("Найдено: " + list.size());
                        for (Equipment e : list) System.out.println(e);
                    }
                    case "2" -> {
                        Boolean avail = selectAvailability();
                        if (avail == null) continue;
                        List<Equipment> list = equipmentService.filterByAvailability(avail);
                        System.out.println("Найдено: " + list.size());
                        for (Equipment e : list) System.out.println(e);
                    }
                    case "3" -> {
                        System.out.println("Статусы:");
                        for (RentalStatus s : RentalStatus.values()) {
                            System.out.println("  " + s.ordinal() + ". " + s.getDisplayName());
                        }
                        System.out.print("Выберите статус (номер): ");
                        int num = Integer.parseInt(readLine().trim());
                        List<RentalRequest> list = rentalService.filterByStatus(statusByNumber(num));
                        System.out.println("Найдено: " + list.size());
                        for (RentalRequest r : list) System.out.println(r);
                    }
                    case "4" -> {
                        System.out.print("Дата от (гггг-мм-дд): ");
                        LocalDate from = LocalDate.parse(readLine().trim(), dateFormatter);
                        System.out.print("Дата до (гггг-мм-дд): ");
                        LocalDate to = LocalDate.parse(readLine().trim(), dateFormatter);
                        List<RentalRequest> list = rentalService.filterByDateRange(from, to);
                        System.out.println("Найдено: " + list.size());
                        for (RentalRequest r : list) System.out.println(r);
                    }
                    case "0" -> back = true;
                    default -> System.out.println("Неверный выбор!");
                }
            } catch (Exception e) {
            if (e instanceof InputEndedException) throw (InputEndedException) e;
                System.err.println("Ошибка: " + friendlyMessage(e));
            }
        }
    }

    // Статистика
    private void statisticsMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("           СТАТИСТИКА СИСТЕМЫ");
        System.out.println("=".repeat(50));

        int totalRentals = rentalService.getTotalRentals();
        BigDecimal revenue = rentalService.getTotalRevenue();

        System.out.println("1. Всего заявок: " + totalRentals);
        System.out.println("2. Выручка завершённых аренд: " + revenue + " руб.");

        int completed = rentalService.getCountByStatus(RentalStatus.COMPLETED);
        int active = rentalService.getCountByStatus(RentalStatus.ACTIVE);
        int cancelled = rentalService.getCountByStatus(RentalStatus.CANCELLED);

        System.out.println("3. Завершённых заявок: " + completed);
        System.out.println("4. Активных заявок: " + active);
        System.out.println("5. Отменённых заявок: " + cancelled);

        System.out.println();
        System.out.println("Всего клиентов: " + clientService.getAllClients().size());
        System.out.println("Всего единиц оборудования: " + equipmentService.getAllEquipment().size());

        long available = equipmentService.filterByAvailability(true).size();
        long busy = equipmentService.filterByAvailability(false).size();
        System.out.println("  Доступно оборудования: " + available);
        System.out.println("  Занято оборудования: " + busy);
        System.out.println("=".repeat(50));
    }

    // Меню экспорта
    private void exportMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- МЕНЮ: ЭКСПОРТ ДАННЫХ ---");
            System.out.println("1. Экспорт клиентов в Excel");
            System.out.println("2. Экспорт оборудования в Excel");
            System.out.println("3. Экспорт заявок в Excel");
            System.out.println("0. Назад");
            System.out.print("Выберите действие: ");

            String choice = readLine().trim();
            try {
                switch (choice) {
                    case "1" -> {
                        List<Client> clients = clientService.getAllClients();
                        List<String[]> data = new ArrayList<>();
                        for (Client c : clients) {
                            data.add(new String[]{
                                    String.valueOf(c.getId()), c.getFullName(), c.getPhone(), c.getEmail()
                            });
                        }
                        DataExporter.exportToExcel("exports/clients.xlsx", data,
                                new String[]{"ID", "ФИО", "Телефон", "Email"});
                    }
                    case "2" -> {
                        List<Equipment> list = equipmentService.getAllEquipment();
                        List<String[]> data = new ArrayList<>();
                        for (Equipment e : list) {
                            data.add(new String[]{
                                    String.valueOf(e.getId()), e.getName(), e.getCategory(),
                                    e.getPricePerDay().toString(), e.isAvailable() ? "Да" : "Нет"
                            });
                        }
                        DataExporter.exportToExcel("exports/equipment.xlsx", data,
                                new String[]{"ID", "Название", "Категория", "Цена/день", "Доступно"});
                    }
                    case "3" -> {
                        List<RentalRequest> list = rentalService.getAllRentals();
                        List<String[]> data = new ArrayList<>();
                        for (RentalRequest r : list) {
                            data.add(new String[]{
                                    String.valueOf(r.getId()),
                                    r.getClientName() != null ? r.getClientName() : String.valueOf(r.getClientId()),
                                    r.getEquipmentName() != null ? r.getEquipmentName() : String.valueOf(r.getEquipmentId()),
                                    r.getStartDate().toString(), r.getEndDate().toString(),
                                    r.getTotalCost().toString(), r.getStatus().getDisplayName()
                            });
                        }
                        DataExporter.exportToExcel("exports/rentals.xlsx", data,
                                new String[]{"ID", "Клиент", "Оборудование", "Начало", "Окончание", "Стоимость", "Статус"});
                    }
                    case "0" -> back = true;
                    default -> System.out.println("Неверный выбор!");
                }
            } catch (Exception e) {
            if (e instanceof InputEndedException) throw (InputEndedException) e;
                System.err.println("Ошибка экспорта: " + e.getMessage());
            }
        }
    }

    // Вывод таблиц базы данных
    private void showDatabaseTables() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- ТАБЛИЦЫ БАЗЫ ДАННЫХ ---");
            System.out.println("1. Таблица clients");
            System.out.println("2. Таблица equipment");
            System.out.println("3. Таблица rental_requests");
            System.out.println("0. Назад");
            System.out.print("Выберите таблицу: ");

            String choice = readLine().trim();
            try {
                switch (choice) {
                    case "1" -> showAllClients();
                    case "2" -> showAllEquipment();
                    case "3" -> showAllRentals();
                    case "0" -> back = true;
                    default -> System.out.println("Неверный выбор!");
                }
            } catch (Exception e) {
            if (e instanceof InputEndedException) throw (InputEndedException) e;
                System.err.println("Ошибка: " + friendlyMessage(e));
            }
        }
    }
}
