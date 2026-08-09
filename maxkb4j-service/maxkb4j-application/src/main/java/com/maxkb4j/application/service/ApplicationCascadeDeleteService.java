package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.application.entity.ApplicationAccessTokenEntity;
import com.maxkb4j.application.entity.ApplicationApiKeyEntity;
import com.maxkb4j.application.entity.ApplicationChatShareLinkEntity;
import com.maxkb4j.application.entity.ApplicationChatUserStatsEntity;
import com.maxkb4j.application.entity.ApplicationLongTermMemoryEntity;
import com.maxkb4j.application.entity.ApplicationVersionEntity;
import com.maxkb4j.application.service.impl.ApplicationServiceImpl;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 应用关联资源级联清理，从 {@link ApplicationServiceImpl} 抽离。
 * 删除应用时负责清理访问令牌、API Key、会话统计、版本、权限、资源映射、分享链接与长期记忆。
 *
 * @author tarzan
 */
@Service
@RequiredArgsConstructor
public class ApplicationCascadeDeleteService {

    private final IApplicationAccessTokenInternalService accessTokenService;
    private final IApplicationApiKeyInternalService applicationApiKeyService;
    private final ApplicationChatUserStatsService chatUserStatsService;
    private final ApplicationVersionService applicationVersionService;
    private final IUserResourcePermissionService userResourcePermissionService;
    private final ApplicationResourceMappingService applicationResourceMappingService;
    private final ApplicationChatShareLinkService applicationChatShareLinkService;
    private final IApplicationLongTermMemoryService applicationLongTermMemoryService;

    /**
     * 删除应用关联的全部资源（不包括应用本身）。
     */
    public void deleteRelatedResources(String appId) {
        accessTokenService.remove(Wrappers.<ApplicationAccessTokenEntity>lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId));
        applicationApiKeyService.remove(Wrappers.<ApplicationApiKeyEntity>lambdaQuery().eq(ApplicationApiKeyEntity::getApplicationId, appId));
        chatUserStatsService.remove(Wrappers.<ApplicationChatUserStatsEntity>lambdaQuery().eq(ApplicationChatUserStatsEntity::getApplicationId, appId));
        applicationVersionService.remove(Wrappers.<ApplicationVersionEntity>lambdaQuery().eq(ApplicationVersionEntity::getApplicationId, appId));
        userResourcePermissionService.remove(AuthTargetType.APPLICATION, appId);
        applicationResourceMappingService.deleteResourceMappings(appId);
        applicationChatShareLinkService.remove(Wrappers.<ApplicationChatShareLinkEntity>lambdaQuery().eq(ApplicationChatShareLinkEntity::getApplicationId, appId));
        applicationLongTermMemoryService.remove(Wrappers.<ApplicationLongTermMemoryEntity>lambdaQuery().eq(ApplicationLongTermMemoryEntity::getApplicationId, appId));
    }
}
