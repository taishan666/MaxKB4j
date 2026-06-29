package com.maxkb4j.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.dto.ToolQuery;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.handler.ToolAssembleHandler;
import com.maxkb4j.tool.handler.ToolConnectionHandler;
import com.maxkb4j.tool.handler.ToolImportExportHandler;
import com.maxkb4j.tool.handler.ToolPermissionHandler;
import com.maxkb4j.tool.handler.ToolSkillHandler;
import com.maxkb4j.tool.handler.ToolValidationHandler;
import com.maxkb4j.tool.mapper.ToolMapper;
import com.maxkb4j.tool.util.McpToolUtil;
import com.maxkb4j.tool.vo.McpToolVO;
import com.maxkb4j.tool.vo.ToolVO;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * 工具服务：仅负责编排，具体职责委托给各 Handler。
 *
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolService extends ServiceImpl<ToolMapper, ToolEntity> implements IToolService {

    private final IUserResourcePermissionService userResourcePermissionService;
    private final ToolValidationHandler validationHandler;
    private final ToolImportExportHandler importExportHandler;
    private final ToolConnectionHandler connectionHandler;
    private final ToolPermissionHandler permissionHandler;
    private final ToolSkillHandler skillHandler;
    private final ToolAssembleHandler assembleHandler;

    public IPage<ToolVO> pageList(int current, int size, ToolQuery query) {
        IPage<ToolEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<ToolEntity> wrapper = buildPageQueryWrapper(query);
        permissionHandler.applyRoleFilter(wrapper);
        wrapper.orderByDesc(ToolEntity::getCreateTime);
        this.page(page, wrapper);
        return assembleHandler.assemblePage(page);
    }

    @Transactional
    public boolean saveTool(ToolEntity entity) {
        this.save(entity);
        skillHandler.onCreate(entity);
        return userResourcePermissionService.ownerSave(AuthTargetType.TOOL, entity.getId(), entity.getUserId());
    }

    public boolean mcpServerConfigValid(ToolEntity entity) {
        return validationHandler.validateMcpServerConfig(entity);
    }

    public void toolExport(String id, HttpServletResponse response) {
        ToolEntity entity = this.getById(id);
        importExportHandler.exportTool(entity, response);
    }

    @Transactional
    public boolean toolImport(MultipartFile file, String folderId) {
        ToolEntity tool = importExportHandler.importTool(file, folderId);
        return this.saveTool(tool);
    }

    public boolean testConnection(String code) {
        try {
            return connectionHandler.testConnection(code);
        } catch (Exception e) {
            log.error("连接测试失败", e);
            return false;
        }
    }

    @Transactional
    public boolean removeToolById(String id) {
        ToolEntity entity = this.getById(id);
        skillHandler.onDelete(entity);
        userResourcePermissionService.remove(AuthTargetType.TOOL, id);
        return this.removeById(id);
    }

    /** 完整字段版本：供需要 code / initParams / inputFieldList 等执行所需信息的场景使用。 */
    public List<ToolEntity> listTools(String folderId, String scope, String toolType) {
        LambdaQueryWrapper<ToolEntity> wrapper = buildListWrapper(folderId, scope, toolType)
                .select(
                        ToolEntity::getId,
                        ToolEntity::getName,
                        ToolEntity::getDesc,
                        ToolEntity::getIcon,
                        ToolEntity::getToolType,
                        ToolEntity::getCode,
                        ToolEntity::getInitFieldList,
                        ToolEntity::getInitParams,
                        ToolEntity::getInputFieldList,
                        ToolEntity::getIsActive
                );
        return this.list(wrapper);
    }

    /** 轻量字段版本：供前端列表展示使用。 */
    public List<ToolEntity> toolList(String folderId, String scope, String toolType) {
        LambdaQueryWrapper<ToolEntity> wrapper = buildListWrapper(folderId, scope, toolType)
                .select(
                        ToolEntity::getId,
                        ToolEntity::getName,
                        ToolEntity::getDesc,
                        ToolEntity::getIcon,
                        ToolEntity::getToolType,
                        ToolEntity::getIsActive
                );
        return this.list(wrapper);
    }

    /**
     * 更新工具：处理 Skill 文件替换后入库，并返回组装好的 VO。
     */
    public ToolVO updateTool(ToolEntity dto) throws IOException {
        ToolEntity oldTool = this.getById(dto.getId());
        if (oldTool == null) {
            return null;
        }
        skillHandler.onUpdate(oldTool, dto);
        this.updateById(dto);
        return assembleHandler.assemble(dto);
    }

    /** 获取工具详情。 */
    public ToolVO getVoById(String id) {
        return assembleHandler.assemble(this.getById(id));
    }

    public String uploadSkillFile(MultipartFile file) throws IOException {
        return skillHandler.uploadSkillFile(file);
    }

    @Override
    public List<McpToolVO> getMcpToolVos(JSONObject mcpServersJson) {
        return McpToolUtil.getToolVos(mcpServersJson);
    }

    @Transactional
    public Boolean delMulApplication(List<String> idList) {
        boolean result = true;
        for (String id : idList) {
            result = removeToolById(id) && result;
        }
        return result;
    }

    /** 构造分页查询条件（不含排序与权限过滤）。 */
    private LambdaQueryWrapper<ToolEntity> buildPageQueryWrapper(ToolQuery query) {
        LambdaQueryWrapper<ToolEntity> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(ToolEntity::getName, query.getName());
        }
        if (StringUtils.isNotBlank(query.getCreateUser())) {
            wrapper.eq(ToolEntity::getUserId, query.getCreateUser());
        }
        wrapper.eq(ToolEntity::getFolderId,
                StringUtils.isNotBlank(query.getFolderId()) ? query.getFolderId() : "default");
        if (StringUtils.isNotBlank(query.getScope())) {
            wrapper.eq(ToolEntity::getScope, query.getScope());
        }
        if (StringUtils.isNotBlank(query.getToolType())) {
            wrapper.eq(ToolEntity::getToolType, query.getToolType());
        }
        if (Objects.nonNull(query.getIsActive())) {
            wrapper.eq(ToolEntity::getIsActive, query.getIsActive());
        }
        return wrapper;
    }

    /** 构造列表查询通用条件（含权限过滤与排序，select 字段由上层指定）。 */
    private LambdaQueryWrapper<ToolEntity> buildListWrapper(String folderId, String scope, String toolType) {
        LambdaQueryWrapper<ToolEntity> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(folderId)) {
            wrapper.eq(ToolEntity::getFolderId, folderId);
        }
        if (StringUtils.isNotBlank(toolType)) {
            wrapper.eq(ToolEntity::getToolType, toolType);
        }
        wrapper.eq(ToolEntity::getIsActive, ToolConstants.Status.ACTIVE);
        wrapper.eq(ToolEntity::getScope, scope);
        wrapper.orderByDesc(ToolEntity::getCreateTime);
        permissionHandler.applyRoleFilter(wrapper);
        return wrapper;
    }
}
