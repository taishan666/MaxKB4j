package com.maxkb4j.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.system.entity.ResourceMappingEntity;
import com.maxkb4j.system.service.ResourceMappingService;
import com.maxkb4j.system.service.impl.UserResourcePermissionServiceImpl;
import com.maxkb4j.user.entity.UserEntity;
import com.maxkb4j.user.vo.ResourceUseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


/**
 * @author 小峰
 * @date 2026-04-05
 */
@RestController
@RequestMapping(AppConst.ADMIN_API + "/workspace/default")
@RequiredArgsConstructor
public class ResourceMappingController {
    private final ResourceMappingService resourceMappingService;

    @GetMapping("/resource_mapping/MODEL/{resourceId}/{current}/{size}")
    public R<IPage<ResourceUseVO>> resourceMappingModelPage(@PathVariable String resourceId, @PathVariable int current, @PathVariable int size, String resourceName, String userName, String[] sourceType) {
        return R.success(resourceMappingService.selectUserPage(resourceId, current, size, resourceName, userName, sourceType, "MODEL"));
    }

    @GetMapping("/resource_mapping/KNOWLEDGE/{resourceId}/{current}/{size}")
    public R<IPage<ResourceUseVO>> resourceMappingKnowledgePage(@PathVariable String resourceId, @PathVariable int current, @PathVariable int size, String resourceName, String userName, String[] sourceType) {
        return R.success(resourceMappingService.selectUserPage(resourceId, current, size, resourceName, userName, sourceType, "KNOWLEDGE"));
    }

}
