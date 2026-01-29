package com.tarzan.maxkb4j.module.application.controller;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.application.domain.dto.PlatformStatusDTO;
import com.tarzan.maxkb4j.module.application.service.ApplicationAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(AppConst.ADMIN_API)
@RequiredArgsConstructor
public class ApplicationAccessController {

    private final ApplicationAccessService applicationAccessService;

    @GetMapping("/workspace/default/application/{id}/platform/status")
    public R<JSONObject> platformStatus(@PathVariable("id") String id) {
        return R.success(applicationAccessService.getPlatformStatus(id));
    }

    @PostMapping("/workspace/default/application/{id}/platform/status")
    public R<Boolean> platformStatus(@PathVariable("id") String id, @RequestBody PlatformStatusDTO params) {
        return R.status(applicationAccessService.updatePlatformStatus(id,params));
    }

    @GetMapping("/workspace/default/application/{id}/platform/{key}")
    public R<JSONObject> platformConfig(@PathVariable("id") String id, @PathVariable("key") String key) {
        return R.success(applicationAccessService.getPlatformConfig(id,key));
    }

    @PostMapping("/workspace/default/application/{id}/platform/{key}")
    public R<Boolean> platformConfig(@PathVariable("id") String id, @PathVariable("key") String key, @RequestBody JSONObject platformConfig) {
        return R.status(applicationAccessService.updatePlatformConfig(id,key,platformConfig));
    }

    @PostMapping("/chat/{key}/{id}")
    public R<Boolean> platformCallback(@PathVariable("id") String id, @PathVariable("key") String key, @RequestBody JSONObject params) {
        return R.status(applicationAccessService.platformCallback(id,key,params));
    }


}

