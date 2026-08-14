package com.maxkb4j.system.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.constant.RoleConst;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.core.support.vo.UserResourcePermissionVO;
import com.maxkb4j.system.service.IUserInternalService;
import com.maxkb4j.system.service.IUserResourcePermissionInternalService;
import com.maxkb4j.system.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * @author tarzan
 * @date 2025-8-25 12:42:39
 */
@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class UserResourcePermissionController {

    private final IUserResourcePermissionInternalService userResourcePermissionService;
    private final IUserInternalService userService;

   // @SaCheckRole(type= LoginType.ADMIN,value = {RoleType.ADMIN, RoleType.USER},mode = SaMode.OR)
    @GetMapping("/user_list")
    public R<List<UserNameVO>> userList(){
        return R.data(userService.listActiveUserNames());
    }

    @SaCheckRole(type= LoginType.ADMIN,value = RoleConst.ADMIN)
    @GetMapping("/user_member")
    public R<List<UserVO>> userMembers(){
        List<UserVO> users=BeanUtil.copyList(userService.listActiveMembers(), UserVO.class);
        users.forEach(user -> user.setRoles(Set.of(user.getRole())));
        return R.data(users);
    }

    @SaCheckRole(type=LoginType.ADMIN,value = RoleConst.ADMIN)
    @GetMapping("/user_resource_permission/user/{userId}/resource/{type}/{current}/{size}")
    public R<IPage<UserResourcePermissionVO>> userResourcePage(@PathVariable String userId, @PathVariable String type, @PathVariable int current, @PathVariable int size, String name, String[] permission){
        return R.data(userResourcePermissionService.userResourcePermissionPage(userId,type,current,size,name, permission));
    }

    @SaCheckRole(type=LoginType.ADMIN,value = RoleConst.ADMIN)
    @GetMapping("/resource_user_permission/resource/{resourceId}/resource/{type}/{current}/{size}")
    public R<IPage<ResourceUserPermissionVO>> resourceUserPage(@PathVariable String resourceId, @PathVariable String type, @PathVariable int current, @PathVariable int size, String nickname, String username, String[] permission){
        return R.data(userResourcePermissionService.resourceUserPermissionPage(resourceId,type,current,size,nickname,username,permission));
    }

    @SaCheckRole(type=LoginType.ADMIN,value = RoleConst.ADMIN)
    @PutMapping("/resource_user_permission/resource/{resourceId}/resource/{resourceType}")
    public R<Boolean> resourcePermissionUpdate(@PathVariable String resourceId, @PathVariable String resourceType, @RequestBody List<ResourcePermissionUpdateVO> list){
        return R.status(userResourcePermissionService.resourcePermissionUpdate(resourceId,resourceType,list));
    }

    @SaCheckRole(type=LoginType.ADMIN,value = RoleConst.ADMIN)
    @PutMapping("/user_resource_permission/user/{userId}/resource/{resourceType}")
    public R<Boolean> userPermissionUpdate(@PathVariable String userId, @PathVariable String resourceType, @RequestBody List<UserPermissionUpdateVO> list){
        return R.status(userResourcePermissionService.userPermissionUpdate(userId,resourceType,list));
    }
}