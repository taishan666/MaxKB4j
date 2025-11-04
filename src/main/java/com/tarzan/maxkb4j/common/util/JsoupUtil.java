package com.tarzan.maxkb4j.common.util;

import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.util.StringUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Objects;

/**
 * @author tarzan
 */
@Slf4j
public class JsoupUtil {

    public static Document getDocument(String url){
        String html=HttpUtil.get(url);
        return Jsoup.parse(html);
    }

    public static String getTitle(Document doc){
        String title=doc.title();
        if(StringUtil.isEmpty(title)){
            Element titleEle=doc.selectFirst("title");
            if(Objects.nonNull(titleEle)){
                title= titleEle.text();
            }else {
                titleEle=doc.selectFirst("[property=og:title]");
                title=titleEle==null?"":titleEle.attr("content");
            }
        }
        return title;
    }

}
