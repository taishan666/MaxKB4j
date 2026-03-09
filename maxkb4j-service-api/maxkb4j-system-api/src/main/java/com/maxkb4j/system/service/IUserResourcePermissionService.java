package com.maxkb4j.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.system.entity.UserResourcePermissionEntity;

import java.util.List;

public interface IUserResourcePermissionService extends IService<UserResourcePermissionEntity> {

    List<String> getTargetIds(String authTargetType, String userId);
    boolean ownerSave(String type, String targetId, String userId);
    boolean remove(String type, String targetId);
}
