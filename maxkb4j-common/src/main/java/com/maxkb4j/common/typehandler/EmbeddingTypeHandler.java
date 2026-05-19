package com.maxkb4j.common.typehandler;

import com.pgvector.PGvector;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EmbeddingTypeHandler extends BaseTypeHandler<List<Float>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Float> parameter, JdbcType jdbcType) throws SQLException {
        if(null != parameter){
            PGvector pGvector = new PGvector(parameter);
            ps.setObject(i, pGvector);
        }
    }

    @Override
    public List<Float> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convert(value);
    }

    @Override
    public List<Float> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convert(value);
    }

    @Override
    public List<Float> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convert(value);
    }


    private List<Float> convert(String value){
        if(notNull(value)){
            // 去掉首尾的方括号（如果存在）
            String trimmedStr = value;
            if (trimmedStr.startsWith("[") && trimmedStr.endsWith("]")) {
                trimmedStr = trimmedStr.substring(1, trimmedStr.length() - 1);
            }
            // 如果字符串为空，返回空列表
            if (trimmedStr.trim().isEmpty()) {
                return Collections.emptyList();
            }
            // 按照逗号分割字符串，并将每个元素转换为 Float
            return Arrays.stream(trimmedStr.split(","))
                    .map(String::trim)
                    .map(Float::parseFloat)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private boolean notNull(String value){
        return (null != value && !value.isEmpty());
    }



}
