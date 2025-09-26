package com.tarzan.maxkb4j.common.util;

import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;

import javax.net.ssl.HostnameVerifier;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

/**
 * @author tarzan
 */
@Slf4j
public class JsoupUtil {

    /**
     *
     * @param url 访问路径
     * @return
     */
/*    public static Document getDocument (String url){
        try {
            HttpsUrlValidator.trustAllHttpsCertificates();
            //5000是设置连接超时时间，单位ms
            return Jsoup.connect(url)
                  .header("Accept-Language", "zh-CN,zh;q=0.8")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36")
                  .timeout(10*1000).get();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }*/

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

    public static String getDescription(Document doc){
        Element descriptionEle=doc.selectFirst("[name=description]");
        String desc="";
        if(Objects.nonNull(descriptionEle)){
            desc= descriptionEle.attr("content");
        }else{
            descriptionEle=doc.selectFirst("[property=og:description]");
            if(Objects.nonNull(descriptionEle)){
                desc= descriptionEle.attr("content");
            }
        }
        return desc;
    }

    public static String getWebIcon(Document doc,String webUrl){
        String url = StringUtils.appendIfMissing(webUrl, "/");
        String iconUrl=getDomain(url)+"favicon.ico";
        Element iconEle=doc.selectFirst("[rel=icon]");
        Element shortIconEle=doc.selectFirst("[rel=shortcut icon]");
        String iconHref=getIconHref(url,iconEle);
        String shortIconHref=getIconHref(url,shortIconEle);
        String result1=checkImageExist(iconHref);
        if(result1!=null){
            return iconHref+","+result1;
        }
        String result2=checkImageExist(shortIconHref);
        if(result2!=null){
            return shortIconHref+","+result2;
        }
        String result3=checkImageExist(iconUrl);
        if(result3!=null){
            return iconUrl+","+result3;
        }
        return null;
    }

    public static String getDomain(String url){
        int fromIndex=url.indexOf(".");
        int endIndex=url.indexOf("/",fromIndex);
        return url.substring(0,endIndex+1);
    }

    private static String getIconHref(String url,Element iconEle){
        if(Objects.nonNull(iconEle)){
            String iconHref= iconEle.attr("href");
            if(iconHref.startsWith("http")||iconHref.startsWith("https")){
                return iconHref;
            }else {
                if(iconHref.startsWith("//")){
                    return url.split(":")[0]+":"+iconHref;
                }if(iconHref.startsWith("/")){
                    return getDomain(url)+iconHref.substring(1);
                }else {
                    return getDomain(url)+iconHref;
                }
            }
        }else {
            return null;
        }
    }

    private static HttpURLConnection getConnection(String httpUrl){
        try {
            URL url=new URL(httpUrl);
            URLConnection urlConnection = url.openConnection();
            //设置读取超时
            urlConnection.setReadTimeout(1000 * 60);
            urlConnection.setRequestProperty("Accept", "*/*");
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            urlConnection.setRequestProperty("Accept-Language", "zh-cn");
            //不进行持久化连接
            urlConnection.setRequestProperty("Connection", "close");
            return (HttpURLConnection) urlConnection;
        } catch (IOException e) {
            log.error(e.getMessage());
           return null;
        }

    }

    public static String checkImageExist(String src){
        try {
            HttpURLConnection connection= getConnection(src);
            if(Objects.nonNull(connection)){
                int code=connection.getResponseCode();
                String contentType=connection.getContentType();
                if(code== HttpStatus.OK.value() &&contentType.startsWith("image")){
                    return contentType;
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    static HostnameVerifier hv = (urlHostName, session) -> {
        log.info("Warning: URL Host: {} vs. {}", urlHostName, session.getPeerHost());
        return true;
    };



}
