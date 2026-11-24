package com.maxkb4j.application.builder;

import com.maxkb4j.application.enums.AppType;
import com.maxkb4j.application.service.IChatService;
import com.maxkb4j.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 按应用类型路由到对应 {@link IChatService} 实现的注册表。
 * <p>依赖通过构造器注入，不再使用静态初始化块 + 服务定位器查找 Bean。</p>
 */
@Component
public class ChatServiceBuilder {

    private final Map<String, IChatService> actuatorPool;

    public ChatServiceBuilder(
            @Qualifier("chatSimpleServiceImpl") IChatService simpleChatService,
            @Qualifier("chatFlowServiceImpl") IChatService flowChatService) {
        this.actuatorPool = Map.of(
                AppType.SIMPLE.name(), simpleChatService,
                AppType.WORK_FLOW.name(), flowChatService);
    }

    /**
     * 获取对应应用类型的 ChatActuator。
     *
     * @param appType 应用类型
     * @return IChatService
     */
    public IChatService getChatService(String appType) {
        IChatService chatActuator = actuatorPool.get(appType);
        if (chatActuator == null) {
            throw new ApiException("application.app.type.not.found");
        }
        return chatActuator;
    }
}
