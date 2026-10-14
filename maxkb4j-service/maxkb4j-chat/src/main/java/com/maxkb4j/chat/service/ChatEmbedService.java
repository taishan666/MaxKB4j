package com.maxkb4j.chat.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.application.dto.ApplicationAccessTokenDTO;
import com.maxkb4j.application.service.IApplicationAccessTokenService;
import com.maxkb4j.chat.query.EmbedQuery;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.IoUtil;
import com.maxkb4j.common.util.WebUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 应用嵌入脚本（embed）渲染逻辑。
 *
 * @author tarzan
 */
@RequiredArgsConstructor
@Service
public class ChatEmbedService {

    /**
     * 允许的协议白名单（嵌入脚本会直接拼接进 iframe src 与 JS 字符串）
     */
    private static final Pattern SAFE_PROTOCOL = Pattern.compile("^https?$", Pattern.CASE_INSENSITIVE);

    /**
     * 允许的主机格式：域名 / IPv4 / 带端口号 / IPv6（方括号），禁止引号、空格、HTML 特殊字符
     */
    private static final Pattern SAFE_HOST = Pattern.compile("^[A-Za-z0-9.\\-]+(:\\d{1,5})?$|^\\[[0-9A-Fa-f:.]+\\](:\\d{1,5})?$");

    private final IApplicationAccessTokenService accessTokenService;

    public String embed(String protocol, String host, String token, Map<String, Object> params) {
        // 移除固定参数，剩下的就是不固定参数
        params.remove("protocol");
        params.remove("host");
        params.remove("token");
        return embed(new EmbedQuery(protocol, host, token, params));
    }

    public String embed(EmbedQuery query) {
        ApplicationAccessTokenDTO token = accessTokenService.getByAccessToken(query.getToken());
        if (token == null || Boolean.FALSE.equals(token.getIsActive())) {
            throw new ApiException("application.token.invalid.or.disabled");
        }
        List<String> whiteList = token.getWhiteList();
        if (Boolean.TRUE.equals(token.getWhiteActive()) && (whiteList == null || !whiteList.contains(WebUtil.getIP()))) {
            throw new ApiException("application.access.white.list.required");
        }
        // 嵌入脚本模板中的值会进入 JS 字符串字面量与 iframe src，
        // 必须白名单校验，防止逃逸引号注入脚本（XSS）
        validateEmbedParams(query.getProtocol(), query.getHost());
        // 校验通过后再打开资源流，避免校验失败时流泄漏（readToString 内部会关闭流）
        String content = IoUtil.readToString(getClass().getClassLoader().getResourceAsStream("template/embed.txt"), StandardCharsets.UTF_8);
        return render(content, getParamsMap(token, query));
    }

    private void validateEmbedParams(String protocol, String host) {
        if (protocol == null || !SAFE_PROTOCOL.matcher(protocol).matches()) {
            throw new ApiException("common.illegal.argument");
        }
        if (host == null || !SAFE_HOST.matcher(host).matches()) {
            throw new ApiException("common.illegal.argument");
        }
    }

    private Map<String, String> getParamsMap(ApplicationAccessTokenDTO token, EmbedQuery query) {
        String floatIcon = query.getProtocol() + "://" + query.getHost() + "/chat/MaxKB.gif";
        List<String> whiteList = token.getWhiteList();
        Map<String, String> map = new HashMap<>();
        Boolean isAuth = token.getIsActive();
        map.put("is_auth", String.valueOf(isAuth));
        map.put("protocol", query.getProtocol());
        map.put("host", query.getHost());
        // 模板中以 JS 单引号字符串字面量插值，转义反斜杠与单引号防止逃逸
        map.put("token", escapeJsString(query.getToken()));
        map.put("white_list_str", escapeJsString(whiteList == null ? "" : String.join(",", whiteList)));
        map.put("white_active", String.valueOf(Boolean.TRUE.equals(token.getWhiteActive())));
        map.put("is_draggable", "false");
        map.put("float_icon", floatIcon);
        map.put("prefix", "/chat");
        String queryStr = getQueryApiInput(query.getParams());
        map.put("query", queryStr);
        map.put("show_guide", "true");
        map.put("x_type", "right");
        map.put("x_value", "0");
        map.put("y_type", "bottom");
        map.put("y_value", "30");
        map.put("max_kb_id", IdWorker.get32UUID());
        map.put("header_font_color", "rgb(100, 106, 115)");
        return map;
    }

    /**
     * 转义 JS 单引号字符串字面量上下文中的特殊字符。
     */
    private static String escapeJsString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'")
                .replace("\r", "").replace("\n", "");
    }

    /**
     * 将用户自定义参数拼成 URL 查询串，key/value 均 URL 编码，
     * 防止特殊字符破坏 iframe src 或注入脚本。
     */
    public String getQueryApiInput(Map<String, Object> params) {
        StringBuilder query = new StringBuilder();
        params.forEach((key, value) -> query.append("&")
                .append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                .append("=")
                .append(URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8)));
        return query.toString();
    }

    private String render(String content, Map<String, String> variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return content;
    }
}
