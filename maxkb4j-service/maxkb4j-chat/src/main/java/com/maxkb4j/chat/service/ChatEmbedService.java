package com.maxkb4j.chat.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.application.entity.ApplicationAccessTokenEntity;
import com.maxkb4j.application.service.IApplicationAccessTokenService;
import com.maxkb4j.chat.query.EmbedQuery;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.IoUtil;
import com.maxkb4j.common.util.WebUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 应用嵌入脚本（embed）渲染逻辑。
 *
 * @author tarzan
 */
@RequiredArgsConstructor
@Service
public class ChatEmbedService {

    private final IApplicationAccessTokenService accessTokenService;

    public String embed(EmbedQuery query) {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("template/embed.txt");
        ApplicationAccessTokenEntity token = accessTokenService.getByAccessToken(query.getToken());
        if (token == null || !token.getIsActive()) {
            throw new ApiException("application.token.invalid.or.disabled");
        }
        List<String> whiteList = token.getWhiteList();
        if (token.getWhiteActive() && !whiteList.contains(WebUtil.getIP())) {
            throw new ApiException("application.access.white.list.required");
        }
        String content = IoUtil.readToString(inputStream, StandardCharsets.UTF_8);
        String result = render(content, getParamsMap(token, query));
        System.out.println(result);
        return result;
    }

    private Map<String, String> getParamsMap(ApplicationAccessTokenEntity token, EmbedQuery query) {
        String floatIcon = query.getProtocol() + "://" + query.getHost() + "/chat/MaxKB.gif";
        List<String> whiteList = token.getWhiteList();
        Map<String, String> map = new HashMap<>();
        Boolean isAuth=token.getIsActive();
        map.put("is_auth", String.valueOf(isAuth));
        map.put("protocol", query.getProtocol());
        map.put("host", query.getHost());
        map.put("token", query.getToken());
        map.put("white_list_str", whiteList == null ? "" : String.join(",", whiteList));
        map.put("white_active", token.getWhiteActive().toString());
        map.put("is_draggable", "false");
        map.put("float_icon", floatIcon);
        map.put("prefix", "/chat");
        String queryStr= JSON.toJSONString(query.getParams());
        map.put("query", "");
        map.put("show_guide", "true");
        map.put("x_type", "right");
        map.put("y_type", "bottom");
        map.put("x_value", "0");
        map.put("y_value", "30");
        map.put("max_kb_id", IdWorker.get32UUID());
        map.put("header_font_color", "rgb(100, 106, 115");
        return map;
    }

    private String render(String content, Map<String, String> variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return content;
    }
}
