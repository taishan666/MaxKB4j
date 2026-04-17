package com.maxkb4j.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.*;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.dto.ToolQuery;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.handler.ToolConnectionHandler;
import com.maxkb4j.tool.handler.ToolImportExportHandler;
import com.maxkb4j.tool.handler.ToolValidationHandler;
import com.maxkb4j.tool.mapper.ToolMapper;
import com.maxkb4j.tool.util.McpToolUtil;
import com.maxkb4j.tool.util.SkillsToolUtil;
import com.maxkb4j.tool.vo.McpToolVO;
import com.maxkb4j.tool.vo.ToolVO;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import com.maxkb4j.user.service.IUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@Service
@RequiredArgsConstructor
public class ToolService  extends ServiceImpl<ToolMapper, ToolEntity> implements IToolService{

    private final IUserService userService;
    private final IOssService mongoFileService;
    private final IUserResourcePermissionService userResourcePermissionService;
    private final ToolValidationHandler validationHandler;
    private final ToolImportExportHandler importExportHandler;
    private final ToolConnectionHandler connectionHandler;

    public IPage<ToolVO> pageList(int current, int size, ToolQuery query) {
        IPage<ToolEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<ToolEntity> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(ToolEntity::getName, query.getName());
        }
        if (StringUtils.isNotBlank(query.getCreateUser())) {
            wrapper.eq(ToolEntity::getUserId, query.getCreateUser());
        }
        if (StringUtils.isNotBlank(query.getFolderId())) {
            wrapper.eq(ToolEntity::getFolderId, query.getFolderId());
        } else {
            wrapper.eq(ToolEntity::getFolderId, "default");
        }
        if (StringUtils.isNotBlank(query.getScope())) {
            wrapper.eq(ToolEntity::getScope, query.getScope());
        }
        if (StringUtils.isNotBlank(query.getToolType())) {
            wrapper.eq(ToolEntity::getToolType, query.getToolType());
        }
        if (Objects.nonNull(query.getIsActive())) {
            wrapper.eq(ToolEntity::getIsActive, query.getIsActive());
        }
        String loginId = StpKit.ADMIN.getLoginIdAsString();
        Set<String> role = userService.getRoleById(loginId);
        if (!CollectionUtils.isEmpty(role)) {
            if (role.contains(RoleType.USER)) {
                List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.TOOL, loginId);
                if (!CollectionUtils.isEmpty(targetIds)) {
                    wrapper.in(ToolEntity::getId, targetIds);
                } else {
                    wrapper.last(" limit 0");
                }
            }
        } else {
            wrapper.last(" limit 0");
        }
        wrapper.orderByDesc(ToolEntity::getCreateTime);
        this.page(page, wrapper);
        Map<String, String> nicknameMap = userService.getNicknameMap();
        return PageUtil.copy(page, func -> {
            ToolVO vo = BeanUtil.copy(func, ToolVO.class);
            vo.setNickname(nicknameMap.get(func.getUserId()));
            return vo;
        });
    }

    @Transactional
    public boolean saveTool(ToolEntity entity) {
        this.save(entity);
        if (ToolConstants.ToolType.SKILL.equals(entity.getToolType())){
            try (InputStream is = mongoFileService.getStream(entity.getCode())) {
                SkillsToolUtil.unzipSkill(is, entity.getId());
            } catch (IOException e) {
                throw new ApiException("Failed to extract the skill file.");
            }
        }
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
            log.error("连接测试失败: {}", e);
            return false;
        }
    }

    @Transactional
    public boolean removeToolById(String id) {
        ToolEntity entity = this.getById(id);
        if (ToolConstants.ToolType.SKILL.equals(entity.getToolType())){
            SkillsToolUtil.deleteDirectory(id);
        }
        userResourcePermissionService.remove(AuthTargetType.TOOL, id);
        return this.removeById(id);
    }

    public List<ToolEntity> listTools(String folderId,String scope, String toolType) {
        String loginId = StpKit.ADMIN.getLoginIdAsString();
        Set<String> role = userService.getRoleById(loginId);
        LambdaQueryWrapper<ToolEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.select(
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
        if (StringUtils.isNotBlank(folderId)) {
            wrapper.eq(ToolEntity::getFolderId, folderId);
        }
        if (StringUtils.isNotBlank(toolType)) {
            wrapper.eq(ToolEntity::getToolType, toolType);
        }
        wrapper.eq(ToolEntity::getIsActive, ToolConstants.Status.ACTIVE);
        wrapper.eq(ToolEntity::getScope, scope);
        wrapper.orderByDesc(ToolEntity::getCreateTime);
        if (role.contains(RoleType.ADMIN)) {
            return this.list(wrapper);
        } else {
            List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.TOOL, StpKit.ADMIN.getLoginIdAsString());
            if (!CollectionUtils.isEmpty(targetIds)) {
                wrapper.in(ToolEntity::getId, targetIds);
            }
        }
        return this.list(wrapper);
    }

    public List<ToolEntity> store(String name) throws IOException {
        List<ToolEntity> list = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:templates/tool/*/*" + ToolConstants.FileType.TOOL_EXTENSION);
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (Objects.requireNonNull(filename).endsWith(ToolConstants.FileType.TOOL_EXTENSION)) {
                // ✅ 安全获取父目录名：从 resource 的 URL 路径中解析
                String parentDirName = JarUtil.getParentDirName(resource);
                String text = IoUtil.readToString(resource.getInputStream());
                ToolEntity tool = JSONObject.parseObject(text, ToolEntity.class);
                if (tool != null) {
                    tool.setLabel(parentDirName);
                    if (StringUtils.isBlank(tool.getVersion())) {
                        tool.setVersion(ToolConstants.Defaults.DEFAULT_VERSION);
                    }
                    list.add(tool);
                }
            }
        }
        if (StringUtils.isNotBlank(name)) {
            list = list.stream().filter(tool -> tool.getName().contains(name)).collect(Collectors.toList());
        }
        return list;
    }

    /**
     * 更新工具
     */
    public ToolVO updateTool(ToolEntity dto) throws IOException {
        // 1. 先查询旧数据，用于后续比对和业务逻辑
        ToolEntity oldTool = this.getById(dto.getId());

        if (oldTool == null) {
            return null; // 或者抛出异常 new BusinessException("工具不存在");
        }

        // 2. 处理 Skill 类型的特殊业务逻辑 (文件更新)
        if (ToolConstants.ToolType.SKILL.equals(dto.getToolType())) {
            if (!oldTool.getCode().equals(dto.getCode())) {
                // 删除旧目录
                SkillsToolUtil.deleteDirectory(oldTool.getId());
                // 解压新文件
                SkillsToolUtil.unzipSkill(mongoFileService.getStream(dto.getCode()), dto.getId());
            }
        }

        // 3. 执行数据库更新
        this.updateById(dto);

        // 4. 复用 VO 组装逻辑 (传入更新后的 dto 作为最新数据)
        return assembleToolVO(dto);
    }

    /**
     * 获取工具详情
     */
    public ToolVO getVoById(String id) {
        ToolEntity tool = this.getById(id);
        // 利用 Optional 简化 null 判断，如果为空则返回 null
        return java.util.Optional.ofNullable(tool)
                .map(this::assembleToolVO)
                .orElse(null);
    }

    /**
     * 【核心重构】提取公共的 VO 组装逻辑
     * 无论是查询还是更新，最终都需要组装 VO 返回，统一在这里处理
     */
    private ToolVO assembleToolVO(ToolEntity tool) {
        // 1. 基础属性拷贝
        ToolVO vo = BeanUtil.copy(tool, ToolVO.class);
        // 2. 补充非实体字段：用户昵称
        String nickname = userService.getNickname(vo.getUserId());
        vo.setNickname(nickname);
        // 3. 补充非实体字段：文件列表 (仅 Skill 类型)
        if (ToolConstants.ToolType.SKILL.equals(tool.getToolType())) {
            OssFile file = mongoFileService.getFile(tool.getCode());
            vo.setFileList(file == null ? List.of() : List.of(file));
        } else {
            // 建议：非 Skill 类型也显式设置为空，避免前端 NPE
            vo.setFileList(List.of());
        }
        return vo;
    }

    public String uploadSkillFile(MultipartFile file) throws IOException {
        return mongoFileService.storeFile(file);
    }

    @Override
    public List<McpToolVO> getMcpToolVos(JSONObject mcpServersJson) {
        return McpToolUtil.getToolVos(mcpServersJson);
    }

    public Boolean delMulApplication(List<String> idList) {
        Boolean result = true;
        for (String id : idList) {
            result = removeToolById(id);
        }
        return result;
    }
}
