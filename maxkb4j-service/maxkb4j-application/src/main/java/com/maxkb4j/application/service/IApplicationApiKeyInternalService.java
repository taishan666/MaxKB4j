package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.entity.ApplicationApiKeyEntity;

import java.util.List;

/**
 * 应用 API Key 服务「对内」接口。
 */
public interface IApplicationApiKeyInternalService extends IApplicationApiKeyService, IService<ApplicationApiKeyEntity> {

    List<ApplicationApiKeyEntity> listApikey(String appId);

    Boolean createApikey(String appId);

    Boolean updateApikey(String appId, String apiKeyId, ApplicationApiKeyEntity apiKeyEntity);

    Boolean deleteApikey(String appId, String apiKeyId);
}