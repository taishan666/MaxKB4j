package com.tarzan.maxkb4j.module.mcplib.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.mcplib.entity.McpLibEntity;
import com.tarzan.maxkb4j.module.mcplib.mapper.McpLibMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@Service
public class McpLibService extends ServiceImpl<McpLibMapper, McpLibEntity>{

    public IPage<McpLibEntity> pageList(int current, int size, String name) {
        IPage<McpLibEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<McpLibEntity> wrapper= Wrappers.lambdaQuery();
        if(StringUtils.isNotBlank(name)){
            wrapper.like(McpLibEntity::getName,name);
        }
        wrapper.eq(McpLibEntity::getPermissionType, "PUBLIC").or().eq(McpLibEntity::getUserId, StpUtil.getLoginIdAsString());
        wrapper.orderByDesc(McpLibEntity::getCreateTime);
        return this.page(page,wrapper);
    }
}
