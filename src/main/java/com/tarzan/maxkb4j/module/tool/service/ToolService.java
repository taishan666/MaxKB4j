package com.tarzan.maxkb4j.module.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.util.*;
import com.tarzan.maxkb4j.module.system.permission.constant.AuthTargetType;
import com.tarzan.maxkb4j.module.system.permission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.user.constants.RoleType;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.module.tool.consts.ToolConstants;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolQuery;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.domain.vo.ToolVO;
import com.tarzan.maxkb4j.module.tool.mapper.ToolMapper;
import com.tarzan.maxkb4j.module.tool.handler.ToolConnectionHandler;
import com.tarzan.maxkb4j.module.tool.handler.ToolImportExportHandler;
import com.tarzan.maxkb4j.module.tool.handler.ToolValidationHandler;
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
public class ToolService extends ServiceImpl<ToolMapper, ToolEntity> {

    private final UserService userService;
    private final UserResourcePermissionService userResourcePermissionService;
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

    public void toolExport(String id, HttpServletResponse response) throws IOException {
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
            log.error("连接测试失败: {}",e);
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
        if (role.contains(RoleType.ADMIN)){
            return this.list(wrapper);
        }else {
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
                String[] parts = filename.split("-", 2);
                String version =parts.length>1?parts[1].substring(0, parts[1].length() - 5): ToolConstants.Defaults.DEFAULT_VERSION;
                String text = IoUtil.readToString(resource.getInputStream());
                ToolEntity tool = JSONObject.parseObject(text, ToolEntity.class);
                if (tool!=null){
                    tool.setLabel(parentDirName);
                    tool.setVersion(version);
                    list.add(tool);
                }
            }
        }
        if(StringUtils.isNotBlank(name)) {
            list = list.stream().filter(tool -> tool.getName().contains(name)).collect(Collectors.toList());
        }
        return list;
    }


}
