package com.tarzan.maxkb4j.core.chat.provider;

import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.core.chat.service.IChatService;
import com.tarzan.maxkb4j.core.chat.service.impl.ChatFlowServiceImpl;
import com.tarzan.maxkb4j.core.chat.service.impl.ChatSimpleServiceImpl;
import com.tarzan.maxkb4j.module.application.enums.AppType;
import com.tarzan.maxkb4j.common.util.SpringUtil;

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
    public static IChatService getActuator(String appType) {
        IChatService chatActuator = ACTUATOR_POOL.get(appType);
        if (chatActuator == null) {
            throw new ApiException("no appType was found");
        } else {
            return ACTUATOR_POOL.get(appType);
        }
    }
}
