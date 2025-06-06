package com.tarzan.maxkb4j.module.application.controller;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.application.dto.PlatformStatusDTO;
import com.tarzan.maxkb4j.module.application.entity.ApplicationPlatformEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationPlatformService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@AllArgsConstructor
public class ApplicationPlatformController {

    private final ApplicationPlatformService applicationPlatformService;

    @GetMapping("api/platform/source")
    public R<JSONObject> source() {
        return R.success(new JSONObject());
    }

    //@SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/platform/{id}/status")
    public R<JSONObject> platformStatus(@PathVariable("id") String id) {
        ApplicationPlatformEntity entity=applicationPlatformService.getById(id);
        if (entity == null){
            Map<String,Object> status = new HashMap<>();
            status.put("wecom",  false);
            status.put("dingtalk", false);
            status.put("wechat",  false);
            status.put("feishu",  false);
            status.put("slack",  false);
            entity=new ApplicationPlatformEntity();
            entity.setApplicationId(id);
            entity.setStatus(new JSONObject(status));
            JSONObject config = new JSONObject();
            JSONObject wechat = new JSONObject()
                    .fluentPut("app_id", "")
                    .fluentPut("app_secret", "")
                    .fluentPut("token", "")
                    .fluentPut("encoding_aes_key", "")
                    .fluentPut("is_certification", false)
                    .fluentPut("callback_url", "");
            JSONObject dingtalk = new JSONObject()
                    .fluentPut("client_id", "")
                    .fluentPut("client_secret", "")
                    .fluentPut("callback_url", "");

            JSONObject wecom = new JSONObject()
                    .fluentPut("app_id", "")
                    .fluentPut("agent_id", "")
                    .fluentPut("secret", "")
                    .fluentPut("token", "")
                    .fluentPut("encoding_aes_key", "")
                    .fluentPut("callback_url", "");

            JSONObject feishu = new JSONObject()
                    .fluentPut("app_id", "")
                    .fluentPut("app_secret", "")
                    .fluentPut("verification_token", "")
                    .fluentPut("callback_url", "");

            JSONObject slack = new JSONObject()
                    .fluentPut("signing_secret", "")
                    .fluentPut("bot_user_token", "")
                    .fluentPut("callback_url", "");

            config.put("wechat", wechat);
            config.put("dingtalk", dingtalk);
            config.put("wecom", wecom);
            config.put("feishu", feishu);
            config.put("slack", slack);
            entity.setConfig(config);
            applicationPlatformService.save(entity);
        }
        return R.success(entity.getStatus());
    }

    @PostMapping("api/platform/{id}/status")
    public R<Boolean> platformStatus(@PathVariable("id") String id, @RequestBody PlatformStatusDTO params) {
        System.out.println(params);
        ApplicationPlatformEntity entity=applicationPlatformService.getById(id);
        JSONObject status = entity.getStatus();
        status.put(params.getType(), params.getStatus());
        entity.setStatus(status);
        return R.status(applicationPlatformService.updateById(entity));
    }

    @GetMapping("api/platform/{id}/{platform}")
    public R<JSONObject> platformConfig(@PathVariable("id") String id, @PathVariable("platform") String platform) {
        ApplicationPlatformEntity entity=applicationPlatformService.getById(id);
        JSONObject config = entity.getConfig();
        return R.success(config.getJSONObject(platform));
    }

    @PostMapping("api/platform/{id}/{platform}")
    public R<Boolean> platformConfig(@PathVariable("id") String id, @PathVariable("platform") String platform, @RequestBody JSONObject platformConfig) {
        ApplicationPlatformEntity entity=applicationPlatformService.getById(id);
        JSONObject config = entity.getConfig();
        config.put(platform, platformConfig);
        entity.setConfig(config);
        return R.status(applicationPlatformService.updateById(entity));
    }


}
