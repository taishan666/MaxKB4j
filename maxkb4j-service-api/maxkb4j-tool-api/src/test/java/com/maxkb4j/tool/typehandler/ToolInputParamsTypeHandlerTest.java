package com.maxkb4j.tool.typehandler;

import com.alibaba.fastjson.JSON;
import com.maxkb4j.tool.entity.ToolInputField;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.postgresql.util.PGobject;

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
 * 回归测试：工具输入参数 TypeHandler 的 jsonb <-> List<ToolInputField> 转换流程。
 */
class ToolInputParamsTypeHandlerTest {

    private final ToolInputParamsTypeHandler handler = new ToolInputParamsTypeHandler();

    @Test
    void getNullableResult_byName_parsesToArray() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("[{\"name\":\"a\",\"type\":\"string\",\"isRequired\":true}]");

        List<ToolInputField> list = handler.getNullableResult(rs, "col");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("a");
        assertThat(list.get(0).getType()).isEqualTo("string");
        assertThat(list.get(0).getIsRequired()).isTrue();
    }

    @Test
    void getNullableResult_byIndex_parsesToArray() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(2)).thenReturn("[{\"name\":\"b\"},{\"name\":\"c\"}]");

        List<ToolInputField> list = handler.getNullableResult(rs, 2);
        assertThat(list).hasSize(2);
        assertThat(list.get(1).getName()).isEqualTo("c");
    }

    @Test
    void getNullableResult_nullAndEmptyReturnNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn(null);
        assertThat(handler.getNullableResult(rs, "col")).isNull();
        when(rs.getString("col")).thenReturn("");
        assertThat(handler.getNullableResult(rs, "col")).isNull();
    }

    @Test
    void getNullableResult_callableStatement() throws Exception {
        CallableStatement cs = mock(CallableStatement.class);
        when(cs.getString(1)).thenReturn("[{\"name\":\"d\"}]");

        assertThat(handler.getNullableResult(cs, 1).get(0).getName()).isEqualTo("d");
    }

    @Test
    void setNonNullParameter_bindsJsonbPgobject() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        ToolInputField field = new ToolInputField();
        field.setName("a");
        field.setType("string");

        handler.setNonNullParameter(ps, 1, List.of(field), null);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ps).setObject(eq(1), captor.capture());
        Object bound = captor.getValue();
        assertThat(bound).isInstanceOf(PGobject.class);
        PGobject pg = (PGobject) bound;
        assertThat(pg.getType()).isEqualTo("jsonb");
        assertThat(JSON.parseArray(pg.getValue(), ToolInputField.class).get(0).getName()).isEqualTo("a");
    }

    @Test
    void toJson_roundTripsList() {
        ToolInputField field = new ToolInputField();
        field.setName("a");
        field.setType("string");
        List<ToolInputField> list = List.of(field);

        List<ToolInputField> back = JSON.parseArray(handler.toJson(list), ToolInputField.class);
        assertThat(back).hasSize(1);
        assertThat(back.get(0).getName()).isEqualTo("a");
        assertThat(back.get(0).getType()).isEqualTo("string");
    }
}