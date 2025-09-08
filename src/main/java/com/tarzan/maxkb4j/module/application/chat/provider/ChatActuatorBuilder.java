package com.tarzan.maxkb4j.module.application.chat.provider;

import com.tarzan.maxkb4j.core.exception.ApiException;
import com.tarzan.maxkb4j.module.application.chat.actuator.ChatFlowActuator;
import com.tarzan.maxkb4j.module.application.chat.actuator.ChatSimpleActuator;
import com.tarzan.maxkb4j.module.application.enums.AppType;
import com.tarzan.maxkb4j.util.SpringUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatActuatorBuilder {

    private static final Map<String, IChatActuator> ACTUATOR_POOL = new ConcurrentHashMap<>();

    static {
        ACTUATOR_POOL.put(AppType.SIMPLE.name(), SpringUtil.getBean(ChatSimpleActuator.class));
        ACTUATOR_POOL.put(AppType.WORK_FLOW.name(), SpringUtil.getBean(ChatFlowActuator.class));
    }

    /**
     * 获取ChatActuator
     *
     * @param appType 应用类型
     * @return IChatActuator
     */
    public static IChatActuator getActuator(String appType) {
        IChatActuator chatActuator = ACTUATOR_POOL.get(appType);
        if (chatActuator == null) {
            throw new ApiException("no appType was found");
        } else {
            return ACTUATOR_POOL.get(appType);
        }
    }
}
