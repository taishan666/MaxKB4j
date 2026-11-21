package com.maxkb4j.knowledge.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;


/**
 * Jsoup工具类 - Web页面抓取
 *
 * @author tarzan
 */
@Slf4j
public class JsoupUtil {

    private static final int TIMEOUT = 15000; // 15秒超时
    private static final int MAX_REDIRECTS = 5;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    /**
     * 抓取URL对应的HTML文档
     */
    public static Document getDocument(String url) {
        URI currentUri = validateUrl(url);
        for (int redirectCount = 0; ; redirectCount++) {
            try (HttpResponse response = HttpRequest.get(currentUri.toString())
                    .timeout(TIMEOUT)
                    .setFollowRedirects(false)
                    .header("User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate")
                    .execute()) {
                if (!isRedirect(response.getStatus())) {
                    return Jsoup.parse(response.body());
                }
                if (redirectCount >= MAX_REDIRECTS) {
                    throw new IllegalArgumentException("URL重定向次数超过限制");
                }
                currentUri = resolveRedirect(currentUri, response.header("Location"));
            }
        }
    }

    /**
     * 仅允许解析结果全部为公网地址的 HTTP(S) URL，防止访问内网及云元数据服务。
     */
    static URI validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL不能为空");
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL格式无效", e);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("仅允许HTTP/HTTPS URL");
        }
        if (uri.getHost() == null || uri.getHost().isBlank() || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL主机无效");
        }
        String host = uri.getHost();
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                throw new IllegalArgumentException("URL主机解析失败");
            }
            for (InetAddress address : addresses) {
                if (!isPublicAddress(address)) {
                    throw new IllegalArgumentException("禁止访问内网、回环、链路本地或保留地址");
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("URL主机解析失败", e);
        }
        return uri;
    }

    static URI resolveRedirect(URI currentUri, String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("重定向缺少Location");
        }
        try {
            return validateUrl(currentUri.resolve(location.trim()).toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("重定向目标不安全或无效", e);
        }
    }

    private static boolean isRedirect(int status) {
        return status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_SEE_OTHER
                || status == 307
                || status == 308;
    }

    static boolean isPublicAddress(InetAddress address) {
        if (address instanceof Inet4Address ipv4) {
            return isPublicIpv4(ipv4.getAddress());
        }
        if (address instanceof Inet6Address ipv6) {
            byte[] bytes = ipv6.getAddress();
            boolean ipv4Mapped = true;
            for (int i = 0; i < 12; i++) {
                if (i < 10 ? bytes[i] != 0 : (bytes[i] & 0xFF) != 0xFF) {
                    ipv4Mapped = false;
                    break;
                }
            }
            if (ipv4Mapped) {
                return isPublicIpv4(new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]});
            }
            if (ipv6.isAnyLocalAddress() || ipv6.isLoopbackAddress()
                    || ipv6.isLinkLocalAddress() || ipv6.isSiteLocalAddress()
                    || ipv6.isMulticastAddress()) {
                return false;
            }
            int first = bytes[0] & 0xFF;
            return first < 0xFC || first > 0xFD;
        }
        return false;
    }

    private static boolean isPublicIpv4(byte[] octets) {
        int o0 = octets[0] & 0xFF;
        int o1 = octets[1] & 0xFF;
        int o2 = octets[2] & 0xFF;
        return !(o0 == 0
                || o0 == 10
                || (o0 == 100 && o1 >= 64 && o1 <= 127)
                || o0 == 127
                || (o0 == 169 && o1 == 254)
                || (o0 == 172 && o1 >= 16 && o1 <= 31)
                || (o0 == 192 && o1 == 168)
                || (o0 == 192 && o1 == 0 && o2 == 0)
                || (o0 == 192 && o1 == 0 && o2 == 2)
                || (o0 == 192 && o1 == 88 && o2 == 99)
                || (o0 == 198 && (o1 == 18 || o1 == 19))
                || (o0 == 198 && o1 == 51 && o2 == 100)
                || (o0 == 203 && o1 == 0 && o2 == 113)
                || o0 >= 224);
    }

    /**
     * 抓取并清洗URL对应的HTML文档（去除广告、脚本等噪音）
     */
    public static Document getCleanDocument(String url) {
        Document doc = getDocument(url);
        WebContentCleaner.clean(doc);
        return doc;
    }

}
