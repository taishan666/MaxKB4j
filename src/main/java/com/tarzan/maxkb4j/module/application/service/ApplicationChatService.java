package com.tarzan.maxkb4j.module.application.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.exception.AccessNumLimitException;
import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.core.chat.provider.ChatActuatorBuilder;
import com.tarzan.maxkb4j.core.chat.provider.IChatActuator;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.*;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatRecordDetailVO;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.chat.cache.ChatCache;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import opennlp.tools.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@Service
@AllArgsConstructor
public class ApplicationChatService extends ServiceImpl<ApplicationChatMapper, ApplicationChatEntity> {

    private final ApplicationChatRecordService chatRecordService;
    private final ApplicationService applicationService;
    private final ApplicationChatUserStatsService chatUserStatsService;
    private final ApplicationAccessTokenService accessTokenService;
    private final ApplicationVersionService applicationVersionService;


    public IPage<ApplicationChatEntity> chatLogs(String appId, int page, int size, ChatQueryDTO query) {
        Page<ApplicationChatEntity> chatPage = new Page<>(page, size);
        return baseMapper.chatLogs(chatPage, appId, query);
    }


    public String chatOpen(String appId, boolean debug) {
        if (!debug) {
            long count = applicationVersionService.lambdaQuery().eq(ApplicationVersionEntity::getApplicationId, appId).count();
            if (count == 0) {
                throw new ApiException("应用未发布，请发布后使用。");
            }
        }
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(IdWorker.get32UUID());
        chatInfo.setAppId(appId);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }


    public ChatInfo getChatInfo(String chatId) {
        ChatInfo chatInfo = ChatCache.get(chatId);
        if (chatInfo == null) {
            ApplicationChatEntity chatEntity = this.lambdaQuery().select(ApplicationChatEntity::getApplicationId).eq(ApplicationChatEntity::getId, chatId).one();
            if (chatEntity == null) {
                return null;
            }
            chatInfo = new ChatInfo();
            chatInfo.setChatId(chatId);
            chatInfo.setAppId(chatEntity.getApplicationId());
            List<ApplicationChatRecordEntity> chatRecordList = chatRecordService.lambdaQuery().eq(ApplicationChatRecordEntity::getChatId, chatId).list();
            chatInfo.setChatRecordList(chatRecordList);
            ChatCache.put(chatInfo.getChatId(), chatInfo);
            return chatInfo;
        }
        return chatInfo;
    }

    public ChatResponse chatMessage(ChatParams chatParams) {
        ChatInfo chatInfo = this.getChatInfo(chatParams.getChatId());
        if (chatInfo == null) {
            chatParams.getSink().tryEmitError(new ApiException("会话不存在"));
            return new ChatResponse("",null);
        } else {
            if (StringUtils.isEmpty(chatParams.getAppId())) {
                chatParams.setAppId(chatInfo.getAppId());
            }
        }
        if (!visitCountCheck(chatParams)){
            return new ChatResponse("",null);
        }
        ApplicationVO application = applicationService.getAppDetail(chatParams.getAppId(), chatParams.getDebug());
        IChatActuator chatActuator = ChatActuatorBuilder.getActuator(application.getType());
        ChatResponse chatResponse = chatActuator.chatMessage(application, chatParams);
        chatParams.getSink().tryEmitComplete();
        return chatResponse;
    }

    @Async("chatTaskExecutor")
    public CompletableFuture<ChatResponse> chatMessageAsync(ChatParams chatParams) {
        String chatId= StringUtils.isNotBlank(chatParams.getChatId()) ? chatParams.getChatId() : IdWorker.get32UUID();
        ChatInfo chatInfo = ChatCache.get(chatId);
        if (chatInfo == null){
            chatInfo = new ChatInfo();
            chatInfo.setChatId(chatId);
            chatInfo.setAppId(chatParams.getAppId());
            ChatCache.put(chatInfo.getChatId(), chatInfo);
        }
        return CompletableFuture.completedFuture(chatMessage(chatParams));
    }

    public boolean visitCountCheck(ChatParams chatParams) {
        String appId=chatParams.getAppId();
        String chatUserId=chatParams.getChatUserId();
        String chatUserType=chatParams.getChatUserType();
        boolean debug=chatParams.getDebug();
        if (!debug && Objects.nonNull(appId)) {
            ApplicationChatUserStatsEntity chatUserStats = chatUserStatsService.getByUserIdAndAppId(chatUserId, appId);
            if (Objects.isNull(chatUserStats)) {
                chatUserStats = new ApplicationChatUserStatsEntity();
                chatUserStats.setChatUserId(chatUserId);
                chatUserStats.setChatUserType(chatUserType);
                chatUserStats.setApplicationId(appId);
                chatUserStats.setAccessNum(0);
                chatUserStats.setIntraDayAccessNum(0);
                chatUserStatsService.save(chatUserStats);
            }
            ApplicationAccessTokenEntity appAccessToken = accessTokenService.lambdaQuery().select(ApplicationAccessTokenEntity::getAccessNum).eq(ApplicationAccessTokenEntity::getApplicationId, appId).one();
            if (Objects.nonNull(appAccessToken)) {
                if (appAccessToken.getAccessNum() < chatUserStats.getIntraDayAccessNum()) {
                    chatParams.getSink().tryEmitError(new AccessNumLimitException("今天的访问次数超过限制"));
                    return false;
                }
            }

        }
        return true;
    }


    public void chatExport(List<String> ids, HttpServletResponse response) throws IOException {
        List<ChatRecordDetailVO> rows = baseMapper.chatRecordDetail(ids);
        EasyExcel.write(response.getOutputStream(), ChatRecordDetailVO.class).sheet("sheet").doWrite(rows);
    }

    @Transactional
    public Boolean deleteById(String chatId) {
        chatRecordService.lambdaUpdate().eq(ApplicationChatRecordEntity::getChatId, chatId).remove();
        return this.removeById(chatId);
    }
}
