package com.tarzan.maxkb4j.module.application.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domain.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationChatUserStatsEntity;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationStatisticsVO;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatUserStatsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-29 10:34:03
 */
@Service
public class ApplicationChatUserStatsService extends ServiceImpl<ApplicationChatUserStatsMapper, ApplicationChatUserStatsEntity>{

    public List<ApplicationStatisticsVO> getCustomerCountTrend(String appId, ChatQueryDTO query) {
        return baseMapper.getCustomerCountTrend(appId,query);
    }

    public ApplicationChatUserStatsEntity getByUserIdAndAppId(String chatUserId, String appId) {
        return this.getOne(Wrappers.<ApplicationChatUserStatsEntity>lambdaQuery().eq(ApplicationChatUserStatsEntity::getChatUserId,chatUserId).eq(ApplicationChatUserStatsEntity::getApplicationId,appId));
    }
}
