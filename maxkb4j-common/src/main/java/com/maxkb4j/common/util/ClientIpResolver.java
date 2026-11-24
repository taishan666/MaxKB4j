package com.maxkb4j.common.util;

import com.maxkb4j.common.props.SystemProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 客户端真实 IP 解析器：封装 {@link WebUtil} 的纯 IP 还原逻辑，并通过构造器注入
 * {@link SystemProperties} 中的可信代理配置，替代原先通过静态工具类从 Spring 容器取 Bean 的做法。
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private final SystemProperties systemProperties;

    public String getIp() {
        return getIp(WebUtil.getRequest());
    }

    @Nullable
    public String getIp(@Nullable HttpServletRequest request) {
        return WebUtil.getIP(request, trustedProxies());
    }

    public boolean isTrustedProxy(String ip) {
        return WebUtil.isTrustedProxy(ip, trustedProxies());
    }

    private List<String> trustedProxies() {
        List<String> proxies = systemProperties.getTrustedProxies();
        return proxies != null ? proxies : List.of();
    }
}
