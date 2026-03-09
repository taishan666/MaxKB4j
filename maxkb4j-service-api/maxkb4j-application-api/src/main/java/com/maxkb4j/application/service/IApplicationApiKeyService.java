package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.entity.ApplicationApiKeyEntity;

public interface IApplicationApiKeyService extends IService<ApplicationApiKeyEntity> {
    ApplicationApiKeyEntity getBySecretKey(String secretKey);
}
