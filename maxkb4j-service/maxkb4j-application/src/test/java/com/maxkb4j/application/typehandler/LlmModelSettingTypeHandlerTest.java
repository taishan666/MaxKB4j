package com.maxkb4j.application.typehandler;

import com.alibaba.fastjson.JSON;
import com.maxkb4j.application.dto.LlmModelSetting;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
 * 回归测试：LLM 模型设置 TypeHandler 的 jsonb <-> LlmModelSetting 转换流程。
 */
class LlmModelSettingTypeHandlerTest {

    private final LlmModelSettingTypeHandler handler = new LlmModelSettingTypeHandler();

    @Test
    void getNullableResult_byName_parsesToObject() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("{\"system\":\"sys\",\"prompt\":\"p\"}");

        LlmModelSetting s = handler.getNullableResult(rs, "col");

        assertThat(s.getSystem()).isEqualTo("sys");
        assertThat(s.getPrompt()).isEqualTo("p");
    }

    @Test
    void getNullableResult_byIndex_parsesToObject() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(2)).thenReturn("{\"system\":\"idx\"}");

        assertThat(handler.getNullableResult(rs, 2).getSystem()).isEqualTo("idx");
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
        when(cs.getString(1)).thenReturn("{\"system\":\"cs\"}");

        assertThat(handler.getNullableResult(cs, 1).getSystem()).isEqualTo("cs");
    }

    @Test
    void setNonNullParameter_bindsJsonbPgobject() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        LlmModelSetting param = new LlmModelSetting();
        param.setSystem("sys");
        param.setPrompt("p");

        handler.setNonNullParameter(ps, 1, param, null);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ps).setObject(eq(1), captor.capture());
        Object bound = captor.getValue();
        assertThat(bound).isInstanceOf(PGobject.class);
        PGobject pg = (PGobject) bound;
        assertThat(pg.getType()).isEqualTo("jsonb");
        assertThat(JSON.parseObject(pg.getValue(), LlmModelSetting.class).getSystem()).isEqualTo("sys");
    }

    @Test
    void toJson_roundTripsFieldsAndWritesNulls() {
        LlmModelSetting s = new LlmModelSetting();
        s.setSystem("sys");
        s.setPrompt("p");

        LlmModelSetting back = JSON.parseObject(handler.toJson(s), LlmModelSetting.class);
        assertThat(back.getSystem()).isEqualTo("sys");
        assertThat(back.getPrompt()).isEqualTo("p");

        // WriteMapNullValue：未设置字段以 null 输出
        assertThat(handler.toJson(new LlmModelSetting())).contains("\"system\":\"\"");  // WriteNullStringAsEmpty: null String -> ""
    }
}