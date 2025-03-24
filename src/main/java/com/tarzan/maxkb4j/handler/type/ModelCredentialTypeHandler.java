package com.tarzan.maxkb4j.handler.type;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.tarzan.maxkb4j.module.model.info.entity.ModelCredential;
import com.tarzan.maxkb4j.module.system.setting.cache.SystemCache;
import com.tarzan.maxkb4j.util.RSAUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
@Slf4j
public class ModelCredentialTypeHandler extends BaseTypeHandler<ModelCredential> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ModelCredential parameter, JdbcType jdbcType) throws SQLException {
        if (null != parameter) {
            String publicKey= SystemCache.getPublicKey();
            String text;
            try {
                text = RSAUtil.encryptPem(JSONObject.toJSONString(parameter), publicKey);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            ps.setObject(i, text);
        }
    }

    @Override
    public ModelCredential getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return convert(value);
    }

    @Override
    public ModelCredential getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return convert(value);
    }

    @Override
    public ModelCredential getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return convert(value);
    }

    private ModelCredential convert(String value) {
        if (notNull(value)) {
            String privateKey= SystemCache.getPrivateKey();
            String text;
            try {
                text = RSAUtil.rsaLongDecrypt(value, privateKey);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return JSON.parseObject(text, ModelCredential.class);
        }
        return null;
    }

    private boolean notNull(String value) {
        return (null != value && !value.isEmpty());
    }


}
