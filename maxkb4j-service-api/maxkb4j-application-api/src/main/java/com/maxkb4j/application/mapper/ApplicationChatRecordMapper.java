package com.maxkb4j.application.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author tarzan
 * @date 2025-01-10 11:46:06
 */
@Mapper
public interface ApplicationChatRecordMapper extends BaseMapper<ApplicationChatRecordEntity>{

    List<ApplicationChatRecordEntity> listByAppIdAndChatUserId(String applicationId, String chatUserId,int pageSize,int offset);

    long countByAppIdAndChatUserId(String applicationId, String chatUserId);
}
