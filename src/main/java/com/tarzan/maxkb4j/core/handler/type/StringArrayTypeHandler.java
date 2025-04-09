package com.tarzan.maxkb4j.core.handler.type;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringArrayTypeHandler extends BaseTypeHandler<List<String>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        PGobject pGobject = new PGobject();
        pGobject.setType("jsonb[]");
        pGobject.setValue(toDBValue(parameter));
        ps.setObject(i, pGobject);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convert(value);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convert(value);
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convert(value);
    }

    private List<String> convert(String value){
        if(notNull(value)){
            // 1. 去掉大括号
            String noBraces = value.replace("{", "").replace("}", "");
            noBraces=noBraces.replace("\\","");
            noBraces=noBraces.replace("\"\"","");
            if(StringUtils.isNotBlank(noBraces)){
                // 2. 分割字符串并去除空格
                return Arrays.stream(noBraces.split(","))
                        .map(String::trim) // 去除每个元素的前后空格
                        .toList();
            }
        }
        return new ArrayList<>();
    }

    private boolean notNull(String value){
        return (null != value && !value.isEmpty());
    }


    public String toDBValue(List<String> value) {
        if(null == value || value.isEmpty()){
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (String text : value) {
            sb.append(escapeText(text)).append(",");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append("}");
        return sb.toString();
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
