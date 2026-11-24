package com.maxkb4j.model.typehandler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.cache.SystemSettingStore;
import com.maxkb4j.common.util.RSAUtil;
import com.maxkb4j.model.entity.ModelCredential;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 模型凭证加密 TypeHandler。
 * <p>作为 Spring Bean 注册并由 MyBatis 自动识别，构造函数注入 {@link SystemSettingStore}
 * 以读取 RSA 密钥对凭证进行加解密。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelCredentialTypeHandler extends BaseTypeHandler<ModelCredential> {

    private final SystemSettingStore systemSettingStore;

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ModelCredential parameter, JdbcType jdbcType) throws SQLException {
        if (null != parameter) {
            String publicKey = systemSettingStore.getPublicKey();
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
            String privateKey = systemSettingStore.getPrivateKey();
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
