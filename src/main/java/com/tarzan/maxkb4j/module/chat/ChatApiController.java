package com.tarzan.maxkb4j.module.chat;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConst.CHAT_API)
@AllArgsConstructor
public class ChatApiController {

    @GetMapping("/profile")
    public R<JSONObject> profile(String accessToken) {
        JSONObject result = new JSONObject();
        result.put("authentication", false);
        return R.success(result);
    }

    @GetMapping("/application/profile")
    public R<ApplicationEntity> appProfile() {
        return R.success(new ApplicationEntity());
    }

    //todo
    @GetMapping("/open")
    public R<String> chatOpen() {
        return R.success("123456789");
    }

    //todo
    @PostMapping("/auth/anonymous")
    public R<String> auth(@RequestBody JSONObject params) {
        return R.success(StpUtil.getTokenValue());
    }

    //todo
    @PostMapping("/chat_message/{chatId}")
    public R<String> auth(@PathVariable String chatId, @RequestBody JSONObject params) {
        return R.success(StpUtil.getTokenValue());
    }

    @GetMapping("/historical_conversation/{current}/{size}")
    public R<List<ApplicationChatRecordEntity>> historicalConversation(@PathVariable int current, @PathVariable int size) {
        return R.success(List.of());
    }
}
