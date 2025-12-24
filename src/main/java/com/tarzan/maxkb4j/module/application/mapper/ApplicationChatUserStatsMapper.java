package com.tarzan.maxkb4j.module.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.application.domain.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationChatUserStatsEntity;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationStatisticsVO;
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

}
