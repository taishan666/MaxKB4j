package com.maxkb4j.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.user.entity.UserEntity;

import java.util.Map;
import java.util.Set;

public interface IUserService extends IService<UserEntity> {
    Set<String> getRoleById(String id);

    Map<String, String> getNicknameMap();

    String getNickname(String userId);

}
