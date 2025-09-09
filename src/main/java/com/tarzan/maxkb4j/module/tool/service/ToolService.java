package com.tarzan.maxkb4j.module.tool.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.system.permission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolQuery;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import com.tarzan.maxkb4j.module.tool.domain.vo.ToolVO;
import com.tarzan.maxkb4j.module.tool.mapper.ToolMapper;
import com.tarzan.maxkb4j.util.BeanUtil;
import com.tarzan.maxkb4j.util.PageUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@Service
@AllArgsConstructor
public class ToolService extends ServiceImpl<ToolMapper, ToolEntity>{

    private final UserService userService;
    private final UserResourcePermissionService userResourcePermissionService;

    public IPage<ToolVO> pageList(int current, int size, ToolQuery query) {
        IPage<ToolEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<ToolEntity> wrapper= Wrappers.lambdaQuery();
        if(StringUtils.isNotBlank(query.getName())){
            wrapper.like(ToolEntity::getName,query.getName());
        }
        if(StringUtils.isNotBlank(query.getCreateUser())){
            wrapper.eq(ToolEntity::getUserId,query.getCreateUser());
        }
        if(StringUtils.isNotBlank(query.getScope())){
            wrapper.eq(ToolEntity::getScope,query.getScope());
        }
        if(StringUtils.isNotBlank(query.getToolType())){
            wrapper.eq(ToolEntity::getToolType,query.getToolType());
        }
/*        List<String> targetIds =userResourcePermissionService.getTargetIds("TOOL",loginId);
        wrapper.and(
                w -> w.eq(ToolEntity::getUserId, loginId)
                        .or()
                        .in(!CollectionUtils.isEmpty(targetIds),ToolEntity::getId, targetIds));*/
        String loginId = StpUtil.getLoginIdAsString();
        UserEntity user = userService.validUserById(loginId);
        if (Objects.nonNull(user)){
            if (!CollectionUtils.isEmpty(user.getRole())){
                if (user.getRole().contains("USER")){
                    List<String> targetIds =userResourcePermissionService.getTargetIds("TOOL",loginId);
                    if (!CollectionUtils.isEmpty(targetIds)){
                        wrapper.in(ToolEntity::getId, targetIds);
                    }else {
                        wrapper.last(" limit 0");
                    }
                }
            }else {
                wrapper.last(" limit 0");
            }
        }else{
            wrapper.last(" limit 0");
        }
        wrapper.orderByDesc(ToolEntity::getCreateTime);
        this.page(page,wrapper);
        Map<String, String> nicknameMap=userService.getNicknameMap();
        return PageUtil.copy(page, func->{
            ToolVO vo = BeanUtil.copy(func, ToolVO.class);
            vo.setNickname(nicknameMap.get(func.getUserId()));
            return vo;
        });
    }

    public List<ToolEntity> getUserId(String userId) {
        return this.list(Wrappers.<ToolEntity>lambdaQuery().eq(ToolEntity::getUserId, userId));
    }

    @Transactional
    public void saveInfo(ToolEntity entity) {
        this.save(entity);
        userResourcePermissionService.save("TOOL", entity.getId(), StpUtil.getLoginIdAsString(), "default");
    }
}
