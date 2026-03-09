package com.maxkb4j.core.chat.provider;


import com.maxkb4j.application.enums.AppType;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.SpringUtil;
import com.maxkb4j.core.chat.service.IChatService;
import com.maxkb4j.core.chat.service.impl.ChatFlowServiceImpl;
import com.maxkb4j.core.chat.service.impl.ChatSimpleServiceImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServiceBuilder {

    private static final Map<String, IChatService> ACTUATOR_POOL = new ConcurrentHashMap<>();

    static {
        ACTUATOR_POOL.put(AppType.SIMPLE.name(), SpringUtil.getBean(ChatSimpleServiceImpl.class));
        ACTUATOR_POOL.put(AppType.WORK_FLOW.name(), SpringUtil.getBean(ChatFlowServiceImpl.class));
    }

    /**
     * 获取ChatActuator
     *
     * @param appType 应用类型
     * @return IChatActuator
     */
    public static IChatService getChatService(String appType) {
        IChatService chatActuator = ACTUATOR_POOL.get(appType);
        if (chatActuator == null) {
            throw new ApiException("no appType was found");
        } else {
            return ACTUATOR_POOL.get(appType);
        }
    }
}
