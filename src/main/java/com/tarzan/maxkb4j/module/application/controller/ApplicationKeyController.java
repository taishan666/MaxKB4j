package com.tarzan.maxkb4j.module.application.controller;

import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationApiKeyEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationApiKeyService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@RestController
@RequestMapping(AppConst.ADMIN_API+"/workspace/default")
@AllArgsConstructor
public class ApplicationKeyController {

    private final ApplicationApiKeyService apiKeyService;

   // @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{appId}/application_key")
    public R<List<ApplicationApiKeyEntity>> listApikey(@PathVariable("appId") String appId) {
        return R.success(apiKeyService.listApikey(appId));
    }

   // @SaCheckPermission("APPLICATION:EDIT")
    @PostMapping("/application/{appId}/application_key")
    public R<Boolean> createApikey(@PathVariable("appId") String appId) {
        return R.success(apiKeyService.createApikey(appId));
    }

   // @SaCheckPermission("APPLICATION:EDIT")
    @PutMapping("/application/{appId}/application_key/{apiKeyId}")
    public R<Boolean> updateApikey(@PathVariable("appId") String appId, @PathVariable("apiKeyId") String apiKeyId, @RequestBody ApplicationApiKeyEntity apiKeyEntity) {
        return R.success(apiKeyService.updateApikey(appId, apiKeyId, apiKeyEntity));
    }

   // @SaCheckPermission("APPLICATION:EDIT")
    @DeleteMapping("/application/{appId}/application_key/{apiKeyId}")
    public R<Boolean> deleteApikey(@PathVariable("appId") String appId, @PathVariable("apiKeyId") String apiKeyId) {
        return R.success(apiKeyService.deleteApikey(appId, apiKeyId));
    }

}
