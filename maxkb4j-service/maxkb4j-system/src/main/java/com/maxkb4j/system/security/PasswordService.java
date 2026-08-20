package com.maxkb4j.system.security;

import cn.dev33.satoken.secure.SaSecureUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 口令哈希服务。
 * <p>新写入一律使用 BCrypt（带盐、慢哈希）；校验时兼容存量无盐 MD5 哈希，
 * 命中旧哈希后由登录流程负责升级为 BCrypt。</p>
 *
 * @author tarzan
 */
@Component
public class PasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 生成 BCrypt 哈希（每次调用盐值随机，结果不可逆）。
     */
    public String encode(CharSequence rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 校验明文口令与存量哈希是否匹配，同时兼容 BCrypt 与旧版 MD5。
     *
     * @param rawPassword 明文口令
     * @param storedHash  数据库中的哈希值
     */
    public boolean matches(CharSequence rawPassword, String storedHash) {
        if (rawPassword == null || StringUtils.isBlank(storedHash)) {
            return false;
        }
        if (isBcryptHash(storedHash)) {
            return encoder.matches(rawPassword, storedHash);
        }
        // 存量数据：无盐 MD5
        return SaSecureUtil.md5(rawPassword.toString()).equals(storedHash);
    }

    /**
     * 判断存量哈希是否为旧版（非 BCrypt）格式，用于登录后自动升级。
     */
    public boolean isLegacyHash(String storedHash) {
        return StringUtils.isNotBlank(storedHash) && !isBcryptHash(storedHash);
    }

    private boolean isBcryptHash(String hash) {
        // BCrypt 哈希形如 $2a$10$... / $2b$... / $2y$...
        return hash != null && hash.startsWith("$2");
    }
}
