package online.ityura.springdigitallibrary.testinfra.generators;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.concurrent.ThreadLocalRandom;

public abstract class RandomDataGenerator {

    /**
     * Генерирует nickname с длиной в диапазоне [minLen..maxLen] (включительно),
     * с выбранным набором символов и регистром.
     */
    public static String generateNickname(int minLen, int maxLen, CharMode charMode, CaseMode caseMode) {
        if (minLen < 0 || maxLen < minLen) {
            throw new IllegalArgumentException("Invalid length range: minLen=" + minLen + ", maxLen=" + maxLen);
        }
        int len = ThreadLocalRandom.current().nextInt(minLen, maxLen + 1);

        boolean letters = (charMode == CharMode.LETTERS || charMode == CharMode.ALPHANUMERIC);
        boolean numbers = (charMode == CharMode.DIGITS || charMode == CharMode.ALPHANUMERIC);

        // RandomStringUtils.random(count, letters, numbers)
        String raw = RandomStringUtils.random(len, letters, numbers);

        return switch (caseMode) {
            case LOWER -> raw.toLowerCase();
            case UPPER -> raw.toUpperCase();
            case MIXED -> raw; // как сгенерировалось, так и оставляем
        };
    }

    /**
     * Генерирует nickname с фиксированной длиной
     */
    public static String generateNickname(int len, CharMode charMode, CaseMode caseMode) {
        return generateNickname(len, len, charMode, caseMode);
    }

    public enum CharMode {LETTERS, DIGITS, ALPHANUMERIC}

    public enum CaseMode {LOWER, UPPER, MIXED}
}

