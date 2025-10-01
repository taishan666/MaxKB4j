package com.tarzan.maxkb4j.module.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationVersionEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationVersionMapper;
import com.tarzan.maxkb4j.common.util.BeanUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author tarzan
 * @date 2024-12-28 18:47:27
 */
@AllArgsConstructor
@Service
public class ApplicationVersionService extends ServiceImpl<ApplicationVersionMapper, ApplicationVersionEntity>{

    public ApplicationVO getAppLatestOne(String appId) {
        LambdaQueryWrapper<ApplicationVersionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApplicationVersionEntity::getApplicationId, appId);
        wrapper.last("limit 1");
        wrapper.orderByDesc(ApplicationVersionEntity::getCreateTime);
        ApplicationVersionEntity entity = this.getOne(wrapper);
        if (entity == null) {
            return null;
        }
        ApplicationVO vo = BeanUtil.copy(entity, ApplicationVO.class);
        vo.setId(entity.getApplicationId());
        vo.setName(entity.getApplicationName());
        return vo;
    }
}
