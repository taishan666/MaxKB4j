package com.tarzan.maxkb4j.module.application.cache;

import com.tarzan.maxkb4j.module.application.dto.ChatInfo;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

import java.io.File;
import java.time.Duration;

public class ChatCache {

    private final static CacheManager cacheManager;

    private final static String persistent = "data";

    static {
        File persistentFile = new File(persistent);
        // 公共配置
        ResourcePoolsBuilder resourcePools = ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(1000, EntryUnit.ENTRIES)
                .offheap(100, MemoryUnit.MB)
                .disk(200, MemoryUnit.MB, true);

        CacheConfigurationBuilder<String, ChatInfo> config =
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                        String.class,
                        ChatInfo.class,
                        resourcePools
                ).withExpiry(  // 关键修改点：添加过期策略
                        ExpiryPolicyBuilder.timeToLiveExpiration(
                                Duration.ofMinutes(30)  // 30分钟后过期
                        )
                );
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(persistentFile))
                .withCache("chatCache", config)
                .build();
        cacheManager.init();  // 重新初始化
        // 验证缓存加载
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing cache manager...");
            cacheManager.close();  // 确保调用 close()
        }));
    }

public static void put(String chatId, ChatInfo chatInfo) {
    Cache<String, ChatInfo> cache = cacheManager.getCache("chatCache", String.class, ChatInfo.class);
    cache.put(chatId, chatInfo);
}

public static ChatInfo get(String chatId) {
    Cache<String, ChatInfo> cache = cacheManager.getCache("chatCache", String.class, ChatInfo.class);
    return cache.get(chatId);
}
}
