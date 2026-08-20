package com.maxkb4j.model.custom.model;

import com.maxkb4j.model.entity.ModelCredential;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenAIClient 实例缓存。
 *
 * <p>OpenAIOkHttpClient 内部持有独立的 OkHttp 连接池与 dispatcher 线程池，
 * 若每次 STT/TTS 调用都新建客户端，高频请求下线程与连接对象会持续堆积。
 * OpenAIClient 不可变且线程安全，按凭证（baseUrl + apiKey）复用。</p>
 */
final class OpenAiClientHolder {

    private static final Map<String, OpenAIClient> CLIENT_CACHE = new ConcurrentHashMap<>();

    private OpenAiClientHolder() {
    }

    static OpenAIClient getOrCreate(ModelCredential credential) {
        String cacheKey = credential.getBaseUrl() + "|" + credential.getApiKey();
        return CLIENT_CACHE.computeIfAbsent(cacheKey, key -> OpenAIOkHttpClient.builder()
                .baseUrl(credential.getBaseUrl())
                .apiKey(credential.getApiKey())
                .build());
    }
}
