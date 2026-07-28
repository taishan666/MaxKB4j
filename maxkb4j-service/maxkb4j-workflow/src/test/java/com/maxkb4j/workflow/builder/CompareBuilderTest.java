package com.maxkb4j.workflow.builder;

import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.compare.impl.EqualCompare;
import com.maxkb4j.workflow.compare.impl.GTCompare;
import com.maxkb4j.workflow.enums.CompareOperator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 回归测试：比较算子注册与查找流程。
 */
class CompareBuilderTest {

    @Test
    void getHandler_unknownOperatorThrows() {
        CompareBuilder builder = new CompareBuilder();
        assertThatThrownBy(() -> builder.getHandler("nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No compare handler found for operator");
    }

    @Test
    void registerAndLookupHandler() {
        CompareBuilder builder = new CompareBuilder();
        EqualCompare eq = new EqualCompare();
        assertThat(builder.registerHandler(new CompareOperator[]{CompareOperator.EQ}, eq)).isFalse();

        Compare handler = builder.getHandler("eq");
        assertThat(handler).isSameAs(eq);
        assertThat(handler.compare("a", "a")).isTrue();
    }

    @Test
    void registerReplacesExistingHandler() {
        CompareBuilder builder = new CompareBuilder();
        EqualCompare first = new EqualCompare();
        GTCompare second = new GTCompare();
        builder.registerHandler(new CompareOperator[]{CompareOperator.EQ}, first);
        assertThat(builder.registerHandler(new CompareOperator[]{CompareOperator.EQ}, second)).isTrue();
        assertThat(builder.getHandler("eq")).isSameAs(second);
    }

    @Test
    void registerNullArgsThrows() {
        CompareBuilder builder = new CompareBuilder();
        assertThatThrownBy(() -> builder.registerHandler(null, new EqualCompare()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.registerHandler(new CompareOperator[]{CompareOperator.EQ}, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registerSkipsNullOperatorWithoutReplacing() {
        CompareBuilder builder = new CompareBuilder();
        assertThat(builder.registerHandler(new CompareOperator[]{null}, new EqualCompare())).isFalse();
        assertThatThrownBy(() -> builder.getHandler("eq"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}