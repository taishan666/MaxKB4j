package com.tarzan.maxkb4j.module.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationVersionEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationVersionMapper;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeService;
import com.tarzan.maxkb4j.util.BeanUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-28 18:47:27
 */
@AllArgsConstructor
@Service
public class ApplicationVersionService extends ServiceImpl<ApplicationVersionMapper, ApplicationVersionEntity>{

    private final ApplicationKnowledgeMappingService knowledgeMappingService;
    private final KnowledgeService datasetService;
    public ApplicationVO getDetail(String appId) {
        LambdaQueryWrapper<ApplicationVersionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApplicationVersionEntity::getApplicationId, appId);
        wrapper.last("limit 1");
        wrapper.orderByDesc(ApplicationVersionEntity::getCreateTime);
        ApplicationVersionEntity entity = this.getOne(wrapper);
        if (entity == null) {
            return null;
        }
        ApplicationVO vo = BeanUtil.copy(entity, ApplicationVO.class);
        List<String> datasetIds = knowledgeMappingService.getDatasetIdsByAppId(appId);
        vo.setKnowledgeIdList(datasetIds);
        if (!CollectionUtils.isEmpty(vo.getKnowledgeIdList())) {
            vo.setKnowledgeList(datasetService.listByIds(datasetIds));
        }else {
            vo.setKnowledgeList(new ArrayList<>());
        }
        return vo;
    }
}
