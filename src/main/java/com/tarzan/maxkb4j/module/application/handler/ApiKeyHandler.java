package com.tarzan.maxkb4j.module.application.handler;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.tarzan.maxkb4j.core.exception.ApiException;
import com.tarzan.maxkb4j.core.handler.AuthHandler;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationApiKeyEntity;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.application.service.ApplicationApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class ApiKeyHandler implements AuthHandler {

    private final ApplicationApiKeyService apiKeyService;

    @Override
    public boolean handle(HttpServletResponse response) {
        String tokenValue = StpUtil.getStpLogic().getTokenValue(false);
        String secretKey = tokenValue.replace(AuthType.API_KEY.name() + "-", "");
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
        // 设置当前用户和权限

        return false;
    }

    @Override
    public boolean support(HttpServletRequest request) {
        return StpUtil.getStpLogic().getTokenValue(false).startsWith(AuthType.API_KEY.name());
    }
}
