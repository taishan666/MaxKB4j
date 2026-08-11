package com.maxkb4j.user.service;

import java.util.List;

public interface IUserResourcePermissionService {

    /**
     * 查询用户对指定资源类型中拥有某权限级别（如 {@code VIEW} / {@code MANAGE}）的目标资源 ID 列表。
     */
    List<String> getTargetIds(String authTargetType, String userId, String permission);

    default boolean ownerSave(String type, String targetId, String userId){
        return ownerSave(type, List.of(targetId), userId);
    }
    boolean ownerSave(String type, List<String> targetIds, String userId);
    boolean remove(String type, String targetId);
    /**
     * 批量删除同一权限目标类型下多个 targetId 的用户资源授权，用 {@code IN (...)} 合并删除。
     */
    boolean remove(String type, List<String> targetIds);
}
