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
import com.maxkb4j.common.cache.ChatInfoStore;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.common.domain.vo.ResultCallback;
import com.maxkb4j.common.exception.AccessNumLimitException;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.StpKit;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@Service
@RequiredArgsConstructor
public class ApplicationChatServiceImpl extends ServiceImpl<ApplicationChatMapper, ApplicationChatEntity> implements IApplicationChatInternalService {

    private final IApplicationChatRecordInternalService chatRecordService;
    private final IApplicationInternalService applicationService;
    private final ApplicationChatUserStatsService chatUserStatsService;
    private final IApplicationAccessTokenInternalService accessTokenService;
    private final ApplicationVersionService applicationVersionService;
    private final PostResponseHandler postResponseHandler;
    private final TaskExecutor chatTaskExecutor;
    private final ApplicationChatShareLinkMapper chatShareLinkMapper;
    private final ChatInfoStore chatInfoStore;
    private final ChatServiceBuilder chatServiceBuilder;


    public IPage<ApplicationChatEntity> chatLogs(String appId, int page, int size, ChatQueryDTO query) {
        return baseMapper.chatLogs(new Page<>(page, size), appId, query);
    }


    public String chatOpen(String appId, boolean debug) {
        return chatOpen(appId, debug, null);
    }

    public String chatOpen(String appId, boolean debug, String chatUserId) {
        if (!debug) {
            long count = applicationVersionService.lambdaQuery().eq(ApplicationVersionEntity::getApplicationId, appId).count();
            if (count == 0) {
                throw new ApiException("application.not.published");
            }
        }
        ChatInfo chatInfo = new ChatInfo(IdWorker.get32UUID(), appId, chatUserId);
        chatInfoStore.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }


    @Override
    public ChatInfo getOrCreateChatInfo(String chatId, String appId, String chatUserId) {
        ChatInfo chatInfo = lookupChatInfo(chatId, appId, chatUserId);
        if (chatInfo != null) {
            return chatInfo;
        }
        chatInfo = new ChatInfo(chatId, appId, chatUserId);
        chatInfoStore.put(chatId, chatInfo);
        return chatInfo;
    }

    private ChatInfo getChatInfo(String chatId, String appId, String chatUserId) {
        ChatInfo chatInfo = lookupChatInfo(chatId, appId, chatUserId);
        if (chatInfo == null) {
            throw new ApiException("common.record.not.exists");
        }
        return chatInfo;
    }

    /**
     * 查找会话信息：缓存优先，缓存 miss 时按 (chatId, appId, chatUserId) 查库并回填缓存；
     * 不存在时返回 null，命中但应用/用户归属不匹配时抛出异常。
     */
    private ChatInfo lookupChatInfo(String chatId, String appId, String chatUserId) {
        ChatInfo chatInfo = chatInfoStore.get(chatId);
        if (chatInfo != null) {
            boolean appMismatch = StringUtils.isNotBlank(appId) && !Objects.equals(appId, chatInfo.getAppId());
            boolean userMismatch = StringUtils.isNotBlank(chatUserId) && chatInfo.getChatUserId() != null
                    && !Objects.equals(chatUserId, chatInfo.getChatUserId());
            if (appMismatch || userMismatch) {
                throw new ApiException("common.record.not.exists");
            }
            if (chatInfo.getChatUserId() == null) {
                chatInfo.setChatUserId(chatUserId);
            }
            return chatInfo;
        }
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(chatUserId)) {
            return null;
        }
        ApplicationChatEntity chatEntity = this.lambdaQuery()
                .select(ApplicationChatEntity::getApplicationId, ApplicationChatEntity::getChatUserId)
                .eq(ApplicationChatEntity::getId, chatId)
                .eq(ApplicationChatEntity::getApplicationId, appId)
                .eq(ApplicationChatEntity::getChatUserId, chatUserId)
                .one();
        if (chatEntity == null) {
            return null;
        }
        chatInfo = new ChatInfo(chatId, chatEntity.getApplicationId(), chatEntity.getChatUserId());
        // ChatInfo.chatRecordList 设计为 CopyOnWriteArrayList 供多线程共享，
        // 必须包装回并发容器，避免被普通 ArrayList 覆盖后并发写导致记录丢失/越界
        chatInfo.setChatRecordList(new CopyOnWriteArrayList<>(chatRecordService.getChatRecords(chatId)));
        chatInfoStore.put(chatInfo.getChatId(), chatInfo);
        return chatInfo;
    }

    public ChatResponse chatMessage(ChatParams chatParams, ChatState chatState, ResultCallback<ChatMessageVO> callback) {
        long startTime = System.currentTimeMillis();
        // 非流式对话 / MCP tools/call 等调用方不传 callback，归一化为空回调，避免链路中 NPE
        ResultCallback<ChatMessageVO> safeCallback = callback != null ? callback : ResultCallback.noop();
        if (visitCountOver(chatState)) {
            safeCallback.onError(new AccessNumLimitException());
            return new ChatResponse(List.of(), null);
        }
        ChatInfo chatInfo = this.getChatInfo(chatParams.getChatId(), chatState.getAppId(), chatState.getChatUserId());
        ApplicationVO application = applicationService.getAppDetail(chatInfo.getAppId(), chatState.getDebug());
        if (Objects.isNull(application)) {
            safeCallback.onError(new ApiException("application.not.found"));
            return new ChatResponse(List.of(), null);
        }
        List<ChatRecordDTO> historyChatRecordList = chatInfo.getChatRecordList();
        chatState.setHistoryChatRecords(historyChatRecordList);
        if (StringUtils.isNotBlank(chatParams.getChatRecordId())) {
            chatState.setChatRecord(resolveChatRecord(chatInfo, chatParams.getChatRecordId()));
        } else {
            chatParams.setChatRecordId(IdWorker.get32UUID());
        }
        IChatService chatService = chatServiceBuilder.getChatService(application.getType());
        ChatResponse chatResponse = chatService.chatMessage(application, chatParams, chatState, safeCallback);
        postResponseHandler.handler(chatParams, chatState, chatResponse, startTime);
        safeCallback.onEvent(new ChatMessageVO(chatParams.getChatId(), chatParams.getChatRecordId(), true));
        safeCallback.onComplete();
        return chatResponse;
    }

    /**
     * 解析重新回答的目标记录。缓存只保留最近 100 条，未命中时必须按 chatId + recordId 查库；
     * 不存在的记录直接拒绝，避免被误当作新增记录而覆盖数据并重复增加会话计数。
     */
    ChatRecordDTO resolveChatRecord(ChatInfo chatInfo, String chatRecordId) {
        ChatRecordDTO chatRecord = chatInfo.getChatRecordList().stream()
                .filter(e -> Objects.equals(e.getId(), chatRecordId))
                .findFirst()
                .orElse(null);
        if (chatRecord == null) {
            chatRecord = chatRecordService.getChatRecord(chatInfo.getAppId(),chatInfo.getChatId(), chatRecordId);
        }
        if (chatRecord == null) {
            throw new ApiException("common.record.not.exists");
        }
        return chatRecord;
    }

    /**
     * 异步执行对话，并负责 SSE 流的统一收尾。
     * <p>
     * sink 错误出口唯一：业务抛出异常时由本方法的 whenComplete 统一 tryEmitError；
     * 业务正常返回时由业务侧自行 emit 终止信号，本方法不再重复 emit。
     * 约定：下层（pipeline/workflow）不得先向 sink emit 错误再抛异常，否则会重复收尾。
     * </p>
     */
    public CompletableFuture<Void> chatMessageAsync(ChatParams chatParams, ChatState chatState, ResultCallback<ChatMessageVO> callback) {
        ResultCallback<ChatMessageVO> safeCallback = callback != null ? callback : ResultCallback.noop();
        return CompletableFuture
                .runAsync(() -> chatMessage(chatParams, chatState, safeCallback), chatTaskExecutor)
                .whenComplete((ignored, throwable) -> {
                    if (throwable == null) {
                        return;
                    }
                    log.error("Async chatMessage failed", throwable);
                    safeCallback.onError(throwable);
                });
    }

    /**
     * 判断当日访问次数是否已达上限。
     * <p>
     * 统计行通过数据库唯一索引 (chat_user_id, application_id) + ON CONFLICT DO NOTHING
     * 原子创建，计数通过 UPDATE ... +1 原子自增，避免并发下的重复行与丢失更新。
     * </p>
     */
    public boolean visitCountOver(ChatState chatState) {
        String appId = chatState.getAppId();
        String chatUserId = chatState.getChatUserId();
        boolean debug = chatState.getDebug();
        if (debug || Objects.isNull(appId)) {
            return false;
        }
        boolean firstVisit = chatUserStatsService.ensureStatsExists(chatUserId, chatState.getChatUserType(), appId);
        if (firstVisit) {
            // 首次访问尚无计数，保持原语义直接放行
            return false;
        }
        ApplicationAccessTokenEntity appAccessToken = accessTokenService.accessToken(appId);
        if (Objects.isNull(appAccessToken) || Objects.isNull(appAccessToken.getAccessNum())) {
            return false;
        }
        ApplicationChatUserStatsEntity chatUserStats = chatUserStatsService.getByUserIdAndAppId(chatUserId, appId);
        return Objects.nonNull(chatUserStats)
                && chatUserStats.getIntraDayAccessNum() >= appAccessToken.getAccessNum();
    }


    public void chatExport(String appId, List<String> ids, HttpServletResponse response) throws IOException {
        if (CollectionUtils.isNotEmpty(ids)) {
            writeChatExport(baseMapper.chatRecordDetail(appId, ids), response);
        }
    }

    void writeChatExport(List<ChatRecordDetailVO> list, HttpServletResponse response) throws IOException {
        List<ChatRecordDetailExcel> rows = BeanUtil.copyList(list, ChatRecordDetailExcel.class);
        EasyExcel.write(response.getOutputStream(), ChatRecordDetailExcel.class).sheet("sheet").doWrite(rows);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteById(String chatId) {
        chatRecordService.lambdaUpdate().eq(ApplicationChatRecordEntity::getChatId, chatId).remove();
        chatInfoStore.remove(chatId);
        return this.removeById(chatId);
    }

    @Override
    public boolean updateByApplicationId(String appId, String chatId, ApplicationChatEntity chatEntity) {
        if (belongsToApp(chatId, appId)) {
            ApplicationChatEntity entity = new ApplicationChatEntity();
            entity.setId(chatId);
            entity.setSummary(chatEntity.getSummary());
            entity.setMarkSum(chatEntity.getMarkSum());
            return this.updateById(entity);
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByApplicationId(String appId, String chatId) {
        if (belongsToApp(chatId, appId)) {
            // 先删关联的 chat_record，避免孤儿记录
            chatRecordService.lambdaUpdate().eq(ApplicationChatRecordEntity::getChatId, chatId).remove();
            return this.removeById(chatId);
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByChatIds(List<String> chatIds) {
        if (CollectionUtils.isEmpty(chatIds)) {
            return false;
        }
        // 先删关联的 chat_record，再删会话，事务保证避免中途失败留下无记录的会话
        chatRecordService.lambdaUpdate().in(ApplicationChatRecordEntity::getChatId, chatIds).remove();
        return this.remove(Wrappers.<ApplicationChatEntity>lambdaQuery().in(ApplicationChatEntity::getId, chatIds));
    }

    private boolean belongsToApp(String chatId, String appId) {
        return baseMapper.selectCount(Wrappers.<ApplicationChatEntity>lambdaQuery()
                .eq(ApplicationChatEntity::getId, chatId)
                .eq(ApplicationChatEntity::getApplicationId, appId)) > 0;
    }

    @Override
    public boolean isOwnedBy(String chatId, String appId, String userId) {
        return baseMapper.selectCount(Wrappers.<ApplicationChatEntity>lambdaQuery()
                .eq(ApplicationChatEntity::getId, chatId)
                .eq(ApplicationChatEntity::getApplicationId, appId)
                .eq(ApplicationChatEntity::getChatUserId, userId)) > 0;
    }

    @Override
    public Map<String, String> shareChat(String id, String chatId, ShareChatDTO dto) {
        if (!belongsToApp(chatId, id)) {
            throw new ApiException("common.record.not.exists");
        }
        List<String> chatRecordIds = dto.getChatRecordIds();
        if (!CollectionUtils.isEmpty(chatRecordIds)) {
            long expectedCount = chatRecordIds.stream().distinct().count();
            long actualCount = chatRecordService.lambdaQuery()
                    .eq(ApplicationChatRecordEntity::getChatId, chatId)
                    .in(ApplicationChatRecordEntity::getId, chatRecordIds)
                    .count();
            if (actualCount != expectedCount) {
                throw new ApiException("common.record.not.exists");
            }
        }
        ApplicationChatShareLinkEntity chatShareLink = new ApplicationChatShareLinkEntity();
        chatShareLink.setChatId(chatId);
        chatShareLink.setApplicationId(id);
        chatShareLink.setUserId(StpKit.USER.getLoginIdAsString());
        chatShareLink.setChatRecordIds(chatRecordIds);
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
        return BeanUtil.copyPage(this.page(page, wrapper), ApplicationChatDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clear(String appId, String userId) {
        // 先查出所有待删除的 chatId，避免孤儿记录
        List<String> chatIds = this.lambdaQuery()
                .select(ApplicationChatEntity::getId)
                .eq(ApplicationChatEntity::getApplicationId, appId)
                .eq(ApplicationChatEntity::getChatUserId, userId)
                .list()
                .stream()
                .map(ApplicationChatEntity::getId)
                .toList();
        if (!chatIds.isEmpty()) {
            chatRecordService.lambdaUpdate().in(ApplicationChatRecordEntity::getChatId, chatIds).remove();
        }
        LambdaQueryWrapper<ApplicationChatEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatEntity::getApplicationId, appId).eq(ApplicationChatEntity::getChatUserId, userId);
        return this.remove(wrapper);
    }

    @Override
    public boolean updateDtoById(ApplicationChatDTO applicationChatDTO) {
        ApplicationChatEntity entity = new ApplicationChatEntity();
        entity.setId(applicationChatDTO.getId());
        entity.setSummary(applicationChatDTO.getSummary());
        entity.setStarNum(applicationChatDTO.getStarNum());
        entity.setTrampleNum(applicationChatDTO.getTrampleNum());
        entity.setMarkSum(applicationChatDTO.getMarkSum());
        return this.updateById(entity);
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
        List<ApplicationChatRecordVO> chatRecordList = chatRecordService.listVOByIds(chatShareLink.getChatId(), chatShareLink.getChatRecordIds());
        shareChatVO.setChatRecordList(chatRecordList);
        return shareChatVO;
    }

}
