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
import java.util.concurrent.atomic.AtomicBoolean;

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

    /**
     * 查看触发器分页接口
     * @param current
     * @param size
     * @param query
     * @return
     */
    @GetMapping("/workspace/default/trigger/{current}/{size}")
    public R<IPage<EventTriggerEntity>> page(@PathVariable int current, @PathVariable int size, EventQuery query) {
        return R.success(eventTriggerService.pageList(current, size, query));
    }

    /**
     * 新增触发器
     * @param dto
     * @return
     */
    @PostMapping("/workspace/default/trigger")
    public R<EventTriggerEntity> addTrigger(@RequestBody EventTriggerEntity dto) {
        eventTriggerService.saveTrigger(dto, false);
        return R.data(dto);
    }

    /**
     * 获取触发器详情
     * @param id
     * @return
     */
    @GetMapping("/workspace/default/trigger/{id}")
    public R<EventTriggerEntity> getTrigger(@PathVariable String id) {
        EventTriggerEntity entity = eventTriggerService.getDetailById(id);
        return R.success(entity);
    }

    /**
     * 编辑触发器
     * @param id
     * @param dto
     * @return
     */
    @PutMapping("/workspace/default/trigger/{id}")
    public R<EventTriggerEntity> updateTrigger(@PathVariable String id, @RequestBody EventTriggerEntity dto) {
        dto.setId(id);
        eventTriggerService.saveTrigger(dto, true);
        return R.data(dto);
    }

    /**
     * 单个删触发器
     * @param id
     * @return
     */
    @DeleteMapping("/workspace/default/trigger/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        AtomicBoolean result = new AtomicBoolean(true);
        var res = eventTriggerService.batchDelete(id);
        if (!res) {
            result.set(false);
        }
        return R.success(result.get());
    }

    /**
     * 批量删除触发器
     * @param dto
     * @return
     */
    @PutMapping("/workspace/default/trigger/batch_delete")
    public R<Boolean> batchDelete(@RequestBody EventTriggerEntity dto) {
        AtomicBoolean result = new AtomicBoolean(true);
        dto.getIdList().forEach(id -> {
            var res = eventTriggerService.batchDelete(id);
            if (!res) {
                result.set(false);
            }
        });
        return R.success(result.get());
    }

    /**
     * 批量启用禁用触发器
     * @param dto
     * @return
     */
    @PutMapping("/workspace/default/trigger/batch_activate")
    public R<Boolean> batchActivate(@RequestBody EventTriggerEntity dto) {
        AtomicBoolean result = new AtomicBoolean(true);
        dto.getIdList().forEach(id -> {
            var res = eventTriggerService.batchActivate(id, dto.getIsActive());
            if (!res) {
                result.set(false);
            }
        });
        return R.success(result.get());
    }

    @GetMapping("/workspace/default/{sourceType}/{sourceId}/trigger")
    public R<List<EventTriggerEntity>> listBySource(@PathVariable String sourceType, @PathVariable String sourceId) {
        return R.success(eventTriggerService.listBySource(sourceType, sourceId));
    }


}
