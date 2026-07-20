package com.maxkb4j.user.service;

import java.util.List;

public interface IUserResourcePermissionService {

    List<String> getTargetIds(String authTargetType, String userId);
    boolean ownerSave(String type, String targetId, String userId);
    boolean remove(String type, String targetId);
    /**
     * 批量删除同一权限目标类型下多个 targetId 的用户资源授权，用 {@code IN (...)} 合并删除。
     */
    boolean remove(String type, List<String> targetIds);
}
