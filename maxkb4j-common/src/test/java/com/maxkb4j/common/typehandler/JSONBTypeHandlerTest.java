package com.maxkb4j.common.typehandler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回归测试：JSONB TypeHandler 的 JSON 字符串 <-> com.alibaba.fastjson.JSON 转换流程。
 */
class JSONBTypeHandlerTest {

    private final JSONBTypeHandler handler = new JSONBTypeHandler();

    @Test
    void getNullableResult_byName_parsesJson() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("{\"a\":1}");

        JSON result = handler.getNullableResult(rs, "col");

        assertThat(result).isInstanceOf(JSONObject.class);
        assertThat(((JSONObject) result).getIntValue("a")).isEqualTo(1);
    }

    @Test
    void getNullableResult_byIndex_parsesJson() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(2)).thenReturn("{\"a\":2}");

        JSON result = handler.getNullableResult(rs, 2);

        assertThat(((JSONObject) result).getIntValue("a")).isEqualTo(2);
    }

    @Test
    void getNullableResult_nullReturnsNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn(null);

        assertThat(handler.getNullableResult(rs, "col")).isNull();
    }

    @Test
    void getNullableResult_emptyReturnsNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("");

        assertThat(handler.getNullableResult(rs, "col")).isNull();
    }

    @Test
    void getNullableResult_callableStatement() throws Exception {
        CallableStatement cs = mock(CallableStatement.class);
        when(cs.getString(1)).thenReturn("{\"a\":3}");

        assertThat(((JSONObject) handler.getNullableResult(cs, 1)).getIntValue("a")).isEqualTo(3);
    }

    @Test
    void setNonNullParameter_bindsJsonbPgobject() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        JSONObject param = JSON.parseObject("{\"a\":1}");

        handler.setNonNullParameter(ps, 1, param, null);

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(ps).setObject(eq(1), captor.capture());
        Object bound = captor.getValue();
        assertThat(bound).isInstanceOf(PGobject.class);
        PGobject pg = (PGobject) bound;
        assertThat(pg.getType()).isEqualTo("jsonb");
        assertThat(JSON.parseObject(pg.getValue()).getIntValue("a")).isEqualTo(1);
    }

    @Test
    void toJson_serializesCompactJson() {
        JSONObject json = JSON.parseObject("{\"a\":1,\"b\":2}");
        String out = handler.toJson(json);

        assertThat(JSON.parseObject(out).getIntValue("a")).isEqualTo(1);
        assertThat(JSON.parseObject(out).getIntValue("b")).isEqualTo(2);
        // 紧凑输出，无换行
        assertThat(out).doesNotContain("\n");
    }
}