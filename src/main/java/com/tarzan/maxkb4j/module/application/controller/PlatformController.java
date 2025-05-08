package com.tarzan.maxkb4j.module.application.controller;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.api.R;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
public class PlatformController {

    //@SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/platform/{id}/status")
    public R<JSONObject> platform(@PathVariable("id") String id) {
        JSONObject result = new JSONObject();
        result.put("wecom", new Boolean[]{true, true});
        result.put("dingtalk", new Boolean[]{true, true});
        result.put("wechat", new Boolean[]{true, true});
        result.put("feishu", new Boolean[]{true, true});
        result.put("slack", new Boolean[]{true, true});
        return R.success(result);
    }

    @GetMapping("api/platform/{id}/{platform}")
    public R<JSONObject> platform(@PathVariable("id") String id, @PathVariable("platform") String platform) {
        JSONObject config = new JSONObject();

        JSONObject wechat = new JSONObject()
                .fluentPut("app_id", "123456")
                .fluentPut("app_secret", "")
                .fluentPut("token", "")
                .fluentPut("encoding_aes_key", "")
                .fluentPut("is_certification", false)
                .fluentPut("callback_url", "");

        JSONObject dingtalk = new JSONObject()
                .fluentPut("client_id", "123456")
                .fluentPut("client_secret", "")
                .fluentPut("callback_url", "");

        JSONObject wecom = new JSONObject()
                .fluentPut("app_id", "123456")
                .fluentPut("agent_id", "")
                .fluentPut("secret", "")
                .fluentPut("token", "")
                .fluentPut("encoding_aes_key", "")
                .fluentPut("callback_url", "");

        JSONObject feishu = new JSONObject()
                .fluentPut("app_id", "123456")
                .fluentPut("app_secret", "")
                .fluentPut("verification_token", "")
                .fluentPut("callback_url", "");

        JSONObject slack = new JSONObject()
                .fluentPut("signing_secret", "123456")
                .fluentPut("bot_user_token", "")
                .fluentPut("callback_url", "");

        config.put("wechat", wechat);
        config.put("dingtalk", dingtalk);
        config.put("wecom", wecom);
        config.put("feishu", feishu);
        config.put("slack", slack);
        return R.success(config.getJSONObject(platform));
    }

    @PostMapping("api/platform/{id}/{platform}")
    public R<JSONObject> platformConfig(@PathVariable("id") String id, @PathVariable("platform") String platform, @RequestBody JSONObject config) {
        return R.success(config);
    }
}
