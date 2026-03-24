package com.maxkb4j.knowledge.util;

import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


/**
 * @author tarzan
 */
@Slf4j
public class JsoupUtil {

    public static Document getDocument(String url){
        String html=HttpUtil.get(url);
        return Jsoup.parse(html);
    }


}
