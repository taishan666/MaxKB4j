package com.tarzan.maxkb4j.handler;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class MemberOperateHandler extends BaseTypeHandler<JSONObject> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JSONObject parameter, JdbcType jdbcType) throws SQLException {
        if(null != parameter){
            PGobject jsonObject = new PGobject();
            jsonObject.setType("varchar[]");
            jsonObject.setValue(toDBValue(parameter));
            ps.setObject(i, jsonObject);
         //   ps.setObject(i, toDBValue(parameter));
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
        JSONObject json = new JSONObject();
        if(notNull(value)){
            // 1. 去掉大括号
            String noBraces = value.replace("{", "").replace("}", "");
            // 2. 分割字符串并去除空格
            Set<String> set = Arrays.stream(noBraces.split(","))
                    .map(String::trim) // 去除每个元素的前后空格
                    .collect(Collectors.toSet());
            if(set.contains("USE")){
                json.put("USE",true);
            }else {
                json.put("USE",false);
            }
            if(set.contains("MANAGE")){
                json.put("MANAGE",true);
            }else {
                json.put("MANAGE",false);
            }
            return json;
        }
        return json;
    }

    private boolean notNull(String value){
        return (null != value && !value.isEmpty());
    }

    public String toDBValue(JSONObject json) {
        if(null == json){
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean userFlag=json.getBooleanValue("USE");
        if(userFlag){
            sb.append("USE");
        }
        boolean manageFlag=json.getBooleanValue("MANAGE");
        if(manageFlag){
            if(userFlag){
                sb.append(",");
            }
            sb.append("MANAGE");
        }
        sb.append("}");
        return sb.toString();
    }
}
