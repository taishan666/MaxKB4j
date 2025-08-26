package com.tarzan.maxkb4j.module.system.resourcepermission.controller;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.system.resourcepermission.service.UserResourcePermissionService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author tarzan
 * @date 2025-8-25 12:42:39
 */
@RestController
@RequestMapping(AppConst.ADMIN_PATH+"/workspace/default")
@AllArgsConstructor
public class UserResourcePermissionController {

    private final UserResourcePermissionService userResourcePermissionService;



    @GetMapping("/user_resource_permission/user/{userId}/resource/{type}")
    public R<JSONObject> getUserResourcePermission(@PathVariable String userId,@PathVariable String type){
        JSONObject result=new JSONObject();
        result.put(type, userResourcePermissionService.getUseTargets(type,userId));
        return R.success(result);
    }
}
