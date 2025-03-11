package com.tarzan.maxkb4j.module.system.setting.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tarzan.maxkb4j.module.system.setting.mapper.SystemSettingMapper;
import com.tarzan.maxkb4j.module.system.setting.entity.SystemSettingEntity;
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
    public boolean saveEmailSetting(JSONObject meta) {
        this.lambdaUpdate().eq(SystemSettingEntity::getType,0).remove();
        SystemSettingEntity systemSetting=new SystemSettingEntity();
        systemSetting.setMeta(meta);
        systemSetting.setType(0);
        return this.save(systemSetting);
    }
}
