package com.tarzan.maxkb4j.module.application.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationAccessTokenMapper;
import com.tarzan.maxkb4j.module.application.entity.ApplicationAccessTokenEntity;

import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 18:05:58
 */
@Service
public class ApplicationAccessTokenService extends ServiceImpl<ApplicationAccessTokenMapper, ApplicationAccessTokenEntity>{
    public ApplicationAccessTokenEntity accessToken(UUID appId) {
        return  this.lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId).one();
    }
}
