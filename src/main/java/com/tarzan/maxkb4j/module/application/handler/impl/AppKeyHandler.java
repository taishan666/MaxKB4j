package com.tarzan.maxkb4j.module.application.handler.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.common.util.WebUtil;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationApiKeyEntity;
import com.tarzan.maxkb4j.module.application.enums.ChatUserType;
import com.tarzan.maxkb4j.module.application.handler.AuthHandler;
import com.tarzan.maxkb4j.module.application.service.ApplicationApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class AppKeyHandler implements AuthHandler {

    private final ApplicationApiKeyService apiKeyService;

    @Override
    public boolean handle(HttpServletResponse response) {
        String tokenValue = WebUtil.getTokenValue();
        String secretKey = tokenValue.replace(ChatUserType.APPLICATION_API_KEY.name() + "-", "");
        if (StrUtil.isBlank(secretKey)){
            throw new ApiException("token不合法");
        }
        ApplicationApiKeyEntity apiKey = apiKeyService.getBySecretKey(secretKey);
        if (apiKey==null || !apiKey.getIsActive()){
            throw new ApiException("token不合法或被禁用");
        }
        if (apiKey.getAllowCrossDomain() && CollUtil.isNotEmpty(apiKey.getCrossDomainList())){
            // 设置跨域
            String domains = String.join(",", apiKey.getCrossDomainList());
            response.setHeader("Access-Control-Allow-Origin", domains);
            response.setHeader("Access-Control-Allow-Methods", "POST,GET,PUT,DELETE");
            response.setHeader("Access-Control-Max-Age", "3600");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        return true;
    }

    @Override
    public boolean support(HttpServletRequest request) {
        String tokenValue = WebUtil.getTokenValue();
      //  StpUtil.setTokenValue(tokenValue);
        return tokenValue.startsWith(ChatUserType.APPLICATION_API_KEY.name());
    }
}
