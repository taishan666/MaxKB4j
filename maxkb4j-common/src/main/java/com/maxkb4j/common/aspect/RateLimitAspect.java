package com.maxkb4j.common.aspect;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.common.annotation.RateLimit;
import com.maxkb4j.common.exception.RateLimitExceededException;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.common.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 接口限流切面
 * <p>
 * 算法：固定窗口计数器（Fixed Window Counter）
 * 存储：Caffeine 本地缓存（单机限流，集群部署需替换为 Redis）
 * <p>
 * 窗口 key 格式：{方法签名}:{维度标识}:{窗口桶编号}
 * 窗口桶编号 = 当前时间戳(秒) / window，天然实现窗口滚动，无需定时清理。
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    /**
     * 计数器缓存，TTL 设为 2 小时（覆盖最大合理窗口），最多缓存 10 万个 key
     */
    private final Cache<String, AtomicInteger> counterCache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.HOURS)
            .maximumSize(100_000)
            .build();

    private final ExpressionParser spelExpressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    private static final String DEFAULT_MESSAGE = "请求过于频繁，请稍后再试";

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String baseKey = buildKey(joinPoint, rateLimit);
        long windowBucket = System.currentTimeMillis() / 1000L / rateLimit.window();
        String cacheKey = baseKey + ":" + windowBucket;

        AtomicInteger counter = counterCache.get(cacheKey, k -> new AtomicInteger(0));
        assert counter != null;
        int count = counter.incrementAndGet();

        if (count > rateLimit.limit()) {
            String msg = rateLimit.message().isEmpty() ? DEFAULT_MESSAGE : rateLimit.message();
            log.warn("接口限流触发: key={}, count={}, limit={}/{}s",
                    cacheKey, count, rateLimit.limit(), rateLimit.window());
            throw new RateLimitExceededException(msg);
        }

        return joinPoint.proceed();
    }

    private String buildKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodKey = signature.getDeclaringTypeName() + "." + signature.getName();

        return switch (rateLimit.keyType()) {
            case IP     -> methodKey + ":ip:" + resolveClientIp();
            case USER   -> methodKey + ":user:" + resolveUserId();
            case GLOBAL -> methodKey + ":global";
            case CUSTOM -> methodKey + ":custom:" + evaluateSpEL(joinPoint, signature, rateLimit.key());
        };
    }

    /**
     * 获取客户端真实 IP（支持反向代理）
     */
    private String resolveClientIp() {
        HttpServletRequest request = WebUtil.getRequest();
        String ip = WebUtil.getIP(request);
        return (ip != null && !ip.isEmpty()) ? ip : "unknown";
    }

    /**
     * 获取当前登录用户标识，未登录时降级为 IP
     */
    private String resolveUserId() {
        try {
            Object adminId = StpKit.ADMIN.getLoginIdDefaultNull();
            if (adminId != null) return "admin:" + adminId;
            Object userId = StpKit.USER.getLoginIdDefaultNull();
            if (userId != null) return "user:" + userId;
        } catch (Exception ignored) {
        }
        return "ip:" + resolveClientIp();
    }

    /**
     * 解析 SpEL 表达式，支持通过 #paramName 引用方法参数
     */
    private String evaluateSpEL(ProceedingJoinPoint joinPoint, MethodSignature signature, String expression) {
        if (expression == null || expression.isBlank()) return "default";
        try {
            Method method = signature.getMethod();
            String[] paramNames = nameDiscoverer.getParameterNames(method);
            Object[] args = joinPoint.getArgs();
            StandardEvaluationContext context = new StandardEvaluationContext();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            Object result = spelExpressionParser.parseExpression(expression).getValue(context);
            return result != null ? result.toString() : "null";
        } catch (Exception e) {
            log.warn("限流 SpEL 解析失败: expression={}", expression, e);
            return "spel_error";
        }
    }
}
