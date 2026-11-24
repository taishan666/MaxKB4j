package com.maxkb4j.trigger.controller;

import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.trigger.model.TriggerSetting;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.util.ClientIpResolver;
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
import java.util.concurrent.atomic.AtomicInteger;

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
    private final ClientIpResolver clientIpResolver;

    /**
     * Webhook 访问频率限制缓存（窗口 key -> 计数器）。
     * 窗口桶编号 = 时间戳(秒) / 窗口长度，写入永远落在当前窗口的新 key 上，
     * 旧窗口条目到期自动淘汰，不会因持续访问被重写而续期。
     */
    private final Cache<String, AtomicInteger> rateLimitCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    /**
     * 限流窗口长度（秒）。
     */
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60;

    /**
     * 单个窗口内每个 IP 允许的最大请求次数。
     */
    private static final int RATE_LIMIT_PER_MINUTE = 60;

    public WebhookTriggerController(IEventTriggerService eventTriggerService, TriggerTaskExecutor triggerTaskExecutor,
                                    ClientIpResolver clientIpResolver) {
        this.eventTriggerService = eventTriggerService;
        this.triggerTaskExecutor = triggerTaskExecutor;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/trigger/v1/webhook/{id}")
    public R<Boolean> webhook(@PathVariable String id, @RequestBody JSONObject data) {
        // 访问频率限制，防止滥用：固定窗口计数，窗口 key 随时间滚动
        String clientIp = clientIpResolver.getIp();
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = "unknown";
        }
        String cacheKey = clientIp + ":" + (System.currentTimeMillis() / 1000L / RATE_LIMIT_WINDOW_SECONDS);
        AtomicInteger counter = rateLimitCache.get(cacheKey, k -> new AtomicInteger(0));
        assert counter != null;
        if (counter.incrementAndGet() > RATE_LIMIT_PER_MINUTE) {
            log.warn("Webhook 限流触发: id={}, ip={}, count={}, limit={}/{}s",
                    id, clientIp, counter.get(), RATE_LIMIT_PER_MINUTE, RATE_LIMIT_WINDOW_SECONDS);
            return R.fail(429, I18nUtil.get("trigger.rate.limit.exceeded"));
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
