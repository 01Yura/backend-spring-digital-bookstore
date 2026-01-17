package online.ityura.springdigitallibrary.testinfra.database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Condition - Класс для представления условий WHERE в SQL запросах
 * 
 * Этот класс инкапсулирует условие для WHERE части SQL запроса:
 * - column - имя колонки
 * - value - значение для сравнения
 * - operator - оператор сравнения (=, !=, LIKE, и т.д.)
 * 
 * Предоставляет статические методы-фабрики для создания типичных условий:
 * - equalTo() - равенство (=)
 * - notEqualTo() - неравенство (!=)
 * - like() - поиск по шаблону (LIKE)
 * 
 * Использование:
 * <pre>
 * Condition condition = Condition.equalTo("username", "john_doe");
 * // Создает условие: username = 'john_doe'
 * </pre>
 * 
 * @author Generated
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Condition {
    
    /** Имя колонки в таблице */
    private String column;
    
    /** Значение для сравнения */
    private Object value;
    
    /** SQL оператор сравнения (=, !=, LIKE, и т.д.) */
    private String operator;

    /**
     * Создает условие равенства (=)
     * 
     * Создает условие для проверки равенства значения колонки заданному значению.
     * Используется для точного поиска по значению.
     * 
     * @param column имя колонки
     * @param value значение для сравнения
     * @return новое условие с оператором "="
     * 
     * @example
     * Condition.equalTo("username", "john_doe")
     * // Создает условие: username = 'john_doe'
     */
    public static Condition equalTo(String column, Object value) {
        return new Condition(column, value, "=");
    }

    /**
     * Создает условие неравенства (!=)
     * 
     * Создает условие для проверки неравенства значения колонки заданному значению.
     * Используется для исключения записей с определенным значением.
     * 
     * @param column имя колонки
     * @param value значение для сравнения
     * @return новое условие с оператором "!="
     * 
     * @example
     * Condition.notEqualTo("role", "ADMIN")
     * // Создает условие: role != 'ADMIN'
     */
    public static Condition notEqualTo(String column, Object value) {
        return new Condition(column, value, "!=");
    }

    /**
     * Создает условие поиска по шаблону (LIKE)
     * 
     * Создает условие для поиска по шаблону с использованием SQL оператора LIKE.
     * Поддерживает wildcard символы:
     * - % - любое количество символов
     * - _ - один символ
     * 
     * @param column имя колонки
     * @param value шаблон для поиска
     * @return новое условие с оператором "LIKE"
     * 
     * @example
     * Condition.like("username", "john%")
     * // Создает условие: username LIKE 'john%'
     * // Найдет: john_doe, johnny, johnson, и т.д.
     */
    public static Condition like(String column, String value) {
        return new Condition(column, value, "LIKE");
    }
}
