package com.maxkb4j.common.cache;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.enums.SettingType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SystemCache {

    private static final Map<Integer, JSONObject> CACHE=new ConcurrentHashMap<>();

    public static void put(Integer type, JSONObject value) {
        CACHE.put(type,value);
    }

    public static Object get(Integer type) {
        return CACHE.get(type);
    }

    /**
     * 获取类型安全的 RSA 密钥设置，未初始化时返回 null。
     */
    public static SystemKeySetting getKeySetting() {
        return SystemKeySetting.from(CACHE.get(SettingType.KEY.getType()));
    }

    public static String getPrivateKey() {
        SystemKeySetting setting = getKeySetting();
        return setting == null ? null : setting.encryptedPrivateKeyPem();
    }

    public static String getPublicKey() {
        SystemKeySetting setting = getKeySetting();
        return setting == null ? null : setting.publicKeyPem();
    }
}
