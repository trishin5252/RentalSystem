package ru.mirea.project.service;
import ru.mirea.project.exception.BusinessException;
import java.math.BigDecimal;
final class Validation {
    private Validation() { }
    static String text(String value,String label,int max) throws BusinessException {
        if (value == null || value.isBlank()) throw new BusinessException(label + " не может быть пустым");
        String result = value.trim();
        if (result.length() > max) throw new BusinessException(label + ": максимум " + max + " символов");
        return result;
    }
    static void money(BigDecimal value,String label) throws BusinessException {
        if (value == null || value.signum() <= 0 || value.compareTo(new BigDecimal("9999999999.99")) > 0
                || value.stripTrailingZeros().scale() > 2)
            throw new BusinessException(label + ": положительное число, максимум 9999999999.99 и два знака после точки");
    }
}
