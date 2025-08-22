package com.tarzan.maxkb4j.module.functionlib.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.core.enums.PermissionType;
import com.tarzan.maxkb4j.module.functionlib.domain.entity.FunctionLibEntity;
import com.tarzan.maxkb4j.module.functionlib.domain.vo.FunctionLibVO;
import com.tarzan.maxkb4j.module.functionlib.mapper.FunctionLibMapper;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.util.BeanUtil;
import com.tarzan.maxkb4j.util.PageUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@Service
@AllArgsConstructor
public class FunctionLibService extends ServiceImpl<FunctionLibMapper, FunctionLibEntity>{

    private final UserService userService;

    public IPage<FunctionLibVO> pageList(int current, int size, String name) {
        IPage<FunctionLibEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<FunctionLibEntity> wrapper= Wrappers.lambdaQuery();
        if(StringUtils.isNotBlank(name)){
            wrapper.like(FunctionLibEntity::getName,name);
        }
        wrapper.eq(FunctionLibEntity::getPermissionType, PermissionType.PUBLIC.name()).or().eq(FunctionLibEntity::getUserId, StpUtil.getLoginIdAsString());
        wrapper.orderByDesc(FunctionLibEntity::getCreateTime);
        this.page(page,wrapper);
        Map<String, String> nicknameMap=userService.getNicknameMap();
        return PageUtil.copy(page, func->{
            FunctionLibVO vo = BeanUtil.copy(func, FunctionLibVO.class);
            vo.setNickname(nicknameMap.get(func.getUserId()));
            return vo;
        });
    }

    public List<FunctionLibEntity> getUserId(String userId) {
        return this.list(Wrappers.<FunctionLibEntity>lambdaQuery().eq(FunctionLibEntity::getUserId, userId));
    }
}
