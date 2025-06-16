package com.tarzan.maxkb4j.core.handler.type;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.tarzan.maxkb4j.module.functionlib.dto.FunctionInputField;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class FunctionInputParamsTypeHandler extends BaseTypeHandler<List<FunctionInputField>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<FunctionInputField> parameter, JdbcType jdbcType) throws SQLException {
        if(null != parameter){
            PGobject pGobject = new PGobject();
            pGobject.setType("jsonb");
            pGobject.setValue(toJson(parameter));
            ps.setObject(i, pGobject);
        }
    }

    @Override
    public List<FunctionInputField> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convert(value);
    }

    @Override
    public List<FunctionInputField> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convert(value);
    }

    @Override
    public List<FunctionInputField> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convert(value);
    }

    private List<FunctionInputField> convert(String value){
        if(notNull(value)){
            return  JSONArray.parseArray(value, FunctionInputField.class);
        }
        return null;
    }

    private boolean notNull(String value){
        return (null != value && !value.isEmpty());
    }

    public String toJson(List<FunctionInputField> obj) {
        return JSON.toJSONString(obj, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullListAsEmpty, SerializerFeature.WriteNullStringAsEmpty);
    }
}
