package com.tarzan.maxkb4j.module.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.module.application.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.vo.ApplicationStatisticsVO;
import com.tarzan.maxkb4j.module.application.vo.ChatRecordDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@Mapper
public interface ApplicationChatMapper extends BaseMapper<ApplicationChatEntity>{

    IPage<ApplicationChatEntity> chatLogs(Page<ApplicationChatEntity> page, String appId,@Param("query") ChatQueryDTO query);


    List<ApplicationStatisticsVO> statistics(String appId,@Param("query") ChatQueryDTO query);

    List<ChatRecordDetailVO> chatRecordDetail(String ids);
}
