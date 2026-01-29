package com.tarzan.maxkb4j.module.chat.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.common.util.StpKit;
import com.tarzan.maxkb4j.common.util.WebUtil;
import com.tarzan.maxkb4j.module.application.domain.dto.EmbedDTO;
import com.tarzan.maxkb4j.module.application.domain.entity.*;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.enums.ChatUserType;
import com.tarzan.maxkb4j.module.application.service.*;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import com.tarzan.maxkb4j.module.chat.dto.McpRequest;
import com.tarzan.maxkb4j.module.chat.dto.McpResponse;
import com.tarzan.maxkb4j.module.chat.service.ChatApiService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Sinks;

import java.io.IOException;

@Tag(name = "MaxKB4J开放接口")
@RestController
@RequestMapping(AppConst.CHAT_API)
@RequiredArgsConstructor
public class ChatApiController {

    private final ApplicationAccessTokenService accessTokenService;
    private final ApplicationService applicationService;
    private final ApplicationChatService chatService;
    private final ApplicationChatRecordService chatRecordService;
    private final ChatApiService chatApiService;
    private final ApplicationApiKeyService apiKeyService;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Hidden
    @GetMapping("/profile")
    public R<JSONObject> profile(String accessToken) {
        ApplicationAccessTokenEntity appAccessToken = accessTokenService.getByAccessToken(accessToken);
        if (appAccessToken == null) {
            return R.fail("未找到应用");
        }
        JSONObject result = new JSONObject();
        result.put("authentication", appAccessToken.getAuthentication());
        return R.success(result);
    }


    @Hidden
    @PostMapping("/auth/anonymous")
    public R<String> auth(@RequestBody JSONObject params) {
        return R.success(chatApiService.authToken(params));
    }


    @Operation(summary = "获取应用相关信息", description = "获取应用相关信息")
    @GetMapping("/application/profile")
    public R<ApplicationEntity> appProfile() {
        if (StpKit.USER.isLogin()) {
            String appId = (String) StpKit.USER.getExtra("applicationId");
            return R.success(chatApiService.appProfile(appId));
        }
        return R.fail("未登录");
    }

    @Operation(summary = "获取应用的会话ID", description = "获取应用的会话ID(首次对话前，需要调用该接口，生成对话ID)")
    @GetMapping("/open")
    public R<String> chatOpen() {
        String appId = (String) StpKit.USER.getExtra("applicationId");
        return R.success(chatService.chatOpen(appId, false));
    }

    @Operation(summary = "聊天对话", description = "聊天对话")
    @PostMapping(path = "/chat_message/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    public Object chatMessage(@PathVariable String chatId, @RequestBody ChatParams params) {
        String userId = StpKit.USER.getLoginIdAsString();
        Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();
        params.setChatId(chatId);
        params.setChatUserId(userId);
        params.setChatUserType(ChatUserType.ANONYMOUS_USER.name());
        params.setDebug(false);
        if (Boolean.TRUE.equals(params.getStream())) {
            // 异步执行业务逻辑
            chatService.chatMessageAsync(params, sink);
            return sink.asFlux();
        } else {
            ChatResponse chatResponse = chatService.chatMessage(params, sink);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(R.data(chatResponse));
        }
    }

    @PostMapping(path = "/mcp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseBodyEmitter handleMcpRequest(@RequestBody McpRequest req) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();
        String secretKey = WebUtil.getTokenValue();
        ApplicationApiKeyEntity apiKey = apiKeyService.getBySecretKey(secretKey);
        if (apiKey==null || !apiKey.getIsActive()){
            emitter.completeWithError(new ApiException("token不合法或被禁用"));
        }else {
            // 异步处理（避免阻塞主线程）
            new Thread(() -> {
                try {
                    McpResponse resp = chatApiService.mcpHandle(apiKey,req);
                    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                    String line = objectMapper.writeValueAsString(resp) + "\n";
                    emitter.send(line);
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }).start();
        }
        return emitter;
    }


    @Hidden
    @GetMapping("/historical_conversation/{current}/{size}")
    public R<Page<ApplicationChatEntity>> historicalConversation(@PathVariable int current, @PathVariable int size) {
        return R.data(chatApiService.historicalConversation(current, size));
    }

    @Hidden
    @GetMapping("/historical_conversation/{chatId}/record/{chatRecordId}")
    public R<ApplicationChatRecordVO> historicalConversation(@PathVariable String chatId, @PathVariable String chatRecordId) {
        return R.success(chatRecordService.getChatRecordInfo(chatId, chatRecordId));
    }

    @Hidden
    @PutMapping("/historical_conversation/{chatId}")
    public R<Boolean> updateConversation(@PathVariable String chatId, @RequestBody ApplicationChatEntity chatEntity) {
        chatEntity.setId(chatId);
        return R.success(chatService.updateById(chatEntity));
    }

    @Hidden
    @DeleteMapping("/historical_conversation/{chatId}")
    public R<Boolean> deleteConversation(@PathVariable String chatId) {
        return R.success(chatService.deleteById(chatId));
    }

    @Hidden
    @DeleteMapping("/historical_conversation/clear")
    public R<Boolean> historicalConversationClear() {
        return R.success(chatApiService.historicalConversationClear());
    }

    @Hidden
    @GetMapping("/historical_conversation_record/{chatId}/{current}/{size}")
    public R<IPage<ApplicationChatRecordVO>> historicalConversationRecord(@PathVariable String chatId, @PathVariable int current, @PathVariable int size) {
        return R.success(chatRecordService.chatRecordPage(chatId, current, size));
    }

    @Hidden
    @PutMapping("/vote/chat/{chatId}/chat_record/{chatRecordId}")
    public R<Boolean> updateConversation(@PathVariable String chatId, @PathVariable String chatRecordId, @RequestBody ApplicationChatRecordEntity chatRecord) {
        return R.success(chatApiService.updateConversation(chatId, chatRecordId, chatRecord));
    }

    @Hidden
    @PostMapping("/text_to_speech")
    public ResponseEntity<byte[]> textToSpeech(@RequestBody JSONObject data) {
        // 设置 HTTP 响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mp3"));
        String appId = StpKit.USER.getLoginIdAsString().split("-")[1];
        return new ResponseEntity<>(applicationService.textToSpeech(appId, data), headers, HttpStatus.OK);
    }

    /**
     * 嵌入第三方
     *
     * @param dto dto
     */
    @Hidden
    @GetMapping("/embed")
    @SaIgnore
    public ResponseEntity<String> embed(EmbedDTO dto) throws IOException {
        return ResponseEntity.ok().header("Content-Type", "text/javascript; charset=utf-8").body(applicationService.embed(dto));
    }




}
