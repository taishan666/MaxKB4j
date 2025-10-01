package com.tarzan.maxkb4j.module.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author tarzan
 * @date 2025-01-10 11:46:06
 */
@Mapper
public interface ApplicationChatRecordMapper extends BaseMapper<ApplicationChatRecordEntity>{

    List<ApplicationStatisticsVO> chatRecordCountTrend(String appId, @Param("query") ChatQueryDTO query);
}
