package com.maxkb4j.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.application.dto.ChatQueryDTO;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.vo.ApplicationStatisticsVO;
import com.maxkb4j.application.vo.ChatRecordDetailVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@Mapper
public interface ApplicationChatMapper extends BaseMapper<ApplicationChatEntity>{

    List<ChatRecordDetailVO> chatRecordDetail(List<String>  ids);

    List<ApplicationStatisticsVO> statistics(String appId, ChatQueryDTO query);

    List<ApplicationStatisticsVO> userTokenUsage(String appId, ChatQueryDTO query);

    List<ApplicationStatisticsVO> topQuestions(String appId, ChatQueryDTO query);
}
