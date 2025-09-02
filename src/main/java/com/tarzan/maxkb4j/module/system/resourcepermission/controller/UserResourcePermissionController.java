package com.tarzan.maxkb4j.module.system.resourcepermission.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.system.resourcepermission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.resourcepermission.vo.UserResourcePermissionVO;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author tarzan
 * @date 2025-8-25 12:42:39
 */
@RestController
@RequestMapping(AppConst.ADMIN_PATH+"/workspace/default")
@AllArgsConstructor
public class UserResourcePermissionController {

    private final UserResourcePermissionService userResourcePermissionService;



    @GetMapping("/user_resource_permission/user/{userId}/resource/{type}/{current}/{size}")
    public R<IPage<UserResourcePermissionVO>> page(@PathVariable String userId, @PathVariable String type, @PathVariable int current, @PathVariable int size){
        return R.success(userResourcePermissionService.userResourcePermissionPage(userId,type,current,size));
    }

    @PutMapping("/user_resource_permission/user/{userId}/resource/{type}")
    public R<Boolean> update(@PathVariable String userId, @PathVariable String type, @RequestBody UserResourcePermissionVO vo){
        return R.status(userResourcePermissionService.update(userId,type,vo));
    }
}
