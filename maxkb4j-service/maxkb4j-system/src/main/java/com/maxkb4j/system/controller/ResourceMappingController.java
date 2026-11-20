package com.maxkb4j.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.core.support.permission.DataPermissionScope;
import com.maxkb4j.core.support.permission.DataPermissionSupport;
import com.maxkb4j.system.service.IResourceMappingInternalService;
import com.maxkb4j.system.vo.ResourceUseVO;
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

    private final IResourceMappingInternalService resourceMappingService;
    private final DataPermissionSupport dataPermissionSupport;

    /**
     * 获取资源依赖关联的资源
     *
     * @param resourceType
     * @param resourceId
     * @param current
     * @param size
     * @param resourceName
     * @param userName
     * @param sourceType
     * @return
     */
    @SaCheckLogin(type = LoginType.ADMIN)
    @GetMapping("/mapping_resource/{resourceType}/{resourceId}/{current}/{size}")
    public R<IPage<ResourceUseVO>> mappingResourcePage(@PathVariable String resourceType, @PathVariable String resourceId, @PathVariable int current, @PathVariable int size, String resourceName, String userName, String[] sourceType) {
        checkReadPermission(resourceType, resourceId);
        return R.data(resourceMappingService.selectDependOnPage(resourceType, resourceId, current, size, resourceName, userName, sourceType));
    }


    /**
     * 获取资源被依赖关联的资源
     *
     * @param resourceType
     * @param resourceId
     * @param current
     * @param size
     * @param resourceName
     * @param userName
     * @param sourceType
     * @return
     */
    @SaCheckLogin(type = LoginType.ADMIN)
    @GetMapping("/resource_mapping/{resourceType}/{resourceId}/{current}/{size}")
    public R<IPage<ResourceUseVO>> resourceMappingPage(@PathVariable String resourceType, @PathVariable String resourceId, @PathVariable int current, @PathVariable int size, String resourceName, String userName, String[] sourceType) {
        checkReadPermission(resourceType, resourceId);
        return R.data(resourceMappingService.selectBeDependedOnPage(resourceType, resourceId, current, size, resourceName, userName, sourceType));
    }

    private void checkReadPermission(String resourceType, String resourceId) {
        DataPermissionScope scope = dataPermissionSupport.resolve(resourceType);
        if (!scope.isAdmin() && !scope.getTargetIds().contains(resourceId)) {
            throw new ApiException("resource.no.permission");
        }
    }

}
