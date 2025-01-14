package com.tarzan.maxkb4j.handler;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

@Component("uuid_array_type_handler")
public class UUIDArrayTypeHandler extends BaseTypeHandler<UUID[]> {


    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID[] parameter, JdbcType jdbcType) throws SQLException {
        if(null != parameter){
            PGobject pGobject = new PGobject();
            pGobject.setType("UUID[]");
            pGobject.setValue(toDBValue(parameter));
            ps.setObject(i, pGobject);
        }
    }

    @Override
    public UUID[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convert(value);
    }

    @Override
    public UUID[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convert(value);
    }

    @Override
    public UUID[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convert(value);
    }

    private UUID[] convert(String value){
        if(notNull(value)){
            // 1. 去掉大括号
            String noBraces = value.replace("{", "").replace("}", "");
            if(StringUtils.isNotBlank(noBraces)){
                // 2. 分割字符串并去除空格
                // 去除每个元素的前后空格
                return Arrays.stream(noBraces.split(","))
                        .map(e -> UUID.fromString(e.trim())).distinct().toArray(UUID[]::new);
            }

        }
        return new UUID[0];
    }

    private boolean notNull(String value){
        return (null != value && !value.isEmpty());
    }

    public String toDBValue(UUID[] value) {
        if(null == value || value.length==0){
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (UUID s : value) {
            sb.append(s).append(",");
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append("}");
        return sb.toString();
    }
}