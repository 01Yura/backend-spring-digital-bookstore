package online.ityura.springdigitallibrary.testinfra.generators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для указания регулярного выражения для генерации случайных значений полей.
 * Используется в сочетании с RandomDtoGeneratorOnlyRegex для автоматической генерации тестовых данных.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegexGen {
    String value();
}

