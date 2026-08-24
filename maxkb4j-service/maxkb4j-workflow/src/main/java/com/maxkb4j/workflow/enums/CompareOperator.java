package com.maxkb4j.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration of comparison operators used in workflow conditions.
 *
 * <p>Each constant carries its comparison logic directly, eliminating the need
 * for separate Compare implementation classes. Use {@link #compare(Object, Object)}
 * to execute the comparison.</p>
 */
@Getter
@AllArgsConstructor
public enum CompareOperator {

    EQ("eq", "等于", Objects::equals),

    NE("ne", "不等于", (source, target) ->
            !Objects.equals(source, target)),

    GT("gt", "大于", (source, target) -> {
        Integer cmp = compareNumeric(source, target);
        return cmp != null && cmp > 0;
    }),

    GE("ge", "大于等于", (source, target) -> {
        Integer cmp = compareNumeric(source, target);
        return cmp != null && cmp >= 0;
    }),

    LT("lt", "小于", (source, target) -> {
        Integer cmp = compareNumeric(source, target);
        return cmp != null && cmp < 0;
    }),

    LE("le", "小于等于", (source, target) -> {
        Integer cmp = compareNumeric(source, target);
        return cmp != null && cmp <= 0;
    }),

    CONTAIN("contain", "包含", (source, target) -> switch (source) {
        case List<?> list -> list.contains(target);
        case String str when target instanceof String targetStr -> str.contains(targetStr);
        case null, default -> false;
    }),

    NOT_CONTAIN("not_contain", "不包含", (source, target) -> switch (source) {
        case List<?> list -> !list.contains(target);
        case String str when target instanceof String targetStr -> !str.contains(targetStr);
        case null, default -> true;
    }),

    IS_NULL("is_null", "为空", (source, target) -> {
        if (source instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return source == null || source.equals("");
    }),

    IS_NOT_NULL("is_not_null", "不为空", (source, target) -> {
        if (source instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return source != null && !source.equals("");
    }),

    IS_TRUE("is_true", "为真", (source, target) -> {
        if (source instanceof Boolean bool) {
            return bool;
        }
        if (source instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return false;
    }),

    IS_NOT_TRUE("is_not_true", "不为真", (source, target) -> {
        if (source instanceof Boolean bool) {
            return !bool;
        }
        if (source instanceof String str) {
            return !Boolean.parseBoolean(str);
        }
        return true;
    }),

    LENGTH_EQ("len_eq", "长度等于", (source, target) -> {
        Integer cmp = compareLength(source, target);
        return cmp != null && cmp == 0;
    }),

    LENGTH_GT("len_gt", "长度大于", (source, target) -> {
        Integer cmp = compareLength(source, target);
        return cmp != null && cmp > 0;
    }),

    LENGTH_GE("len_ge", "长度大于等于", (source, target) -> {
        Integer cmp = compareLength(source, target);
        return cmp != null && cmp >= 0;
    }),

    LENGTH_LT("len_lt", "长度小于", (source, target) -> {
        Integer cmp = compareLength(source, target);
        return cmp != null && cmp < 0;
    }),

    LENGTH_LE("len_le", "长度小于等于", (source, target) -> {
        Integer cmp = compareLength(source, target);
        return cmp != null && cmp <= 0;
    });

    private final String code;
    private final String description;
    private final BiPredicate<Object, Object> predicate;

    /**
     * Static map for O(1) code-based lookup
     */
    private static final Map<String, CompareOperator> CODE_MAP;

    static {
        CODE_MAP = Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(CompareOperator::getCode, Function.identity()));
    }

    /**
     * Get CompareOperator by code with O(1) lookup
     *
     * @param code the operator code (e.g., "eq", "contain")
     * @return CompareOperator or null if not found
     */
    public static CompareOperator fromCode(String code) {
        return code == null ? null : CODE_MAP.get(code);
    }

    /**
     * Execute the comparison.
     *
     * @param sourceValue the source value to compare
     * @param targetValue the target value to compare against
     * @return the comparison result
     */
    public boolean compare(Object sourceValue, Object targetValue) {
        return predicate.test(sourceValue, targetValue);
    }

    /**
     * Compare two numeric values.
     * Supports Number, Collection (size), and String parsing.
     *
     * @return comparison result (-1, 0, 1), or null if not comparable
     */
    private static Integer compareNumeric(Object source, Object target) {
        if (source == null || target == null) {
            return null;
        }
        try {
            double sourceNum = toDouble(source);
            double targetNum = Double.parseDouble(target.toString());
            return Double.compare(sourceNum, targetNum);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Compare lengths of two values.
     * Supports List (size) and String (length).
     *
     * @return comparison result (-1, 0, 1), or null if not comparable
     */
    private static Integer compareLength(Object source, Object target) {
        if (source == null || target == null) {
            return null;
        }
        try {
            int sourceLen = getLength(source);
            int targetLen = Integer.parseInt(target.toString());
            return Integer.compare(sourceLen, targetLen);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof Collection<?> collection) {
            return collection.size();
        }
        return Double.parseDouble(value.toString());
    }

    private static int getLength(Object value) {
        if (value instanceof List<?> list) {
            return list.size();
        }
        if (value instanceof String str) {
            return str.length();
        }
        return 0;
    }
}
