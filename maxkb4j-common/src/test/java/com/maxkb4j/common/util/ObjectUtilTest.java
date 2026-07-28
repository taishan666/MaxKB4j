package com.maxkb4j.common.util;

import com.maxkb4j.common.enums.ChatSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：空安全相等判断与简单类型识别（含各基本类型数组分支）。
 */
class ObjectUtilTest {

    @Test
    void nullSafeEquals_bothNullAreEqual() {
        assertThat(ObjectUtil.nullSafeEquals(null, null)).isTrue();
    }

    @Test
    void nullSafeEquals_oneNullIsNotEqual() {
        assertThat(ObjectUtil.nullSafeEquals(null, "x")).isFalse();
        assertThat(ObjectUtil.nullSafeEquals("x", null)).isFalse();
    }

    @Test
    void nullSafeEquals_sameReferenceIsEqual() {
        Object o = new Object();
        assertThat(ObjectUtil.nullSafeEquals(o, o)).isTrue();
    }

    @Test
    void nullSafeEquals_equalAndUnequalStrings() {
        assertThat(ObjectUtil.nullSafeEquals("abc", "abc")).isTrue();
        assertThat(ObjectUtil.nullSafeEquals("abc", "abd")).isFalse();
    }

    @Test
    void nullSafeEquals_objectArrays() {
        assertThat(ObjectUtil.nullSafeEquals(new Object[]{"a", "b"}, new Object[]{"a", "b"})).isTrue();
        assertThat(ObjectUtil.nullSafeEquals(new Object[]{"a"}, new Object[]{"a", "b"})).isFalse();
    }

    @Test
    void nullSafeEquals_primitiveArrays() {
        assertThat(ObjectUtil.nullSafeEquals(new int[]{1, 2, 3}, new int[]{1, 2, 3})).isTrue();
        assertThat(ObjectUtil.nullSafeEquals(new int[]{1, 2}, new int[]{1, 3})).isFalse();
        assertThat(ObjectUtil.nullSafeEquals(new boolean[]{true, false}, new boolean[]{true, false})).isTrue();
        assertThat(ObjectUtil.nullSafeEquals(new long[]{1L, 2L}, new long[]{1L, 2L})).isTrue();
        assertThat(ObjectUtil.nullSafeEquals(new byte[]{1, 2}, new byte[]{1, 2})).isTrue();
    }

    @Test
    void nullSafeEquals_mixedArrayTypesAreNotEqual() {
        assertThat(ObjectUtil.nullSafeEquals(new int[]{1, 2}, new Object[]{1, 2})).isFalse();
        assertThat(ObjectUtil.nullSafeEquals(new int[]{1, 2}, "string")).isFalse();
    }

    @Test
    void isSimpleType_nullIsFalse() {
        assertThat(ObjectUtil.isSimpleType(null)).isFalse();
    }

    @Test
    void isSimpleType_wrappersAndStringAreTrue() {
        assertThat(ObjectUtil.isSimpleType("text")).isTrue();
        assertThat(ObjectUtil.isSimpleType(Integer.valueOf(1))).isTrue();
        assertThat(ObjectUtil.isSimpleType(Boolean.TRUE)).isTrue();
        assertThat(ObjectUtil.isSimpleType(Double.valueOf(1.5))).isTrue();
        assertThat(ObjectUtil.isSimpleType(Long.valueOf(2L))).isTrue();
    }

    @Test
    void isSimpleType_enumIsTrue() {
        assertThat(ObjectUtil.isSimpleType(ChatSource.ONLINE)).isTrue();
    }

    @Test
    void isSimpleType_collectionAndCustomObjectAreFalse() {
        assertThat(ObjectUtil.isSimpleType(List.of("a"))).isFalse();
        assertThat(ObjectUtil.isSimpleType(new int[]{1})).isFalse();
        assertThat(ObjectUtil.isSimpleType(new Object())).isFalse();
    }
}