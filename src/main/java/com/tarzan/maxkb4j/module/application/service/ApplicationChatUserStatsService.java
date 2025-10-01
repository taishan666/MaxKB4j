package com.tarzan.maxkb4j.module.application.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatUserStatsEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatUserStatsMapper;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationChatUserStatsVO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-29 10:34:03
 */
@Service
public class ApplicationChatUserStatsService extends ServiceImpl<ApplicationChatUserStatsMapper, ApplicationChatUserStatsEntity>{

    public List<ApplicationChatUserStatsVO> getCustomerCountTrend(String appId, ChatQueryDTO query) {
        return baseMapper.getCustomerCountTrend(appId,query);
    }
}
