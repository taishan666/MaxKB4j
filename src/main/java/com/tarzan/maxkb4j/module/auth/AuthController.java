package com.tarzan.maxkb4j.module.auth;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.api.R;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AuthController {

    @GetMapping("api/auth/{type}/detail")
    public R<JSONObject> platform(@PathVariable("type") String type) {
        JSONObject result = new JSONObject();
        return R.success(result);
    }
}
