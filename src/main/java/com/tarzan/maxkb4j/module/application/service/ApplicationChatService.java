package com.tarzan.maxkb4j.module.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.vo.ApplicationStatisticsVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@Service
public class ApplicationChatService extends ServiceImpl<ApplicationChatMapper, ApplicationChatEntity>{

    public IPage<ApplicationChatEntity> chatLogs(Page<ApplicationChatEntity> page, UUID appId, ChatQueryDTO query) {
        return baseMapper.chatLogs(page,appId,query);
    }

    public List<ApplicationStatisticsVO> statistics(UUID appId, ChatQueryDTO query) {
        return baseMapper.statistics(appId,query);
    }
}
