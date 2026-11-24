package com.maxkb4j.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.constant.LoginType;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.constant.RoleConst;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.trigger.dto.EventQuery;
import com.maxkb4j.trigger.dto.EventTriggerDTO;
import com.maxkb4j.trigger.service.IEventTriggerService;
import com.maxkb4j.trigger.vo.EventTriggerVO;
import com.maxkb4j.trigger.vo.SourceEventTriggerVO;
import jakarta.validation.Valid;
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
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
@Slf4j
public class TriggerController {

    private final IEventTriggerService eventTriggerService;

    /**
     * 分页查询触发器列表
     */
    @SaCheckRole(type = LoginType.ADMIN, value = RoleConst.ADMIN)
    @GetMapping("/trigger/{current}/{size}")
    public R<IPage<EventTriggerVO>> page(@PathVariable int current, @PathVariable int size, EventQuery query) {
        return R.data(eventTriggerService.pageList(current, size, query));
    }

    /**
     * 新增触发器
     */
    @SaCheckRole(type = LoginType.ADMIN, value = RoleConst.ADMIN)
    @PostMapping("/trigger")
    public R<EventTriggerDTO> addTrigger(@Valid @RequestBody EventTriggerDTO dto) {
        dto.setIsActive(false);
        eventTriggerService.saveTrigger(dto, false);
        return R.data(dto);
    }

    /**
     * 获取触发器详情
     */
    @SaCheckRole(type = LoginType.ADMIN, value = RoleConst.ADMIN)
    @GetMapping("/trigger/{id}")
    public R<EventTriggerVO> getTrigger(@PathVariable String id) {
        return R.data(eventTriggerService.getDetailById(id));
    }

    /**
     * 编辑触发器
     */
    @SaCheckRole(type = LoginType.ADMIN, value = RoleConst.ADMIN)
    @PutMapping("/trigger/{id}")
    public R<EventTriggerDTO> updateTrigger(@PathVariable String id, @Valid @RequestBody EventTriggerDTO dto) {
        dto.setId(id);
        eventTriggerService.saveTrigger(dto, true);
        return R.data(dto);
    }

    /**
     * 删除触发器
     */
    @SaCheckRole(type = LoginType.ADMIN, value = RoleConst.ADMIN)
    @DeleteMapping("/trigger/{id}")
    public R<Boolean> delete(@PathVariable String id) {
        return R.data(eventTriggerService.deleteTrigger(id));
    }

    /**
     * 批量删除触发器
     */
    @SaCheckRole(type = LoginType.ADMIN, value = RoleConst.ADMIN)
    @PutMapping("/trigger/batch_delete")
    public R<Boolean> batchDelete(@Valid @RequestBody EventTriggerDTO dto) {
        return R.status(eventTriggerService.deleteBatch(dto.getIdList()));
    }

    /**
     * 批量启用/禁用触发器
     */
    @SaCheckRole(type = LoginType.ADMIN, value = RoleConst.ADMIN)
    @PutMapping("/trigger/batch_activate")
    public R<Boolean> batchActivate(@Valid @RequestBody EventTriggerDTO dto) {
        return R.status(eventTriggerService.batchActivate(dto.getIdList(), dto.getIsActive()));
    }

    /**
     * 为工具新增触发器（需对该工具具备管理权限）
     */
    @SaCheckPerm(PermissionEnum.TOOL_TRIGGER_CREATE)
    @PostMapping("/TOOL/{id}/trigger")
    public R<EventTriggerDTO> addToolTrigger(@PathVariable String id, @Valid @RequestBody EventTriggerDTO dto) {
        return addSourceTrigger(ResourceType.TOOL, id, dto);
    }

    /**
     * 为应用新增触发器（需对该应用具备管理权限）
     */
    @SaCheckPerm(PermissionEnum.APPLICATION_TRIGGER_CREATE)
    @PostMapping("/APPLICATION/{id}/trigger")
    public R<EventTriggerDTO> addApplicationTrigger(@PathVariable String id, @Valid @RequestBody EventTriggerDTO dto) {
        return addSourceTrigger(ResourceType.APPLICATION, id, dto);
    }

    /**
     * 获取工具触发器详情（需对该工具具备管理权限）
     */
    @SaCheckPerm(PermissionEnum.TOOL_TRIGGER_READ)
    @GetMapping("/TOOL/{id}/trigger/{triggerId}")
    public R<SourceEventTriggerVO> getToolTrigger(@PathVariable String id, @PathVariable String triggerId) {
        return getSourceTrigger(ResourceType.TOOL, id, triggerId);
    }

    /**
     * 获取应用触发器详情（需对该应用具备管理权限）
     */
    @SaCheckPerm(PermissionEnum.APPLICATION_TRIGGER_READ)
    @GetMapping("/APPLICATION/{id}/trigger/{triggerId}")
    public R<SourceEventTriggerVO> getApplicationTrigger(@PathVariable String id, @PathVariable String triggerId) {
        return getSourceTrigger(ResourceType.APPLICATION, id, triggerId);
    }

    /**
     * 编辑工具触发器（需对该工具具备管理权限）
     */
    @SaCheckPerm(PermissionEnum.TOOL_TRIGGER_EDIT)
    @PutMapping("/TOOL/{id}/trigger/{triggerId}")
    public R<EventTriggerDTO> updateToolTrigger(@PathVariable String id, @PathVariable String triggerId, @Valid @RequestBody EventTriggerDTO dto) {
        return updateSourceTrigger(ResourceType.TOOL, id, triggerId, dto);
    }

    /**
     * 编辑应用触发器（需对该应用具备管理权限）
     */
    @SaCheckPerm(PermissionEnum.APPLICATION_TRIGGER_EDIT)
    @PutMapping("/APPLICATION/{id}/trigger/{triggerId}")
    public R<EventTriggerDTO> updateApplicationTrigger(@PathVariable String id, @PathVariable String triggerId, @Valid @RequestBody EventTriggerDTO dto) {
        return updateSourceTrigger(ResourceType.APPLICATION, id, triggerId, dto);
    }

    /**
     * 查询工具关联的触发器列表（需对该工具具备管理权限）
     */
    @SaCheckPerm(PermissionEnum.TOOL_TRIGGER_READ)
    @GetMapping("/TOOL/{id}/trigger")
    public R<List<EventTriggerVO>> listToolTriggers(@PathVariable String id) {
        return listSourceTriggers(ResourceType.TOOL, id);
    }

    /**
     * 查询应用关联的触发器列表（需对该应用具备管理权限）
     */
    @SaCheckPerm(PermissionEnum.APPLICATION_TRIGGER_READ)
    @GetMapping("/APPLICATION/{id}/trigger")
    public R<List<EventTriggerVO>> listApplicationTriggers(@PathVariable String id) {
        return listSourceTriggers(ResourceType.APPLICATION, id);
    }

    /**
     * 删除工具触发器（需对该工具具备管理权限）
     */
    @SaCheckPerm(PermissionEnum.TOOL_TRIGGER_DELETE)
    @DeleteMapping("/TOOL/{id}/trigger/{triggerId}")
    public R<Boolean> deleteToolTrigger(@PathVariable String id, @PathVariable String triggerId) {
        return R.status(eventTriggerService.deleteTriggerBySource(triggerId, ResourceType.TOOL, id));
    }

    /**
     * 删除应用触发器（需对该应用具备管理权限）
     */
    @SaCheckPerm(PermissionEnum.APPLICATION_TRIGGER_DELETE)
    @DeleteMapping("/APPLICATION/{id}/trigger/{triggerId}")
    public R<Boolean> deleteApplicationTrigger(@PathVariable String id, @PathVariable String triggerId) {
        return R.status(eventTriggerService.deleteTriggerBySource(triggerId, ResourceType.APPLICATION, id));
    }

    /**
     * 资源维度触发器接口的公共逻辑。路径变量固定用 {id} 承载资源 ID，
     * 以便 {@code SaCheckPermAspect} 从路径变量中解析出资源级权限串
     */
    private R<EventTriggerDTO> addSourceTrigger(String sourceType, String sourceId, EventTriggerDTO dto) {
        dto.setIsActive(true);
        eventTriggerService.saveTriggerBySource(dto, false, sourceType, sourceId);
        return R.data(dto);
    }

    private R<SourceEventTriggerVO> getSourceTrigger(String sourceType, String sourceId, String triggerId) {
        return R.data(eventTriggerService.getDetailBySourceId(triggerId, sourceType, sourceId));
    }

    private R<EventTriggerDTO> updateSourceTrigger(String sourceType, String sourceId, String triggerId, EventTriggerDTO dto) {
        dto.setId(triggerId);
        eventTriggerService.saveTriggerBySource(dto, true, sourceType, sourceId);
        return R.data(dto);
    }

    private R<List<EventTriggerVO>> listSourceTriggers(String sourceType, String sourceId) {
        return R.data(eventTriggerService.listBySource(sourceType, sourceId));
    }

}
