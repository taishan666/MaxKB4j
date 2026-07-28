package com.maxkb4j.common.typehandler;

import org.junit.jupiter.api.Test;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归测试：字符串列表 TypeHandler 的数组 <-> List<String> 转换流程。
 * 通过 Mockito 打桩 JDBC 对象，聚焦于读取路径的转换逻辑。
 */
class StringListTypeHandlerTest {

    private final StringListTypeHandler handler = new StringListTypeHandler();

    /** 构造一个返回给定原始值的 Array mock（先完成自身打桩，避免嵌套打桩）。 */
    private Array arrayWith(Object raw) throws Exception {
        Array array = mock(Array.class);
        when(array.getArray()).thenReturn(raw);
        return array;
    }

    @Test
    void getNullableResult_byName_convertsArrayToList() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        Array array = arrayWith(new Object[]{"a", "b", null});
        when(rs.getArray("col")).thenReturn(array);

        List<String> result = handler.getNullableResult(rs, "col");

        assertThat(result).containsExactly("a", "b", null);
    }

    @Test
    void getNullableResult_byIndex_convertsArrayToList() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        Array array = arrayWith(new Object[]{"x", "y"});
        when(rs.getArray(2)).thenReturn(array);

        List<String> result = handler.getNullableResult(rs, 2);

        assertThat(result).containsExactly("x", "y");
    }

    @Test
    void getNullableResult_nullArrayReturnsNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getArray("col")).thenReturn(null);

        assertThat(handler.getNullableResult(rs, "col")).isNull();
    }

    @Test
    void getNullableResult_nullRawReturnsNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        Array array = arrayWith(null);
        when(rs.getArray("col")).thenReturn(array);

        assertThat(handler.getNullableResult(rs, "col")).isNull();
    }

    @Test
    void getNullableResult_nonArrayRawBecomesSingleElementList() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        Array array = arrayWith("scalar");
        when(rs.getArray("col")).thenReturn(array);

        assertThat(handler.getNullableResult(rs, "col")).containsExactly("scalar");
    }

    @Test
    void getNullableResult_callableStatement() throws Exception {
        CallableStatement cs = mock(CallableStatement.class);
        Array array = arrayWith(new Object[]{"a"});
        when(cs.getArray(1)).thenReturn(array);

        assertThat(handler.getNullableResult(cs, 1)).containsExactly("a");
    }

    @Test
    void setNonNullParameter_bindsArrayFromList() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        Connection conn = mock(Connection.class);
        Array array = mock(Array.class);
        when(ps.getConnection()).thenReturn(conn);
        when(conn.createArrayOf(eq("VARCHAR"), any(Object[].class))).thenReturn(array);

        handler.setNonNullParameter(ps, 1, List.of("a", "b"), null);

        verify(ps).setArray(1, array);
    }

    @Test
    void setNonNullParameter_nullListBindsNull() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setNonNullParameter(ps, 1, null, null);

        verify(ps).setArray(1, null);
    }
}