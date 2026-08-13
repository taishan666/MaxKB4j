package com.maxkb4j.system.cache;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.cache.SystemCache;
import com.maxkb4j.common.cache.SystemSettingStore;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统设置缓存 Bean 化实现（第 3 期）。
 * <p>构造时注册到 {@link SystemCache} 静态门面，替换其默认实现；
 * 测试中可直接构造本类并经由门面断言缓存行为。</p>
 */
@Service
public class SystemSettingCacheService implements SystemSettingStore {

    private final Map<Integer, JSONObject> cache = new ConcurrentHashMap<>();

    public SystemSettingCacheService() {
        SystemCache.init(this);
    }

    @Override
    public void put(Integer type, JSONObject value) {
        cache.put(type, value);
    }

    @Override
    public JSONObject get(Integer type) {
        return cache.get(type);
    }
}