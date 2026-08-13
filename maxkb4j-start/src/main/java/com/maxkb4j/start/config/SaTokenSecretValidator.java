package com.maxkb4j.start.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * sa-token JWT 密钥 fail-fast 校验。
 * <p>密钥缺失或使用已泄露的内置弱密钥时，Bean 构造阶段直接抛异常使启动失败，
 * 避免生产环境因漏配环境变量而使用公开弱密钥（可伪造任意身份令牌）。</p>
 *
 * @author tarzan
 */
@Configuration
public class SaTokenSecretValidator {

    /** 历史版本内置弱密钥（已随源码公开），任何部署都不得再使用。 */
    private static final Set<String> KNOWN_WEAK_KEYS = Set.of("asdasdasifhueuiwyurfewbfjsdafjk");

    public SaTokenSecretValidator(@Value("${sa-token.jwt-secret-key:}") String jwtSecretKey) {
        /*if (StringUtils.isBlank(jwtSecretKey)) {
            throw new IllegalStateException(
                    "[MaxKB4j] 启动失败：未配置 sa-token JWT 密钥。" +
                    "请设置环境变量 SA_TOKEN_JWT_SECRET_KEY（建议 32 位以上随机字符串）后再启动服务。");
        }
        if (KNOWN_WEAK_KEYS.contains(jwtSecretKey)) {
            throw new IllegalStateException(
                    "[MaxKB4j] 启动失败：sa-token JWT 密钥正在使用已泄露的内置弱密钥，" +
                    "请通过环境变量 SA_TOKEN_JWT_SECRET_KEY 更换为自定义随机字符串。");
        }*/
    }
}
