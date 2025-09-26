package com.tarzan.maxkb4j.module.application.chat.base;

import com.tarzan.maxkb4j.module.application.chat.provider.IChatActuator;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import jakarta.annotation.Resource;

public abstract class ChatBaseActuator implements IChatActuator {


    @Resource
    private  ApplicationService applicationService;

    public  ApplicationVO getAppDetail(String appId,boolean  debug){
        if (debug){
            return applicationService.getDetail(appId);
        }else {
            return applicationService.getPublishedDetail(appId);
        }
    }

}
