package com.maxkb4j.trigger.controller;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.trigger.service.TriggerTaskExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 触发器管理控制器
 *
 * @author tarzan
 * @date 2025-03-15 22:00:45
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@RequiredArgsConstructor
@Slf4j
public class WebhookTriggerController {

    private final TriggerTaskExecutor triggerTaskExecutor;

    @PostMapping("/trigger/v1/webhook/{id}")
    public R<Boolean> webhook(@PathVariable String id, @RequestBody JSONObject params) {
        triggerTaskExecutor.executeTriggerTasks(id);
        return R.data(true);
    }


}
