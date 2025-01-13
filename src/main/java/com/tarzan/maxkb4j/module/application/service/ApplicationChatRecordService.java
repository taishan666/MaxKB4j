package com.tarzan.maxkb4j.module.application.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.chatpipeline.ChatCache;
import com.tarzan.maxkb4j.module.chatpipeline.ChatInfo;
import com.tarzan.maxkb4j.util.BeanUtil;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @author tarzan
 * @date 2025-01-10 11:46:06
 */
@Service
public class ApplicationChatRecordService extends ServiceImpl<ApplicationChatRecordMapper, ApplicationChatRecordEntity>{

    public ApplicationChatRecordVO getChatRecordInfo(UUID chatId,UUID chatRecordId) {
       // ApplicationChatRecordEntity chatRecordEntity = this.getById(chatRecordId);
        ChatInfo chatInfo= ChatCache.get(chatId);
        ApplicationChatRecordEntity chatRecord=chatInfo.getChatRecordList().stream().filter(e->e.getId().equals(chatRecordId)).findAny().orElse(null);
        ApplicationChatRecordVO chatRecordVO = BeanUtil.copy(chatRecord, ApplicationChatRecordVO.class);
        return chatRecordVO;

    }
}
