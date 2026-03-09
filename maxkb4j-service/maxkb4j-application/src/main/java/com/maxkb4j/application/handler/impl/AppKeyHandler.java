package com.maxkb4j.application.handler.impl;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.maxkb4j.application.entity.ApplicationApiKeyEntity;
import com.maxkb4j.application.service.ApplicationApiKeyService;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.util.ResponseProvider;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.common.util.WebUtil;
import com.maxkb4j.core.handler.AuthHandler;
import com.maxkb4j.system.enums.ChatUserType;
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

    private final ApplicationApiKeyService apiKeyService;

    @Override
    public boolean handle(HttpServletResponse response) {
        String secretKey = WebUtil.getTokenValue();
        if (StrUtil.isBlank(secretKey)){
            log.warn("token不存在");
            ResponseProvider.write( response);
            return false;
        }
        ApplicationApiKeyEntity apiKey = apiKeyService.getBySecretKey(secretKey);
        if (apiKey==null || !apiKey.getIsActive()){
            log.warn("token不合法或被禁用");
            ResponseProvider.write(response);
            return false;
        }
        SaLoginModel loginModel = new SaLoginModel();
        loginModel.setExtra("applicationId", apiKey.getApplicationId());
        loginModel.setExtra("chatUserType", ChatUserType.APPLICATION_API_KEY.name());
        String secretKeyId = secretKey.replace(AppConst.APP_KEY_PREFIX, "");
        StpKit.USER.login(secretKeyId,loginModel);
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
        String tokenValue = WebUtil.getTokenValue(request);
        return Objects.nonNull(tokenValue)&&tokenValue.startsWith(AppConst.APP_KEY_PREFIX);
    }
}
