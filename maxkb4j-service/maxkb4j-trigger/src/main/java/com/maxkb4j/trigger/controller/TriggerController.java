package com.maxkb4j.trigger.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.dto.EventQuery;
import com.maxkb4j.trigger.service.IEventTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author tarzan
 * @date 2025-03-15 22:00:45
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@RequiredArgsConstructor
@Slf4j
public class TriggerController {

    private final IEventTriggerService eventTriggerService;

    @GetMapping("/workspace/default/trigger/{current}/{size}")
    public R<IPage<EventTriggerEntity>> page(@PathVariable int current, @PathVariable int size, EventQuery query) {
        return R.success(eventTriggerService.pageList(current, size,query));
    }
}
