package com.tarzan.maxkb4j.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JOSNObjectTypeHandler extends BaseTypeHandler<JSONObject> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JSONObject parameter, JdbcType jdbcType) throws SQLException {
        if(null != parameter){
            System.out.println(parameter+" "+jdbcType);
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(toJson(parameter));
            ps.setObject(i, jsonObject);
        }
    }

    @Override
    public JSONObject getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convert(value);
    }

    @Override
    public JSONObject getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convert(value);
    }

    @Override
    public JSONObject getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convert(value);
    }

    private JSONObject convert(String value){
        if(notNull(value)){
            return JSON.parseObject(value);
        }
        return null;
    }

    private boolean notNull(String value){
        return (null != value && !value.isEmpty());
    }

    public String toJson(Object obj) {
        return JSON.toJSONString(obj, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteNullListAsEmpty, SerializerFeature.WriteNullStringAsEmpty);
    }
}
