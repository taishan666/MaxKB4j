package com.tarzan.maxkb4j.module.system.permission.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.system.permission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.permission.vo.ResourceUserPermissionVO;
import com.tarzan.maxkb4j.module.system.permission.vo.UserResourcePermissionVO;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2025-8-25 12:42:39
 */
@RestController
@RequestMapping(AppConst.ADMIN_API+"/workspace/default")
@AllArgsConstructor
public class UserResourcePermissionController {

    private final UserResourcePermissionService userResourcePermissionService;



    @GetMapping("/user_resource_permission/user/{userId}/resource/{type}/{current}/{size}")
    public R<IPage<UserResourcePermissionVO>> userResourcePage(@PathVariable String userId, @PathVariable String type, @PathVariable int current, @PathVariable int size){
        return R.success(userResourcePermissionService.userResourcePermissionPage(userId,type,current,size));
    }

    @GetMapping("/resource_user_permission/resource/{resourceId}/resource/{type}/{current}/{size}")
    public R<IPage<ResourceUserPermissionVO>> resourceUserPage(@PathVariable String resourceId, @PathVariable String type, @PathVariable int current, @PathVariable int size){
        return R.success(userResourcePermissionService.resourceUserPermissionPage(resourceId,type,current,size));
    }

    @PutMapping("/resource_user_permission/resource/{resourceId}/resource/{type}")
    public R<Boolean> resourcePermissionUpdate(@PathVariable String resourceId, @PathVariable String type, @RequestBody List<ResourceUserPermissionVO> list){
        return R.status(userResourcePermissionService.resourcePermissionUpdate(resourceId,type,list));
    }

    @PutMapping("/user_resource_permission/user/{userId}/resource/{type}")
    public R<Boolean> userPermissionUpdate(@PathVariable String userId, @PathVariable String type, @RequestBody List<UserResourcePermissionVO> list){
        return R.status(userResourcePermissionService.userPermissionUpdate(userId,type,list));
    }
}
