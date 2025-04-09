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
import java.util.List;

@Component
public class JOSNBArrayTypeHandler extends BaseTypeHandler<List<JSONObject>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<JSONObject> parameter, JdbcType jdbcType) throws SQLException {
        PGobject pGobject = new PGobject();
        pGobject.setType("jsonb[]");
        pGobject.setValue(toDBValue(parameter));
        ps.setObject(i, pGobject);
    }

    @Override
    public List<JSONObject> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convert(value);
    }

    @Override
    public List<JSONObject> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convert(value);
    }

    @Override
    public List<JSONObject> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convert(value);
    }

    private List<JSONObject> convert(String value){
        if(notNull(value)){
            // 1. 去掉最外层大括号
            String noBraces =  value.substring(1, value.length() - 1);
            noBraces="["+noBraces+"]";
            noBraces=noBraces.replace("\\","");
            noBraces=noBraces.replace("\"\"","");
            noBraces=noBraces.replace("\"{","{");
            noBraces=noBraces.replace("}\"","}");
            if(StringUtils.isNotBlank(noBraces)){
                return JSON.parseArray(noBraces,JSONObject.class);
            }
        }
        return new ArrayList<>();
    }

    private boolean notNull(String value){
        return (null != value && !value.isEmpty());
    }


    public String toDBValue(List<JSONObject> value) {
        if(null == value || value.isEmpty()){
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (JSONObject obj : value) {
            sb.append(escapeJsonText(obj.toJSONString())).append(",");
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

}
