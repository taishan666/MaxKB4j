package com.maxkb4j.common.cache;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.enums.SettingType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统设置缓存静态门面（第 3 期过渡态）。
 * <p>默认实现为进程内 ConcurrentHashMap；Spring 启动后由
 * {@code SystemSettingCacheService} 通过 {@link #init(SystemSettingStore)} 注册
 * Bean 实现替换静态默认实现。调用方暂继续使用静态 API，后续迁移为直接注入 Bean。</p>
 */
public final class SystemCache {

    private static volatile SystemSettingStore store = new InMemoryStore();

    private SystemCache() {
    }

    /** 注册存储实现（由 Spring Bean 构造时调用），后注册者生效。 */
    public static void init(SystemSettingStore newStore) {
        store = newStore;
    }

    /** 恢复默认进程内实现，供测试隔离使用。 */
    public static void reset() {
        store = new InMemoryStore();
    }

    public static void put(Integer type, JSONObject value) {
        store.put(type, value);
    }

    public static Object get(Integer type) {
        return store.get(type);
    }

    /**
     * 获取类型安全的 RSA 密钥设置，未初始化时返回 null。
     */
    public static SystemKeySetting getKeySetting() {
        return SystemKeySetting.from(store.get(SettingType.KEY.getType()));
    }

    public static String getPrivateKey() {
        SystemKeySetting setting = getKeySetting();
        return setting == null ? null : setting.encryptedPrivateKeyPem();
    }

    public static String getPublicKey() {
        SystemKeySetting setting = getKeySetting();
        return setting == null ? null : setting.publicKeyPem();
    }

    private static final class InMemoryStore implements SystemSettingStore {
        private final Map<Integer, JSONObject> cache = new ConcurrentHashMap<>();

        @Override
        public void put(Integer type, JSONObject value) {
            cache.put(type, value);
        }

        @Override
        public JSONObject get(Integer type) {
            return cache.get(type);
        }
    }
}