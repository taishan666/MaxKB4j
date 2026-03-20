package com.maxkb4j.workflow.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumeration of comparison operators used in workflow conditions.
 * Replaces magic strings with type-safe constants.
 */
@Getter
@AllArgsConstructor
public enum CompareOperator {

    EQ("eq", "等于"),
    NE("ne", "不等于"),
    GT("gt", "大于"),
    GE("ge", "大于等于"),
    LT("lt", "小于"),
    LE("le", "小于等于"),
    CONTAIN("contain", "包含"),
    NOT_CONTAIN("not_contain", "不包含"),
    IS_NULL("is_null", "为空"),
    IS_NOT_NULL("is_not_null", "不为空"),
    IS_TRUE("is_true", "为真"),
    IS_NOT_TRUE("is_not_true", "不为真"),
    LENGTH_EQ("len_eq", "长度等于"),
    LENGTH_GT("len_gt", "长度大于"),
    LENGTH_GE("len_ge", "长度大于等于"),
    LENGTH_LT("len_lt", "长度小于"),
    LENGTH_LE("len_le", "长度小于等于");

    private final String code;
    private final String description;

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
        return CODE_MAP.get(code);
    }
}