package com.tarzan.maxkb4j.module.application.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationAccessTokenEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationAccessTokenMapper;
import org.springframework.stereotype.Service;

/**
 * @author tarzan
 * @date 2024-12-25 18:05:58
 */
@Service
public class ApplicationAccessTokenService extends ServiceImpl<ApplicationAccessTokenMapper, ApplicationAccessTokenEntity>{
    public ApplicationAccessTokenEntity accessToken(String appId) {
        return  this.lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId).one();
    }

    public ApplicationAccessTokenEntity getByToken(String accessToken) {
        return this.lambdaQuery().eq(ApplicationAccessTokenEntity::getAccessToken, accessToken).one();
    }
}
