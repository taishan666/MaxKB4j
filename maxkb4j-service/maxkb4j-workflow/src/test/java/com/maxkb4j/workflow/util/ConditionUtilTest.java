package com.maxkb4j.workflow.util;

import com.maxkb4j.workflow.builder.CompareBuilder;
import com.maxkb4j.workflow.compare.impl.*;
import com.maxkb4j.workflow.enums.CompareOperator;
import com.maxkb4j.workflow.model.Condition;
import com.maxkb4j.workflow.model.IWorkflow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 回归测试：工作流条件分支断言流程（and/or 组合、字段长度校验、未知算子降级）。
 * 使用真实 CompareBuilder + 全量比较算子，配合 Workflow 打桩，验证 ConditionUtil 端到端逻辑。
 */
class ConditionUtilTest {

    private final CompareBuilder builder = newFullCompareBuilder();
    private final ConditionUtil util = new ConditionUtil(builder);
    private final IWorkflow workflow = mock(IWorkflow.class);

    private static CompareBuilder newFullCompareBuilder() {
        CompareBuilder b = new CompareBuilder();
        b.registerHandler(new CompareOperator[]{CompareOperator.EQ}, new EqualCompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.NE}, new NotEqualCompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.CONTAIN}, new ContainCompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.NOT_CONTAIN}, new NotContainCompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.GT}, new GTCompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.GE}, new GECompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.LT}, new LTCompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.LE}, new LECompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.IS_NULL}, new IsNullCompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.IS_NOT_NULL}, new IsNotNullCompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.IS_TRUE}, new IsTrueCompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.IS_NOT_TRUE}, new IsNotTrueCompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.LENGTH_EQ}, new LengthEqualCompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.LENGTH_GT}, new LengthGTCompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.LENGTH_GE}, new LengthGECompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.LENGTH_LT}, new LengthLTCompare());
        b.registerHandler(new CompareOperator[]{CompareOperator.LENGTH_LE}, new LengthLECompare());
        return b;
    }

    private Condition cond(List<String> field, String compare, String value) {
        Condition c = new Condition();
        c.setField(field);
        c.setCompare(compare);
        c.setValue(value);
        return c;
    }

    @Test
    void assertion_andSingleConditionTrue() {
        when(workflow.getReferenceField(List.of("n1", "answer"))).thenReturn("hello");
        assertThat(util.assertion(workflow, "and", List.of(cond(List.of("n1", "answer"), "eq", "hello")))).isTrue();
    }

    @Test
    void assertion_andRequiresAllConditions() {
        when(workflow.getReferenceField(List.of("n1", "answer"))).thenReturn("hello");
        List<Condition> conds = List.of(
                cond(List.of("n1", "answer"), "eq", "hello"), // true
                cond(List.of("n1", "answer"), "eq", "hi"));    // false
        assertThat(util.assertion(workflow, "and", conds)).isFalse();
    }

    @Test
    void assertion_orRequiresAnyCondition() {
        when(workflow.getReferenceField(List.of("n1", "answer"))).thenReturn("hello");
        List<Condition> conds = List.of(
                cond(List.of("n1", "answer"), "eq", "hi"),     // false
                cond(List.of("n1", "answer"), "eq", "hello"));  // true
        assertThat(util.assertion(workflow, "or", conds)).isTrue();
    }

    @Test
    void assertion_emptyOrNullConditionsAreSatisfied() {
        assertThat(util.assertion(workflow, "and", null)).isTrue();
        assertThat(util.assertion(workflow, "and", List.of())).isTrue();
    }

    @Test
    void assertion_fieldSizeNotTwoReturnsFalse() {
        when(workflow.getReferenceField(List.of("only"))).thenReturn("x");
        assertThat(util.assertion(workflow, "and", List.of(cond(List.of("only"), "eq", "x")))).isFalse();
    }

    @Test
    void assertion_unknownOperatorReturnsFalse() {
        when(workflow.getReferenceField(List.of("n1", "answer"))).thenReturn("hello");
        assertThat(util.assertion(workflow, "or", List.of(cond(List.of("n1", "answer"), "no_such_op", "hello")))).isFalse();
    }

    @Test
    void assertion_numericAndContainAndIsNullOperators() {
        when(workflow.getReferenceField(List.of("n1", "score"))).thenReturn(5);
        assertThat(util.assertion(workflow, "and", List.of(cond(List.of("n1", "score"), "gt", "3")))).isTrue();
        assertThat(util.assertion(workflow, "and", List.of(cond(List.of("n1", "score"), "gt", "9")))).isFalse();

        when(workflow.getReferenceField(List.of("n1", "text"))).thenReturn("hello world");
        assertThat(util.assertion(workflow, "and", List.of(cond(List.of("n1", "text"), "contain", "world")))).isTrue();

        when(workflow.getReferenceField(List.of("n1", "x"))).thenReturn(null);
        assertThat(util.assertion(workflow, "and", List.of(cond(List.of("n1", "x"), "is_null", "")))).isTrue();
    }
}