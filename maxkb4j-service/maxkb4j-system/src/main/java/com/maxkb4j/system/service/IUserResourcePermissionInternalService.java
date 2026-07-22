package com.maxkb4j.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.core.support.vo.UserResourcePermissionVO;
import com.maxkb4j.system.entity.UserResourcePermissionEntity;
import com.maxkb4j.system.vo.ResourceUserPermissionVO;
import com.maxkb4j.user.service.IUserResourcePermissionService;

import java.util.List;

/**
 * 用户资源权限服务「对内」接口：供 Controller 使用的完整服务契约。
 *
 * <p>与 {@link IUserResourcePermissionService}（对外跨模块契约，位于 maxkb4j-user-api）区分。
 * 本接口位于 service 模块，可引用 service 模块类型，并继承 {@link IService}
 * 获得 MyBatis-Plus 通用方法，使 Controller 依赖抽象而非具体实现。</p>
 */
public interface IUserResourcePermissionInternalService extends IUserResourcePermissionService, IService<UserResourcePermissionEntity> {

    IPage<UserResourcePermissionVO> userResourcePermissionPage(String userId, String type, int current, int size, String name, String[] permissions);

    IPage<ResourceUserPermissionVO> resourceUserPermissionPage(String resourceId, String type, int current, int size, String nickname, String username, String[] permissions);

    boolean userPermissionUpdate(String userId, String type, List<UserResourcePermissionVO> list);

    boolean resourcePermissionUpdate(String resourceId, String type, List<ResourceUserPermissionVO> list);
}