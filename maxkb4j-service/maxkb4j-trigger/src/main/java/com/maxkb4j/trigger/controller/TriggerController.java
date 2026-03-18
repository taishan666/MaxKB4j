package com.maxkb4j.trigger.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.trigger.dto.EventQuery;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.service.IEventTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
public class TriggerController {

    private final IEventTriggerService eventTriggerService;

    /**
     * 分页查询触发器列表
     */
    @GetMapping("/workspace/default/trigger/{current}/{size}")
    public R<IPage<EventTriggerEntity>> page(@PathVariable int current, @PathVariable int size, EventQuery query) {
        return R.success(eventTriggerService.pageList(current, size, query));
    }

    /**
     * 新增触发器
     */
    @PostMapping("/workspace/default/trigger")
    public R<EventTriggerEntity> addTrigger(@RequestBody EventTriggerEntity dto) {
        eventTriggerService.saveTrigger(dto, false);
        return R.data(dto);
    }

    /**
     * 获取触发器详情
     */
    @GetMapping("/workspace/default/trigger/{id}")
    public R<EventTriggerEntity> getTrigger(@PathVariable String id) {
        return R.success(eventTriggerService.getDetailById(id));
    }

    /**
     * 编辑触发器
     */
    @PutMapping("/workspace/default/trigger/{id}")
    public R<EventTriggerEntity> updateTrigger(@PathVariable String id, @RequestBody EventTriggerEntity dto) {
        dto.setId(id);
        eventTriggerService.saveTrigger(dto, true);
        return R.data(dto);
    }

    /**
     * 删除触发器
     */
    @DeleteMapping("/workspace/default/trigger/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.success(eventTriggerService.batchDelete(id));
    }

    /**
     * 批量删除触发器
     */
    @PutMapping("/workspace/default/trigger/batch_delete")
    public R<Boolean> batchDelete(@RequestBody EventTriggerEntity dto) {
        boolean allSuccess = dto.getIdList().stream()
                .allMatch(eventTriggerService::batchDelete);
        return R.success(allSuccess);
    }

    /**
     * 批量启用/禁用触发器
     */
    @PutMapping("/workspace/default/trigger/batch_activate")
    public R<Boolean> batchActivate(@RequestBody EventTriggerEntity dto) {
        boolean allSuccess = dto.getIdList().stream()
                .allMatch(id -> eventTriggerService.batchActivate(id, dto.getIsActive()));
        return R.success(allSuccess);
    }

    @GetMapping("/workspace/default/{sourceType}/{sourceId}/trigger")
    public R<List<EventTriggerEntity>> listBySource(@PathVariable String sourceType, @PathVariable String sourceId) {
        return R.success(eventTriggerService.listBySource(sourceType, sourceId));
    }


}
