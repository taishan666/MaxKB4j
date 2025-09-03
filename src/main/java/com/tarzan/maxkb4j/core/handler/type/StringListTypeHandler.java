package com.tarzan.maxkb4j.core.handler.type;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringListTypeHandler extends BaseTypeHandler<List<String>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter != null) {
            Connection conn = ps.getConnection();
            Array array = conn.createArrayOf("VARCHAR", parameter.toArray(new String[0]));
            ps.setArray(i, array);
        } else {
            ps.setArray(i, null);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Array array = rs.getArray(columnName);
        if (array != null) {
            String[] stringArray = (String[]) array.getArray();
            return Arrays.asList(stringArray);
        }
        return null;
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Array array = rs.getArray(columnIndex);
        if (array != null) {
            String[] stringArray = (String[]) array.getArray();
            return Arrays.asList(stringArray);
        }
        return null;
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return new ArrayList<>();
    }
}
