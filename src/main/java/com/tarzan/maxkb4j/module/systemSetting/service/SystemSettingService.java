package com.tarzan.maxkb4j.module.systemSetting.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tarzan.maxkb4j.module.systemSetting.mapper.SystemSettingMapper;
import com.tarzan.maxkb4j.module.systemSetting.entity.SystemSettingEntity;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author tarzan
 * @date 2024-12-31 17:33:32
 */
@Service
public class SystemSettingService extends ServiceImpl<SystemSettingMapper, SystemSettingEntity>{

    @Autowired
    private EmailService emailService;

    public boolean testConnect(JSONObject meta) {
       return emailService.testConnect(meta);
    }

    @Transactional
    public boolean saveEmailSetting(JSONObject meta) {
        this.lambdaUpdate().eq(SystemSettingEntity::getType,0).remove();
        SystemSettingEntity systemSetting=new SystemSettingEntity();
        systemSetting.setMeta(meta);
        systemSetting.setType(0);
        return this.save(systemSetting);
    }
}
