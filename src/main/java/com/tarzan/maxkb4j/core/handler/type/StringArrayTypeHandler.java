package com.tarzan.maxkb4j.core.handler.type;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.lang.String;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

@Component
public class StringArrayTypeHandler extends BaseTypeHandler<String[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String[] parameter, JdbcType jdbcType) throws SQLException {
        if(null != parameter){
            PGobject pGobject = new PGobject();
            pGobject.setType("varchar[]");
            pGobject.setValue(toDBValue(parameter));
            ps.setObject(i, pGobject);
        }
    }

    @Override
    public String[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convert(value);
    }

    @Override
    public String[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convert(value);
    }

    @Override
    public String[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convert(value);
    }

    private String[] convert(String value){
        if(notNull(value)){
            // 1. 去掉大括号
            String noBraces = value.replace("{", "").replace("}", "");
            if(StringUtils.isNotBlank(noBraces)){
                // 2. 分割字符串并去除空格
                // 去除每个元素的前后空格
                return Arrays.stream(noBraces.split(","))
                        .map(String::trim).distinct().toArray(String[]::new);
            }

        }
        return new String[0];
    }

    private boolean notNull(String value){
        return (null != value && !value.isEmpty());
    }

    public String toDBValue(String[] value) {
        if(null == value || value.length==0){
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (String s : value) {
            sb.append(s).append(",");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append("}");
        return sb.toString();
    }
}