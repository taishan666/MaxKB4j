package com.maxkb4j.application.handler.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.handler.PostResponseHandler;
import com.maxkb4j.application.mapper.ApplicationChatMapper;
import com.maxkb4j.application.service.ApplicationChatUserStatsService;
import com.maxkb4j.application.service.IApplicationChatRecordInternalService;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.common.util.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class ChatPostHandler implements PostResponseHandler {

    /**
     * 未投票状态
     */
    private static final String VOTE_STATUS_NONE = "-1";
    /**
     * 会话摘要最大长度
     */
    private static final int SUMMARY_MAX_LENGTH = 50;

    private final ApplicationChatUserStatsService chatUserStatsService;
    private final ApplicationChatMapper chatMapper;
    private final IApplicationChatRecordInternalService chatRecordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handler(ChatParams chatParams, ChatState chatState, ChatResponse chatResponse, long startTime) {
        String chatId = chatParams.getChatId();
        boolean debug = chatState.getDebug();
        float runTime = (System.currentTimeMillis() - startTime) / 1000F;

        // 1. 构建对话记录
        ChatInfo existing = ChatCache.get(chatId);
        final ChatInfo chatInfo;
        if (existing == null) {
            // 缓存缺失：立即放入空容器供并发请求共享；记录本身待事务提交后再写入
            chatInfo = new ChatInfo(chatId, chatState.getAppId());
            ChatCache.put(chatId, chatInfo);
        } else {
            chatInfo = existing;
        }
        ChatRecordDTO chatRecord = chatState.getChatRecord();
        ApplicationChatRecordEntity chatRecordEntity = buildChatRecordEntity(chatParams, chatResponse, runTime, chatInfo, chatRecord);
        ChatRecordDTO chatRecordDTO = BeanUtil.copy(chatRecordEntity, ChatRecordDTO.class);
        // 2. 先持久化，缓存更新延后到事务提交：回滚时缓存中不留"幽灵回答"
        if (!debug) {
            // chatRecord == null 表示新增对话记录，chatRecord != null 表示补充/覆盖已有记录
            saveChat(chatId, chatState, chatInfo, chatParams.getMessage(), chatRecord == null);
            chatRecordService.saveOrUpdate(chatRecordEntity);
        }
        registerAfterCommit(() -> chatInfo.addChatRecord(chatRecordDTO));
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
     * 保存/更新会话：新会话插入，已存在则 chat_record_count 原子自增（不依赖缓存计数，避免缓存丢失后写错）；
     * 仅新增对话记录时计数，修改/覆盖已有记录时不重复计数。
     * <p>
     * 单条 INSERT ... ON CONFLICT 原子完成"插入或自增"：PostgreSQL 中 INSERT 唯一键冲突会中止整个事务，
     * 同一事务内 catch 后回退 UPDATE 无法执行（"current transaction is aborted"），故不能用"先查/先插、冲突再更新"的写法。
     * </p>
     */
    private void saveChat(String chatId, ChatState chatState, ChatInfo chatInfo, String problemText, boolean isNewRecord) {
        // 补充/覆盖已存在的对话记录时，会话必然已创建，直接返回，避免重复累加 chat_record_count
        if (!isNewRecord) {
            return;
        }
        // 原子自增，避免并发"读-改-写"丢失更新；统计行已由 visitCountOver 确保存在
        chatUserStatsService.incrementAccessNum(chatState.getChatUserId(), chatInfo.getAppId());
        ApplicationChatEntity chatEntity = new ApplicationChatEntity();
        chatEntity.setId(chatId);
        chatEntity.setApplicationId(chatInfo.getAppId());
        // problemText 理论上已被上游校验非空（@NotBlank）；此处兜底判空，防御手工构建 ChatParams
        // 的内部调用链（如 AgentExecutor、OpenAI 兼容接口）绕过校验传入 null
        String overview = problemText == null ? "" : problemText;
        String problemOverview = overview.length() > SUMMARY_MAX_LENGTH ? overview.substring(0, SUMMARY_MAX_LENGTH) : overview;
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
        chatMapper.insertOrIncrementRecordCount(chatEntity);
    }

    /**
     * 事务提交后再执行缓存写入，回滚时不留"幽灵回答"；无事务上下文时（如测试直调）立即执行
     */
    private void registerAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}

