package com.maxkb4j.application.handler.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.handler.PostResponseHandler;
import com.maxkb4j.application.mapper.ApplicationChatMapper;
import com.maxkb4j.application.mapper.ApplicationChatRecordMapper;
import com.maxkb4j.application.service.ApplicationChatUserStatsService;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.common.util.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class ChatPostHandler implements PostResponseHandler {

    /** 未投票状态 */
    private static final String VOTE_STATUS_NONE = "-1";
    /** 会话摘要最大长度 */
    private static final int SUMMARY_MAX_LENGTH = 50;

    private final ApplicationChatUserStatsService chatUserStatsService;
    private final ApplicationChatMapper chatMapper;
    private final ApplicationChatRecordMapper chatRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handler(ChatParams chatParams, ChatState chatState, ChatResponse chatResponse, long startTime) {
        String chatId = chatParams.getChatId();
        boolean debug = chatState.getDebug();
        float runTime = (System.currentTimeMillis() - startTime) / 1000F;

        // 1. 构建对话记录并刷新缓存
        ChatInfo chatInfo = ChatCache.get(chatId);
        ChatRecordDTO chatRecord = chatState.getChatRecord();
        ApplicationChatRecordEntity chatRecordEntity = buildChatRecordEntity(chatParams, chatResponse, runTime, chatInfo, chatRecord);
        ChatRecordDTO chatRecordDTO = BeanUtil.copy(chatRecordEntity, ChatRecordDTO.class);
        if (chatInfo == null) {
            chatInfo = new ChatInfo(chatId, chatState.getAppId());
        }
        chatInfo.addChatRecord(chatRecordDTO);
        ChatCache.put(chatId, chatInfo);

        // 2. 持久化
        if (!debug) {
            saveChat(chatId, chatState, chatInfo, chatParams.getMessage());
            chatRecordMapper.insertOrUpdate(chatRecordEntity);
        }
    }

    /**
     * 构建本轮对话记录实体：区分"补充上轮记录"与"新记录"两种场景
     */
    private ApplicationChatRecordEntity buildChatRecordEntity(ChatParams chatParams,
                                                             ChatResponse chatResponse, float runTime,
                                                             ChatInfo chatInfo, ChatRecordDTO chatRecord) {
        int messageTokens = chatResponse.getMessageTokens();
        int answerTokens = chatResponse.getAnswerTokens();
        ApplicationChatRecordEntity chatRecordEntity = new ApplicationChatRecordEntity();
        // 上游已保证 chatRecordId 非空；兜底生成，确保缓存中的记录 id 不为 null
        // （若依赖 ASSIGN_UUID 在 insert 时才填充，缓存 DTO 及 debug 记录的 id 将为 null）
        chatRecordEntity.setId(StringUtils.isNotBlank(chatParams.getChatRecordId())
                ? chatParams.getChatRecordId() : IdWorker.get32UUID());
        chatRecordEntity.setChatId(chatParams.getChatId());
        chatRecordEntity.setProblemText(chatParams.getMessage());
        chatRecordEntity.setAnswerTextList(chatResponse.getAnswerTextList());
        chatRecordEntity.setAnswerText(chatResponse.getAnswer());
        chatRecordEntity.setDetails(chatResponse.getRunDetails());
        chatRecordEntity.setImproveParagraphIdList(List.of());
        chatRecordEntity.setMessageTokens(messageTokens);
        chatRecordEntity.setAnswerTokens(answerTokens);
        chatRecordEntity.setCost(messageTokens + answerTokens);
        if (chatRecord != null) {
            // 补充/覆盖已有记录：运行时间累加
            chatRecordEntity.setIndex(chatRecord.getIndex());
            chatRecordEntity.setRunTime(runTime + chatRecord.getRunTime());
            chatRecordEntity.setVoteStatus(chatRecord.getVoteStatus());
        } else {
            // 新记录：索引取决于缓存中已有记录数（缓存丢失时回退为 0）
            chatRecordEntity.setIndex(chatInfo != null ? chatInfo.getChatRecordList().size() + 1 : 0);
            chatRecordEntity.setRunTime(runTime);
            chatRecordEntity.setVoteStatus(VOTE_STATUS_NONE);
        }
        return chatRecordEntity;
    }

    /**
     * 保存/更新会话：首次创建会话，已存在则原子自增记录数（不依赖缓存计数，避免缓存丢失后写错）
     */
    private void saveChat(String chatId, ChatState chatState, ChatInfo chatInfo, String problemText) {
        // 原子自增，避免并发"读-改-写"丢失更新；统计行已由 visitCountOver 确保存在
        chatUserStatsService.incrementAccessNum(chatState.getChatUserId(), chatInfo.getAppId());
        boolean chatExists = chatMapper.exists(Wrappers.<ApplicationChatEntity>lambdaQuery().eq(ApplicationChatEntity::getId, chatId));
        if (chatExists) {
            chatMapper.update(null, Wrappers.<ApplicationChatEntity>lambdaUpdate()
                    .eq(ApplicationChatEntity::getId, chatId)
                    .setSql("chat_record_count = chat_record_count + 1"));
            return;
        }
        ApplicationChatEntity chatEntity = new ApplicationChatEntity();
        chatEntity.setId(chatId);
        chatEntity.setApplicationId(chatInfo.getAppId());
        String problemOverview = problemText.length() > SUMMARY_MAX_LENGTH ? problemText.substring(0, SUMMARY_MAX_LENGTH) : problemText;
        chatEntity.setSummary(problemOverview);
        chatEntity.setChatUserId(chatState.getChatUserId());
        ChatUserType chatUserType = chatState.getChatUserType();
        chatEntity.setChatUserType(chatUserType == null ? ChatUserType.CHAT_USER.getKey() : chatUserType.getKey());
        chatEntity.setIsDeleted(false);
        chatEntity.setAsker(chatState.getChatUser());
        chatEntity.setMeta(new JSONObject());
        chatEntity.setStarNum(0);
        chatEntity.setTrampleNum(0);
        chatEntity.setChatRecordCount(1);
        chatEntity.setMarkSum(0);
        chatEntity.setIpAddress(chatState.getIpAddress());
        chatEntity.setSource(new JSONObject(Map.of("type", chatState.getSource())));
        try {
            chatMapper.insert(chatEntity);
        } catch (DuplicateKeyException e) {
            // 并发下已被其他请求创建，退化为自增
            chatMapper.update(null, Wrappers.<ApplicationChatEntity>lambdaUpdate()
                    .eq(ApplicationChatEntity::getId, chatId)
                    .setSql("chat_record_count = chat_record_count + 1"));
        }
    }
}

