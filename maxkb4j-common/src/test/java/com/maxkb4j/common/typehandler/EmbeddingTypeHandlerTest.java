package com.maxkb4j.common.typehandler;

import com.pgvector.PGvector;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归测试：向量 Embedding TypeHandler 的字符串 <-> List<Float> 转换流程。
 */
class EmbeddingTypeHandlerTest {

    private final EmbeddingTypeHandler handler = new EmbeddingTypeHandler();

    @Test
    void getNullableResult_byName_parsesBracketedList() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("[1.0,2.0,3.0]");

        List<Float> result = handler.getNullableResult(rs, "col");

        assertThat(result).containsExactly(1.0f, 2.0f, 3.0f);
    }

    @Test
    void getNullableResult_parsesWithoutBrackets() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("4.0,5.0");

        assertThat(handler.getNullableResult(rs, "col")).containsExactly(4.0f, 5.0f);
    }

    @Test
    void getNullableResult_trimsWhitespace() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("[1.0, 2.0 , 3.0]");

        assertThat(handler.getNullableResult(rs, "col")).containsExactly(1.0f, 2.0f, 3.0f);
    }

    @Test
    void getNullableResult_emptyBracketsReturnsEmpty() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("[]");

        assertThat(handler.getNullableResult(rs, "col")).isEmpty();
    }

    @Test
    void getNullableResult_nullAndEmptyReturnEmpty() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn(null);
        assertThat(handler.getNullableResult(rs, "col")).isEmpty();
        when(rs.getString("col")).thenReturn("");
        assertThat(handler.getNullableResult(rs, "col")).isEmpty();
    }

    @Test
    void getNullableResult_byIndexAndCallable() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("[1.0,2.0]");
        assertThat(handler.getNullableResult(rs, 1)).containsExactly(1.0f, 2.0f);

        CallableStatement cs = mock(CallableStatement.class);
        when(cs.getString(2)).thenReturn("[3.0]");
        assertThat(handler.getNullableResult(cs, 2)).containsExactly(3.0f);
    }

    @Test
    void setNonNullParameter_bindsPgvector() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);

        handler.setNonNullParameter(ps, 1, List.of(1.0f, 2.0f), null);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ps).setObject(eq(1), captor.capture());
        Object bound = captor.getValue();
        assertThat(bound).isInstanceOf(PGvector.class);
        assertThat(((PGvector) bound).toArray()).containsExactly(1.0f, 2.0f);
    }
}