package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.entity.ApplicationApiKeyEntity;
import com.maxkb4j.application.mapper.ApplicationApiKeyMapper;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.util.StpKit;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author tarzan
 * @date 2025-01-02 09:01:12
 */
@Service
public class ApplicationApiKeyService extends ServiceImpl<ApplicationApiKeyMapper, ApplicationApiKeyEntity> implements IApplicationApiKeyService {

    public List<ApplicationApiKeyEntity> listApikey(String appId) {
        return this.lambdaQuery().eq(ApplicationApiKeyEntity::getApplicationId, appId).list();
    }

    public Boolean createApikey(String appId) {
        ApplicationApiKeyEntity entity = new ApplicationApiKeyEntity();
        entity.setApplicationId(appId);
        entity.setIsActive(true);
        entity.setAllowCrossDomain(false);
        entity.setSecretKey(AppConst.APP_KEY_PREFIX + IdWorker.get32UUID());
        entity.setUserId(StpKit.ADMIN.getLoginIdAsString());
        entity.setCrossDomainList(List.of());
        return this.save(entity);
    }

    public Boolean updateApikey(String appId, String apiKeyId, ApplicationApiKeyEntity apiKeyEntity) {
        apiKeyEntity.setId(apiKeyId);
        apiKeyEntity.setApplicationId(appId);
        return this.updateById(apiKeyEntity);
    }

    public Boolean deleteApikey(String appId, String apiKeyId) {
        return this.removeById(apiKeyId);
    }

    public ApplicationApiKeyEntity getBySecretKey(String secretKey) {
        return this.lambdaQuery().eq(ApplicationApiKeyEntity::getSecretKey, secretKey).one();
    }
}
