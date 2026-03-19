package com.maxkb4j.trigger.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.trigger.dto.EventTaskQuery;
import com.maxkb4j.trigger.entity.EventTriggerTaskEntity;
import com.maxkb4j.trigger.service.IEventTriggerTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
public class TriggerTaskController {

    private final IEventTriggerTaskService eventTriggerTaskService;

    /**
     * 分页查询触发器列表
     */
    @GetMapping("/workspace/default/trigger/{id}/task_record/{current}/{size}")
    public R<IPage<EventTriggerTaskEntity>> page(@PathVariable String id, @PathVariable int current, @PathVariable int size, EventTaskQuery query) {
        return R.success(eventTriggerTaskService.pageList(id,current, size, query));
    }


}
