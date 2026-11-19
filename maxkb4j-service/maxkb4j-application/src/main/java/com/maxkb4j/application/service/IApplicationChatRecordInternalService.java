package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.dto.AddChatImproveDTO;
import com.maxkb4j.application.dto.ChatImproveDTO;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.vo.ApplicationChatRecordVO;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.knowledge.dto.ParagraphDTO;

import java.util.List;

/**
 * 应用对话记录服务「对内」接口。
 */
public interface IApplicationChatRecordInternalService extends IApplicationChatRecordService, IService<ApplicationChatRecordEntity> {

    List<ChatRecordDTO> getChatRecords(String chatId);

    ApplicationChatRecordVO getChatRecordInfo(String appId, String chatId, String chatRecordId);

    IPage<ApplicationChatRecordVO> chatRecordPage(String appId, String chatId, int current, int size);

    boolean addChatLogs(String appId, AddChatImproveDTO dto);

    ApplicationChatRecordEntity improveChatLog(String appId, String chatId, String chatRecordId, String knowledgeId, String docId, ChatImproveDTO dto);

    boolean removeImproveChatLog(String appId, String chatId, String chatRecordId, String knowledgeId, String docId, String paragraphId);

    List<ParagraphDTO> improveChatLog(String appId, String chatId, String chatRecordId);

    List<ApplicationChatRecordEntity> listByAppIdAndChatUserId(String applicationId, String chatUserId, int pageSize, int offset);

    long countByAppIdAndChatUserId(String applicationId, String chatUserId);

    List<ApplicationChatRecordVO> listVOByIds(String chatId, List<String> ids);
}