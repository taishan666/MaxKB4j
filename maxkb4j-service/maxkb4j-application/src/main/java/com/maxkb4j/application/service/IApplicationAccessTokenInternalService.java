package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.dto.ApplicationAccessTokenDTO;
import com.maxkb4j.application.entity.ApplicationAccessTokenEntity;

/**
 * 应用访问令牌服务「对内」接口。
 */
public interface IApplicationAccessTokenInternalService extends IApplicationAccessTokenService, IService<ApplicationAccessTokenEntity> {

    ApplicationAccessTokenEntity accessToken(String appId);

    ApplicationAccessTokenEntity updateAccessToken(String appId, ApplicationAccessTokenDTO dto);
}