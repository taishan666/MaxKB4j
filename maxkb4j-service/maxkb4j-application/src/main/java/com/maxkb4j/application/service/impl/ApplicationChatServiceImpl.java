package com.maxkb4j.application.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.builder.ChatServiceBuilder;
import com.maxkb4j.application.dto.ApplicationChatDTO;
import com.maxkb4j.application.dto.ChatQueryDTO;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.application.dto.ShareChatDTO;
import com.maxkb4j.application.entity.*;
import com.maxkb4j.application.enums.ShareLinkType;
import com.maxkb4j.application.excel.ChatRecordDetailExcel;
import com.maxkb4j.application.handler.PostResponseHandler;
import com.maxkb4j.application.mapper.ApplicationChatMapper;
import com.maxkb4j.application.mapper.ApplicationChatShareLinkMapper;
import com.maxkb4j.application.service.*;
import com.maxkb4j.application.vo.ApplicationChatRecordVO;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.application.vo.ChatRecordDetailVO;
import com.maxkb4j.application.vo.ShareChatVO;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.*;
import com.maxkb4j.common.exception.AccessNumLimitException;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.DateTimeUtil;
import com.maxkb4j.common.util.PageUtil;
import com.maxkb4j.common.util.StpKit;
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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@Service
@RequiredArgsConstructor
public class ApplicationChatServiceImpl extends ServiceImpl<ApplicationChatMapper, ApplicationChatEntity> implements IApplicationChatInternalService {

    private final ApplicationChatRecordServiceImpl chatRecordService;
    private final IApplicationService applicationService;
    private final ApplicationChatUserStatsService chatUserStatsService;
    private final ApplicationAccessTokenServiceImpl accessTokenService;
    private final ApplicationVersionService applicationVersionService;
    private final PostResponseHandler postResponseHandler;
    private final TaskExecutor chatTaskExecutor;
    private final ApplicationChatShareLinkMapper chatShareLinkMapper;


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
                throw new ApiException("application.not.published");
            }
        }
        ChatInfo chatInfo = new ChatInfo(IdWorker.get32UUID(), appId);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }


    public ChatInfo getChatInfo(String chatId, String appId) {
        ChatInfo chatInfo = ChatCache.get(chatId);
        if (chatInfo == null) {
            if (StringUtils.isBlank(appId)) {
                ApplicationChatEntity chatEntity = this.lambdaQuery().select(ApplicationChatEntity::getApplicationId).eq(ApplicationChatEntity::getId, chatId).one();
                if (chatEntity != null) {
                    appId = chatEntity.getApplicationId();
                }
            }
            chatInfo = new ChatInfo(chatId, appId);
            chatInfo.setChatRecordList(chatRecordService.getChatRecords(appId));
            ChatCache.put(chatInfo.getChatId(), chatInfo);
            return chatInfo;
        }
        return chatInfo;
    }

    public ChatResponse chatMessage(ChatParams chatParams, ChatContext chatContext, Sinks.Many<ChatMessageVO> sink) {
        long startTime = System.currentTimeMillis();
        if (visitCountOver(chatContext)) {
            sink.tryEmitError(new AccessNumLimitException());
            return new ChatResponse(List.of(), null);
        }
        ChatInfo chatInfo = this.getChatInfo(chatParams.getChatId(), chatContext.getAppId());
        List<ChatRecordDTO> historyChatRecordList = chatInfo.getChatRecordList();
        chatContext.setHistoryChatRecords(historyChatRecordList);
        if (StringUtils.isNotBlank(chatParams.getChatRecordId())) {
            ChatRecordDTO chatRecord = historyChatRecordList.stream().filter(e -> e.getId().equals(chatParams.getChatRecordId())).findFirst().orElse(null);
            chatContext.setChatRecord(chatRecord);
        } else {
            chatParams.setChatRecordId(IdWorker.get32UUID());
        }
        ApplicationVO application = applicationService.getAppDetail(chatInfo.getAppId(), chatContext.getDebug());
        if (Objects.isNull(application)) {
            sink.tryEmitError(new ApiException("application.not.found"));
            return new ChatResponse(List.of(), null);
        }
        IChatService chatService = ChatServiceBuilder.getChatService(application.getType());
        ChatResponse chatResponse = chatService.chatMessage(application, chatParams, chatContext, sink);
        postResponseHandler.handler(chatParams, chatContext, chatResponse, startTime);
        sink.tryEmitNext(new ChatMessageVO(chatParams.getChatId(), chatParams.getChatRecordId(), true));
        sink.tryEmitComplete();
        return chatResponse;
    }

    public void chatMessageAsync(ChatParams chatParams, ChatContext chatContext, Sinks.Many<ChatMessageVO> sink) {
        CompletableFuture.supplyAsync(() -> chatMessage(chatParams, chatContext, sink), chatTaskExecutor)
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

    public boolean visitCountOver(ChatContext chatContext) {
        String appId = chatContext.getAppId();
        String chatUserId = chatContext.getChatUserId();
        boolean debug = chatContext.getDebug();
        if (!debug && Objects.nonNull(appId)) {
            ApplicationChatUserStatsEntity chatUserStats = chatUserStatsService.getByUserIdAndAppId(chatUserId, appId);
            if (Objects.isNull(chatUserStats)) {
                String chatUserType = chatContext.getChatUserType();
                chatUserStats = new ApplicationChatUserStatsEntity();
                chatUserStats.setChatUserId(chatUserId);
                chatUserStats.setChatUserType(chatUserType);
                chatUserStats.setApplicationId(appId);
                chatUserStats.setAccessNum(0);
                chatUserStats.setIntraDayAccessNum(0);
                chatUserStatsService.save(chatUserStats);
            }else {
                ApplicationAccessTokenEntity appAccessToken = accessTokenService.lambdaQuery().select(ApplicationAccessTokenEntity::getAccessNum).eq(ApplicationAccessTokenEntity::getApplicationId, appId).one();
                if (Objects.nonNull(appAccessToken)) {
                    return  chatUserStats.getIntraDayAccessNum()>=appAccessToken.getAccessNum();
                }
            }
        }
        return false;
    }


    public void chatExport(List<String> ids, HttpServletResponse response) throws IOException {
        if (CollectionUtils.isNotEmpty(ids)) {
            List<ChatRecordDetailVO> list = baseMapper.chatRecordDetail(ids);
            List<ChatRecordDetailExcel> rows = BeanUtil.copyList(list, ChatRecordDetailExcel.class);
            EasyExcel.write(response.getOutputStream(), ChatRecordDetailExcel.class).sheet("sheet").doWrite(rows);
        }
    }

    @Transactional
    public Boolean deleteById(String chatId) {
        chatRecordService.lambdaUpdate().eq(ApplicationChatRecordEntity::getChatId, chatId).remove();
        ChatCache.remove(chatId);
        return this.removeById(chatId);
    }

    @Override
    public Map<String, String> shareChat(String id, String chatId, ShareChatDTO dto) {
        ApplicationChatShareLinkEntity chatShareLink = new ApplicationChatShareLinkEntity();
        chatShareLink.setChatId(chatId);
        chatShareLink.setApplicationId(id);
        chatShareLink.setUserId(StpKit.USER.getLoginIdAsString());
        chatShareLink.setChatRecordIds(dto.getChatRecordIds());
        chatShareLink.setShareType(ShareLinkType.PUBLIC.name());
        chatShareLinkMapper.insert(chatShareLink);
        return Map.of("link", chatShareLink.getId());
    }

    @Override
    public IPage<ApplicationChatDTO> page(String appId, String userId, int current, int size) {
        Page<ApplicationChatEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<ApplicationChatEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatEntity::getApplicationId, appId).eq(ApplicationChatEntity::getChatUserId, userId);
        wrapper.orderByDesc(ApplicationChatEntity::getCreateTime);
        return PageUtil.copy(this.page(page, wrapper), ApplicationChatDTO.class);
    }

    @Override
    public boolean clear(String appId, String userId) {
        LambdaQueryWrapper<ApplicationChatEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatEntity::getApplicationId, appId).eq(ApplicationChatEntity::getChatUserId, userId);
        return this.remove(wrapper);
    }

    @Override
    public boolean updateDtoById(ApplicationChatDTO applicationChatDTO) {
        return this.updateById(BeanUtil.copy(applicationChatDTO, ApplicationChatEntity.class));
    }

    @Override
    public ShareChatVO shareChat(String id) {
        ShareChatVO shareChatVO = new ShareChatVO();
        ApplicationChatShareLinkEntity chatShareLink = chatShareLinkMapper.selectById(id);
        if (chatShareLink == null) {
            throw new ApiException("common.record.not.exists");
        }
        ApplicationChatEntity chatEntity = this.lambdaQuery().select(ApplicationChatEntity::getSummary).eq(ApplicationChatEntity::getId, chatShareLink.getChatId()).one();
        shareChatVO.setSummary(chatEntity != null ? chatEntity.getSummary() : null);
        List<ApplicationChatRecordVO> chatRecordList = chatRecordService.listVOByIds(chatShareLink.getChatRecordIds());
        shareChatVO.setChatRecordList(chatRecordList);
        return shareChatVO;
    }

}
