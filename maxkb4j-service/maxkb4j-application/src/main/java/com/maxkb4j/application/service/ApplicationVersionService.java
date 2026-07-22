package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.entity.ApplicationVersionEntity;
import com.maxkb4j.application.service.IApplicationVersionService;
import com.maxkb4j.application.mapper.ApplicationVersionMapper;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.util.BeanUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-28 18:47:27
 */
@Service
public class ApplicationVersionService extends ServiceImpl<ApplicationVersionMapper, ApplicationVersionEntity> implements IApplicationVersionService {

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

    /**
     * 查询指定应用的版本列表，按创建时间倒序。
     */
    public List<ApplicationVersionEntity> listByApplicationId(String appId) {
        return this.lambdaQuery()
                .eq(ApplicationVersionEntity::getApplicationId, appId)
                .orderByDesc(ApplicationVersionEntity::getCreateTime)
                .list();
    }
}
