package com.maxkb4j.chat.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.application.dto.*;
import com.maxkb4j.application.service.*;
import com.maxkb4j.application.vo.ApplicationChatRecordVO;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.application.vo.ShareChatVO;
import com.maxkb4j.chat.dto.McpRequest;
import com.maxkb4j.chat.service.ChatApiService;
import com.maxkb4j.chat.service.ChatEmbedService;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.domain.dto.*;
import com.maxkb4j.common.enums.ChatSource;
import com.maxkb4j.common.enums.ChatUserType;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.common.util.StpKit;
import com.maxkb4j.common.util.WebUtil;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.Map;

@Tag(name = "MaxKB4J开放接口")
@RestController
@RequestMapping(AppConst.CHAT_API)
@RequiredArgsConstructor
public class ChatApiController {

    private final IApplicationAccessTokenService accessTokenService;
    private final IApplicationSpeechService applicationSpeechService;
    private final ChatEmbedService chatEmbedService;
    private final IApplicationChatService chatService;
    private final IApplicationChatRecordService chatRecordService;
    private final ChatApiService chatApiService;
    private final IApplicationApiKeyService apiKeyService;


    @Hidden
    @GetMapping("/profile")
    public R<JSONObject> profile(String accessToken) {
        ApplicationAccessTokenDTO appAccessToken = accessTokenService.getByAccessToken(accessToken);
        if (appAccessToken == null) {
            return R.fail(I18nUtil.get("application.app.not.found"));
        }
        JSONObject result = new JSONObject();
        result.put("authentication", appAccessToken.getAuthentication());
        return R.data(result);
    }


    @Hidden
    @PostMapping("/auth/anonymous")
    public R<String> auth(@RequestBody JSONObject params) {
        return R.data(chatApiService.authToken(params));
    }


    @Operation(summary = "获取应用相关信息", description = "获取应用相关信息")
    @GetMapping("/application/profile")
    public R<ApplicationVO> appProfile() {
        if (StpKit.USER.isLogin()) {
            String appId = (String) StpKit.USER.getExtra("applicationId");
            return R.data(chatApiService.appProfile(appId));
        }
        return R.fail(I18nUtil.get("login.not.login"));
    }

    @Operation(summary = "获取应用的会话ID", description = "获取应用的会话ID(首次对话前，需要调用该接口，生成对话ID)")
    @GetMapping("/open")
    public R<String> chatOpen() {
        String appId = (String) StpKit.USER.getExtra("applicationId");
        return R.data(chatService.chatOpen(appId, false));
    }

    @Operation(summary = "聊天对话", description = "聊天对话")
    @PostMapping(path = "/chat_message/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    public Object chatMessage(@PathVariable String chatId, @RequestBody ChatParams params) {
        String userId = StpKit.USER.getLoginIdAsString();
        Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();
        params.setChatId(chatId);
        ChatContext chatContext = ChatContext.builder()
                .chatUserId(userId)
                .chatUserType(ChatUserType.ANONYMOUS_USER.name())
                .source(ChatSource.ONLINE)
                .ipAddress(WebUtil.getIP())
                .debug(false)
                .build();
        if (Boolean.TRUE.equals(params.getStream())) {
            // 异步执行业务逻辑
            chatService.chatMessageAsync(params, chatContext, sink);
            return sink.asFlux();
        } else {
            ChatResponse chatResponse = chatService.chatMessage(params, chatContext, sink);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(R.data(chatResponse));
        }
    }

    @Hidden
    @PostMapping(path = "/mcp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseBodyEmitter handleMcpRequest(@RequestBody McpRequest req) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter();
        String secretKey = WebUtil.getTokenValue();
        ApplicationApiKeyDTO apiKey = apiKeyService.getBySecretKey(secretKey);
        if (apiKey == null || !apiKey.getIsActive()) {
            emitter.completeWithError(new ApiException("chat.token.invalid.or.disabled"));
        } else {
            // 异步处理（避免阻塞主线程）
            chatApiService.mcpHandleAsync(apiKey, req, emitter);
        }
        return emitter;
    }


    @Hidden
    @GetMapping("/historical_conversation/{current}/{size}")
    public R<IPage<ApplicationChatDTO>> historicalConversation(@PathVariable int current, @PathVariable int size) {
        return R.data(chatApiService.historicalConversation(current, size));
    }

    @Hidden
    @GetMapping("/historical_conversation/{chatId}/record/{chatRecordId}")
    public R<ApplicationChatRecordVO> historicalConversation(@PathVariable String chatId, @PathVariable String chatRecordId) {
        return R.data(chatRecordService.getChatRecordInfo(chatId, chatRecordId));
    }

    @Hidden
    @PutMapping("/historical_conversation/{chatId}")
    public R<Boolean> updateConversation(@PathVariable String chatId, @RequestBody ApplicationChatDTO chatDTO) {
        chatDTO.setId(chatId);
        return R.status(chatService.updateDtoById(chatDTO));
    }

    @Hidden
    @DeleteMapping("/historical_conversation/{chatId}")
    public R<Boolean> deleteConversation(@PathVariable String chatId) {
        return R.status(chatService.deleteById(chatId));
    }

    @Hidden
    @DeleteMapping("/historical_conversation/clear")
    public R<Boolean> historicalConversationClear() {
        return R.status(chatApiService.historicalConversationClear());
    }

    @Hidden
    @GetMapping("/historical_conversation_record/{chatId}/{current}/{size}")
    public R<IPage<ApplicationChatRecordVO>> historicalConversationRecord(@PathVariable String chatId, @PathVariable int current, @PathVariable int size) {
        return R.data(chatRecordService.chatRecordPage(chatId, current, size));
    }

    @Hidden
    @PutMapping("/vote/chat/{chatId}/chat_record/{chatRecordId}")
    public R<Boolean> updateConversation(@PathVariable String chatId, @PathVariable String chatRecordId, @RequestBody ApplicationChatRecordDTO chatRecord) {
        return R.status(chatApiService.updateConversation(chatId, chatRecordId, chatRecord));
    }

    @Hidden
    @PostMapping("/speech_to_text")
    public R<String> speechToText(MultipartFile file) throws IOException {
        String appId = (String) StpKit.USER.getExtra("applicationId");
        return R.data(applicationSpeechService.speechToText(appId, file, false));
    }


    @Hidden
    @PostMapping("/text_to_speech")
    public ResponseEntity<byte[]> textToSpeech(@RequestBody JSONObject data) {
        // 设置 HTTP 响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mp3"));
        String appId = (String) StpKit.USER.getExtra("applicationId");
        return new ResponseEntity<>(applicationSpeechService.textToSpeech(appId, data, false), headers, HttpStatus.OK);
    }


    /**
     * 嵌入第三方
     *
     * @param params 参数
     */
    @Hidden
    @GetMapping("/embed")
    @SaIgnore
    public ResponseEntity<String> embed(
            @NotBlank @RequestParam String protocol,
            @NotBlank @RequestParam String host,
            @NotBlank @RequestParam String token,
            @RequestParam Map<String, Object> params) {
        return ResponseEntity.ok()
                .header("Content-Type", "text/javascript; charset=utf-8")
                .body(chatEmbedService.embed(protocol,host,token,params));
    }

    @Hidden
    @PostMapping("/{id}/chat/{chatId}/share_chat")
    public R<Map<String, String>> shareChat(@PathVariable String id, @PathVariable String chatId, @Valid @RequestBody ShareChatDTO dto) {
        return R.data(chatService.shareChat(id, chatId, dto));
    }

    @Hidden
    @GetMapping("/share/{id}")
    public R<ShareChatVO> shareChat(@PathVariable String id) {
        return R.data(chatService.shareChat(id));
    }


}
