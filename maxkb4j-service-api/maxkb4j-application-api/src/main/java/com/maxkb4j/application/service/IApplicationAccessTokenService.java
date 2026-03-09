package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.entity.ApplicationAccessTokenEntity;

public interface IApplicationAccessTokenService extends IService<ApplicationAccessTokenEntity> {
    ApplicationAccessTokenEntity getByAccessToken(String accessToken);
}
