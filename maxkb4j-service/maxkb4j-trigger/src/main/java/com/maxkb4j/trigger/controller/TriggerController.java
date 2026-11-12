package com.maxkb4j.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.trigger.dto.EventQuery;
import com.maxkb4j.trigger.dto.EventTriggerDTO;
import com.maxkb4j.trigger.entity.EventTriggerEntity;
import com.maxkb4j.trigger.service.IEventTriggerService;
import com.maxkb4j.trigger.vo.EventTriggerVO;
import com.maxkb4j.trigger.vo.SourceEventTriggerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 触发器管理控制器
 *
 * @author tarzan
 * @date 2025-03-15 22:00:45
 */
@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
@Slf4j
public class TriggerController {

    private final IEventTriggerService eventTriggerService;

    /**
     * 分页查询触发器列表
     */
    @SaCheckLogin(type = LoginType.ADMIN)
    @GetMapping("/trigger/{current}/{size}")
    public R<IPage<EventTriggerVO>> page(@PathVariable int current, @PathVariable int size, EventQuery query) {
        return R.data(eventTriggerService.pageList(current, size, query));
    }

    /**
     * 新增触发器
     */
    @SaCheckLogin(type = LoginType.ADMIN)
    @PostMapping("/trigger")
    public R<EventTriggerDTO> addTrigger(@Valid @RequestBody EventTriggerDTO dto) {
        dto.setIsActive(false);
        eventTriggerService.saveTrigger(dto, false);
        return R.data(dto);
    }

    /**
     * 获取触发器详情
     */
    @SaCheckLogin(type = LoginType.ADMIN)
    @GetMapping("/trigger/{id}")
    public R<EventTriggerVO> getTrigger(@PathVariable String id) {
        return R.data(eventTriggerService.getDetailById(id));
    }

    /**
     * 编辑触发器
     */
    @SaCheckLogin(type = LoginType.ADMIN)
    @PutMapping("/trigger/{id}")
    public R<EventTriggerDTO> updateTrigger(@PathVariable String id, @Valid @RequestBody EventTriggerDTO dto) {
        dto.setId(id);
        eventTriggerService.saveTrigger(dto, true);
        return R.data(dto);
    }

    /**
     * 删除触发器
     */
    @SaCheckLogin(type = LoginType.ADMIN)
    @DeleteMapping("/trigger/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.data(eventTriggerService.deleteTrigger(id));
    }

    /**
     * 批量删除触发器
     */
    @SaCheckLogin(type = LoginType.ADMIN)
    @PutMapping("/trigger/batch_delete")
    public R<Boolean> batchDelete(@Valid @RequestBody EventTriggerDTO dto) {
        return R.status(eventTriggerService.deleteBatch(dto.getIdList()));
    }

    /**
     * 批量启用/禁用触发器
     */
    @SaCheckLogin(type = LoginType.ADMIN)
    @PutMapping("/trigger/batch_activate")
    public R<Boolean> batchActivate(@Valid @RequestBody EventTriggerDTO dto) {
        return R.status(eventTriggerService.batchActivate(dto.getIdList(), dto.getIsActive()));
    }

    @SaCheckLogin(type = LoginType.ADMIN)
    @PostMapping("/{sourceType}/{sourceId}/trigger")
    public R<EventTriggerDTO> addTriggerBySourceId(@PathVariable String sourceType, @PathVariable String sourceId, @Valid @RequestBody EventTriggerDTO dto) {
        dto.setIsActive(true);
        eventTriggerService.saveTriggerBySource(dto, false, sourceType, sourceId);
        return R.data(dto);
    }

    @SaCheckLogin(type = LoginType.ADMIN)
    @GetMapping("/{sourceType}/{sourceId}/trigger/{id}")
    public R<SourceEventTriggerVO> getTriggerBySourceId(@PathVariable String sourceType, @PathVariable String sourceId, @PathVariable String id) {
        return R.data(eventTriggerService.getDetailBySourceId(id, sourceType, sourceId));
    }

    @SaCheckLogin(type = LoginType.ADMIN)
    @PutMapping("/{sourceType}/{sourceId}/trigger/{id}")
    public R<EventTriggerDTO> updateTriggerBySourceId(@PathVariable String sourceType, @PathVariable String sourceId, @PathVariable String id, @Valid @RequestBody EventTriggerDTO dto) {
        dto.setId(id);
        eventTriggerService.saveTriggerBySource(dto, true, sourceType, sourceId);
        return R.data(dto);
    }

    @SaCheckLogin(type = LoginType.ADMIN)
    @GetMapping("/{sourceType}/{sourceId}/trigger")
    public R<List<EventTriggerVO>> listBySourceId(@PathVariable String sourceType, @PathVariable String sourceId) {
        return R.data(BeanUtil.copyList(eventTriggerService.listBySource(sourceType, sourceId), EventTriggerVO.class));
    }

    @SaCheckLogin(type = LoginType.ADMIN)
    @DeleteMapping("/{sourceType}/{sourceId}/trigger/{id}")
    public R<Boolean> deleteBySourceId(@PathVariable String sourceType, @PathVariable String sourceId, @PathVariable String id) {
        return R.status(eventTriggerService.deleteTriggerBySource(id, sourceType, sourceId));
    }

}
