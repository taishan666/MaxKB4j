package com.tarzan.maxkb4j.module.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.application.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.vo.ApplicationStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author tarzan
 * @date 2025-01-10 11:46:06
 */
@Mapper
public interface ApplicationChatRecordMapper extends BaseMapper<ApplicationChatRecordEntity>{

    List<ApplicationStatisticsVO> statistics(String appId, @Param("query") ChatQueryDTO query);
}
