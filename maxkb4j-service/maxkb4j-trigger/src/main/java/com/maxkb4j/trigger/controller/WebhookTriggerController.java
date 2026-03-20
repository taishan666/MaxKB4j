package com.maxkb4j.trigger.controller;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.enums.TriggerType;
import com.maxkb4j.trigger.service.IEventTriggerService;
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

    private final IEventTriggerService eventTriggerService;
    private final TriggerTaskExecutor triggerTaskExecutor;

    @PostMapping("/trigger/v1/webhook/{id}")
    public R<Boolean> webhook(@PathVariable String id, @RequestBody JSONObject params) {
        EventTriggerEntity eventTrigger =eventTriggerService.getById( id);
        if (eventTrigger == null){
            return R.fail("触发器不存在");
        }
        if (!TriggerType.EVENT.name().equals(eventTrigger.getTriggerType())){
            return R.fail("触发器不是webhook类型");
        }
        if (!eventTrigger.getIsActive()){
            return R.fail("触发器已禁用");
        }
        triggerTaskExecutor.executeTriggerTasks(id);
        return R.data(true);
    }


}
