package com.maxkb4j.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.application.dto.ChatQueryDTO;
import com.maxkb4j.application.entity.ApplicationChatUserStatsEntity;
import com.maxkb4j.application.vo.ApplicationStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-29 10:34:03
 */
@Mapper
public interface ApplicationChatUserStatsMapper extends BaseMapper<ApplicationChatUserStatsEntity>{

    List<ApplicationStatisticsVO> getCustomerCountTrend(String appId, @Param("query") ChatQueryDTO query);

    /**
     * 原子插入统计行，依赖唯一索引 (chat_user_id, application_id)：行已存在时不做任何事。
     *
     * @return 插入返回 1；行已存在返回 0
     */
    int insertIfAbsent(ApplicationChatUserStatsEntity entity);

    /**
     * 原子自增 access_num 与 intra_day_access_num，避免并发"读-改-写"丢失更新。
     *
     * @return 受影响行数（0 表示统计行不存在）
     */
    int incrementAccessNum(@Param("chatUserId") String chatUserId, @Param("applicationId") String applicationId);

}
