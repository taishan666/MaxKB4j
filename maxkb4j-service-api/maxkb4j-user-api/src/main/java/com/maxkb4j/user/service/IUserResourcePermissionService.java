package com.maxkb4j.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.user.entity.UserResourcePermissionEntity;
import com.maxkb4j.user.vo.ResourceUserPermissionVO;
import com.maxkb4j.user.vo.UserResourcePermissionVO;

import java.util.List;

public interface IUserResourcePermissionService extends IService<UserResourcePermissionEntity> {

    List<String> getTargetIds(String authTargetType, String userId);
    boolean ownerSave(String type, String targetId, String userId);
    boolean remove(String type, String targetId);
    List<UserResourcePermissionEntity> getByUserId(String userId);

    IPage<UserResourcePermissionVO> userResourcePermissionPage(String userId, String type, int current, int size);

    IPage<ResourceUserPermissionVO> resourceUserPermissionPage(String resourceId, String type, int current, int size, String nickname, String username, String[] permission);

    boolean resourcePermissionUpdate(String resourceId, String type, List<ResourceUserPermissionVO> list);

    boolean userPermissionUpdate(String userId, String type, List<UserResourcePermissionVO> list);
}
