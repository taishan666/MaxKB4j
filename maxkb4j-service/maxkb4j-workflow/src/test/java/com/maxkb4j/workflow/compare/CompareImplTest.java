package com.maxkb4j.workflow.compare;

import com.maxkb4j.workflow.compare.impl.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：工作流条件分支各比较算子。
 * 覆盖 eq/ne/contain/not_contain、数值 gt/ge/lt/le、长度 len_*、is_null/is_not_null/is_true/is_not_true。
 */
class CompareImplTest {

    @Test
    void equalCompare() {
        EqualCompare cmp = new EqualCompare();
        assertThat(cmp.compare("a", "a")).isTrue();
        assertThat(cmp.compare("a", "b")).isFalse();
        assertThat(cmp.compare(null, null)).isTrue();
        assertThat(cmp.compare(null, "a")).isFalse();
    }

    @Test
    void notEqualCompare() {
        NotEqualCompare cmp = new NotEqualCompare();
        assertThat(cmp.compare("a", "b")).isTrue();
        assertThat(cmp.compare("a", "a")).isFalse();
    }

    @Test
    void containCompare_string() {
        ContainCompare cmp = new ContainCompare();
        assertThat(cmp.compare("hello world", "world")).isTrue();
        assertThat(cmp.compare("hello world", "mars")).isFalse();
        assertThat(cmp.compare(null, "x")).isFalse();
        assertThat(cmp.compare(5, "5")).isFalse(); // 非字符串/集合
    }

    @Test
    void containCompare_list() {
        ContainCompare cmp = new ContainCompare();
        assertThat(cmp.compare(List.of("a", "b"), "a")).isTrue();
        assertThat(cmp.compare(List.of("a", "b"), "c")).isFalse();
    }

    @Test
    void notContainCompare() {
        NotContainCompare cmp = new NotContainCompare();
        assertThat(cmp.compare(null, "x")).isTrue();
        assertThat(cmp.compare("hello", "world")).isTrue();
        assertThat(cmp.compare("hello world", "world")).isFalse();
        assertThat(cmp.compare(List.of("a", "b"), "c")).isTrue();
        assertThat(cmp.compare(List.of("a", "b"), "a")).isFalse();
    }

    @Test
    void numericComparisons() {
        GTCompare gt = new GTCompare();
        GECompare ge = new GECompare();
        LTCompare lt = new LTCompare();
        LECompare le = new LECompare();

        assertThat(gt.compare(5, "3")).isTrue();
        assertThat(gt.compare(3, "5")).isFalse();
        assertThat(ge.compare(5, "5")).isTrue();
        assertThat(ge.compare(4, "5")).isFalse();
        assertThat(lt.compare(3, "5")).isTrue();
        assertThat(lt.compare(5, "5")).isFalse();
        assertThat(le.compare(5, "5")).isTrue();
        assertThat(le.compare(6, "5")).isFalse();

        // null 安全
        assertThat(gt.compare(null, "5")).isFalse();
        // 非数字回退 false
        assertThat(gt.compare("abc", "5")).isFalse();
        // 集合按 size 作为数值
        assertThat(ge.compare(List.of(1, 2, 3), "2")).isTrue();
    }

    @Test
    void lengthComparisons() {
        LengthEqualCompare eq = new LengthEqualCompare();
        LengthGTCompare gt = new LengthGTCompare();
        LengthGECompare ge = new LengthGECompare();
        LengthLTCompare lt = new LengthLTCompare();
        LengthLECompare le = new LengthLECompare();

        assertThat(eq.compare("hello", "5")).isTrue();
        assertThat(eq.compare(List.of(1, 2, 3), "3")).isTrue();
        assertThat(gt.compare("hello", "4")).isTrue();
        assertThat(ge.compare("hello", "5")).isTrue();
        assertThat(lt.compare("hello", "6")).isTrue();
        assertThat(le.compare("hello", "5")).isTrue();

        assertThat(eq.compare(null, "5")).isFalse();
        assertThat(eq.compare("hello", "abc")).isFalse();
    }

    @Test
    void isNullCompare() {
        IsNullCompare cmp = new IsNullCompare();
        assertThat(cmp.compare(null, null)).isTrue();
        assertThat(cmp.compare("", null)).isTrue();
        assertThat(cmp.compare("x", null)).isFalse();
        assertThat(cmp.compare(List.of(), null)).isTrue();
        assertThat(cmp.compare(List.of("a"), null)).isFalse();
    }

    @Test
    void isNotNullCompare() {
        IsNotNullCompare cmp = new IsNotNullCompare();
        assertThat(cmp.compare(null, null)).isFalse();
        assertThat(cmp.compare("", null)).isFalse();
        assertThat(cmp.compare("x", null)).isTrue();
        assertThat(cmp.compare(List.of(), null)).isFalse();
        assertThat(cmp.compare(List.of("a"), null)).isTrue();
    }

    @Test
    void isTrueCompare() {
        IsTrueCompare cmp = new IsTrueCompare();
        assertThat(cmp.compare(Boolean.TRUE, null)).isTrue();
        assertThat(cmp.compare(Boolean.FALSE, null)).isFalse();
        assertThat(cmp.compare("true", null)).isTrue();
        assertThat(cmp.compare("false", null)).isFalse();
        assertThat(cmp.compare("abc", null)).isFalse();
        assertThat(cmp.compare(null, null)).isFalse();
        assertThat(cmp.compare(1, null)).isFalse();
    }

    @Test
    void isNotTrueCompare() {
        IsNotTrueCompare cmp = new IsNotTrueCompare();
        assertThat(cmp.compare(null, null)).isTrue();
        assertThat(cmp.compare(Boolean.FALSE, null)).isTrue();
        assertThat(cmp.compare(Boolean.TRUE, null)).isFalse();
        assertThat(cmp.compare("false", null)).isTrue();
        assertThat(cmp.compare("true", null)).isFalse();
        assertThat(cmp.compare(1, null)).isTrue();
    }
}