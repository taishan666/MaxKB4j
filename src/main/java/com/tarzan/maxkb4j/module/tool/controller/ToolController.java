package com.tarzan.maxkb4j.module.tool.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tarzan.maxkb4j.common.aop.SaCheckPerm;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.util.StpKit;
import com.tarzan.maxkb4j.module.system.user.enums.PermissionEnum;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolDTO;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolInputField;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolQuery;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.domain.vo.ToolVO;
import com.tarzan.maxkb4j.module.tool.service.ToolService;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
@RequiredArgsConstructor
@Slf4j
public class ToolController {

    private final ToolService toolService;

    @SaCheckPerm(PermissionEnum.TOOL_READ)
    @GetMapping("/workspace/default/tool/{current}/{size}")
    public R<IPage<ToolVO>> page(@PathVariable int current, @PathVariable int size, ToolQuery query) {
        return R.success(toolService.pageList(current, size, query));
    }

    @SaCheckPerm(PermissionEnum.TOOL_READ)
    @GetMapping("/workspace/default/tool")
    public R<Map<String, List<ToolEntity>>> list(String folderId, String toolType) {
        return R.success(Map.of("folders", List.of(), "tools", toolService.listTools("WORKSPACE", toolType)));
    }

    @SaCheckPerm(PermissionEnum.TOOL_READ)
    @GetMapping("/workspace/default/tool/tool_list")
    public R<Map<String, List<ToolEntity>>> toolList(String scope, String toolType) {
        return R.success(Map.of("shared_tools", List.of(), "tools", toolService.listTools(scope, toolType)));
    }
    @SaCheckPerm(PermissionEnum.TOOL_READ)
    @GetMapping("/workspace/internal/tool")
    public R<List<ToolEntity>> internalTools(String name) {
        LambdaQueryWrapper<ToolEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ToolEntity::getToolType, "INTERNAL");
        if (StringUtils.isNotBlank(name)) {
            wrapper.like(ToolEntity::getName, name);
        }
        return R.success(toolService.list(wrapper));
    }

    @SaCheckPerm(PermissionEnum.TOOL_CREATE)
    @PostMapping("/workspace/default/tool/{templateId}/add_internal_tool")
    public R<ToolEntity> addInternalTool(@PathVariable String templateId) {
        ToolEntity entity = toolService.getById(templateId);
        entity.setId(null);
        entity.setUserId(StpKit.ADMIN.getLoginIdAsString());
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setScope("WORKSPACE");
        entity.setToolType("CUSTOM");
        entity.setUpdateTime(now);
        toolService.saveInfo(entity);
        return R.data(entity);
    }

    @SaCheckPerm(PermissionEnum.TOOL_CREATE)
    @PostMapping("/workspace/default/tool")
    public R<ToolEntity> toolLib(@RequestBody ToolEntity dto) {
        dto.setIsActive(true);
        if (StringUtils.isBlank(dto.getToolType())) {
            dto.setToolType("CUSTOM");
        }
        dto.setUserId(StpKit.ADMIN.getLoginIdAsString());
        dto.setScope("WORKSPACE");
        if (toolService.mcpServerConfigValid(dto)){
            toolService.saveInfo(dto);
        }else {
            return R.fail("请检查配置信息");
        }
        return R.data(dto);
    }

    @SaCheckPerm(PermissionEnum.TOOL_EDIT)
    @PostMapping("/workspace/default/tool/debug")
    public R<Object> debug(@RequestBody ToolDTO dto) {
        Map<String, Object> params = new HashMap<>(5);
        params.putAll(dto.getInitParams());
        if (!CollectionUtils.isEmpty(dto.getDebugFieldList())) {
            for (ToolInputField inputField : dto.getDebugFieldList()) {
                params.put(inputField.getName(), inputField.getValue());
            }
        }
        log.info("Groovy binding params: {}", params);
        Binding binding = new Binding(params);
        // 创建 GroovyShell 并执行脚本
        GroovyShell shell = new GroovyShell(binding);
        // 执行脚本并返回结果
        return R.data(shell.evaluate(dto.getCode()));
    }

    @SaCheckPerm(PermissionEnum.TOOL_READ)
    @GetMapping("/workspace/default/tool/{id}")
    public R<ToolEntity> get(@PathVariable String id) {
        return R.data(toolService.getById(id));
    }

    @SaCheckPerm(PermissionEnum.TOOL_EDIT)
    @PutMapping("/workspace/default/tool/{id}")
    public R<ToolEntity> tool(@PathVariable String id, @RequestBody ToolEntity dto) {
        dto.setId(id);
        if (toolService.mcpServerConfigValid(dto)){
            toolService.updateById(dto);
        }else {
            return R.fail("请检查配置信息");
        }
        return R.data(toolService.getById(id));
    }

    @SaCheckPerm(PermissionEnum.TOOL_DELETE)
    @DeleteMapping("/workspace/default/tool/{id}")
    public R<Boolean> tool(@PathVariable String id) {
        return R.status(toolService.removeToolById(id));
    }

    @PostMapping("/workspace/default/tool/pylint")
    public R<List<ToolEntity>> pylint(@RequestBody ToolEntity dto) {
        return R.success(Collections.emptyList());
    }

    @SaCheckPerm(PermissionEnum.TOOL_EXPORT)
    @GetMapping("/workspace/default/tool/{id}/export")
    public void toolExport(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        toolService.toolExport(id, response);
    }

    @SaCheckPerm(PermissionEnum.TOOL_IMPORT)
    @PostMapping("/workspace/default/tool/import")
    public R<Boolean> toolImport(MultipartFile file, String folderId) throws IOException {
        return R.status(toolService.toolImport(file, folderId));
    }

    @SaCheckPerm(PermissionEnum.TOOL_EDIT)
    @PostMapping("/workspace/default/tool/test_connection")
    public R<Boolean> testConnection(@RequestBody ToolEntity dto) {
        return R.status(toolService.testConnection(dto.getCode()));
    }


}
