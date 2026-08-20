package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.dto.ChatQueryDTO;
import com.maxkb4j.application.entity.ApplicationChatUserStatsEntity;
import com.maxkb4j.application.mapper.ApplicationChatUserStatsMapper;
import com.maxkb4j.application.vo.ApplicationStatisticsVO;
import com.maxkb4j.common.enums.ChatUserType;
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
        return this.getOne(Wrappers.<ApplicationChatUserStatsEntity>lambdaQuery()
                        .select(ApplicationChatUserStatsEntity::getId,ApplicationChatUserStatsEntity::getAccessNum,ApplicationChatUserStatsEntity::getIntraDayAccessNum)
                .eq(ApplicationChatUserStatsEntity::getChatUserId,chatUserId).eq(ApplicationChatUserStatsEntity::getApplicationId,appId));
    }

    /**
     * 确保统计行存在（依赖唯一索引的原子 upsert，替代旧的"先查后插"）。
     *
     * @return true 表示本次调用新建了统计行（首次访问）；false 表示行已存在
     */
    public boolean ensureStatsExists(String chatUserId, ChatUserType chatUserType, String appId) {
        ApplicationChatUserStatsEntity entity = new ApplicationChatUserStatsEntity();
        entity.setId(IdWorker.get32UUID());
        entity.setChatUserId(chatUserId);
        entity.setChatUserType(chatUserType == null ? null : chatUserType.getKey());
        entity.setApplicationId(appId);
        entity.setAccessNum(0);
        entity.setIntraDayAccessNum(0);
        return baseMapper.insertIfAbsent(entity) > 0;
    }

    /**
     * 访问计数原子自增（access_num 与 intra_day_access_num 各加 1）。
     *
     * @return 受影响行数（0 表示统计行不存在）
     */
    public int incrementAccessNum(String chatUserId, String appId) {
        return baseMapper.incrementAccessNum(chatUserId, appId);
    }
}
