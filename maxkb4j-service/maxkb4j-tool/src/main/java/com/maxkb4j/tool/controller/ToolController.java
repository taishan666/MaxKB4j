package com.maxkb4j.tool.controller;

import cn.hutool.http.HttpResponse;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.domain.api.R;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.core.executor.GroovyScriptExecutor;
import com.maxkb4j.core.executor.HttpRequestExecutor;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.dto.ToolDTO;
import com.maxkb4j.tool.dto.ToolInputField;
import com.maxkb4j.tool.dto.ToolQuery;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.service.ToolService;
import com.maxkb4j.tool.vo.ToolVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;
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
        return R.success(Map.of("folders", List.of(), "tools", toolService.listTools(ToolConstants.Scope.WORKSPACE, toolType)));
    }

    @SaCheckPerm(PermissionEnum.TOOL_READ)
    @GetMapping("/workspace/default/tool/tool_list")
    public R<Map<String, List<ToolEntity>>> toolList(String scope, String toolType) {
        return R.success(Map.of("shared_tools", List.of(), "tools", toolService.listTools(scope, toolType)));
    }
    @SaCheckPerm(PermissionEnum.TOOL_READ)
    @GetMapping("/workspace/internal/tool")
    public R<List<ToolEntity>> internalTools(String name) throws IOException, URISyntaxException {
        return R.success(toolService.store(name));
    }

    @SaCheckPerm(PermissionEnum.TOOL_CREATE)
    @PostMapping("/workspace/default/tool/{templateId}/add_internal_tool")
    public R<ToolEntity> addInternalTool(@PathVariable String templateId,@RequestBody ToolEntity dto) {
        dto.setId(null);
        dto.setUserId(StpKit.ADMIN.getLoginIdAsString());
        dto.setTemplateId(templateId);
        dto.setScope("WORKSPACE");
        dto.setToolType(ToolConstants.ToolType.CUSTOM);
        Date now = new Date();
        dto.setCreateTime(now);
        dto.setUpdateTime(now);
        dto.setIsActive(false);
        toolService.saveTool(dto);
        return R.data(dto);
    }

    @SaCheckPerm(PermissionEnum.TOOL_CREATE)
    @PostMapping("/workspace/default/tool")
    public R<ToolEntity> toolLib(@RequestBody ToolEntity dto) {
        dto.setIsActive(true);
        if (StringUtils.isBlank(dto.getToolType())) {
            dto.setToolType(ToolConstants.ToolType.CUSTOM);
        }
        dto.setUserId(StpKit.ADMIN.getLoginIdAsString());
        dto.setScope("WORKSPACE");
        if (toolService.mcpServerConfigValid(dto)){
            toolService.saveTool(dto);
        }else {
            return R.fail("请检查配置信息");
        }
        return R.data(dto);
    }

    @SaCheckPerm(PermissionEnum.TOOL_DEBUG)
    @PostMapping("/workspace/default/tool/debug")
    public R<Object> debug(@RequestBody ToolDTO dto) {
        Map<String, Object> params = new HashMap<>(5);
        if (!CollectionUtils.isEmpty(dto.getDebugFieldList())) {
            for (ToolInputField inputField : dto.getDebugFieldList()) {
                params.put(inputField.getName(), inputField.getValue());
            }
        }
        log.info("input params: {}", params);
        String toolType = dto.getToolType();
        Object result="";
        if (ToolConstants.ToolType.HTTP.equals(toolType)){
            HttpResponse response=  new HttpRequestExecutor(dto.getCode()).execute(params);
            result=response.body();
        }else {
            // 执行脚本并返回结果
            result=new GroovyScriptExecutor(dto.getCode(),dto.getInitParams()).execute(params);;
        }
        return R.data(result);

    }

    @SaCheckPerm(PermissionEnum.TOOL_READ)
    @GetMapping("/workspace/default/tool/{id}")
    public R<ToolVO> get(@PathVariable String id) {
        return R.data(toolService.getVoById(id));
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
        return R.data(toolService.getVoById(id));
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
    public void toolExport(@PathVariable("id") String id, HttpServletResponse response) {
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

    @SaCheckPerm(PermissionEnum.TOOL_EDIT)
    @PutMapping("/workspace/default/tool/upload_skill_file")
    public R<String> uploadSkillFile(MultipartFile file) throws IOException {
        return R.data(toolService.uploadSkillFile(file));
    }


}
