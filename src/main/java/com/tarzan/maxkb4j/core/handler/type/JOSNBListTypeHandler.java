/*
package com.tarzan.maxkb4j.core.handler.type;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class JOSNBListTypeHandler extends BaseTypeHandler<List<Object>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Object> parameter, JdbcType jdbcType) throws SQLException {
        PGobject pGobject = new PGobject();
        pGobject.setType("jsonb[]");
        pGobject.setValue(toDBValue(parameter));
        ps.setObject(i, pGobject);
    }

    @Override
    public List<Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convert(value);
    }

    @Override
    public List<Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convert(value);
    }

    @Override
    public List<Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convert(value);
    }

    private List<Object> convert(String value){
        if(notNull(value)){
            // 1. 去掉最外层大括号
            String noBraces =  value.substring(1, value.length() - 1);
            noBraces=noBraces.replace("\\","");
            noBraces=noBraces.replace("\"\"","");
            noBraces=noBraces.replace("\"{","{");
            noBraces=noBraces.replace("}\"","}");
            if(StringUtils.isNotBlank(noBraces)){
                if (noBraces.startsWith("{")&& noBraces.endsWith("}")){
                    noBraces="["+noBraces+"]";
                    return JSON.parseArray(noBraces, Object.class);
                }else {
                    // 2. 分割字符串并去除空格
                    return Arrays.stream(noBraces.split(","))
                            .map(e->(Object) e.trim()) // 去除每个元素的前后空格
                            .toList();
                }
            }
        }
        return new ArrayList<>();
    }

    private boolean notNull(String value){
        return (null != value && !value.isEmpty());
    }


    public String toDBValue(List<Object> value) {
        if(null == value || value.isEmpty()){
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Object obj : value) {
            if (obj instanceof JSONObject jsonObject){
                sb.append(escapeJsonText(jsonObject.toJSONString())).append(",");
            }else if(obj instanceof String){
                sb.append(escapeText(obj.toString())).append(",");
            }else {
                sb.append(escapeJsonText(JSON.toJSONString(obj))).append(",");
            }
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append("}");
        return sb.toString();
    }

    public static String escapeJsonText(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder escapedString = new StringBuilder();
        escapedString.append("\"");
        for (char ch : input.toCharArray()) {
            switch (ch) {
                case ' ':
                    escapedString.append("\\t"); // 空格替换为\t
                    break;
                case '\"':
                    escapedString.append("\\\""); // 双引号替换为\"
                    break;
                case '\n':
                    escapedString.append("\\n"); // 换行符替换为\n
                    break;
                default:
                    escapedString.append(ch); // 其他字符保持不变
                    break;
            }
        }
        escapedString.append("\"");
        return escapedString.toString();
    }

    public static String escapeText(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder escapedString = new StringBuilder();
        escapedString.append("\"").append("\\").append("\"");
        for (char ch : input.toCharArray()) {
            switch (ch) {
                case ' ':
                    escapedString.append("\\t"); // 空格替换为\t
                    break;
                case '\"':
                    escapedString.append("\\\""); // 双引号替换为\"
                    break;
                case '\n':
                    escapedString.append("\\n"); // 换行符替换为\n
                    break;
                default:
                    escapedString.append(ch); // 其他字符保持不变
                    break;
            }
        }
        escapedString.append("\\").append("\"").append("\"");
        return escapedString.toString();
    }

}
*/
