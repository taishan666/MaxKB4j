package com.maxkb4j.knowledge.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.InetSocketAddress;
import java.net.Proxy;


/**
 * Jsoup工具类 - Web页面抓取
 *
 * @author tarzan
 */
@Slf4j
public class JsoupUtil {

    private static final int TIMEOUT = 15000; // 15秒超时

    /**
     * 抓取URL对应的HTML文档
     */
    public static Document getDocument(String url) {
        String html = HttpRequest.get(url)
                .timeout(TIMEOUT)
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Accept-Encoding", "gzip, deflate")
                .execute()
                .body();
        return Jsoup.parse(html);
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
