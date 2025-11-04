package com.tarzan.maxkb4j.common.util;

import jakarta.servlet.ServletInputStream;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Objects;
import java.util.function.Predicate;

public class WebUtil extends WebUtils {
    private static final Logger log = LoggerFactory.getLogger(WebUtil.class);
    public static final String USER_AGENT_HEADER = "user-agent";
    private static final String[] IP_HEADER_NAMES = new String[]{"x-forwarded-for", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
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
            out.append(JsonUtil.toJson(result));
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
        } else {
            String ip = null;
            String[] var2 = IP_HEADER_NAMES;

            for (String ipHeader : var2) {
                ip = request.getHeader(ipHeader);
                if (!IP_PREDICATE.test(ip)) {
                    break;
                }
            }

            if (IP_PREDICATE.test(ip)) {
                ip = request.getRemoteAddr();
            }

            return StringUtils.isBlank(ip) ? null : splitTrim(ip, ",")[0];
        }
    }

    public static String[] splitTrim(String splitStr, String regex) {
        return splitStr.trim().split(regex);
    }


    public static String getHeader(String name) {
        HttpServletRequest request = getRequest();
        return Objects.requireNonNull(request).getHeader(name);
    }

    public static String getTokenValue() {
        String authorization=getHeader("Authorization");
        return  authorization.substring(7);
    }

    public static String getTokenValue(HttpServletRequest request) {
        String authorization=request.getHeader("Authorization");
        return  authorization.substring(7);
    }

    public static Enumeration<String> getHeaders(String name) {
        HttpServletRequest request = getRequest();
        return Objects.requireNonNull(request).getHeaders(name);
    }

    public static Enumeration<String> getHeaderNames() {
        HttpServletRequest request = getRequest();
        return Objects.requireNonNull(request).getHeaderNames();
    }

    public static String getParameter(String name) {
        HttpServletRequest request = getRequest();
        return Objects.requireNonNull(request).getParameter(name);
    }

    public static String getRequestBody(ServletInputStream servletInputStream) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new InputStreamReader(servletInputStream, StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException var16) {
            var16.printStackTrace();
        } finally {
            if (servletInputStream != null) {
                try {
                    servletInputStream.close();
                } catch (IOException var15) {
                    var15.printStackTrace();
                }
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException var14) {
                    var14.printStackTrace();
                }
            }

        }

        return sb.toString();
    }

    public static String getRequestContent(HttpServletRequest request) {
        try {
            String queryString = request.getQueryString();
            if (StringUtils.isNotBlank(queryString)) {
                return (new String(queryString.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)).replaceAll("&amp;", "&").replaceAll("%22", "\"");
            } else {
                String charEncoding = request.getCharacterEncoding();
                if (charEncoding == null) {
                    charEncoding = "UTF-8";
                }

                byte[] buffer = getRequestBody(request.getInputStream()).getBytes();
                String str = (new String(buffer, charEncoding)).trim();
                if (StringUtils.isBlank(str)) {
                    StringBuilder sb = new StringBuilder();
                    Enumeration<?> parameterNames = request.getParameterNames();

                    while (parameterNames.hasMoreElements()) {
                        String key = (String) parameterNames.nextElement();
                        String value = request.getParameter(key);
                        StringUtil.appendBuilder(sb, key, "=", value, "&");
                    }

                    str = StringUtil.removeSuffix(sb.toString(), "&");
                }

                return str.replaceAll("&amp;", "&");
            }
        } catch (Exception var9) {
            var9.printStackTrace();
            return "";
        }
    }


}
