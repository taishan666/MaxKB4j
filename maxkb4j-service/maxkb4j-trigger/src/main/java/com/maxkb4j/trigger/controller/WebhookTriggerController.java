package com.maxkb4j.trigger.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.trigger.model.TriggerSetting;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.common.util.WebUtil;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.enums.TriggerType;
import com.maxkb4j.trigger.service.IEventTriggerService;
import com.maxkb4j.trigger.service.TriggerTaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 触发器管理控制器
 *
 * @author tarzan
 * @date 2025-03-15 22:00:45
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@Slf4j
public class WebhookTriggerController {

    private final IEventTriggerService eventTriggerService;
    private final TriggerTaskExecutor triggerTaskExecutor;

    /**
     * Webhook 访问频率限制缓存（IP -> 最后访问时间）。
     * 限制每个 IP 在 1 分钟内最多访问 60 次。
     */
    private final Cache<String, Long> rateLimitCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

    /**
     * 每分钟允许的最大请求次数。
     */
    private static final int RATE_LIMIT_PER_MINUTE = 60;

    public WebhookTriggerController(IEventTriggerService eventTriggerService, TriggerTaskExecutor triggerTaskExecutor) {
        this.eventTriggerService = eventTriggerService;
        this.triggerTaskExecutor = triggerTaskExecutor;
    }

    @PostMapping("/trigger/v1/webhook/{id}")
    public R<Boolean> webhook(@PathVariable String id, @RequestBody JSONObject data) {
        // 访问频率限制，防止滥用
        String clientIp = WebUtil.getIP();
        Long lastAccess = rateLimitCache.getIfPresent(clientIp);
        if (lastAccess != null) {
            long count = rateLimitCache.estimatedSize();
            // 简化实现：使用时间窗口计数
            rateLimitCache.put(clientIp, lastAccess + 1);
            if (lastAccess >= RATE_LIMIT_PER_MINUTE) {
                return R.fail(429, "trigger.rate.limit.exceeded");
            }
        } else {
            rateLimitCache.put(clientIp, 1L);
        }

        EventTriggerEntity eventTrigger = eventTriggerService.getById(id);
        if (eventTrigger == null) {
            return R.fail(I18nUtil.get("trigger.event.not.found"));
        }
        if (!TriggerType.EVENT.name().equals(eventTrigger.getTriggerType())) {
            return R.fail(I18nUtil.get("trigger.event.type.invalid"));
        }
        if (!eventTrigger.getIsActive()) {
            return R.fail(I18nUtil.get("trigger.event.disabled"));
        }
        TriggerSetting setting = TriggerSetting.from(eventTrigger.getTriggerSetting());
        if (Objects.nonNull(setting) && Objects.nonNull(setting.token())) {
            String tokenValue = WebUtil.getTokenValue();
            // 使用 MessageDigest.isEqual 进行常量时间比对，防止时序侧信道攻击
            if (constantTimeEquals(tokenValue, setting.token())) {
                triggerTaskExecutor.execute(id, data);
                return R.data(true);
            }
        }
        return R.fail(I18nUtil.get("trigger.token.auth.failed"));
    }

    /**
     * 常量时间字符串比对，防止时序侧信道攻击。
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
