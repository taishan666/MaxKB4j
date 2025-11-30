package com.tarzan.maxkb4j.module.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationStatisticsVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatRecordDetailVO;
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
}
