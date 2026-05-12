package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.entity.ApplicationLongTermMemoryEntity;

public interface IApplicationLongTermMemoryService extends IService<ApplicationLongTermMemoryEntity> {

    void saveMemory(String applicationId, String chatUserId, String modelId, int pageSize);
    String getMemory(String applicationId, String chatUserId);
}
