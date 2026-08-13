package com.maxkb4j.tool.controller;

import cn.hutool.http.HttpResponse;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.annotation.CurrentUserId;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.tool.entity.ToolInputField;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.dto.ToolDTO;
import com.maxkb4j.tool.dto.ToolQuery;
import com.maxkb4j.tool.dto.ToolSaveDTO;
import com.maxkb4j.tool.dto.ToolConnectionTestDTO;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.service.IToolExecuteService;
import com.maxkb4j.tool.service.IToolInternalService;
import com.maxkb4j.tool.vo.ToolCardVO;
import com.maxkb4j.tool.vo.ToolListVO;
import com.maxkb4j.tool.vo.ToolVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
@Slf4j
public class ToolController {

    private final IToolInternalService toolService;
    private final IToolExecuteService toolExecuteService;

    @SaCheckPerm(PermissionEnum.TOOL_READ)
    @GetMapping("/tool/{current}/{size}")
    public R<IPage<ToolCardVO>> page(@PathVariable int current, @PathVariable int size, ToolQuery query) {
        return R.data(toolService.pageList(current, size, query));
    }

    @SaCheckPerm(PermissionEnum.TOOL_READ)
    @GetMapping("/tool")
    public R<Map<String, List<ToolCardVO>>> tools(String folderId, String[] toolTypeList) {
        return R.data(Map.of("folders", List.of(), "tools", toolService.listTools(folderId,ToolConstants.Scope.WORKSPACE, toolTypeList)));
    }

    @SaCheckPerm(PermissionEnum.TOOL_READ)
    @GetMapping("/tool/tool_list")
    public R<Map<String, List<ToolListVO>>> toolList(String scope, String toolType) {
        return R.data(Map.of("shared_tools", List.of(), "tools", toolService.toolList(scope, toolType)));
    }

    @SaCheckPerm(PermissionEnum.TOOL_CREATE)
    @PostMapping("/tool/{templateId}/add_internal_tool")
    public R<ToolVO> addInternalTool(@PathVariable String templateId,@RequestBody ToolSaveDTO dto, @CurrentUserId String userId) {
        ToolEntity entity = BeanUtil.copy(dto, ToolEntity.class);
        entity.setId(null);
        entity.setUserId(userId);
        entity.setTemplateId(templateId);
        entity.setScope(ToolConstants.Scope.WORKSPACE);
        if (StringUtils.isBlank(entity.getFolderId())){
            entity.setFolderId(ToolConstants.Defaults.DEFAULT_FOLDER_ID);
        }
        entity.setToolType(ToolConstants.ToolType.CUSTOM);
        Date now = new Date();
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setIsActive(false);
        toolService.saveTool(entity);
        return R.data(BeanUtil.copy(entity, ToolVO.class));
    }

    @SaCheckPerm(PermissionEnum.TOOL_CREATE)
    @PostMapping("/tool")
    public R<ToolVO> toolLib(@RequestBody ToolSaveDTO dto, @CurrentUserId String userId) {
        ToolEntity entity = BeanUtil.copy(dto, ToolEntity.class);
        entity.setIsActive(true);
        if (StringUtils.isBlank(entity.getToolType())) {
            entity.setToolType(ToolConstants.ToolType.CUSTOM);
        }
        if (StringUtils.isBlank(entity.getFolderId())){
            entity.setFolderId(ToolConstants.Defaults.DEFAULT_FOLDER_ID);
        }
        entity.setUserId(userId);
        entity.setScope("WORKSPACE");
        if (toolService.mcpServerConfigValid(entity)){
            toolService.saveTool(entity);
        }else {
            return R.fail(I18nUtil.get("tool.config.invalid"));
        }
        return R.data(BeanUtil.copy(entity, ToolVO.class));
    }

    @SaCheckPerm(PermissionEnum.TOOL_DEBUG)
    @PostMapping("/tool/debug")
    public R<Object> debug(@Valid @RequestBody ToolDTO dto) throws IOException {
        Map<String, Object> params = new HashMap<>(5);
        if (!CollectionUtils.isEmpty(dto.getDebugFieldList())) {
            for (ToolInputField inputField : dto.getDebugFieldList()) {
                params.put(inputField.getName(), inputField.getValue());
            }
        }
        log.info("input params: {}", params);
        Object result;
        if (ToolConstants.ToolType.HTTP.equals(dto.getToolType())){
            HttpResponse httpResponse = toolExecuteService.httpExecute(dto.getCode(),params);
            result = httpResponse.body();
        }else {
            result = toolExecuteService.customExecute(dto.getCode(), dto.getInitParams(),params);
        }
        return R.data(result);

    }

    @SaCheckPerm(PermissionEnum.TOOL_READ)
    @GetMapping("/tool/{id}")
    public R<ToolVO> get(@PathVariable String id) {
        return R.data(toolService.getVoById(id));
    }

    @SaCheckPerm(PermissionEnum.TOOL_EDIT)
    @PutMapping("/tool/{id}")
    public R<ToolVO> tool(@PathVariable String id, @RequestBody ToolSaveDTO dto) throws IOException {
        ToolEntity entity = BeanUtil.copy(dto, ToolEntity.class);
        entity.setId(id);
        if (toolService.mcpServerConfigValid(entity)){
            return R.data(toolService.updateTool(entity));
        }else {
            return R.fail(I18nUtil.get("tool.config.invalid"));
        }
    }

    @SaCheckPerm(PermissionEnum.TOOL_DELETE)
    @DeleteMapping("/tool/{id}")
    public R<Boolean> tool(@PathVariable String id) {
        return R.status(toolService.removeToolById(id));
    }

    @SaCheckPerm(PermissionEnum.TOOL_BATCH_DELETE)
    @DeleteMapping("/tool/batchDelete")
    public R<Boolean> delMulTool(@RequestParam("idList") List<String> idList) {
        return R.status(toolService.delMulApplication(idList));
    }

    @PostMapping("/tool/pylint")
    public R<List<ToolVO>> pylint() {
        return R.data(Collections.emptyList());
    }

    @SaCheckPerm(PermissionEnum.TOOL_EXPORT)
    @GetMapping("/tool/{id}/export")
    public void toolExport(@PathVariable("id") String id, HttpServletResponse response) {
        toolService.toolExport(id, response);
    }

    @SaCheckPerm(PermissionEnum.TOOL_IMPORT)
    @PostMapping("/tool/import")
    public R<Boolean> toolImport(MultipartFile file, String folderId) {
        return R.status(toolService.toolImport(file, folderId));
    }

    @SaCheckPerm(PermissionEnum.TOOL_EDIT)
    @PostMapping("/tool/test_connection")
    public R<Boolean> testConnection(@RequestBody ToolConnectionTestDTO dto) {
        return R.status(toolService.testConnection(dto.getCode()));
    }

    @SaCheckPerm(PermissionEnum.TOOL_EDIT)
    @PutMapping("/tool/upload_skill_file")
    public R<String> uploadSkillFile(MultipartFile file) throws IOException {
        return R.data(toolService.uploadSkillFile(file));
    }


}
