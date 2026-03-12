package com.maxkb4j.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.util.*;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.dto.ToolQuery;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.handler.ToolConnectionHandler;
import com.maxkb4j.tool.handler.ToolImportExportHandler;
import com.maxkb4j.tool.handler.ToolValidationHandler;
import com.maxkb4j.tool.mapper.ToolMapper;
import com.maxkb4j.tool.util.McpToolUtil;
import com.maxkb4j.tool.vo.McpToolVO;
import com.maxkb4j.tool.vo.ToolVO;
import com.maxkb4j.user.service.IUserService;
import dev.langchain4j.mcp.client.McpClient;
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
import java.net.URISyntaxException;
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
    public boolean toolImport(MultipartFile file, String folderId) throws IOException {
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
        userResourcePermissionService.remove(AuthTargetType.APPLICATION, id);
        return this.removeById(id);
    }

    public List<ToolEntity> listTools(String scope, String toolType) {
        String loginId = StpKit.ADMIN.getLoginIdAsString();
        Set<String> role = userService.getRoleById(loginId);
        LambdaQueryWrapper<ToolEntity> wrapper = Wrappers.lambdaQuery();
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

    public List<ToolEntity> store(String name) throws IOException, URISyntaxException {
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


    public ToolVO getVoById(String id) {
        ToolVO vo = new ToolVO();
        ToolEntity tool = this.getById(id);
        if (tool != null) {
            vo = BeanUtil.copy(tool, ToolVO.class);
            String nickname =userService.getNickname(vo.getUserId());
            vo.setNickname(nickname);
            if (ToolConstants.ToolType.SKILL.equals(tool.getToolType())) {
                OssFile file = mongoFileService.getFile(tool.getCode());
                vo.setFileList(file == null ? List.of() : List.of(file));
            }
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

    @Override
    public List<McpClient> getMcpClients(JSONObject mcpServersJson) {
        return McpToolUtil.getMcpClients(mcpServersJson);
    }
}
