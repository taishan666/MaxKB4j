package com.tarzan.maxkb4j.module.tool.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.util.BeanUtil;
import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.common.util.McpToolUtil;
import com.tarzan.maxkb4j.common.util.PageUtil;
import com.tarzan.maxkb4j.module.system.permission.constant.AuthTargetType;
import com.tarzan.maxkb4j.module.system.permission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolQuery;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.domain.vo.ToolVO;
import com.tarzan.maxkb4j.module.tool.mapper.ToolMapper;
import dev.langchain4j.mcp.client.McpClient;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@Service
@AllArgsConstructor
public class ToolService extends ServiceImpl<ToolMapper, ToolEntity> {

    private final UserService userService;
    private final UserResourcePermissionService userResourcePermissionService;

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
        String loginId = StpUtil.getLoginIdAsString();
        UserEntity user = userService.getUserById(loginId);
        if (Objects.nonNull(user)) {
            if (!CollectionUtils.isEmpty(user.getRole())) {
                if (user.getRole().contains("USER")) {
                    List<String> targetIds = userResourcePermissionService.getTargetIds("TOOL", loginId);
                    if (!CollectionUtils.isEmpty(targetIds)) {
                        wrapper.in(ToolEntity::getId, targetIds);
                    } else {
                        wrapper.last(" limit 0");
                    }
                }
            } else {
                wrapper.last(" limit 0");
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
    public void saveInfo(ToolEntity entity) {
        this.save(entity);
        userResourcePermissionService.ownerSave(AuthTargetType.TOOL, entity.getId(), entity.getUserId());
    }

    public void toolExport(String id, HttpServletResponse response) throws IOException {
        ToolEntity entity = this.getById(id);
        byte[] bytes = JSONUtil.toJsonStr(entity).getBytes(StandardCharsets.UTF_8);
        response.setContentType("text/plain");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode(entity.getName(), StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".tool");
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(bytes);
    }

    public boolean toolImport(MultipartFile file, String folderId) throws IOException {
        String text = IoUtil.readToString(file.getInputStream());
        ToolEntity tool = JSONObject.parseObject(text, ToolEntity.class);
        tool.setId(null);
        tool.setIsActive(false);
        tool.setFolderId(folderId);
        return this.save(tool);
    }

    public boolean testConnection(String code) {
        try {
            JSONObject serverConfig = JSONObject.parseObject(code);
            List<McpClient> mcpClients = McpToolUtil.getMcpClients(serverConfig);
            McpClient mcpClient = mcpClients.get(0);
            mcpClient.checkHealth();
            mcpClient.close();
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    @Transactional
    public boolean removeToolById(String id) {
        userResourcePermissionService.remove(AuthTargetType.APPLICATION, id);
        return this.removeById(id);
    }
}
