package com.tarzan.maxkb4j.handler;

import com.tarzan.maxkb4j.common.dto.MemberOperate;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MemberOperateTypeHandler extends BaseTypeHandler<MemberOperate> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, MemberOperate parameter, JdbcType jdbcType) throws SQLException {
        PGobject pGobject = new PGobject();
        pGobject.setType("varchar[]");
        pGobject.setValue(toDBValue(parameter));
        ps.setObject(i, pGobject);
    }

    @Override
    public MemberOperate getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convert(value);
    }

    @Override
    public MemberOperate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convert(value);
    }

    @Override
    public MemberOperate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convert(value);
    }

    private MemberOperate convert(String value){
        MemberOperate obj = new MemberOperate();
        if(notNull(value)){
            // 1. 去掉大括号
            String noBraces = value.replace("{", "").replace("}", "");
            // 2. 分割字符串并去除空格
            Set<String> set = Arrays.stream(noBraces.split(","))
                    .map(String::trim) // 去除每个元素的前后空格
                    .collect(Collectors.toSet());
            obj.setUse(set.contains("USE"));
            obj.setManage(set.contains("MANAGE"));
        }
        return obj;
    }

    private boolean notNull(String value){
        return (null != value && !value.isEmpty());
    }

    public String toDBValue(MemberOperate value) {
        if(null == value){
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if(Objects.nonNull(value.getUse())&&value.getUse()){
            sb.append("USE");
        }
        if(Objects.nonNull(value.getManage())&&value.getManage()){
            if(Objects.nonNull(value.getUse())&&value.getUse()){
                sb.append(",");
            }
            sb.append("MANAGE");
        }
        sb.append("}");
        return sb.toString();
    }
}
