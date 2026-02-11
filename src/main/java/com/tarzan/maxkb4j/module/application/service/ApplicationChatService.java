package com.tarzan.maxkb4j.module.application.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.exception.AccessNumLimitException;
import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.common.util.DateTimeUtil;
import com.tarzan.maxkb4j.core.chat.provider.ChatServiceBuilder;
import com.tarzan.maxkb4j.core.chat.service.IChatService;
import com.tarzan.maxkb4j.module.application.domain.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domain.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domain.entity.*;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatRecordDetailVO;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.chat.cache.ChatCache;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@Service
@RequiredArgsConstructor
public class ApplicationChatService extends ServiceImpl<ApplicationChatMapper, ApplicationChatEntity> {

    private final ApplicationChatRecordService chatRecordService;
    private final ApplicationService applicationService;
    private final ApplicationChatUserStatsService chatUserStatsService;
    private final ApplicationAccessTokenService accessTokenService;
    private final ApplicationVersionService applicationVersionService;
    private final PostResponseHandler postResponseHandler;
    private final TaskExecutor chatTaskExecutor;


    public IPage<ApplicationChatEntity> chatLogs(String appId, int page, int size, ChatQueryDTO query) {
        Page<ApplicationChatEntity> chatPage = new Page<>(page, size);
        LambdaQueryWrapper<ApplicationChatEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApplicationChatEntity::getApplicationId, appId);
        wrapper.like(StringUtils.isNotBlank(query.getSummary()), ApplicationChatEntity::getSummary, query.getSummary());
        if (StringUtils.isNotBlank(query.getStartTime())) {
            LocalDateTime startOfDay = DateTimeUtil.parseDate(query.getStartTime()).atStartOfDay();
            wrapper.gt(ApplicationChatEntity::getCreateTime, startOfDay);
        }
        if (StringUtils.isNotBlank(query.getEndTime())) {
            LocalDateTime endOfDay = DateTimeUtil.parseDate(query.getEndTime()).atTime(LocalTime.MAX);
            wrapper.le(ApplicationChatEntity::getCreateTime, endOfDay);
        }
        wrapper.ge(Objects.nonNull(query.getMinStar()), ApplicationChatEntity::getStarNum, query.getMinStar());
        wrapper.ge(Objects.nonNull(query.getMinTrample()), ApplicationChatEntity::getTrampleNum, query.getMinTrample());
        wrapper.orderByDesc(ApplicationChatEntity::getCreateTime);
        return this.page(chatPage, wrapper);
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


    //todo 优化，考虑放到redis中
    public ChatInfo getChatInfo(String chatId,String appId) {
        ChatInfo chatInfo = ChatCache.get(chatId);
        if (chatInfo == null) {
            chatInfo = new ChatInfo();
            chatInfo.setChatId(chatId);
            if (StringUtils.isNotBlank(appId)){
                chatInfo.setAppId(appId);
            }else {
                ApplicationChatEntity chatEntity = this.lambdaQuery().select(ApplicationChatEntity::getApplicationId).eq(ApplicationChatEntity::getId, chatId).one();
                if (chatEntity != null) {
                    chatInfo.setAppId(chatEntity.getApplicationId());
                }
            }
            List<ApplicationChatRecordEntity> chatRecordList = chatRecordService.lambdaQuery().eq(ApplicationChatRecordEntity::getChatId, chatId).list();
            chatInfo.setChatRecordList(chatRecordList);
            ChatCache.put(chatInfo.getChatId(), chatInfo);
            return chatInfo;
        }
        return chatInfo;
    }

    public ChatResponse chatMessage(ChatParams chatParams, Sinks.Many<ChatMessageVO> sink) {
        long startTime = System.currentTimeMillis();
        ChatInfo chatInfo = this.getChatInfo(chatParams.getChatId(),chatParams.getAppId());
        if (StringUtils.isEmpty(chatParams.getAppId())) {
            chatParams.setAppId(chatInfo.getAppId());
        }
        if (!visitCountCheck(chatParams)) {
            sink.tryEmitError(new AccessNumLimitException());
            return new ChatResponse(List.of(), null);
        }
        List<ApplicationChatRecordEntity> historyChatRecordList = chatRecordService.getChatRecords(chatParams.getChatId());
        chatParams.setHistoryChatRecords(historyChatRecordList);
        if (StringUtils.isNotBlank(chatParams.getChatRecordId())) {
            ApplicationChatRecordEntity chatRecord = historyChatRecordList.stream().filter(e -> e.getId().equals(chatParams.getChatRecordId())).findFirst().orElse(null);
            chatParams.setChatRecord(chatRecord);
        }
        ApplicationVO application = applicationService.getAppDetail(chatParams.getAppId(), chatParams.getDebug());
        IChatService chatService = ChatServiceBuilder.getChatService(application.getType());
        ChatResponse chatResponse = chatService.chatMessage(application, chatParams, sink);
        postResponseHandler.handler(chatParams, chatResponse, startTime);
        sink.tryEmitNext(new ChatMessageVO(chatParams.getChatId(), chatParams.getChatRecordId(), true));
        sink.tryEmitComplete();
        return chatResponse;
    }

    public CompletableFuture<ChatResponse> chatMessageAsync(ChatParams chatParams, Sinks.Many<ChatMessageVO> sink) {
        return CompletableFuture.supplyAsync(() -> chatMessage(chatParams, sink), chatTaskExecutor)
                .exceptionally(throwable -> {
                    // 记录异常日志（关键！）
                    log.error("Async chatMessage failed", throwable);
                    // 可选：向 sink 发送错误消息（如果前端需要感知）
                    sink.tryEmitError(throwable);
                    // 返回一个默认/空响应，或重新抛出（但需包装为 CompletionException）
                    throw new CompletionException(throwable); // 如果调用方需要捕获
                    // 或 return ChatResponse.empty(); // 如果允许返回默认值
                });
    }

    public boolean visitCountCheck(ChatParams chatParams) {
        String appId = chatParams.getAppId();
        String chatUserId = chatParams.getChatUserId();
        String chatUserType = chatParams.getChatUserType();
        boolean debug = chatParams.getDebug();
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
                return appAccessToken.getAccessNum() >= chatUserStats.getIntraDayAccessNum();
            }
        }
        return true;
    }


    public void chatExport(List<String> ids, HttpServletResponse response) throws IOException {
        if (CollectionUtils.isNotEmpty(ids)) {
            List<ChatRecordDetailVO> rows = baseMapper.chatRecordDetail(ids);
            EasyExcel.write(response.getOutputStream(), ChatRecordDetailVO.class).sheet("sheet").doWrite(rows);
        }
    }

    @Transactional
    public Boolean deleteById(String chatId) {
        chatRecordService.lambdaUpdate().eq(ApplicationChatRecordEntity::getChatId, chatId).remove();
        return this.removeById(chatId);
    }

}
