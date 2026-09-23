package ru.mirea.project;

import ru.mirea.project.exception.*;
import ru.mirea.project.model.*;
import ru.mirea.project.repository.*;
import ru.mirea.project.service.*;
import ru.mirea.project.util.*;
import ru.mirea.project.view.Menu;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Самостоятельный интеграционный набор без дополнительных тестовых библиотек.
 * Создаёт уникальную временную схему PostgreSQL и удаляет только эту схему. */
public class RegressionChecks {
    static int passed;
    static final ClientService clients = new ClientService();
    static final EquipmentService equipment = new EquipmentService();
    static final RentalRequestService rentals = new RentalRequestService();
    static final LocalDate today = LocalDate.now();
    @FunctionalInterface interface Checked { void run() throws Exception; }
    static void check(boolean ok,String name) {
        if (!ok) throw new AssertionError(name);
        passed++;
        System.out.println("PASS: " + name);
    }
    static void rejects(Class<? extends Throwable> type,Checked action,String name) throws Exception {
        try { action.run(); }
        catch (Throwable e) { if (type.isInstance(e)) { check(true,name); return; } throw new AssertionError(name,e); }
        throw new AssertionError("Ожидалось исключение: " + name);
    }
    static int newEquipment() throws Exception {
        return equipment.addEquipment(new Equipment("Тестовый инструмент","Тест",new BigDecimal("125.50"),true));
    }
    static int create(int id) throws Exception { return rentals.createRental(1,id,today,today.plusDays(2)); }
    static int sqlCount(Connection c,String sql) throws Exception {
        try (Statement st=c.createStatement(); ResultSet rs=st.executeQuery(sql)) { rs.next(); return rs.getInt(1); }
    }
    public static void main(String[] args) throws Exception {
        String oldUrl = System.getProperty("db.url");
        String schema = "rental_verify_" + Long.toUnsignedString(System.nanoTime());
        if (!schema.matches("rental_verify_[0-9]+")) throw new IllegalStateException("Неверное имя тестовой схемы");
        try (Connection admin=DatabaseConnection.getConnection()) {
            String url=admin.getMetaData().getURL();
            if (url.toLowerCase(Locale.ROOT).contains("currentschema=")) throw new IllegalStateException("Тестам нужен URL без currentSchema");
            try (Statement st=admin.createStatement()) { st.execute("CREATE SCHEMA " + schema); }
            try {
                System.setProperty("db.url",url+(url.contains("?")?"&":"?")+"currentSchema="+schema);
                try (Connection c=DatabaseConnection.getConnection(); Statement st=c.createStatement()) {
                    st.execute(Files.readString(Path.of("database.sql"),StandardCharsets.UTF_8));
                }
                runChecks();
                System.out.println("ALL PASSED: " + passed);
            } finally {
                if (oldUrl==null) System.clearProperty("db.url"); else System.setProperty("db.url",oldUrl);
                try (Statement st=admin.createStatement()) { st.execute("DROP SCHEMA " + schema + " CASCADE"); }
            }
        }
    }
    static void runChecks() throws Exception {
        check(clients.getAllClients().size()==5,"5 начальных клиентов");
        check(equipment.getAllEquipment().size()==8,"8 единиц оборудования");
        check(rentals.getAllRentals().size()==10,"10 начальных заявок");
        check(rentals.getAllRentals().stream().map(RentalRequest::getStatus).distinct().count()==5,"5 разных статусов");
        rejects(BusinessException.class,()->clients.addClient(new Client(" ","12345","a@b.test")),"Пустое ФИО");
        rejects(BusinessException.class,()->clients.addClient(new Client("Тест","12345","wrong")),"Неверный email");
        rejects(BusinessException.class,()->clients.addClient(new Client("Тест","abc","a@b.test")),"Неверный телефон");
        Client edited=clients.getClientById(1);
        edited.setEmail("wrong");
        rejects(BusinessException.class,()->clients.updateClient(edited),"Валидация при редактировании клиента");
        rejects(DataAccessException.class,()->clients.addClient(new Client("Дубликат","12345","IVANOV@example.test")),"Уникальность email без учёта регистра");
        int clientId=clients.addClient(new Client(" Новый клиент ","+79000000099","new@example.test"));
        Client newClient=clients.getClientById(clientId);
        check(newClient.getFullName().equals("Новый клиент"),"Нормализация ФИО");
        newClient.setFullName("Изменённый клиент"); clients.updateClient(newClient);
        check(clients.getClientById(clientId).getFullName().equals("Изменённый клиент"),"Изменение клиента");
        clients.deleteClient(clientId);
        rejects(EntityNotFoundException.class,()->clients.getClientById(clientId),"Удаление клиента");
        rejects(EntityNotFoundException.class,()->rentals.getRentalById(-1),"Несуществующий ID");
        rejects(BusinessException.class,()->equipment.addEquipment(new Equipment("X","Y",BigDecimal.ZERO,true)),"Нулевая цена");
        rejects(BusinessException.class,()->equipment.addEquipment(new Equipment("X","Y",new BigDecimal("1.001"),true)),"Доли копейки");
        int eq=newEquipment();
        Equipment item=equipment.getEquipmentById(eq); item.setCategory(" ");
        rejects(BusinessException.class,()->equipment.updateEquipment(item),"Пустая категория при изменении");
        rejects(BusinessException.class,()->rentals.createRental(1,eq,null,today),"Неуказанная дата");
        rejects(BusinessException.class,()->rentals.createRental(1,eq,today,today),"Нулевая длительность");
        rejects(BusinessException.class,()->rentals.createRental(1,eq,today,today.minusDays(1)),"Обратные даты");
        rejects(BusinessException.class,()->rentals.createRental(1,eq,today.minusDays(1),today.plusDays(1)),"Дата в прошлом");
        rejects(EntityNotFoundException.class,()->rentals.createRental(-1,eq,today,today.plusDays(1)),"Несуществующий клиент проверяется сервисом");
        rejects(EntityNotFoundException.class,()->rentals.createRental(1,-1,today,today.plusDays(1)),"Несуществующее оборудование");
        int id=create(eq);
        check(rentals.getRentalById(id).getTotalCost().compareTo(new BigDecimal("251.00"))==0,"Расчёт стоимости двух дней");
        check(!equipment.getEquipmentById(eq).isAvailable(),"Создание резервирует оборудование");
        rejects(BusinessException.class,()->create(eq),"Повторная бронь отклоняется");
        RentalRequest r=rentals.getRentalById(id);
        r.setEndDate(today.plusDays(3)); r.setTotalCost(BigDecimal.ZERO); rentals.updateRental(r);
        check(rentals.getRentalById(id).getTotalCost().compareTo(new BigDecimal("376.50"))==0,"Сервис сам пересчитывает стоимость");
        r.setEndDate(today);
        rejects(BusinessException.class,()->rentals.updateRental(r),"Нельзя обойти проверку дат редактированием");
        r.setEndDate(today.plusDays(3)); r.setStatus(RentalStatus.COMPLETED);
        rejects(BusinessException.class,()->rentals.updateRental(r),"Нельзя обойти переход статуса редактированием");
        r.setStatus(RentalStatus.CREATED); r.setClientId(2);
        rejects(BusinessException.class,()->rentals.updateRental(r),"Запрет подмены клиента");
        Equipment busy=equipment.getEquipmentById(eq); busy.setAvailable(true);
        rejects(BusinessException.class,()->equipment.updateEquipment(busy),"Нельзя вручную освободить занятое оборудование");
        rejects(BusinessException.class,()->clients.deleteClient(1),"Удаление связанного клиента отклоняется");
        rejects(BusinessException.class,()->equipment.deleteEquipment(eq),"Удаление связанного оборудования отклоняется");
        rejects(DataAccessException.class,()->new ClientRepository().delete(1),"FK запрещает каскадное удаление истории");
        rejects(BusinessException.class,()->rentals.changeStatus(id,RentalStatus.COMPLETED),"CREATED -> COMPLETED запрещён");
        rejects(BusinessException.class,()->rentals.changeStatus(id,null),"Неуказанный статус");
        rentals.changeStatus(id,RentalStatus.ACTIVE);
        rejects(BusinessException.class,()->rentals.changeStatus(id,RentalStatus.CANCELLED),"ACTIVE -> CANCELLED запрещён");
        rejects(BusinessException.class,()->rentals.changeStatus(id,RentalStatus.OVERDUE),"Нельзя объявить просрочку раньше срока");
        rejects(BusinessException.class,()->rentals.deleteRental(id),"Нельзя удалить активную выдачу");
        rejects(BusinessException.class,()->rentals.updateRental(rentals.getRentalById(id)),"Нельзя менять даты активной выдачи");
        RentalRequestService future=new RentalRequestService(Clock.fixed(today.plusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant(),ZoneId.systemDefault()));
        future.changeStatus(id,RentalStatus.OVERDUE);
        check(!equipment.getEquipmentById(eq).isAvailable(),"Просрочка сохраняет занятость");
        rejects(BusinessException.class,()->rentals.deleteRental(id),"Нельзя удалить просроченную выдачу");
        future.changeStatus(id,RentalStatus.COMPLETED);
        check(equipment.getEquipmentById(eq).isAvailable(),"Возврат освобождает оборудование");
        rejects(BusinessException.class,()->rentals.changeStatus(id,RentalStatus.ACTIVE),"Завершённую аренду нельзя возобновить");
        int other=create(eq);
        rentals.deleteRental(id);
        check(!equipment.getEquipmentById(eq).isAvailable(),"Удаление истории не освобождает новую бронь");
        rentals.changeStatus(other,RentalStatus.CANCELLED);
        check(equipment.getEquipmentById(eq).isAvailable(),"Отмена освобождает оборудование");
        rejects(BusinessException.class,()->rentals.changeStatus(other,RentalStatus.ACTIVE),"Отменённую заявку нельзя возобновить");
        int deleted=create(eq); rentals.deleteRental(deleted);
        check(equipment.getEquipmentById(eq).isAvailable(),"Удаление CREATED освобождает оборудование");
        int futureId=rentals.createRental(1,eq,today.plusDays(1),today.plusDays(2));
        rejects(BusinessException.class,()->rentals.changeStatus(futureId,RentalStatus.ACTIVE),"Запрет выдачи раньше начала");
        rentals.deleteRental(futureId);

        int failEq=newEquipment();
        try (Connection c=DatabaseConnection.getConnection(); Statement st=c.createStatement()) {
            st.execute("ALTER TABLE equipment ADD CONSTRAINT verification_failure CHECK(id<>"+failEq+" OR is_available)");
            int before=rentals.getTotalRentals();
            rejects(DataAccessException.class,()->create(failEq),"Ошибка второй записи в транзакции");
            check(rentals.getTotalRentals()==before && equipment.getEquipmentById(failEq).isAvailable(),"Полный откат заявки и доступности");
            st.execute("ALTER TABLE equipment DROP CONSTRAINT verification_failure");
        }
        concurrentBooking();
        check(!rentals.searchByClient("Иванов").isEmpty(),"Поиск по клиенту");
        check(!rentals.searchByEquipment("Перфоратор").isEmpty(),"Поиск по оборудованию");
        check(rentals.searchRentals("' OR 1=1 --").isEmpty(),"Поисковый ввод не меняет SQL");
        check(rentals.filterByStatus(RentalStatus.OVERDUE).stream().allMatch(x->x.getStatus()==RentalStatus.OVERDUE),"Фильтр по статусу");
        check(rentals.filterByDateRange(today,today).stream().allMatch(x->x.getStartDate().equals(today)),"Границы фильтра дат включены");
        rejects(BusinessException.class,()->rentals.filterByDateRange(today,today.minusDays(1)),"Неверный диапазон фильтра");
        List<RentalRequest> byDate=rentals.sortByDate(true);
        check(isOrdered(byDate,Comparator.comparing(RentalRequest::getStartDate)),"Сортировка по дате");
        check(isOrdered(rentals.sortByCost(false),Comparator.comparing(RentalRequest::getTotalCost).reversed()),"Сортировка по цене");
        check(rentals.getTotalRevenue().compareTo(new BigDecimal("6300"))==0,"Выручка только завершённых аренд");
        exportChecks();
        check(menu("xyz\n0\n").contains("До свидания"),"Неверный пункт не завершает меню");
        check(menu("1\n3\nabc\n0\n0\n").contains("целым числом"),"Обработка текста вместо ID");
        check(menu("3\n6\n1\n99\n0\n0\n").contains("Номер статуса"),"Проверка номера статуса");
        check(menu("5\n1\n999\n0\n0\n").contains("Нет категории"),"Проверка номера категории");
        check(menu("5\n4\n2026-02-30\n0\n0\n").contains("существующую дату"),"Строгое чтение даты");
        check(menu("1\n1\n").contains("Ввод завершён"),"EOF внутри операции завершает программу");
        check(menu("").contains("Ввод завершён"),"EOF главного меню");
        String url=System.getProperty("db.url");
        try {
            System.setProperty("db.url","jdbc:postgresql://127.0.0.1:1/unavailable");
            String output=menu("6\n0\n");
            check(output.contains("Нет соединения") && output.contains("До свидания"),"Ошибка подключения возвращает в меню");
        } finally { System.setProperty("db.url",url); }
    }
    static boolean isOrdered(List<RentalRequest> rows,Comparator<RentalRequest> cmp) {
        for(int i=1;i<rows.size();i++) if(cmp.compare(rows.get(i-1),rows.get(i))>0)return false;
        return true;
    }
    static void concurrentBooking() throws Exception {
        int eq=newEquipment();
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        Callable<Boolean> task=()->{ start.await(); try { create(eq); return true; } catch(BusinessException e) { return false; } };
        try {
            Future<Boolean> a=pool.submit(task),b=pool.submit(task); start.countDown();
            int successes=(a.get(20,TimeUnit.SECONDS)?1:0)+(b.get(20,TimeUnit.SECONDS)?1:0);
            check(successes==1,"Одновременные бронирования: ровно один успех");
            check(new RentalRequestRepository().findAll().stream().filter(r->r.getEquipmentId()==eq).count()==1,"Без двойной брони при конкуренции");
        } finally { pool.shutdownNow(); }
    }
    static String menu(String input) throws Exception {
        InputStream oldIn=System.in; PrintStream oldOut=System.out,oldErr=System.err;
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        try (PrintStream output=new PrintStream(buffer,true,StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(output); System.setErr(output);
            new Menu().start();
        } finally { System.setIn(oldIn); System.setOut(oldOut); System.setErr(oldErr); }
        return buffer.toString(StandardCharsets.UTF_8);
    }
    static void exportChecks() throws Exception {
        Path dir=Files.createTempDirectory("rental-export-check-");
        Path xlsx=dir.resolve("check.xlsx"),csv=dir.resolve("check.csv");
        List<String[]> rows=Collections.singletonList(new String[]{"Текст; \"кавычки\"\nстрока","=1+1"});
        try {
            DataExporter.exportToCSV(csv.toString(),rows,new String[]{"Поле","Текст"});
            String text=Files.readString(csv,StandardCharsets.UTF_8);
            check(text.startsWith("\uFEFF") && text.contains("\"\"кавычки\"\"") && text.contains("'=1+1"),"CSV: UTF-8, кавычки, переносы, текстовые формулы");
            DataExporter.exportToExcel(xlsx.toString(),rows,new String[]{"Поле","Текст"});
            try (InputStream in=Files.newInputStream(xlsx); XSSFWorkbook book=new XSSFWorkbook(in)) {
                check(book.getSheetAt(0).getPhysicalNumberOfRows()==2,"Excel открывается, число строк верно");
                check(book.getSheetAt(0).getRow(1).getCell(0).getStringCellValue().equals(rows.get(0)[0]),"Excel сохраняет кириллицу и переносы");
                check(book.getSheetAt(0).getRow(1).getCell(1).getCellType()==org.apache.poi.ss.usermodel.CellType.STRING,"Excel экспортирует формулы как текст");
            }
        } finally { Files.deleteIfExists(xlsx); Files.deleteIfExists(csv); Files.deleteIfExists(dir); }
    }
}
