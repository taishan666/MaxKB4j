package com.maxkb4j.system.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.system.entity.SystemSettingEntity;
import com.maxkb4j.system.mapper.SystemSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author tarzan
 * @date 2024-12-31 17:33:32
 */
@Service
@RequiredArgsConstructor
public class SystemSettingService extends ServiceImpl<SystemSettingMapper, SystemSettingEntity>{

    private final EmailService emailService;

    public boolean testConnect(JSONObject meta) {
       return emailService.testConnect(meta);
    }

    @Transactional
    public boolean saveOrUpdate(JSONObject meta,int type) {
        this.lambdaUpdate().eq(SystemSettingEntity::getType,type).remove();
        SystemSettingEntity systemSetting=new SystemSettingEntity();
        systemSetting.setMeta(meta);
        systemSetting.setType(type);
        return this.saveOrUpdate(systemSetting);
    }
}
