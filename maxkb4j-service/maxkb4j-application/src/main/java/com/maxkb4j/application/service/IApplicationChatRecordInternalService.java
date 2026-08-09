package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.dto.AddChatImproveDTO;
import com.maxkb4j.application.dto.ChatImproveDTO;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.knowledge.dto.ParagraphDTO;

import java.util.List;

/**
 * 应用对话记录服务「对内」接口。
 */
public interface IApplicationChatRecordInternalService extends IApplicationChatRecordService, IService<ApplicationChatRecordEntity> {

    List<ChatRecordDTO> getChatRecords(String chatId);

    boolean addChatLogs(String appId, AddChatImproveDTO dto);

    ApplicationChatRecordEntity improveChatLog(String chatId, String chatRecordId, String knowledgeId, String docId, ChatImproveDTO dto);

    boolean removeImproveChatLog(String chatId, String chatRecordId, String knowledgeId, String paragraphId);
    List<ParagraphDTO> improveChatLog(String chatRecordId);

    List<ApplicationChatRecordEntity> listByAppIdAndChatUserId(String applicationId, String chatUserId, int pageSize, int offset);

    long countByAppIdAndChatUserId(String applicationId, String chatUserId);
}