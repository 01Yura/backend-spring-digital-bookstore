package online.ityura.springdigitallibrary.testinfra.generators;

import com.github.javafaker.Faker;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility generator: fills DTO fields with random (but "smart") values using Faker.
 * - static API
 * - supports nested DTOs, Optional, List/Set
 * - smart generation by field name for: email / nickname(username/login) / password / names / url / etc.
 * - enums (including Genre) are generated as random enum constants
 */
public final class RandomDtoGeneratorWithFaker {

    private static final Faker FAKER = new Faker(Locale.ENGLISH);

    private static final int DEFAULT_COLLECTION_SIZE = 2;
    private static final int MAX_DEPTH = 3;
    private static final boolean OVERWRITE_EXISTING = true;

    private RandomDtoGeneratorWithFaker() { }

    /** Create new instance (no-args ctor) and fill it */
    public static <T> T generateRandomDtoObject(Class<T> clazz) {
        T instance = newInstance(clazz);
        return generateRandomDtoObject(instance);
    }

    /** Fill existing instance */
    public static <T> T generateRandomDtoObject(T dto) {
        IdentityHashMap<Object, Boolean> visiting = new IdentityHashMap<>();
        fillObject(dto, 0, visiting);
        return dto;
    }

    // ========================= CORE FILL =========================

    private static void fillObject(Object obj, int depth, IdentityHashMap<Object, Boolean> visiting) {
        if (obj == null) return;
        if (depth > MAX_DEPTH) return;

        // protect against cycles (A->B->A)
        if (visiting.put(obj, Boolean.TRUE) != null) return;

        for (Field field : getAllFields(obj.getClass())) {
            if (shouldSkip(field)) continue;

            field.setAccessible(true);
            try {
                Object current = field.get(obj);
                if (!OVERWRITE_EXISTING && current != null) continue;

                Object generated = generateValueForField(field, depth, visiting);
                if (generated != null) field.set(obj, generated);

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot set field: " + field.getName(), e);
            }
        }
    }

    private static Object generateValueForField(Field field, int depth, IdentityHashMap<Object, Boolean> visiting) {
        Class<?> type = field.getType();
        String fieldName = field.getName();

        // Optional<T>
        if (Optional.class.isAssignableFrom(type)) {
            Type gt = field.getGenericType();
            if (gt instanceof ParameterizedType pt) {
                Class<?> argClass = toClass(pt.getActualTypeArguments()[0]);
                Object v = generateValueByClass(argClass, fieldName, depth, visiting);
                return Optional.ofNullable(v);
            }
            return Optional.empty();
        }

        // Collection<T> (List/Set)
        if (Collection.class.isAssignableFrom(type)) {
            Class<?> elementClass = String.class;
            Type gt = field.getGenericType();
            if (gt instanceof ParameterizedType pt) {
                elementClass = toClass(pt.getActualTypeArguments()[0]);
            }

            Collection<Object> col = List.class.isAssignableFrom(type) ? new ArrayList<>() : new HashSet<>();
            for (int i = 0; i < DEFAULT_COLLECTION_SIZE; i++) {
                col.add(generateValueByClass(elementClass, fieldName, depth, visiting));
            }
            return col;
        }

        // Map<K,V> (skip by default)
        if (Map.class.isAssignableFrom(type)) {
            return null;
        }

        return generateValueByClass(type, fieldName, depth, visiting);
    }

    private static Object generateValueByClass(Class<?> type,
                                               String fieldName,
                                               int depth,
                                               IdentityHashMap<Object, Boolean> visiting) {

        // ---------- String: smart by field name ----------
        if (type == String.class) {
            return smartString(fieldName);
        }

        // ---------- Numbers: smart by field name ----------
        if (type == int.class || type == Integer.class) return smartInt(fieldName);
        if (type == long.class || type == Long.class) return smartLong(fieldName);
        if (type == double.class || type == Double.class) return smartDouble(fieldName);

        if (type == BigDecimal.class) {
            // BigDecimal.ROUND_HALF_UP deprecated, use RoundingMode
            return BigDecimal.valueOf(smartDouble(fieldName)).setScale(2, java.math.RoundingMode.HALF_UP);
        }

        // ---------- boolean ----------
        if (type == boolean.class || type == Boolean.class) {
            String n = normalize(fieldName);
            if (n.contains("active") || n.contains("enabled") || n.startsWith("is")) return true;
            return FAKER.bool().bool();
        }

        // ---------- dates / time ----------
        if (type == LocalDate.class) return LocalDate.now().minusDays(FAKER.number().numberBetween(0, 3650));
        if (type == LocalDateTime.class) return LocalDateTime.now().minusHours(FAKER.number().numberBetween(0, 10_000));
        if (type == Instant.class) return Instant.now().minusSeconds(FAKER.number().numberBetween(0, 1_000_000));
        if (type == UUID.class) return UUID.randomUUID();

        // ---------- enums (Genre included) ----------
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            if (constants != null && constants.length > 0) {
                return constants[ThreadLocalRandom.current().nextInt(constants.length)];
            }
            return null;
        }

        // ---------- nested POJO / DTO ----------
        if (isPojo(type)) {
            Object nested = newInstance(type);
            fillObject(nested, depth + 1, visiting);
            return nested;
        }

        return null;
    }

    // ========================= SMART STRING =========================

    private static String smartString(String fieldName) {
        String n = normalize(fieldName);

        // EMAIL
        if (containsAny(n, "email", "e-mail", "mail")) {
            return generateEmail();
        }

        // NICKNAME / USERNAME / LOGIN
        if (containsAny(n, "nickname", "nick", "username", "login", "user_name", "user")) {
            return generateNickname(6, 16);
        }

        // PASSWORD
        if (containsAny(n, "password", "pass", "pwd")) {
            return generatePassword(8, 16);
        }

        // phone
        if (containsAny(n, "phone", "mobile", "tel", "telephone")) {
            return FAKER.phoneNumber().cellPhone();
        }

        // url / link
        if (containsAny(n, "url", "link", "website", "site")) {
            return "https://" + FAKER.internet().domainName() + "/" + FAKER.internet().slug();
        }

        // names
        if (containsAny(n, "firstname", "first_name", "givenname")) return FAKER.name().firstName();
        if (containsAny(n, "lastname", "last_name", "surname", "familyname")) return FAKER.name().lastName();
        if (equalsAny(n, "name", "fullname", "full_name")) return FAKER.name().fullName();
        if (containsAny(n, "author")) return FAKER.book().author();

        // book-ish
        if (containsAny(n, "title", "booktitle")) return FAKER.book().title();
        // genre is Enum Genre now -> handled by enum block (NOT here)
        if (containsAny(n, "isbn")) return generateIsbn13();

        // address-ish
        if (containsAny(n, "city")) return FAKER.address().city();
        if (containsAny(n, "country")) return FAKER.address().country();
        if (containsAny(n, "street")) return FAKER.address().streetAddress();
        if (containsAny(n, "zip", "postal")) return FAKER.address().zipCode();

        // description / comment
        if (containsAny(n, "description", "about", "comment", "note", "message", "text", "summary")) {
            return FAKER.lorem().sentence(8);
        }

        // string id-like
        if (endsWithAny(n, "id", "_id") || containsAny(n, "uuid")) {
            return UUID.randomUUID().toString();
        }

        return FAKER.lorem().characters(5, 12);
    }

    // ========================= SMART NUMBERS =========================

    private static int smartInt(String fieldName) {
        String n = normalize(fieldName);

        if (containsAny(n, "age")) return FAKER.number().numberBetween(18, 80);
        if (containsAny(n, "year", "publishedyear", "publishyear")) return FAKER.number().numberBetween(1900, LocalDate.now().getYear());
        if (containsAny(n, "count", "qty", "quantity", "size")) return FAKER.number().numberBetween(1, 20);
        if (containsAny(n, "page", "pages")) return FAKER.number().numberBetween(50, 1200);
        if (endsWithAny(n, "id", "_id")) return FAKER.number().numberBetween(1, 1_000_000);

        return FAKER.number().numberBetween(1, 10_000);
    }

    private static long smartLong(String fieldName) {
        String n = normalize(fieldName);

        if (endsWithAny(n, "id", "_id")) return FAKER.number().numberBetween(1L, 10_000_000L);
        if (containsAny(n, "timestamp", "epoch")) {
            return Instant.now()
                    .minusSeconds(FAKER.number().numberBetween(0, 1_000_000))
                    .getEpochSecond();
        }
        return FAKER.number().numberBetween(1L, 1_000_000L);
    }

    private static double smartDouble(String fieldName) {
        String n = normalize(fieldName);

        if (containsAny(n, "price", "cost", "amount", "sum", "total")) {
            return FAKER.number().randomDouble(2, 1, 500);
        }
        if (containsAny(n, "rating", "score")) {
            return FAKER.number().randomDouble(1, 1, 5);
        }
        return FAKER.number().randomDouble(2, 1, 10_000);
    }

    // ========================= SPECIAL (EMAIL / NICK / PASS) =========================

    /** Valid + unique enough for tests: john.smith+483920@example.com */
    private static String generateEmail() {
        String first = FAKER.name().firstName().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        String last = FAKER.name().lastName().toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (first.isBlank()) first = "user";
        if (last.isBlank()) last = "test";

        String local = (first + "." + last + "+" + randomDigits(6))
                .replaceAll("\\.+", ".")
                .replaceAll("^\\.|\\.$", "");

        // stable domain for automated tests
        return local + "@example.com";
    }

    /**
     * Username regex-like: starts with letter, then [a-z0-9_], len 6..16 by default.
     * Example: maria_smith_48
     */
    private static String generateNickname(int minLen, int maxLen) {
        int len = ThreadLocalRandom.current().nextInt(minLen, maxLen + 1);

        String base = (FAKER.name().username() + "_" + randomDigits(4))
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_")
                .replaceAll("_+", "_");

        if (base.isEmpty() || !Character.isLetter(base.charAt(0))) base = "u_" + base;

        if (base.length() < len) base = base + randomAlphaNumUnderscore(len - base.length());
        if (base.length() > len) base = base.substring(0, len);

        if (!Character.isLetter(base.charAt(0))) base = "u" + base.substring(1);

        return base;
    }

    /** 
     * Strong password matching validation: 
     * - at least one digit, one lower case, one upper case, one special character (!@#$%^&+=)
     * - no spaces
     * - minimum 8 characters
     */
    private static String generatePassword(int minLen, int maxLen) {
        // Ensure minimum length is at least 8
        int actualMinLen = Math.max(minLen, 8);
        int len = ThreadLocalRandom.current().nextInt(actualMinLen, maxLen + 1);

        // Required characters according to validation: !@#$%^&+=
        String specialChars = "!@#$%^&+=";
        
        char upper = randomChar("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        char lower = randomChar("abcdefghijklmnopqrstuvwxyz");
        char digit = randomChar("0123456789");
        char spec = randomChar(specialChars);

        // Start with required characters
        StringBuilder sb = new StringBuilder(len);
        sb.append(upper).append(lower).append(digit).append(spec);

        // Fill the rest with any allowed characters (no spaces)
        String allChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" + specialChars;
        while (sb.length() < len) {
            sb.append(randomChar(allChars));
        }

        // Shuffle to avoid predictable pattern
        List<Character> chars = new ArrayList<>(sb.length());
        for (int i = 0; i < sb.length(); i++) chars.add(sb.charAt(i));
        Collections.shuffle(chars, ThreadLocalRandom.current());

        StringBuilder out = new StringBuilder(len);
        for (char c : chars) out.append(c);

        return out.toString();
    }

    /** Simple stable ISBN-13 pattern (not checksum-valid, but looks real) */
    private static String generateIsbn13() {
        return "978-" + randomDigits(10);
    }

    // ========================= REFLECTION HELPERS =========================

    private static boolean isPojo(Class<?> type) {
        if (type.isPrimitive()) return false;
        if (type.getName().startsWith("java.")) return false;
        if (type.isInterface()) return false;
        if (Modifier.isAbstract(type.getModifiers())) return false;
        return hasNoArgsConstructor(type);
    }

    private static boolean hasNoArgsConstructor(Class<?> type) {
        try {
            type.getDeclaredConstructor();
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create instance of " + clazz.getName()
                    + ". Add no-args constructor or handle via factory.", e);
        }
    }

    private static boolean shouldSkip(Field field) {
        int m = field.getModifiers();
        return Modifier.isStatic(m) || Modifier.isFinal(m) || Modifier.isTransient(m) || field.isSynthetic();
    }

    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> cur = type;
        while (cur != null && cur != Object.class) {
            fields.addAll(Arrays.asList(cur.getDeclaredFields()));
            cur = cur.getSuperclass();
        }
        return fields;
    }

    private static Class<?> toClass(Type t) {
        if (t instanceof Class<?> c) return c;
        if (t instanceof ParameterizedType pt) return (Class<?>) pt.getRawType();
        return Object.class;
    }

    // ========================= STRING UTILS =========================

    private static String normalize(String fieldName) {
        return fieldName == null ? "" : fieldName.replace("-", "_").toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String hay, String... needles) {
        for (String n : needles) {
            if (hay.contains(normalize(n))) return true;
        }
        return false;
    }

    private static boolean equalsAny(String hay, String... candidates) {
        for (String c : candidates) {
            if (hay.equals(normalize(c))) return true;
        }
        return false;
    }

    private static boolean endsWithAny(String hay, String... suffixes) {
        for (String s : suffixes) {
            if (hay.endsWith(normalize(s))) return true;
        }
        return false;
    }

    private static String randomDigits(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(ThreadLocalRandom.current().nextInt(10));
        return sb.toString();
    }

    private static char randomChar(String alphabet) {
        return alphabet.charAt(ThreadLocalRandom.current().nextInt(alphabet.length()));
    }

    private static String randomAlphaNumUnderscore(int len) {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789_";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(randomChar(alphabet));
        return sb.toString();
    }
}
