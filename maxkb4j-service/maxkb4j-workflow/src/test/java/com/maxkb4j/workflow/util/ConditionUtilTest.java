package com.maxkb4j.workflow.util;

import com.maxkb4j.workflow.model.Condition;
import com.maxkb4j.workflow.model.IWorkflow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests: end-to-end condition evaluation on a stubbed workflow.
 */
class ConditionUtilTest {

    private final IWorkflow workflow = mock(IWorkflow.class);

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
        assertThat(ConditionUtil.assertion(workflow, "and", List.of(cond(List.of("n1", "answer"), "eq", "hello")))).isTrue();
    }

    @Test
    void assertion_andRequiresAllConditions() {
        when(workflow.getReferenceField(List.of("n1", "answer"))).thenReturn("hello");
        List<Condition> conds = List.of(
                cond(List.of("n1", "answer"), "eq", "hello"),
                cond(List.of("n1", "answer"), "eq", "hi"));
        assertThat(ConditionUtil.assertion(workflow, "and", conds)).isFalse();
    }

    @Test
    void assertion_orRequiresAnyCondition() {
        when(workflow.getReferenceField(List.of("n1", "answer"))).thenReturn("hello");
        List<Condition> conds = List.of(
                cond(List.of("n1", "answer"), "eq", "hi"),
                cond(List.of("n1", "answer"), "eq", "hello"));
        assertThat(ConditionUtil.assertion(workflow, "or", conds)).isTrue();
    }

    @Test
    void assertion_emptyOrNullConditionsAreSatisfied() {
        assertThat(ConditionUtil.assertion(workflow, "and", null)).isTrue();
        assertThat(ConditionUtil.assertion(workflow, "and", List.of())).isTrue();
    }

    @Test
    void assertion_fieldSizeNotTwoReturnsFalse() {
        when(workflow.getReferenceField(List.of("only"))).thenReturn("x");
        assertThat(ConditionUtil.assertion(workflow, "and", List.of(cond(List.of("only"), "eq", "x")))).isFalse();
    }

    @Test
    void assertion_unknownOperatorReturnsFalse() {
        when(workflow.getReferenceField(List.of("n1", "answer"))).thenReturn("hello");
        assertThat(ConditionUtil.assertion(workflow, "or", List.of(cond(List.of("n1", "answer"), "no_such_op", "hello")))).isFalse();
    }

    @Test
    void assertion_numericAndContainAndIsNullOperators() {
        when(workflow.getReferenceField(List.of("n1", "score"))).thenReturn(5);
        assertThat(ConditionUtil.assertion(workflow, "and", List.of(cond(List.of("n1", "score"), "gt", "3")))).isTrue();
        assertThat(ConditionUtil.assertion(workflow, "and", List.of(cond(List.of("n1", "score"), "gt", "9")))).isFalse();

        when(workflow.getReferenceField(List.of("n1", "text"))).thenReturn("hello world");
        assertThat(ConditionUtil.assertion(workflow, "and", List.of(cond(List.of("n1", "text"), "contain", "world")))).isTrue();

        when(workflow.getReferenceField(List.of("n1", "x"))).thenReturn(null);
        assertThat(ConditionUtil.assertion(workflow, "and", List.of(cond(List.of("n1", "x"), "is_null", "")))).isTrue();
    }
}
