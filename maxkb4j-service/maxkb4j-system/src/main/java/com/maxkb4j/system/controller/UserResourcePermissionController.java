package com.maxkb4j.system.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.core.support.vo.UserResourcePermissionVO;
import com.maxkb4j.system.service.IUserInternalService;
import com.maxkb4j.system.service.IUserResourcePermissionInternalService;
import com.maxkb4j.system.vo.ResourceUserPermissionVO;
import com.maxkb4j.system.vo.UserNameVO;
import com.maxkb4j.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @SaCheckRole(type= LoginType.ADMIN,value = RoleType.ADMIN)
    @GetMapping("/user_member")
    public R<List<UserVO>> userMembers(){
        return R.data(BeanUtil.copyList(userService.listActiveMembers(), UserVO.class));
    }

    @SaCheckRole(type=LoginType.ADMIN,value = RoleType.ADMIN)
    @GetMapping("/user_resource_permission/user/{userId}/resource/{type}/{current}/{size}")
    public R<IPage<UserResourcePermissionVO>> userResourcePage(@PathVariable String userId, @PathVariable String type, @PathVariable int current, @PathVariable int size, String name, String[] permission){
        return R.data(userResourcePermissionService.userResourcePermissionPage(userId,type,current,size,name, permission));
    }

    @SaCheckRole(type=LoginType.ADMIN,value = RoleType.ADMIN)
    @GetMapping("/resource_user_permission/resource/{resourceId}/resource/{type}/{current}/{size}")
    public R<IPage<ResourceUserPermissionVO>> resourceUserPage(@PathVariable String resourceId, @PathVariable String type, @PathVariable int current, @PathVariable int size, String nickname, String username, String[] permission){
        return R.data(userResourcePermissionService.resourceUserPermissionPage(resourceId,type,current,size,nickname,username,permission));
    }

    @SaCheckRole(type=LoginType.ADMIN,value = RoleType.ADMIN)
    @PutMapping("/resource_user_permission/resource/{resourceId}/resource/{type}")
    public R<Boolean> resourcePermissionUpdate(@PathVariable String resourceId, @PathVariable String type, @RequestBody List<ResourceUserPermissionVO> list){
        return R.status(userResourcePermissionService.resourcePermissionUpdate(resourceId,type,list));
    }

    @SaCheckRole(type=LoginType.ADMIN,value = RoleType.ADMIN)
    @PutMapping("/user_resource_permission/user/{userId}/resource/{type}")
    public R<Boolean> userPermissionUpdate(@PathVariable String userId, @PathVariable String type, @RequestBody List<UserResourcePermissionVO> list){
        return R.status(userResourcePermissionService.userPermissionUpdate(userId,type,list));
    }
}