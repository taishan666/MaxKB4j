package com.maxkb4j.common.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.ArrayList;
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
        return toList(rs.getArray(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getArray(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Array array = cs.getArray(columnIndex);
        return toList(array);
    }

    private List<String> toList(Array array) throws SQLException {
        if (array == null) {
            return null;
        }
        Object raw = array.getArray();
        if (raw == null) {
            return null;
        }
        if (raw instanceof Object[] elements) {
            List<String> result = new ArrayList<>(elements.length);
            for (Object e : elements) {
                result.add(e == null ? null : e.toString());
            }
            return result;
        }
        List<String> result = new ArrayList<>(1);
        result.add(raw.toString());
        return result;
    }
}
