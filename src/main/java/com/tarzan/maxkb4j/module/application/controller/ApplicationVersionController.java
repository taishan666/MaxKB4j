package com.tarzan.maxkb4j.module.application.controller;

import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationVersionEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationVersionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Tag(name = "APP应用管理", description = "APP应用管理")
@RestController
@RequestMapping(AppConst.ADMIN_PATH+"/workspace/default")
@AllArgsConstructor
public class ApplicationVersionController {

    private final ApplicationVersionService applicationVersionService;


   // @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{id}/work_flow_version")
    public R<List<ApplicationVersionEntity>> workFlowVersionList(@PathVariable("id") String id) {
        List<ApplicationVersionEntity> list= applicationVersionService.lambdaQuery().eq(ApplicationVersionEntity::getApplicationId, id).orderByDesc(ApplicationVersionEntity::getCreateTime).list();
        return R.success(list);
    }

   // @SaCheckPermission("APPLICATION:EDIT")
    @PutMapping("/application/{id}/work_flow_version/{versionId}")
    public R<Boolean> updateWorkFlowVersion(@PathVariable("id") String id,@PathVariable("versionId") String versionId,@RequestBody ApplicationVersionEntity versionEntity) {
        versionEntity.setId(versionId);
        return R.success(applicationVersionService.updateById(versionEntity));
    }


}
