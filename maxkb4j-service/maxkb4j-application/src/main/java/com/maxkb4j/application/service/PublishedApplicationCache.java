package com.maxkb4j.application.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.application.service.impl.ApplicationServiceImpl;
import com.maxkb4j.application.vo.ApplicationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 已发布应用详情缓存，从 {@link ApplicationServiceImpl} 抽离。
 * 缓存已组装好的发布版本详情，读取时返回深拷贝，防止调用方修改返回值污染缓存。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class PublishedApplicationCache {

    private final ApplicationVersionService applicationVersionService;
    private final ApplicationDetailAssembler detailAssembler;

    private final Cache<String, ApplicationVO> cache = Caffeine.newBuilder()
            .initialCapacity(64)
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    /**
     * 获取应用最新发布版本的详情，未发布过或无版本时返回 null。
     */
    public ApplicationVO get(String appId) {
        ApplicationVO cached = cache.get(appId, id -> {
            ApplicationVO vo = applicationVersionService.getAppLatestOne(id);
            return vo == null ? null : detailAssembler.wrap(vo);
        });
        return deepCopy(cached);
    }

    /**
     * 应用内容变化时失效缓存。
     */
    public void invalidate(String appId) {
        if (appId != null) {
            cache.invalidate(appId);
        }
    }

    private static ApplicationVO deepCopy(ApplicationVO source) {
        if (source == null) {
            return null;
        }
        return JSONObject.parseObject(JSON.toJSONString(source), ApplicationVO.class);
    }
}
