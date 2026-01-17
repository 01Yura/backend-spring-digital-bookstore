package online.ityura.springdigitallibrary.testinfra.comparators;

import org.assertj.core.api.SoftAssertions;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class UniversalComparator {

    private static final String PROPS_PATH = "/dto-comparison.properties";

    // cache: "LeftSimple|RightSimple" -> parsed rules
    private static final Map<String, ComparisonConfig> CACHE = new ConcurrentHashMap<>();
    private static volatile Properties PROPS;

    private UniversalComparator() {}

    /**
     * Сравнивает два объекта по правилам из /resources/dto-comparison.properties.
     * Использует SoftAssertions: собирает все расхождения и валит тест только в конце.
     */
    public static void match(Object left, Object right) {
        Objects.requireNonNull(left, "left object is null");
        Objects.requireNonNull(right, "right object is null");

        ComparisonConfig cfg = loadConfig(left.getClass(), right.getClass());
        SoftAssertions softly = new SoftAssertions();

        for (Rule rule : cfg.rules) {
            if (rule.kind == RuleKind.FIELD_TO_FIELD) {
                Object lVal = readPath(left, rule.leftPath);
                Object rVal = rule.isConstant ? rule.constantValue : readPath(right, rule.rightPath);

                String label = cfg.leftClass + "." + rule.leftPath + " " + rule.op + " " + (rule.isConstant ? "\"" + rule.constantValue + "\"" : cfg.rightClass + "." + rule.rightPath)
                        + " | left=" + shortVal(lVal) + ", right=" + shortVal(rVal);

                assertFieldToField(softly, rule.op, lVal, rVal, label);

            } else if (rule.kind == RuleKind.NOT_NULL) {
                Object lVal = readPath(left, rule.leftPath);

                String label = cfg.leftClass + "." + rule.leftPath + " @NOT_NULL | value=" + shortVal(lVal);
                softly.assertThat(lVal).as(label).isNotNull();

            } else if (rule.kind == RuleKind.TYPE) {
                Object lVal = readPath(left, rule.leftPath);

                String label = cfg.leftClass + "." + rule.leftPath + " @TYPE=" + rule.typeName
                        + " | value=" + shortVal(lVal);

                Class<?> expected = typeFromName(rule.typeName);
                softly.assertThat(lVal).as(label).isNotNull();
                if (expected != null) {
                    softly.assertThat(lVal).as(label).isInstanceOf(expected);
                } else {
                    softly.fail(label + " | Unknown type: " + rule.typeName);
                }
            } else if (rule.kind == RuleKind.NOT_NULL_RIGHT) {
                Object rVal = readPath(right, rule.rightPath);

                String label = cfg.rightClass + "." + rule.rightPath + " @NOT_NULL | value=" + shortVal(rVal);
                softly.assertThat(rVal).as(label).isNotNull();

            } else if (rule.kind == RuleKind.TYPE_RIGHT) {
                Object rVal = readPath(right, rule.rightPath);

                String label = cfg.rightClass + "." + rule.rightPath + " @TYPE=" + rule.typeName
                        + " | value=" + shortVal(rVal);

                Class<?> expected = typeFromName(rule.typeName);
                softly.assertThat(rVal).as(label).isNotNull();
                if (expected != null) {
                    softly.assertThat(rVal).as(label).isInstanceOf(expected);
                } else {
                    softly.fail(label + " | Unknown type: " + rule.typeName);
                }
            } else if (rule.kind == RuleKind.FIELD_TO_FIELD_RIGHT) {
                Object rVal = readPath(right, rule.rightPath);

                String label = cfg.rightClass + "." + rule.rightPath + " " + rule.op + " \"" + rule.constantValue + "\""
                        + " | value=" + shortVal(rVal);

                assertFieldToField(softly, rule.op, rVal, rule.constantValue, label);
            }
        }

        softly.assertAll();
    }

    // ========================= Parsing & Config =========================

    private static ComparisonConfig loadConfig(Class<?> left, Class<?> right) {
        Properties props = loadPropsOnce();

        String leftSimple = left.getSimpleName();
        String rightSimple = right.getSimpleName();
        String leftFqcn = left.getName();
        String rightFqcn = right.getName();

        // try matching key by left class name (simple or fqcn)
        String raw = props.getProperty(leftSimple);
        if (raw == null) raw = props.getProperty(leftFqcn);

        if (raw == null) {
            throw new IllegalStateException("No comparison config for left class: " + leftSimple + " (or " + leftFqcn + ")");
        }

        // value: RightClass:rules...
        ParsedLine parsed = parseLineValue(raw);

        // right class check: allow either simple or fqcn
        boolean okRight = parsed.rightClass.equals(rightSimple) || parsed.rightClass.equals(rightFqcn);
        if (!okRight) {
            throw new IllegalStateException("Config mismatch. Left=" + leftSimple
                    + " expects Right=" + parsed.rightClass + ", but actual right is " + rightSimple);
        }

        String cacheKey = leftSimple + "|" + rightSimple;
        return CACHE.computeIfAbsent(cacheKey, k -> new ComparisonConfig(
                leftSimple, rightSimple, parseRules(parsed.rulesPart)
        ));
    }

    private static Properties loadPropsOnce() {
        if (PROPS != null) return PROPS;
        synchronized (UniversalComparator.class) {
            if (PROPS != null) return PROPS;

            Properties p = new Properties();
            try (InputStream is = UniversalComparator.class.getResourceAsStream(PROPS_PATH)) {
                if (is == null) {
                    throw new IllegalStateException("Cannot find " + PROPS_PATH + " on classpath");
                }
                p.load(is);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load " + PROPS_PATH, e);
            }
            PROPS = p;
            return p;
        }
    }

    private static ParsedLine parseLineValue(String value) {
        // RightClass:rules...
        int colon = value.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("Invalid config line (no ':'): " + value);
        }
        String rightClass = value.substring(0, colon).trim();
        String rulesPart = value.substring(colon + 1).trim();
        if (rightClass.isEmpty() || rulesPart.isEmpty()) {
            throw new IllegalArgumentException("Invalid config line (empty rightClass or rules): " + value);
        }
        return new ParsedLine(rightClass, rulesPart);
    }

    private static List<Rule> parseRules(String rulesPart) {
        // split by comma (simple format; keep it strict)
        String[] tokens = rulesPart.split("\\s*,\\s*");
        List<Rule> rules = new ArrayList<>();

        for (String token : tokens) {
            if (token.isBlank()) continue;

            // One-sided checks:
            // leftPath@NOT_NULL
            // leftPath@TYPE=String
            // @RIGHT:rightPath@NOT_NULL  (check field in right object)
            // @RIGHT:rightPath@TYPE=String
            // @RIGHT:rightPath="constant" (compare right field with constant)
            if (token.startsWith("@RIGHT:")) {
                String rest = token.substring("@RIGHT:".length()).trim();
                
                // Check for @NOT_NULL or @TYPE
                if (rest.contains("@")) {
                    String[] a = rest.split("@", 2);
                    String rightPath = a[0].trim();
                    String right = a[1].trim();

                    if (right.equalsIgnoreCase("NOT_NULL")) {
                        rules.add(Rule.notNullRight(rightPath));
                        continue;
                    }
                    if (right.toUpperCase(Locale.ROOT).startsWith("TYPE=")) {
                        String typeName = right.substring("TYPE=".length()).trim();
                        rules.add(Rule.typeRight(rightPath, typeName));
                        continue;
                    }
                }
                
                // Check for constant comparison: @RIGHT:rightPath="constant"
                int eq = rest.indexOf('=');
                if (eq >= 0) {
                    String rightPath = rest.substring(0, eq).trim();
                    String rightValue = rest.substring(eq + 1).trim();
                    
                    if (rightValue.startsWith("\"") && rightValue.endsWith("\"") && rightValue.length() >= 2) {
                        Object constantValue = rightValue.substring(1, rightValue.length() - 1);
                        rules.add(Rule.fieldToFieldRight(rightPath, Op.EQ, constantValue));
                        continue;
                    }
                }
                
                throw new IllegalArgumentException("Invalid @RIGHT: rule: " + token);
            }
            
            if (token.contains("@")) {
                String[] a = token.split("@", 2);
                String leftPath = a[0].trim();
                String right = a[1].trim();

                if (right.equalsIgnoreCase("NOT_NULL")) {
                    rules.add(Rule.notNull(leftPath));
                    continue;
                }
                if (right.toUpperCase(Locale.ROOT).startsWith("TYPE=")) {
                    String typeName = right.substring("TYPE=".length()).trim();
                    rules.add(Rule.type(leftPath, typeName));
                    continue;
                }

                throw new IllegalArgumentException("Unknown one-sided rule: " + token);
            }

            // Field-to-field:
            // leftPath=rightPath                (default EQ)
            // leftPath~OP=rightPath
            // OP is optional, default EQ
            int eq = token.indexOf('=');
            if (eq < 0) {
                throw new IllegalArgumentException("Invalid rule (no '='): " + token);
            }

            String leftPart = token.substring(0, eq).trim();
            String rightPath = token.substring(eq + 1).trim();

            if (rightPath.isEmpty() || leftPart.isEmpty()) {
                throw new IllegalArgumentException("Invalid rule (empty side): " + token);
            }

            String leftPath;
            Op op = Op.EQ;

            int tilde = leftPart.indexOf('~');
            if (tilde >= 0) {
                leftPath = leftPart.substring(0, tilde).trim();
                String opStr = leftPart.substring(tilde + 1).trim();
                op = Op.from(opStr);
            } else {
                leftPath = leftPart;
            }

            // Check if rightPath is a constant (string in quotes)
            boolean isConstant = false;
            Object constantValue = null;
            if (rightPath.startsWith("\"") && rightPath.endsWith("\"") && rightPath.length() >= 2) {
                isConstant = true;
                constantValue = rightPath.substring(1, rightPath.length() - 1);
            }

            rules.add(Rule.fieldToField(leftPath, rightPath, op, isConstant, constantValue));
        }

        return rules;
    }

    // ========================= Assertions =========================

    private static void assertFieldToField(SoftAssertions softly, Op op, Object left, Object right, String label) {
        // Normalize values for comparison: if one is enum and other is string, convert enum to string
        Object normalizedLeft = normalizeForComparison(left, right);
        Object normalizedRight = normalizeForComparison(right, left);
        
        switch (op) {
            case EQ -> {
                softly.assertThat(normalizedLeft).as(label).isEqualTo(normalizedRight);
            }
            case NE -> {
                softly.assertThat(normalizedLeft).as(label).isNotEqualTo(normalizedRight);
            }
            case CONTAINS -> {
                String l = str(normalizedLeft);
                String r = str(normalizedRight);
                softly.assertThat(l).as(label).contains(r);
            }
            case IN -> {
                String l = str(normalizedLeft);
                String r = str(normalizedRight);
                softly.assertThat(r).as(label).contains(l);
            }
            case GT, GE, LT, LE -> {
                BigDecimal l = toBigDecimal(normalizedLeft);
                BigDecimal r = toBigDecimal(normalizedRight);
                softly.assertThat(l).as(label + " | left is not numeric").isNotNull();
                softly.assertThat(r).as(label + " | right is not numeric").isNotNull();
                if (l == null || r == null) return;

                int cmp = l.compareTo(r);
                switch (op) {
                    case GT -> softly.assertThat(cmp).as(label).isGreaterThan(0);
                    case GE -> softly.assertThat(cmp).as(label).isGreaterThanOrEqualTo(0);
                    case LT -> softly.assertThat(cmp).as(label).isLessThan(0);
                    case LE -> softly.assertThat(cmp).as(label).isLessThanOrEqualTo(0);
                    default -> {}
                }
            }
        }
    }

    // ========================= Path reading (nested via dot) =========================

    private static Object readPath(Object root, String path) {
        if (root == null) return null;
        if (path == null || path.isBlank()) return null;

        Object cur = root;
        String[] parts = path.split("\\.");
        for (String p : parts) {
            if (cur == null) return null;
            cur = readSingle(cur, p);
        }
        return cur;
    }

    private static Object readSingle(Object obj, String name) {
        Class<?> c = obj.getClass();

        // try getter: getX(), isX()
        String cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        Method m = findMethod(c, "get" + cap);
        if (m == null) m = findMethod(c, "is" + cap);
        if (m != null) {
            try {
                m.setAccessible(true);
                return m.invoke(obj);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke getter " + m.getName() + " on " + c.getName(), e);
            }
        }

        // try field
        Field f = findField(c, name);
        if (f != null) {
            try {
                f.setAccessible(true);
                return f.get(obj);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read field " + name + " on " + c.getName(), e);
            }
        }

        throw new IllegalArgumentException("Cannot resolve path part '" + name + "' on class " + c.getName());
    }

    private static Method findMethod(Class<?> c, String name) {
        Class<?> cur = c;
        while (cur != null && cur != Object.class) {
            try {
                return cur.getDeclaredMethod(name);
            } catch (NoSuchMethodException ignored) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }

    private static Field findField(Class<?> c, String name) {
        Class<?> cur = c;
        while (cur != null && cur != Object.class) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        return null;
    }

    // ========================= Helpers =========================

    /**
     * Нормализует значение для сравнения: если один объект - enum, а другой - строка,
     * преобразует enum в строку через .name() для корректного сравнения.
     */
    private static Object normalizeForComparison(Object value, Object other) {
        if (value == null) return null;
        
        // Если value - enum, а other - строка, преобразуем enum в строку
        if (value.getClass().isEnum() && other instanceof String) {
            return ((Enum<?>) value).name();
        }
        
        // Если value - строка, а other - enum, оставляем строку как есть
        // (enum будет преобразован в отдельном вызове normalizeForComparison(other, value))
        return value;
    }

    private static String str(Object o) {
        return o == null ? "null" : String.valueOf(o);
    }

    private static String shortVal(Object o) {
        if (o == null) return "null";
        String s = String.valueOf(o);
        if (s.length() > 80) return s.substring(0, 77) + "...";
        return s;
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return new BigDecimal(String.valueOf(n));
        if (o instanceof String s) {
            try {
                return new BigDecimal(s.trim());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static Class<?> typeFromName(String t) {
        if (t == null) return null;
        String n = t.trim().toLowerCase(Locale.ROOT);
        return switch (n) {
            case "string" -> String.class;
            case "integer", "int" -> Integer.class;
            case "long" -> Long.class;
            case "double" -> Double.class;
            case "bigdecimal" -> BigDecimal.class;
            case "boolean" -> Boolean.class;
            case "uuid" -> UUID.class;
            case "localdate" -> LocalDate.class;
            case "localdatetime" -> LocalDateTime.class;
            case "instant" -> Instant.class;
            case "offsetdatetime" -> OffsetDateTime.class;
            case "zoneddatetime" -> ZonedDateTime.class;
            default -> null;
        };
    }

    // ========================= Model =========================

    private enum RuleKind { FIELD_TO_FIELD, NOT_NULL, TYPE, NOT_NULL_RIGHT, TYPE_RIGHT, FIELD_TO_FIELD_RIGHT }

    private enum Op {
        EQ, NE, CONTAINS, IN, GT, GE, LT, LE;

        static Op from(String s) {
            if (s == null) return EQ;
            String n = s.trim().toUpperCase(Locale.ROOT);
            return switch (n) {
                case "EQ", "=", "EQUALS" -> EQ;
                case "NE", "!=", "NOT_EQUALS" -> NE;
                case "CONTAINS" -> CONTAINS;
                case "IN" -> IN;
                case "GT", ">" -> GT;
                case "GE", ">=" -> GE;
                case "LT", "<" -> LT;
                case "LE", "<=" -> LE;
                default -> throw new IllegalArgumentException("Unknown OP: " + s);
            };
        }
    }

    private static final class Rule {
        final RuleKind kind;

        final String leftPath;
        final String rightPath;   // only for FIELD_TO_FIELD
        final Op op;              // only for FIELD_TO_FIELD

        final String typeName;    // only for TYPE

        final boolean isConstant; // only for FIELD_TO_FIELD
        final Object constantValue; // only for FIELD_TO_FIELD when isConstant is true

        private Rule(RuleKind kind, String leftPath, String rightPath, Op op, String typeName, boolean isConstant, Object constantValue) {
            this.kind = kind;
            this.leftPath = leftPath;
            this.rightPath = rightPath;
            this.op = op;
            this.typeName = typeName;
            this.isConstant = isConstant;
            this.constantValue = constantValue;
        }

        static Rule fieldToField(String leftPath, String rightPath, Op op, boolean isConstant, Object constantValue) {
            return new Rule(RuleKind.FIELD_TO_FIELD, leftPath, rightPath, op, null, isConstant, constantValue);
        }

        static Rule notNull(String leftPath) {
            return new Rule(RuleKind.NOT_NULL, leftPath, null, null, null, false, null);
        }

        static Rule type(String leftPath, String typeName) {
            return new Rule(RuleKind.TYPE, leftPath, null, null, typeName, false, null);
        }

        static Rule notNullRight(String rightPath) {
            return new Rule(RuleKind.NOT_NULL_RIGHT, null, rightPath, null, null, false, null);
        }

        static Rule typeRight(String rightPath, String typeName) {
            return new Rule(RuleKind.TYPE_RIGHT, null, rightPath, null, typeName, false, null);
        }

        static Rule fieldToFieldRight(String rightPath, Op op, Object constantValue) {
            return new Rule(RuleKind.FIELD_TO_FIELD_RIGHT, null, rightPath, op, null, true, constantValue);
        }
    }

    private static final class ComparisonConfig {
        final String leftClass;
        final String rightClass;
        final List<Rule> rules;

        ComparisonConfig(String leftClass, String rightClass, List<Rule> rules) {
            this.leftClass = leftClass;
            this.rightClass = rightClass;
            this.rules = rules;
        }
    }

    private static final class ParsedLine {
        final String rightClass;
        final String rulesPart;

        ParsedLine(String rightClass, String rulesPart) {
            this.rightClass = rightClass;
            this.rulesPart = rulesPart;
        }
    }
}
