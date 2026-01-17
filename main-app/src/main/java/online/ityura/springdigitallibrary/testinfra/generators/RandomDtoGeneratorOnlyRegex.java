package online.ityura.springdigitallibrary.testinfra.generators;

import com.github.curiousoddman.rgxgen.RgxGen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Random;

/**
 * Генератор случайных объектов для DTO классов.
 * Поддерживает генерацию значений на основе аннотации @RegexGen,
 * автоматический выбор значений из Enum и рекурсивную генерацию вложенных объектов.
 */
public class RandomDtoGeneratorOnlyRegex {
    
    private static final Random random = new Random();
    
    /**
     * Генерирует случайный объект указанного класса.
     * 
     * @param clazz класс для генерации
     * @param <T> тип возвращаемого объекта
     * @return объект с заполненными полями на основе аннотаций
     * @throws RuntimeException если не удалось создать объект
     */
    public static <T> T generateRandomModel(Class<T> clazz) {
        try {
            return generateRandomModelInternal(clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate random model for class: " + clazz.getName(), e);
        }
    }
    
    private static <T> T generateRandomModelInternal(Class<T> clazz) throws Exception {
        // Создаем экземпляр класса
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        T instance = constructor.newInstance();
        
        // Получаем все поля класса (включая наследованные)
        Field[] fields = getAllFields(clazz);
        
        for (Field field : fields) {
            // Пропускаем статические и финальные поля
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            
            field.setAccessible(true);
            
            // Проверяем наличие аннотации @RegexGen
            RegexGen regexGenAnnotation = field.getAnnotation(RegexGen.class);
            
            if (regexGenAnnotation != null) {
                // Генерируем значение на основе regexp
                String regexp = regexGenAnnotation.value();
                Object generatedValue = generateValueByRegexp(regexp, field.getType());
                field.set(instance, generatedValue);
            } else {
                // Если аннотации нет, пытаемся сгенерировать значение по типу поля
                Object generatedValue = generateValueByType(field.getType());
                if (generatedValue != null) {
                    field.set(instance, generatedValue);
                }
            }
        }
        
        return instance;
    }
    
    /**
     * Получает все поля класса, включая поля из родительских классов.
     */
    private static Field[] getAllFields(Class<?> clazz) {
        java.util.List<Field> fields = new java.util.ArrayList<>();
        Class<?> currentClass = clazz;
        
        while (currentClass != null && currentClass != Object.class) {
            fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }
        
        return fields.toArray(new Field[0]);
    }
    
    /**
     * Генерирует значение на основе регулярного выражения.
     */
    private static Object generateValueByRegexp(String regexp, Class<?> fieldType) {
        try {
            RgxGen rgxGen = new RgxGen(regexp);
            String generatedString = rgxGen.generate();
            
            // Преобразуем строку в нужный тип
            return convertStringToType(generatedString, fieldType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate value by regexp: " + regexp, e);
        }
    }
    
    /**
     * Генерирует значение на основе типа поля (без аннотации).
     */
    private static Object generateValueByType(Class<?> fieldType) {
        // Если это Enum, выбираем случайное значение
        if (fieldType.isEnum()) {
            Object[] enumConstants = fieldType.getEnumConstants();
            if (enumConstants != null && enumConstants.length > 0) {
                return enumConstants[random.nextInt(enumConstants.length)];
            }
            return null;
        }
        
        // Если это вложенный класс (не примитив, не String, не Enum, не стандартные типы)
        if (isCustomClass(fieldType)) {
            try {
                return generateRandomModelInternal(fieldType);
            } catch (Exception e) {
                // Если не удалось создать вложенный объект, возвращаем null
                return null;
            }
        }
        
        // Для остальных типов без аннотации не генерируем значение
        return null;
    }
    
    /**
     * Проверяет, является ли класс пользовательским (не стандартным типом Java).
     */
    private static boolean isCustomClass(Class<?> clazz) {
        // Пропускаем примитивы и их обертки
        if (clazz.isPrimitive() || 
            clazz == String.class ||
            clazz == Integer.class ||
            clazz == Long.class ||
            clazz == Double.class ||
            clazz == Float.class ||
            clazz == Boolean.class ||
            clazz == Byte.class ||
            clazz == Short.class ||
            clazz == Character.class ||
            clazz == java.util.List.class ||
            clazz == java.util.Set.class ||
            clazz == java.util.Map.class ||
            clazz == java.time.LocalDateTime.class ||
            clazz == java.time.LocalDate.class ||
            clazz == java.time.LocalTime.class ||
            clazz == java.util.Date.class) {
            return false;
        }
        
        // Пропускаем массивы
        if (clazz.isArray()) {
            return false;
        }
        
        // Пропускаем Enum
        if (clazz.isEnum()) {
            return false;
        }
        
        // Проверяем, что это не стандартный класс Java (начинается с java.)
        String packageName = clazz.getPackage() != null ? clazz.getPackage().getName() : "";
        if (packageName.startsWith("java.") || packageName.startsWith("javax.")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Преобразует строку в нужный тип поля.
     */
    private static Object convertStringToType(String value, Class<?> targetType) {
        if (targetType == String.class) {
            return value;
        }
        
        if (targetType == Integer.class || targetType == int.class) {
            try {
                // Пытаемся извлечь число из строки
                String numberStr = value.replaceAll("[^0-9-]", "");
                if (!numberStr.isEmpty()) {
                    return Integer.parseInt(numberStr);
                }
                return random.nextInt(1000);
            } catch (NumberFormatException e) {
                return random.nextInt(1000);
            }
        }
        
        if (targetType == Long.class || targetType == long.class) {
            try {
                String numberStr = value.replaceAll("[^0-9-]", "");
                if (!numberStr.isEmpty()) {
                    return Long.parseLong(numberStr);
                }
                return random.nextLong();
            } catch (NumberFormatException e) {
                return random.nextLong();
            }
        }
        
        if (targetType == Double.class || targetType == double.class) {
            try {
                String numberStr = value.replaceAll("[^0-9.-]", "");
                if (!numberStr.isEmpty()) {
                    return Double.parseDouble(numberStr);
                }
                return random.nextDouble();
            } catch (NumberFormatException e) {
                return random.nextDouble();
            }
        }
        
        if (targetType == Float.class || targetType == float.class) {
            try {
                String numberStr = value.replaceAll("[^0-9.-]", "");
                if (!numberStr.isEmpty()) {
                    return Float.parseFloat(numberStr);
                }
                return random.nextFloat();
            } catch (NumberFormatException e) {
                return random.nextFloat();
            }
        }
        
        if (targetType == Boolean.class || targetType == boolean.class) {
            return value.toLowerCase().contains("true") || random.nextBoolean();
        }
        
        if (targetType == Byte.class || targetType == byte.class) {
            return (byte) random.nextInt(256);
        }
        
        if (targetType == Short.class || targetType == short.class) {
            return (short) random.nextInt(Short.MAX_VALUE);
        }
        
        if (targetType == Character.class || targetType == char.class) {
            return value.isEmpty() ? (char) (random.nextInt(26) + 'a') : value.charAt(0);
        }
        
        // Если это Enum, пытаемся найти значение по строке
        if (targetType.isEnum()) {
            Object[] enumConstants = targetType.getEnumConstants();
            if (enumConstants != null && enumConstants.length > 0) {
                // Пытаемся найти по имени
                for (Object enumConstant : enumConstants) {
                    if (enumConstant.toString().equalsIgnoreCase(value) || 
                        enumConstant.toString().equals(value)) {
                        return enumConstant;
                    }
                }
                // Если не нашли, возвращаем случайное значение
                return enumConstants[random.nextInt(enumConstants.length)];
            }
        }
        
        // По умолчанию возвращаем строку
        return value;
    }
}

