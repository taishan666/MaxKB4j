package com.tarzan.maxkb4j.module.application.controller;

import com.tarzan.maxkb4j.module.application.entity.ApplicationApiKeyEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationApiKeyService;
import com.tarzan.maxkb4j.tool.api.R;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@RestController
@AllArgsConstructor
public class ApplicationKeyController {

    @Autowired
    private ApplicationApiKeyService apiKeyService;

    @GetMapping("api/application/{appId}/api_key")
    public R<List<ApplicationApiKeyEntity>> listApikey(@PathVariable("appId") String appId) {
        return R.success(apiKeyService.listApikey(appId));
    }

    @PostMapping("api/application/{appId}/api_key")
    public R<Boolean> createApikey(@PathVariable("appId") String appId) {
        return R.success(apiKeyService.createApikey(appId));
    }

    @PutMapping("api/application/{appId}/api_key/{apiKeyId}")
    public R<Boolean> updateApikey(@PathVariable("appId") String appId, @PathVariable("apiKeyId") String apiKeyId, @RequestBody ApplicationApiKeyEntity apiKeyEntity) {
        return R.success(apiKeyService.updateApikey(appId, apiKeyId, apiKeyEntity));
    }

    @DeleteMapping("api/application/{appId}/api_key/{apiKeyId}")
    public R<Boolean> deleteApikey(@PathVariable("appId") String appId, @PathVariable("apiKeyId") String apiKeyId) {
        return R.success(apiKeyService.deleteApikey(appId, apiKeyId));
    }

}
