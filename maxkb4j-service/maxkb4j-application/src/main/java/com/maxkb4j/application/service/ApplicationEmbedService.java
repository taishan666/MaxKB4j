package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.application.dto.EmbedDTO;
import com.maxkb4j.application.entity.ApplicationAccessTokenEntity;
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
 * 应用嵌入脚本（embed）渲染逻辑，从 {@link ApplicationService} 抽离。
 *
 * @author tarzan
 */
@RequiredArgsConstructor
@Service
public class ApplicationEmbedService implements IApplicationEmbedService{

    private final ApplicationAccessTokenService accessTokenService;

    @Override
    public String embed(EmbedDTO dto) {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("templates/embed.txt");
        ApplicationAccessTokenEntity token = accessTokenService.getByAccessToken(dto.getToken());
        if (token == null || !token.getIsActive()) {
            throw new ApiException("application.token.invalid.or.disabled");
        }
        List<String> whiteList = token.getWhiteList();
        if (token.getWhiteActive() && !whiteList.contains(WebUtil.getIP())) {
            throw new ApiException("application.access.white.list.required");
        }
        String content = IoUtil.readToString(inputStream, StandardCharsets.UTF_8);
        return render(content, getParamsMap(token, dto));
    }

    private Map<String, String> getParamsMap(ApplicationAccessTokenEntity token, EmbedDTO dto) {
        String floatIcon = dto.getProtocol() + "://" + dto.getHost() + "/chat/MaxKB.gif";
        List<String> whiteList = token.getWhiteList();
        Map<String, String> map = new HashMap<>();
        map.put("is_auth", String.valueOf(token.getIsActive()));
        map.put("protocol", dto.getProtocol());
        map.put("query", "");
        map.put("host", dto.getHost());
        map.put("token", dto.getToken());
        map.put("white_list_str", whiteList == null ? "" : String.join(",", whiteList));
        map.put("white_active", token.getWhiteActive().toString());
        map.put("float_icon", floatIcon);
        map.put("is_draggable", "false");
        map.put("show_guide", "false");
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
