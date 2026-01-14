package com.tarzan.maxkb4j.module.application.controller;

import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationApiKeyEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@RestController
@RequestMapping(AppConst.ADMIN_API+"/workspace/default")
@RequiredArgsConstructor
public class ApplicationKeyController {

    private final ApplicationApiKeyService apiKeyService;

    @GetMapping("/application/{id}/application_key")
    public R<List<ApplicationApiKeyEntity>> listApikey(@PathVariable("id") String id) {
        return R.success(apiKeyService.listApikey(id));
    }

    @PostMapping("/application/{id}/application_key")
    public R<Boolean> createApikey(@PathVariable("id") String id) {
        return R.success(apiKeyService.createApikey(id));
    }

    @PutMapping("/application/{id}/application_key/{apiKeyId}")
    public R<Boolean> updateApikey(@PathVariable("id") String id, @PathVariable("apiKeyId") String apiKeyId, @RequestBody ApplicationApiKeyEntity apiKeyEntity) {
        return R.success(apiKeyService.updateApikey(id, apiKeyId, apiKeyEntity));
    }

    @DeleteMapping("/application/{id}/application_key/{apiKeyId}")
    public R<Boolean> deleteApikey(@PathVariable("id") String id, @PathVariable("apiKeyId") String apiKeyId) {
        return R.success(apiKeyService.deleteApikey(id, apiKeyId));
    }

}
