package com.maxkb4j.common.util;

import cn.hutool.json.JSONUtil;
import com.maxkb4j.common.props.SystemProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class WebUtil extends WebUtils {
    public static final String USER_AGENT_HEADER = "user-agent";
    private static final Logger log = LoggerFactory.getLogger(WebUtil.class);
    private static final String XFF_HEADER = "x-forwarded-for";
    private static final String X_REAL_IP_HEADER = "x-real-ip";
    private static final Set<String> LOOPBACK_ADDRESSES = Set.of("127.0.0.1", "::1", "0:0:0:0:0:0:0:1");
    private static final Pattern IPV6_LIKE_PATTERN = Pattern.compile("[0-9A-Fa-f:.]+");
    private static final Predicate<String> IP_PREDICATE = (ip) -> StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip);

    public WebUtil() {
    }

    public static boolean isBody(HandlerMethod handlerMethod) {
        ResponseBody responseBody = ClassUtil.getAnnotation(handlerMethod, ResponseBody.class);
        return responseBody != null;
    }

    @Nullable
    public static String getCookieVal(String name) {
        HttpServletRequest request = getRequest();
        Assert.notNull(request, "request from RequestContextHolder is null");
        return getCookieVal(request, name);
    }

    @Nullable
    public static String getCookieVal(HttpServletRequest request, String name) {
        Cookie cookie = getCookie(request, name);
        return cookie != null ? cookie.getValue() : null;
    }

    public static void removeCookie(HttpServletResponse response, String key) {
        setCookie(response, key, null, 0);
    }

    public static void setCookie(HttpServletResponse response, String name, @Nullable String value, int maxAgeInSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeInSeconds);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        return requestAttributes == null ? null : ((ServletRequestAttributes) requestAttributes).getRequest();
    }

    public static void renderJson(HttpServletResponse response, Object result) {
        renderJson(response, result, "application/json;charset=UTF-8");
    }

    public static void renderJson(HttpServletResponse response, Object result, String contentType) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType(contentType);

        try (PrintWriter out = response.getWriter()) {
            out.append(JSONUtil.toJsonStr(result));
        } catch (IOException var16) {
            log.error(var16.getMessage(), var16);
        }

    }

    public static String getIP() {
        return getIP(getRequest());
    }

    @Nullable
    public static String getIP(@Nullable HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String remoteAddr = request.getRemoteAddr();
        // X-Forwarded-For / X-Real-IP 可由任意客户端填充，只有当 TCP 对端是可信代理
        // （回环地址或 system.trusted-proxies 配置项）时才允许采信转发头，
        // 否则一律以 remoteAddr 为准，防止伪造请求头绕过 IP 白名单、限流与验证码绑定
        if (!isTrustedProxy(StringUtils.defaultString(remoteAddr))) {
            return StringUtils.isBlank(remoteAddr) ? null : remoteAddr.trim();
        }
        String clientIp = pickClientIpBehindTrustedProxy(request);
        return StringUtils.isBlank(clientIp) ? (StringUtils.isBlank(remoteAddr) ? null : remoteAddr.trim()) : clientIp;
    }

    /**
     * TCP 对端为可信代理时，从转发头中还原真实客户端 IP。
     * <p>X-Forwarded-For 是追加式头部：客户端伪造值恒在链首，每经过一层代理在链尾追加一个它观测到的地址，
     * 因此从链右侧（可信代理追加的一侧）回溯，跳过可信代理地址，取第一个非可信地址。</p>
     */
    private static String pickClientIpBehindTrustedProxy(HttpServletRequest request) {
        String chain = joinHeaderValues(request, XFF_HEADER);
        if (!IP_PREDICATE.test(chain)) {
            String[] hops = splitTrim(chain, ",");
            for (int i = hops.length - 1; i >= 0; i--) {
                String hop = hops[i].trim();
                if (!IP_PREDICATE.test(hop) && isValidIp(hop) && !isTrustedProxy(hop)) {
                    return hop;
                }
            }
        }
        // X-Real-IP：由反向代理整头覆盖写入的单值头，仅在可信对端场景下采信
        String realIp = request.getHeader(X_REAL_IP_HEADER);
        if (!IP_PREDICATE.test(realIp)) {
            realIp = realIp.trim();
            if (isValidIp(realIp) && !isTrustedProxy(realIp)) {
                return realIp;
            }
        }
        return null;
    }

    @Nullable
    private static String joinHeaderValues(HttpServletRequest request, String name) {
        Enumeration<String> values = request.getHeaders(name);
        if (values == null || !values.hasMoreElements()) {
            return null;
        }
        return String.join(",", Collections.list(values));
    }

    /**
     * 是否可信反向代理：回环地址，或 system.trusted-proxies 配置的精确 IP / IPv4 CIDR。
     */
    static boolean isTrustedProxy(String ip) {
        return isTrustedProxy(ip, trustedProxyConfig());
    }

    static boolean isTrustedProxy(String ip, List<String> trustedProxies) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        String candidate = ip.trim();
        if (LOOPBACK_ADDRESSES.contains(candidate)) {
            return true;
        }
        for (String entry : trustedProxies) {
            if (StringUtils.isBlank(entry)) {
                continue;
            }
            String trimmed = entry.trim();
            int slash = trimmed.indexOf('/');
            if (slash < 0) {
                if (trimmed.equalsIgnoreCase(candidate)) {
                    return true;
                }
            } else if (ipv4InCidr(candidate, trimmed.substring(0, slash), trimmed.substring(slash + 1))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> trustedProxyConfig() {
        try {
            SystemProperties props = SpringUtil.getBean(SystemProperties.class);
            if (props != null && props.getTrustedProxies() != null) {
                return props.getTrustedProxies();
            }
        } catch (Exception ignored) {
            // 非 Spring 环境或上下文未就绪：退回默认（仅回环可信）
        }
        return List.of();
    }

    static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        if (parseIpv4Long(ip) != null) {
            return true;
        }
        // 粗粒度 IPv6 校验（含 ::ffff:a.b.c.d 映射形式），用于排除客户端注入的任意字符串
        return ip.indexOf(':') >= 0 && IPV6_LIKE_PATTERN.matcher(ip).matches();
    }

    private static boolean ipv4InCidr(String ip, String network, String prefixText) {
        Long address = parseIpv4Long(ip);
        Long networkAddress = parseIpv4Long(network);
        if (address == null || networkAddress == null || prefixText.isEmpty() || prefixText.length() > 2) {
            return false;
        }
        int prefix;
        try {
            prefix = Integer.parseInt(prefixText);
        } catch (NumberFormatException e) {
            return false;
        }
        if (prefix < 0 || prefix > 32) {
            return false;
        }
        long mask = prefix == 0 ? 0L : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
        return (address & mask) == (networkAddress & mask);
    }

    @Nullable
    private static Long parseIpv4Long(String ip) {
        String[] octets = ip.split("\\.", -1);
        if (octets.length != 4) {
            return null;
        }
        long value = 0L;
        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3) {
                return null;
            }
            for (int i = 0; i < octet.length(); i++) {
                char c = octet.charAt(i);
                if (c < '0' || c > '9') {
                    return null;
                }
            }
            int number = Integer.parseInt(octet);
            if (number > 255) {
                return null;
            }
            value = (value << 8) | number;
        }
        return value;
    }

    public static String[] splitTrim(String splitStr, String regex) {
        return splitStr.trim().split(regex);
    }


    @Nullable
    public static String getHeader(String name) {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getHeader(name);
    }

    public static String getTokenValue() {
        String authorization = getHeader("Authorization");
        if (authorization == null || authorization.length() < 9) return null;
        return authorization.substring(7);
    }

    public static String getTokenValue(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.length() < 9) return null;
        return authorization.substring(7);
    }

    @Nullable
    public static Enumeration<String> getHeaders(String name) {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getHeaders(name);
    }

    @Nullable
    public static Enumeration<String> getHeaderNames() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getHeaderNames();
    }

    @Nullable
    public static String getParameter(String name) {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getParameter(name);
    }


}
