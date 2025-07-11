package com.tarzan.maxkb4j.core.handler.type;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.tarzan.maxkb4j.module.application.domian.entity.LlmModelSetting;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class LlmModelSettingTypeHandler extends BaseTypeHandler<LlmModelSetting> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LlmModelSetting parameter, JdbcType jdbcType) throws SQLException {
        if(null != parameter){
            PGobject pGobject = new PGobject();
            pGobject.setType("jsonb");
            pGobject.setValue(toJson(parameter));
            ps.setObject(i, pGobject);
        }
    }

    @Override
    public LlmModelSetting getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convert(value);
    }

    @Override
    public LlmModelSetting getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convert(value);
    }

    @Override
    public LlmModelSetting getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convert(value);
    }

    private LlmModelSetting convert(String value){
        if(notNull(value)){
            return  JSON.parseObject(value,LlmModelSetting.class);
        }
        return null;
    }

    private boolean notNull(String value){
        return (null != value && !value.isEmpty());
    }

    public String toJson(LlmModelSetting obj) {
        return JSON.toJSONString(obj, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullListAsEmpty, SerializerFeature.WriteNullStringAsEmpty);
    }
}
