package com.maxkb4j.chat.handler.impl;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.maxkb4j.application.dto.ApplicationApiKeyDTO;
import com.maxkb4j.application.service.IApplicationApiKeyService;
import com.maxkb4j.chat.handler.AuthHandler;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.chat.util.ResponseProvider;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.common.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Slf4j
@Component
@RequiredArgsConstructor
public class AppKeyHandler implements AuthHandler {

    private final IApplicationApiKeyService apiKeyService;

    @Override
    public boolean handle(HttpServletResponse response) {
        String secretKey = WebUtil.getTokenValue();
        if (StrUtil.isBlank(secretKey)) {
            log.warn("token不存在");
            ResponseProvider.write(response);
            return false;
        }
        ApplicationApiKeyDTO apiKey = apiKeyService.getBySecretKey(secretKey);
        if (apiKey == null || !apiKey.getIsActive()) {
            log.warn("token不合法或被禁用");
            ResponseProvider.write(response);
            return false;
        }

        SaLoginModel loginModel = new SaLoginModel();
        loginModel.setExtra("applicationId", apiKey.getApplicationId());
        loginModel.setExtra("chatUserType", ChatUserType.APPLICATION_API_KEY.name());
        String secretKeyId = secretKey.replace(AppConst.APP_KEY_PREFIX, "");
        StpKit.USER.login(secretKeyId, loginModel);
        if (Boolean.TRUE.equals(apiKey.getAllowCrossDomain()) && CollUtil.isNotEmpty(apiKey.getCrossDomainList())) {
            // ACAO 只能回显单个 Origin；多值逗号拼接会被浏览器直接判定跨域失败
            String origin = WebUtil.getHeader("Origin");
            if (StrUtil.isNotBlank(origin) && apiKey.getCrossDomainList().contains(origin)) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Vary", "Origin");
            }
        }
        return true;
    }

    @Override
    public boolean support(HttpServletRequest request) {
        String tokenValue = WebUtil.getTokenValue(request);
        return Objects.nonNull(tokenValue) && tokenValue.startsWith(AppConst.APP_KEY_PREFIX);
    }
}
