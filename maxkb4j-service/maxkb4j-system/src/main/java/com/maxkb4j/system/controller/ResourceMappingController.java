package com.maxkb4j.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.system.service.ResourceMappingService;
import com.maxkb4j.user.vo.ResourceUseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author 小峰
 * @date 2026-04-05
 */
@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class ResourceMappingController {

    private final ResourceMappingService resourceMappingService;

    @GetMapping("/resource_mapping/{resourceType}/{resourceId}/{current}/{size}")
    public R<IPage<ResourceUseVO>> resourceMappingKnowledgePage(@PathVariable String resourceType, @PathVariable String resourceId, @PathVariable int current, @PathVariable int size, String resourceName, String userName, String[] sourceType) {
        return R.success(resourceMappingService.selectPage(resourceType, resourceId, current, size, resourceName, userName, sourceType));
    }

}
