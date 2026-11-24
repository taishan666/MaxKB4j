package com.maxkb4j.system.cache;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.cache.SystemSettingStore;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统设置缓存 Bean 化实现。
 * <p>进程内 {@link ConcurrentHashMap} 存储，启动时由 StartedListener 加载系统设置；
 * 各消费方通过构造函数注入 {@link SystemSettingStore} 使用。</p>
 */
@Service
public class SystemSettingCacheService implements SystemSettingStore {

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
