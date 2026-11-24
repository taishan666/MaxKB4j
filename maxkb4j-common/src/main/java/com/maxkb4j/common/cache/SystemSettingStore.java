package com.maxkb4j.common.cache;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.enums.SettingType;

/**
 * 系统设置缓存存储抽象。
 *
 * <p>实现为 system 模块的 {@code SystemSettingCacheService} Bean，各消费方通过构造函数注入使用。</p>
 */
public interface SystemSettingStore {

    void put(Integer type, JSONObject value);

    JSONObject get(Integer type);

    /**
     * 获取类型安全的 RSA 密钥设置，未初始化时返回 null。
     */
    default SystemKeySetting getKeySetting() {
        return SystemKeySetting.from(get(SettingType.KEY.getType()));
    }

    /**
     * 获取加密后的私钥 PEM，未初始化时返回 null。
     */
    default String getPrivateKey() {
        SystemKeySetting setting = getKeySetting();
        return setting == null ? null : setting.encryptedPrivateKeyPem();
    }

    /**
     * 获取公钥 PEM，未初始化时返回 null。
     */
    default String getPublicKey() {
        SystemKeySetting setting = getKeySetting();
        return setting == null ? null : setting.publicKeyPem();
    }
}
