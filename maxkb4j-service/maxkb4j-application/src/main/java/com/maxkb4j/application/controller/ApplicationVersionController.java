package com.maxkb4j.application.controller;

import com.maxkb4j.application.entity.ApplicationVersionEntity;
import com.maxkb4j.application.service.ApplicationVersionService;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.domain.api.R;
import com.maxkb4j.common.enums.PermissionEnum;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Tag(name = "APP应用管理", description = "APP应用管理")
@RestController
@RequestMapping(AppConst.ADMIN_API + "/workspace/default")
@RequiredArgsConstructor
public class ApplicationVersionController {

    private final ApplicationVersionService applicationVersionService;

    @SaCheckPerm(PermissionEnum.APPLICATION_READ)
    @GetMapping("/application/{id}/application_version")
    public R<List<ApplicationVersionEntity>> workFlowVersionList(@PathVariable("id") String id) {
        List<ApplicationVersionEntity> list = applicationVersionService.lambdaQuery().eq(ApplicationVersionEntity::getApplicationId, id).orderByDesc(ApplicationVersionEntity::getCreateTime).list();
        return R.success(list);
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_EDIT)
    @PutMapping("/application/{id}/application_version/{versionId}")
    public R<Boolean> updateWorkFlowVersion(@PathVariable("id") String id, @PathVariable("versionId") String versionId, @RequestBody ApplicationVersionEntity versionEntity) {
        versionEntity.setId(versionId);
        return R.success(applicationVersionService.updateById(versionEntity));
    }


}
