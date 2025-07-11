package com.tarzan.maxkb4j.module.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationApiKeyEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationApiKeyMapper;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2025-01-02 09:01:12
 */
@Service
public class ApplicationApiKeyService extends ServiceImpl<ApplicationApiKeyMapper, ApplicationApiKeyEntity>{

    public List<ApplicationApiKeyEntity> listApikey(String appId) {
        return this.lambdaQuery().eq(ApplicationApiKeyEntity::getApplicationId, appId).list();
    }

    public Boolean createApikey(String appId) {
        ApplicationApiKeyEntity entity = new ApplicationApiKeyEntity();
        entity.setApplicationId(appId);
        entity.setIsActive(true);
        entity.setAllowCrossDomain(false);
        String uuid = UUID.randomUUID().toString();
        entity.setSecretKey("maxKb4j-" + uuid.replaceAll("-", ""));
        entity.setUserId(StpUtil.getLoginIdAsString());
        entity.setCrossDomainList(new HashSet<>());
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
