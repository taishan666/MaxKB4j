package com.maxkb4j.start.listener;

import cn.dev33.satoken.config.SaTokenConfig;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.enums.SettingType;
import com.maxkb4j.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class JwtSecretInitializer {

    private static final String JWT_SECRET_PROPERTY = "sa-token.jwt-secret-key";
    private static final String JWT_SECRET_FIELD = "jwtSecret";
    private static final int SECRET_BYTES = 32;

    private final SystemSettingService systemSettingService;
    private final Environment environment;
    private final SaTokenConfig saTokenConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    public void initialize() {
        String configuredSecret = environment.getProperty(JWT_SECRET_PROPERTY);
        if (StringUtils.hasText(configuredSecret)) {
            return;
        }

        JSONObject meta = systemSettingService.getSettingMeta(SettingType.JWT_SECRET);
        String secret = meta == null ? null : meta.getString(JWT_SECRET_FIELD);
        if (!StringUtils.hasText(secret)) {
            secret = generateSecret();
            meta = new JSONObject();
            meta.put(JWT_SECRET_FIELD, secret);
            if (!systemSettingService.saveOrUpdate(meta, SettingType.JWT_SECRET)) {
                throw new IllegalStateException("JWT 密钥持久化失败，应用无法安全启动");
            }
        }
        saTokenConfig.setJwtSecretKey(secret);
    }

    private String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
