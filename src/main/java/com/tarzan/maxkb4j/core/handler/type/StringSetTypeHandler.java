package com.tarzan.maxkb4j.core.handler.type;

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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class StringSetTypeHandler extends BaseTypeHandler<Set<String>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Set<String> parameter, JdbcType jdbcType) throws SQLException {
        PGobject pGobject = new PGobject();
        pGobject.setType("varchar");
        pGobject.setValue(toDBValue(parameter));
        ps.setObject(i, pGobject);
    }

    @Override
    public Set<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convert(value);
    }

    @Override
    public Set<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convert(value);
    }

    @Override
    public Set<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convert(value);
    }

    private Set<String> convert(String value){
        if(notNull(value)){
            if(StringUtils.isNotBlank(value)){
                //分割字符串并去除空格
                return Arrays.stream(value.split(","))
                        .map(String::trim) // 去除每个元素的前后空格
                        .collect(Collectors.toSet());
            }
        }
        return new HashSet<>();
    }

    private boolean notNull(String value){
        return (null != value && !value.isEmpty());
    }


    public String toDBValue(Set<String> value) {
        return String.join(",", value);
    }
}
