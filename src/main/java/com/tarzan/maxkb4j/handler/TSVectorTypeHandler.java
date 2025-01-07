package com.tarzan.maxkb4j.handler;

import com.tarzan.maxkb4j.module.common.dto.SearchIndex;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;


public class TSVectorTypeHandler extends BaseTypeHandler<Set<SearchIndex>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Set<SearchIndex> parameter, JdbcType jdbcType) throws SQLException {
        if(null != parameter){
            PGobject pGobject = new PGobject();
            pGobject.setType("tsvector");
            pGobject.setValue(toTSVector(parameter));
            ps.setObject(i, pGobject);
        }
    }

    @Override
    public Set<SearchIndex> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convert(value);
    }

    @Override
    public Set<SearchIndex> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convert(value);
    }

    @Override
    public Set<SearchIndex> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convert(value);
    }


    private Set<SearchIndex>  convert(String value){
        if(notNull(value)){
            Set<SearchIndex> set = new HashSet<>();
            String[] words = value.split(" ");
            for (String word : words) {
                SearchIndex index = new SearchIndex();
                String[] kv = word.split(":");
                index.setWord(kv[0].replaceAll("'",""));
                index.setWord(kv[1]);
                set.add(index);
            }
        }
        return new HashSet<>();
    }

    private boolean notNull(String value){
        return (null != value && !value.isEmpty());
    }

    public String toTSVector(Set<SearchIndex> parameter) {
        StringBuilder sb = new StringBuilder();
        parameter.forEach(e->{
            sb.append("'").append(e.getWord()).append("'").append(":").append(e.getIndices()).append(" ");
        });
        return sb.toString();
    }
}
