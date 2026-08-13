package com.maxkb4j.common.cache;

import com.alibaba.fastjson.JSONObject;

/**
 * 系统 RSA 密钥对设置，对应 system_setting 表中 type=KEY 的 meta JSON。
 *
 * <p>历史原因，持久化 JSON 的字段名与语义不一致：
 * {@code key} 字段存放公钥 PEM，{@code value} 字段存放加密后的私钥 PEM。
 * 本类型将该映射收敛到一处，其余代码一律通过 {@link #publicKeyPem()} /
 * {@link #encryptedPrivateKeyPem()} 访问，避免字段名误用。</p>
 *
 * @param publicKeyPem           公钥 PEM 明文
 * @param encryptedPrivateKeyPem 加密后的私钥 PEM
 * @author tarzan
 */
public record SystemKeySetting(String publicKeyPem, String encryptedPrivateKeyPem) {

    /** meta JSON 中存放公钥 PEM 的字段名（历史命名，涉及存量数据，勿改）。 */
    public static final String FIELD_PUBLIC_KEY = "key";

    /** meta JSON 中存放加密私钥 PEM 的字段名（历史命名，涉及存量数据，勿改）。 */
    public static final String FIELD_ENCRYPTED_PRIVATE_KEY = "value";

    /**
     * 从 system_setting 的 meta JSON 解析密钥设置，meta 为 null 时返回 null。
     */
    public static SystemKeySetting from(JSONObject meta) {
        if (meta == null) {
            return null;
        }
        return new SystemKeySetting(
                meta.getString(FIELD_PUBLIC_KEY),
                meta.getString(FIELD_ENCRYPTED_PRIVATE_KEY));
    }

    /**
     * 序列化为持久化用的 meta JSON（字段名与存量数据保持一致）。
     */
    public JSONObject toMetaJson() {
        JSONObject json = new JSONObject();
        json.put(FIELD_PUBLIC_KEY, publicKeyPem);
        json.put(FIELD_ENCRYPTED_PRIVATE_KEY, encryptedPrivateKeyPem);
        return json;
    }
}
