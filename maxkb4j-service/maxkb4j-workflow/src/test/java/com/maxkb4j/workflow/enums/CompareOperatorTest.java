package com.maxkb4j.workflow.enums;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests: code lookup and comparison behavior of CompareOperator.
 */
class CompareOperatorTest {

    @Test
    void fromCode_knownOperators() {
        assertThat(CompareOperator.fromCode("eq")).isEqualTo(CompareOperator.EQ);
        assertThat(CompareOperator.fromCode("ne")).isEqualTo(CompareOperator.NE);
        assertThat(CompareOperator.fromCode("contain")).isEqualTo(CompareOperator.CONTAIN);
        assertThat(CompareOperator.fromCode("not_contain")).isEqualTo(CompareOperator.NOT_CONTAIN);
        assertThat(CompareOperator.fromCode("is_null")).isEqualTo(CompareOperator.IS_NULL);
        assertThat(CompareOperator.fromCode("len_eq")).isEqualTo(CompareOperator.LENGTH_EQ);
    }

    @Test
    void fromCode_unknownAndNullReturnNull() {
        assertThat(CompareOperator.fromCode("does_not_exist")).isNull();
        assertThat(CompareOperator.fromCode(null)).isNull();
    }

    @Test
    void everyCodeRoundTrips() {
        for (CompareOperator op : CompareOperator.values()) {
            assertThat(CompareOperator.fromCode(op.getCode())).isEqualTo(op);
        }
    }

    @Test
    void equalAndNotEqual() {
        assertThat(CompareOperator.EQ.compare("a", "a")).isTrue();
        assertThat(CompareOperator.EQ.compare("a", "b")).isFalse();
        assertThat(CompareOperator.EQ.compare(null, null)).isTrue();
        assertThat(CompareOperator.EQ.compare(null, "a")).isFalse();

        assertThat(CompareOperator.NE.compare("a", "b")).isTrue();
        assertThat(CompareOperator.NE.compare("a", "a")).isFalse();
    }

    @Test
    void containAndNotContain() {
        assertThat(CompareOperator.CONTAIN.compare("hello world", "world")).isTrue();
        assertThat(CompareOperator.CONTAIN.compare("hello world", "mars")).isFalse();
        assertThat(CompareOperator.CONTAIN.compare(null, "x")).isFalse();
        assertThat(CompareOperator.CONTAIN.compare(5, "5")).isFalse();
        assertThat(CompareOperator.CONTAIN.compare(List.of("a", "b"), "a")).isTrue();
        assertThat(CompareOperator.CONTAIN.compare(List.of("a", "b"), "c")).isFalse();

        assertThat(CompareOperator.NOT_CONTAIN.compare(null, "x")).isTrue();
        assertThat(CompareOperator.NOT_CONTAIN.compare("hello", "world")).isTrue();
        assertThat(CompareOperator.NOT_CONTAIN.compare("hello world", "world")).isFalse();
        assertThat(CompareOperator.NOT_CONTAIN.compare(List.of("a", "b"), "c")).isTrue();
        assertThat(CompareOperator.NOT_CONTAIN.compare(List.of("a", "b"), "a")).isFalse();
    }

    @Test
    void numericComparisons() {
        assertThat(CompareOperator.GT.compare(5, "3")).isTrue();
        assertThat(CompareOperator.GT.compare(3, "5")).isFalse();
        assertThat(CompareOperator.GE.compare(5, "5")).isTrue();
        assertThat(CompareOperator.GE.compare(4, "5")).isFalse();
        assertThat(CompareOperator.LT.compare(3, "5")).isTrue();
        assertThat(CompareOperator.LT.compare(5, "5")).isFalse();
        assertThat(CompareOperator.LE.compare(5, "5")).isTrue();
        assertThat(CompareOperator.LE.compare(6, "5")).isFalse();

        assertThat(CompareOperator.GT.compare(null, "5")).isFalse();
        assertThat(CompareOperator.GT.compare("abc", "5")).isFalse();
        assertThat(CompareOperator.GE.compare(List.of(1, 2, 3), "2")).isTrue();

        assertThat(CompareOperator.LT.compare(null, "5")).isFalse();
        assertThat(CompareOperator.LT.compare("abc", "5")).isFalse();
        assertThat(CompareOperator.LE.compare(null, "5")).isFalse();
        assertThat(CompareOperator.LE.compare("abc", "5")).isFalse();
        assertThat(CompareOperator.LE.compare(5, null)).isFalse();
        assertThat(CompareOperator.LE.compare(5, "abc")).isFalse();
    }

    @Test
    void lengthComparisons() {
        assertThat(CompareOperator.LENGTH_EQ.compare("hello", "5")).isTrue();
        assertThat(CompareOperator.LENGTH_EQ.compare(List.of(1, 2, 3), "3")).isTrue();
        assertThat(CompareOperator.LENGTH_GT.compare("hello", "4")).isTrue();
        assertThat(CompareOperator.LENGTH_GE.compare("hello", "5")).isTrue();
        assertThat(CompareOperator.LENGTH_LT.compare("hello", "6")).isTrue();
        assertThat(CompareOperator.LENGTH_LE.compare("hello", "5")).isTrue();

        assertThat(CompareOperator.LENGTH_EQ.compare(null, "5")).isFalse();
        assertThat(CompareOperator.LENGTH_EQ.compare("hello", "abc")).isFalse();

        assertThat(CompareOperator.LENGTH_LT.compare(null, "5")).isFalse();
        assertThat(CompareOperator.LENGTH_LE.compare(null, "5")).isFalse();
        assertThat(CompareOperator.LENGTH_LT.compare("hello", "abc")).isFalse();
        assertThat(CompareOperator.LENGTH_LE.compare("hello", "abc")).isFalse();
    }

    @Test
    void nullChecks() {
        assertThat(CompareOperator.IS_NULL.compare(null, null)).isTrue();
        assertThat(CompareOperator.IS_NULL.compare("", null)).isTrue();
        assertThat(CompareOperator.IS_NULL.compare("x", null)).isFalse();
        assertThat(CompareOperator.IS_NULL.compare(List.of(), null)).isTrue();
        assertThat(CompareOperator.IS_NULL.compare(List.of("a"), null)).isFalse();

        assertThat(CompareOperator.IS_NOT_NULL.compare(null, null)).isFalse();
        assertThat(CompareOperator.IS_NOT_NULL.compare("", null)).isFalse();
        assertThat(CompareOperator.IS_NOT_NULL.compare("x", null)).isTrue();
        assertThat(CompareOperator.IS_NOT_NULL.compare(List.of(), null)).isFalse();
        assertThat(CompareOperator.IS_NOT_NULL.compare(List.of("a"), null)).isTrue();
    }

    @Test
    void booleanChecks() {
        assertThat(CompareOperator.IS_TRUE.compare(Boolean.TRUE, null)).isTrue();
        assertThat(CompareOperator.IS_TRUE.compare(Boolean.FALSE, null)).isFalse();
        assertThat(CompareOperator.IS_TRUE.compare("true", null)).isTrue();
        assertThat(CompareOperator.IS_TRUE.compare("false", null)).isFalse();
        assertThat(CompareOperator.IS_TRUE.compare("abc", null)).isFalse();
        assertThat(CompareOperator.IS_TRUE.compare(null, null)).isFalse();
        assertThat(CompareOperator.IS_TRUE.compare(1, null)).isFalse();

        assertThat(CompareOperator.IS_NOT_TRUE.compare(null, null)).isTrue();
        assertThat(CompareOperator.IS_NOT_TRUE.compare(Boolean.FALSE, null)).isTrue();
        assertThat(CompareOperator.IS_NOT_TRUE.compare(Boolean.TRUE, null)).isFalse();
        assertThat(CompareOperator.IS_NOT_TRUE.compare("false", null)).isTrue();
        assertThat(CompareOperator.IS_NOT_TRUE.compare("true", null)).isFalse();
        assertThat(CompareOperator.IS_NOT_TRUE.compare(1, null)).isTrue();
    }
}
