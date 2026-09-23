package ru.mirea.project;
import ru.mirea.project.view.Menu;
public class Main {
    public static void main(String[] args) {
        System.out.println("Запуск системы проката оборудования...");
        try { new Menu().start(); }
        catch (Exception e) {
            System.err.println("Ошибка запуска: " + e.getMessage());
            System.exit(1);
        }
    }
}
