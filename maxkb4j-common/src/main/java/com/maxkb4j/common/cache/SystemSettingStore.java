package com.maxkb4j.common.cache;

import com.alibaba.fastjson.JSONObject;

/**
 * 系统设置缓存存储抽象。
 * <p>默认实现为 {@link SystemCache} 门面内置的进程内 Map；Spring 启动后由
 * system 模块的 {@code SystemSettingCacheService} 注册 Bean 实现替换。</p>
 */
public interface SystemSettingStore {

    void put(Integer type, JSONObject value);

    JSONObject get(Integer type);
}