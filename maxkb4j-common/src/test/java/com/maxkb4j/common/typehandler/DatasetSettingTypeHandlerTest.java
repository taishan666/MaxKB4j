package com.maxkb4j.common.typehandler;

import com.alibaba.fastjson.JSON;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
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
 * 回归测试：知识库数据集设置 TypeHandler 的 jsonb <-> KnowledgeSetting 转换流程。
 */
class DatasetSettingTypeHandlerTest {

    private final DatasetSettingTypeHandler handler = new DatasetSettingTypeHandler();

    @Test
    void getNullableResult_byName_parsesToObject() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("{\"topN\":5,\"searchMode\":\"embedding\"}");

        KnowledgeSetting s = handler.getNullableResult(rs, "col");

        assertThat(s.getTopN()).isEqualTo(5);
        assertThat(s.getSearchMode()).isEqualTo("embedding");
    }

    @Test
    void getNullableResult_byIndex_parsesToObject() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(2)).thenReturn("{\"topN\":3,\"searchMode\":\"blend\"}");

        KnowledgeSetting s = handler.getNullableResult(rs, 2);
        assertThat(s.getTopN()).isEqualTo(3);
        assertThat(s.getSearchMode()).isEqualTo("blend");
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
        when(cs.getString(1)).thenReturn("{\"topN\":7}");

        assertThat(handler.getNullableResult(cs, 1).getTopN()).isEqualTo(7);
    }

    @Test
    void setNonNullParameter_bindsJsonbPgobject() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        KnowledgeSetting param = new KnowledgeSetting();
        param.setTopN(5);
        param.setSearchMode("embedding");

        handler.setNonNullParameter(ps, 1, param, null);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ps).setObject(eq(1), captor.capture());
        Object bound = captor.getValue();
        assertThat(bound).isInstanceOf(PGobject.class);
        PGobject pg = (PGobject) bound;
        assertThat(pg.getType()).isEqualTo("jsonb");
        KnowledgeSetting parsed = JSON.parseObject(pg.getValue(), KnowledgeSetting.class);
        assertThat(parsed.getTopN()).isEqualTo(5);
        assertThat(parsed.getSearchMode()).isEqualTo("embedding");
    }

    @Test
    void toJson_roundTripsFields() {
        KnowledgeSetting s = new KnowledgeSetting();
        s.setTopN(5);
        s.setSearchMode("embedding");

        KnowledgeSetting back = JSON.parseObject(handler.toJson(s), KnowledgeSetting.class);
        assertThat(back.getTopN()).isEqualTo(5);
        assertThat(back.getSearchMode()).isEqualTo("embedding");
    }
}