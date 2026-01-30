package com.tarzan.maxkb4j.module.application.controller;

import com.tarzan.maxkb4j.common.annotation.SaCheckPerm;
import com.tarzan.maxkb4j.common.domain.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationApiKeyEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationApiKeyService;
import com.tarzan.maxkb4j.module.system.user.enums.PermissionEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@RestController
@RequestMapping(AppConst.ADMIN_API + "/workspace/default")
@RequiredArgsConstructor
public class ApplicationKeyController {

    private final ApplicationApiKeyService apiKeyService;

    @SaCheckPerm(PermissionEnum.APPLICATION_READ)
    @GetMapping("/application/{id}/application_key")
    public R<List<ApplicationApiKeyEntity>> listApikey(@PathVariable("id") String id) {
        return R.success(apiKeyService.listApikey(id));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_CREATE)
    @PostMapping("/application/{id}/application_key")
    public R<Boolean> createApikey(@PathVariable("id") String id) {
        return R.success(apiKeyService.createApikey(id));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_EDIT)
    @PutMapping("/application/{id}/application_key/{apiKeyId}")
    public R<Boolean> updateApikey(@PathVariable("id") String id, @PathVariable("apiKeyId") String apiKeyId, @RequestBody ApplicationApiKeyEntity apiKeyEntity) {
        return R.success(apiKeyService.updateApikey(id, apiKeyId, apiKeyEntity));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_DELETE)
    @DeleteMapping("/application/{id}/application_key/{apiKeyId}")
    public R<Boolean> deleteApikey(@PathVariable("id") String id, @PathVariable("apiKeyId") String apiKeyId) {
        return R.success(apiKeyService.deleteApikey(id, apiKeyId));
    }

}
