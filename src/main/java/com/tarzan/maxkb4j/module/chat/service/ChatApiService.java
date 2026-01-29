package com.tarzan.maxkb4j.module.chat.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.common.util.StpKit;
import com.tarzan.maxkb4j.common.util.WebUtil;
import com.tarzan.maxkb4j.module.application.domain.entity.*;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.enums.ChatUserType;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.application.service.ApplicationAccessTokenService;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import com.tarzan.maxkb4j.module.chat.dto.McpRequest;
import com.tarzan.maxkb4j.module.chat.dto.McpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ChatApiService {

    private final ApplicationMapper applicationMapper;
    private final ApplicationAccessTokenService accessTokenService;
    private final ApplicationService applicationService;
    private final ApplicationChatService chatService;
    private final ApplicationChatRecordService chatRecordService;


    public String authToken(JSONObject params) {
        String accessToken = params.getString("accessToken");
        ApplicationAccessTokenEntity accessTokenEntity = accessTokenService.getByAccessToken(accessToken);
        String tokenValue = WebUtil.getTokenValue();
        StpKit.USER.setTokenValue(tokenValue);
        String chatUserId = IdWorker.get32UUID();
        if (StpKit.USER.isLogin()) {
            chatUserId = StpKit.USER.getLoginIdAsString();
        }
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("applicationId", accessTokenEntity.getApplicationId());
        extraData.put("chatUserType", ChatUserType.ANONYMOUS_USER.name());
        extraData.put("accessToken", accessToken);
        return StpKit.USER.createTokenValue(chatUserId, StpKit.USER.getLoginDevice(), -1L, extraData);
    }

    public ApplicationEntity appProfile(String appId) {
        ApplicationAccessTokenEntity appAccessToken = accessTokenService.getById(appId);
        ApplicationVO application = applicationService.appProfile(appId);
        if (appAccessToken != null && application != null) {
            application.setLanguage(appAccessToken.getLanguage());
            application.setShowSource(appAccessToken.getShowSource());
            application.setShowExec(appAccessToken.getShowExec());
        }
        return application;
    }

    public Page<ApplicationChatEntity> historicalConversation(int current, int size) {
        String tokenValue = WebUtil.getTokenValue();
        StpKit.USER.setTokenValue(tokenValue);
        String appId = (String) StpKit.USER.getExtra("applicationId");
        String userId = StpKit.USER.getLoginIdAsString();
        Page<ApplicationChatEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<ApplicationChatEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatEntity::getApplicationId, appId).eq(ApplicationChatEntity::getChatUserId, userId);
        wrapper.orderByDesc(ApplicationChatEntity::getCreateTime);
        return chatService.page(page, wrapper);
    }

    public boolean historicalConversationClear() {
        String tokenValue = WebUtil.getTokenValue();
        StpKit.USER.setTokenValue(tokenValue);
        String appId = (String) StpKit.USER.getExtra("applicationId");
        String userId = StpKit.USER.getLoginIdAsString();
        LambdaQueryWrapper<ApplicationChatEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatEntity::getApplicationId, appId).eq(ApplicationChatEntity::getChatUserId, userId);
        return chatService.remove(wrapper);
    }

    @Transactional
    public boolean updateConversation(String chatId, String chatRecordId, ApplicationChatRecordEntity chatRecord) {
        chatRecord.setChatId(chatId);
        chatRecord.setId(chatRecordId);
        chatRecordService.updateById(chatRecord);
        List<ApplicationChatRecordEntity> chatRecordEntities = chatRecordService.lambdaQuery().select(ApplicationChatRecordEntity::getVoteStatus).eq(ApplicationChatRecordEntity::getChatId, chatId).list();
        ApplicationChatEntity chatEntity = new ApplicationChatEntity();
        chatEntity.setId(chatId);
        int starNum = (int) chatRecordEntities.stream().filter(item -> item.getVoteStatus().equals("0")).count();
        int trampleNum = (int) chatRecordEntities.stream().filter(item -> item.getVoteStatus().equals("1")).count();
        chatEntity.setStarNum(starNum);
        chatEntity.setTrampleNum(trampleNum);
        return chatService.updateById(chatEntity);
    }

    public McpResponse mcpHandle(ApplicationApiKeyEntity apiKey, McpRequest req) {
        McpResponse resp = new McpResponse();
        resp.id = req.id;
        try {
            if ("initialize".equals(req.method)) {
                resp.result = Map.of(
                        "protocolVersion", "2025-06-18",
                        "serverInfo", Map.of(
                                "name", "MaxKb4j",
                                "description", "MaxKb4j is a knowledge-based AI assistant",
                                "url", "https://gitee.com/taisan/MaxKB4j"
                        ),
                        "capabilities", Map.of("tools", Map.of())
                );
            } else if ("notifications/initialized".equals(req.method)) {
                resp.result = Map.of();
            } else if ("ping".equals(req.method)) {
                resp.result = Map.of();
            } else if ("tools/list".equals(req.method)) {
                ApplicationEntity app=applicationMapper.selectOne(Wrappers.<ApplicationEntity>lambdaQuery()
                        .select(ApplicationEntity::getId)
                        .select(ApplicationEntity::getName)
                        .select(ApplicationEntity::getDesc)
                        .eq(ApplicationEntity::getId, apiKey.getApplicationId()));
                resp.result = Map.of("tools", List.of(
                        Map.of(
                                "name", "ai_chat",
                                "description", app.getName()+" "+app.getDesc(),
                                "inputSchema", Map.of(
                                        "type", "object",
                                        "properties", Map.of("message", Map.of("type", "string", "description", "The message to send to the AI.")),
                                        "required", List.of("message")
                                )
                        )
                ));
            } else if ("tools/call".equals(req.method)) {
                JSONObject args = req.params.getJSONObject("arguments");
                String message = args.getString("message");
                String chatId=chatService.chatOpen(apiKey.getApplicationId(),false);
                ChatParams params = ChatParams.builder()
                        .message(message)
                        .reChat(false)
                        .stream(false)
                        .chatId(chatId)
                        .appId(apiKey.getApplicationId())
                        .chatUserId(IdWorker.get32UUID())
                        .chatUserType(ChatUserType.ANONYMOUS_USER.name())
                        .debug(false)
                        .build();
                ChatResponse chatResponse = chatService.chatMessage(params, Sinks.many().unicast().onBackpressureBuffer());
                Map<String, Object> content=Map.of("type","text","text",chatResponse.getAnswer());
                resp.result=Map.of("content", List.of(content));
            } else {
                resp.error = Map.of("code", -32601, "message", "Method not supported");
            }
        } catch (Exception e) {
            resp.error = Map.of("code", -32000, "message", e.getMessage());
        }
        return resp;
    }
}
