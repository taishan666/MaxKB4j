package com.tarzan.maxkb4j.module.functionlib.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.functionlib.entity.FunctionLibEntity;
import com.tarzan.maxkb4j.module.functionlib.mapper.FunctionLibMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
/**
 * @author tarzan
 * @date 2025-01-25 22:00:45
 */
@Service
public class FunctionLibService extends ServiceImpl<FunctionLibMapper, FunctionLibEntity>{

    public IPage<FunctionLibEntity> pageList(int current, int size, String name) {
        IPage<FunctionLibEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<FunctionLibEntity> wrapper= Wrappers.lambdaQuery();
        if(StringUtils.isNotBlank(name)){
            wrapper.like(FunctionLibEntity::getName,name);
        }
        wrapper.orderByDesc(FunctionLibEntity::getCreateTime);
        return this.page(page,wrapper);
    }
}
