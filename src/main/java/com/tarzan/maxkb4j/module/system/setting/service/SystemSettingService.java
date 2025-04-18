package com.tarzan.maxkb4j.module.system.setting.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.system.setting.entity.SystemSettingEntity;
import com.tarzan.maxkb4j.module.system.setting.mapper.SystemSettingMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author tarzan
 * @date 2024-12-31 17:33:32
 */
@Service
@AllArgsConstructor
public class SystemSettingService extends ServiceImpl<SystemSettingMapper, SystemSettingEntity>{

    private final EmailService emailService;

    public boolean testConnect(JSONObject meta) {
       return emailService.testConnect(meta);
    }

    @Transactional
    public boolean save(JSONObject meta,int type) {
        this.lambdaUpdate().eq(SystemSettingEntity::getType,1).remove();
        SystemSettingEntity systemSetting=new SystemSettingEntity();
        systemSetting.setMeta(meta);
        systemSetting.setType(1);
        return this.save(systemSetting);
    }
}
