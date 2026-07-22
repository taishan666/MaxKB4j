package com.maxkb4j.application.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.dto.ApplicationAccessTokenDTO;
import com.maxkb4j.application.entity.ApplicationAccessTokenEntity;
import com.maxkb4j.application.mapper.ApplicationAccessTokenMapper;
import com.maxkb4j.application.service.IApplicationAccessTokenInternalService;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.MD5Util;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 18:05:58
 */
@Service
public class ApplicationAccessTokenServiceImpl extends ServiceImpl<ApplicationAccessTokenMapper, ApplicationAccessTokenEntity> implements IApplicationAccessTokenInternalService {
    public ApplicationAccessTokenEntity accessToken(String appId) {
        return  this.lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId).one();
    }

    public ApplicationAccessTokenEntity updateAccessToken(String appId, ApplicationAccessTokenDTO dto) {
        dto.setApplicationId(appId);
        if (dto.getAccessTokenReset() != null && dto.getAccessTokenReset()) {
            dto.setAccessToken(MD5Util.encrypt(UUID.randomUUID().toString(), 8, 24));
        }
        this.updateById(BeanUtil.copy(dto, ApplicationAccessTokenEntity.class));
        return this.getById(appId);
    }

    public ApplicationAccessTokenDTO getByAccessToken(String accessToken) {
        ApplicationAccessTokenEntity entity = this.lambdaQuery().eq(ApplicationAccessTokenEntity::getAccessToken, accessToken).eq(ApplicationAccessTokenEntity::getIsActive, true).one();
        return BeanUtil.copy(entity, ApplicationAccessTokenDTO.class);
    }

    @Override
    public ApplicationAccessTokenDTO getByAppId(String appId) {
        ApplicationAccessTokenEntity entity =this.lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId).one();
        return BeanUtil.copy(entity, ApplicationAccessTokenDTO.class);
    }
}
