package com.maxkb4j.workflow.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：比较算子枚举的 code 反查流程。
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
}