package com.maxkb4j.chat.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxkb4j.application.dto.*;
import com.maxkb4j.application.service.IApplicationAccessTokenService;
import com.maxkb4j.application.service.IApplicationChatRecordService;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.application.service.IApplicationService;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.dto.ChatContext;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.chat.dto.McpRequest;
import com.maxkb4j.chat.dto.McpResponse;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.common.util.WebUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ChatApiService {

    private final IApplicationAccessTokenService accessTokenService;
    private final IApplicationService applicationService;
    private final IApplicationChatService chatService;
    private final IApplicationChatRecordService chatRecordService;
    private final ChatTokenService chatTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();


    public String authToken(JSONObject params) {
        String accessToken = params.getString("accessToken");
        ApplicationAccessTokenDTO accessTokenEntity = accessTokenService.getByAccessToken(accessToken);
        if (accessTokenEntity == null){
            throw new ApiException("application.app.not.found");
        }
        String chatUserId = IdWorker.get32UUID();
        String tokenValue = WebUtil.getTokenValue();
        if (StringUtils.isNotBlank(tokenValue)){
            StpKit.USER.setTokenValue(tokenValue);
            chatUserId= StpKit.USER.getLoginIdAsString();
        }
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("applicationId", accessTokenEntity.getApplicationId());
        extraData.put("chatUserType", ChatUserType.ANONYMOUS_USER.name());
        extraData.put("accessToken", accessToken);
        return chatTokenService.issueAnonymousToken(chatUserId, extraData);
    }

    public ApplicationVO appProfile(String appId) {
        ApplicationAccessTokenDTO appAccessToken = accessTokenService.getByAppId(appId);
        ApplicationVO application = applicationService.appProfile(appId);
        if (appAccessToken != null && application != null) {
            application.setLanguage(appAccessToken.getLanguage());
            application.setShowSource(appAccessToken.getShowSource());
            application.setShowExec(appAccessToken.getShowExec());
        }
        return application;
    }

    public IPage<ApplicationChatDTO> historicalConversation(int current, int size) {
        String appId = (String) StpKit.USER.getExtra("applicationId");
        String userId = StpKit.USER.getLoginIdAsString();
        return chatService.page(appId, userId, current, size);
    }

    public boolean historicalConversationClear() {
        String appId = (String) StpKit.USER.getExtra("applicationId");
        String userId = StpKit.USER.getLoginIdAsString();
        return chatService.clear(appId, userId);
    }

    @Transactional
    public boolean updateConversation(String chatId, String chatRecordId, ApplicationChatRecordDTO chatRecord) {
        chatRecord.setChatId(chatId);
        chatRecord.setId(chatRecordId);
        chatRecordService.updateDtoById(chatRecord);
        List<ApplicationChatRecordDTO> chatRecordEntities = chatRecordService.listVoteStatusByChatId(chatId);
        ApplicationChatDTO chatDTO = new ApplicationChatDTO();
        chatDTO.setId(chatId);
        int starNum = (int) chatRecordEntities.stream().filter(item -> item.getVoteStatus().equals("0")).count();
        int trampleNum = (int) chatRecordEntities.stream().filter(item -> item.getVoteStatus().equals("1")).count();
        chatDTO.setStarNum(starNum);
        chatDTO.setTrampleNum(trampleNum);
        return chatService.updateDtoById(chatDTO);
    }

    @Async
    public void mcpHandleAsync(ApplicationApiKeyDTO apiKey, McpRequest req, ResponseBodyEmitter emitter) {
        McpResponse resp = this.mcpHandle(apiKey,req);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            String line = objectMapper.writeValueAsString(resp) + "\n";
            emitter.send(line);
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

    }

    public McpResponse mcpHandle(ApplicationApiKeyDTO apiKey, McpRequest req) {
        McpResponse resp = new McpResponse();
        resp.id = req.id;
        try {
            switch (req.method) {
                case "initialize" -> resp.result = Map.of(
                        "protocolVersion", "2025-06-18",
                        "serverInfo", Map.of(
                                "name", "MaxKb4j",
                                "description", "MaxKb4j is a knowledge-based AI assistant",
                                "url", "https://gitee.com/taisan/MaxKB4j"
                        ),
                        "capabilities", Map.of("tools", Map.of())
                );
                case "notifications/initialized", "ping" -> resp.result = Map.of();
                case "tools/list" -> {
                    ApplicationSimple app = applicationService.getAppSimpleById(apiKey.getApplicationId());
                    resp.result = Map.of("tools", List.of(
                            Map.of(
                                    "name", String.format("agent_%s", apiKey.getApplicationId()),
                                    "description", app.getName() + " - " + app.getDesc(),
                                    "inputSchema", Map.of(
                                            "type", "object",
                                            "properties", Map.of("message", Map.of("type", "string", "description", "The message to send to the AI.")),
                                            "required", List.of("message")
                                    )
                            )
                    ));
                }
                case "tools/call" -> {
                    JSONObject args = req.params.getJSONObject("arguments");
                    String message = args.getString("message");
                    String chatId = chatService.chatOpen(apiKey.getApplicationId(), false);
                    ChatParams params = ChatParams.builder()
                            .message(message)
                            .reChat(false)
                            .stream(false)
                            .chatId(chatId)
                            .build();
                    ChatContext chatContext = ChatContext.builder()
                            .appId(apiKey.getApplicationId())
                            .chatUserId(IdWorker.get32UUID())
                            .chatUserType(ChatUserType.ANONYMOUS_USER.name())
                            .debug(false)
                            .build();
                    ChatResponse chatResponse = chatService.chatMessage(params, chatContext, Sinks.many().unicast().onBackpressureBuffer());
                    Map<String, Object> content = Map.of("type", "text", "text", chatResponse.getAnswer());
                    resp.result = Map.of("content", List.of(content));
                }
                case null, default -> resp.error = Map.of("code", -32601, "message", "Method not supported");
            }
        } catch (Exception e) {
            resp.error = Map.of("code", -32000, "message", e.getMessage());
        }
        return resp;
    }
}
