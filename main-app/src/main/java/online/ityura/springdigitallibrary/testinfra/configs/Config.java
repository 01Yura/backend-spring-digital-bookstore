package online.ityura.springdigitallibrary.testinfra.configs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {
    //    Это единственный экземпляр класса Config, создаётся при загрузке класса (eager initialization).
    private static final Config INSTANCE = new Config();
    private final Properties properties = new Properties();
    private static Properties applicationProperties;

    /*
    Приватный конструктор, чтобы нельзя было создать new Config() извне → это часть паттерна Singleton
    Загружает файл config.properties из resources в Properties
    Если файла нет → бросает RuntimeException
    Таким образом, конфиг загружается один раз при старте и кэшируется в памяти.
    */
    private Config() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (inputStream == null) {
                throw new RuntimeException("config.properties is not found in resources");
            }
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Fail to load config.properties", e);
        }
    }


    public static String getProperty(String key) {
//        ПРИОРИТЕТ 1 - это системное свойство apiBaseUrl = ...
//        используется для кубера и продакшн
        String systemValue = System.getProperty(key);
        if (systemValue != null) return systemValue;

//        ПРИОРИТЕТ 2 - это переменная окружения apiBaseUrl - APIBASEURL
//        используется для докера
//        admin.username -> ADMIN_USERNAME
        String envKey = key.toUpperCase().replace(".", "_");
        String envValue = System.getenv(envKey);
        if (envValue != null) return envValue;

//        ПРИОРИТЕТ 3 - это config.properties
//        используется для локального запуска прямо из IDE
        return INSTANCE.properties.getProperty(key);
    }

    /**
     * Получает значение свойства из application.properties с учетом приоритетов
     * 
     * Приоритет чтения:
     * 1. Системные свойства (System.getProperty)
     * 2. Переменные окружения (System.getenv) - преобразует точку в подчеркивание и в верхний регистр
     * 3. application.properties файл (с обработкой синтаксиса ${VAR:default})
     * 
     * @param key ключ свойства
     * @return значение свойства
     * @throws RuntimeException если свойство не найдено
     */
    public static String getApplicationProperty(String key) {
        // ПРИОРИТЕТ 1 - системное свойство
        String systemValue = System.getProperty(key);
        if (systemValue != null) return systemValue;

        // ПРИОРИТЕТ 2 - переменная окружения
        // spring.datasource.url -> SPRING_DATASOURCE_URL
        String envKey = key.toUpperCase().replace(".", "_");
        String envValue = System.getenv(envKey);
        if (envValue != null) return envValue;

        // ПРИОРИТЕТ 3 - application.properties
        Properties props = loadApplicationProperties();
        String propValue = props.getProperty(key);
        if (propValue != null) {
            // Обрабатываем синтаксис Spring ${VAR:default}
            return resolvePropertyValue(propValue);
        }

        throw new RuntimeException("Property '" + key + "' not found in system properties, environment variables, or application.properties");
    }

    /**
     * Загружает application.properties файл из ресурсов
     * 
     * Использует кэширование для оптимизации - файл загружается один раз.
     * 
     * @return Properties объект с настройками из application.properties
     */
    private static Properties loadApplicationProperties() {
        if (applicationProperties != null) {
            return applicationProperties;
        }
        
        synchronized (Config.class) {
            if (applicationProperties != null) {
                return applicationProperties;
            }
            
            Properties props = new Properties();
            try (InputStream inputStream = Config.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {
                if (inputStream == null) {
                    throw new RuntimeException("application.properties is not found in resources");
                }
                props.load(inputStream);
                applicationProperties = props;
            } catch (IOException e) {
                throw new RuntimeException("Failed to load application.properties", e);
            }
            return applicationProperties;
        }
    }

    /**
     * Обрабатывает значение свойства, заменяя синтаксис ${VAR:default} на реальные значения
     * 
     * Поддерживает синтаксис:
     * - ${VAR} - заменяется на переменную окружения VAR или системное свойство
     * - ${VAR:default} - заменяется на переменную окружения VAR, если она есть, иначе на default
     * 
     * @param value исходное значение из properties файла
     * @return обработанное значение
     */
    private static String resolvePropertyValue(String value) {
        if (value == null) return null;
        
        // Регулярное выражение для поиска ${VAR:default} или ${VAR}
        Pattern pattern = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?\\}");
        Matcher matcher = pattern.matcher(value);
        
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String defaultValue = matcher.group(2);
            
            // Преобразуем имя переменной: spring.datasource.url -> SPRING_DATASOURCE_URL
            String envKey = varName.toUpperCase().replace(".", "_");
            
            // Ищем в системных свойствах
            String resolvedValue = System.getProperty(varName);
            
            // Если не найдено, ищем в переменных окружения
            if (resolvedValue == null) {
                resolvedValue = System.getenv(envKey);
            }
            
            // Если не найдено, используем значение по умолчанию
            if (resolvedValue == null) {
                resolvedValue = defaultValue != null ? defaultValue : "";
            }
            
            // Экранируем специальные символы для замены
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolvedValue));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}

