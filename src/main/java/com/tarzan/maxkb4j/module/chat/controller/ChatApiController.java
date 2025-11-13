package com.tarzan.maxkb4j.module.chat.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.SaLoginModel;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.util.StpKit;
import com.tarzan.maxkb4j.common.util.WebUtil;
import com.tarzan.maxkb4j.module.application.domian.dto.EmbedDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationAccessTokenEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.enums.ChatUserType;
import com.tarzan.maxkb4j.module.application.service.ApplicationAccessTokenService;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Sinks;

import java.io.IOException;

@Tag(name = "MaxKB4J开放接口")
@RestController
@RequestMapping(AppConst.CHAT_API)
@AllArgsConstructor
public class ChatApiController {

    private final ApplicationAccessTokenService accessTokenService;
    private final ApplicationService applicationService;
    private final ApplicationChatService chatService;
    private final ApplicationChatRecordService chatRecordService;
    private final TaskExecutor chatTaskExecutor;


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
        //todo 匿名用户和后台用户区分
        String accessToken = params.getString("accessToken");
        ApplicationAccessTokenEntity accessTokenEntity = accessTokenService.getByAccessToken(accessToken);
        //防止刷新时，会话中的token丢失
        String tokenValue = WebUtil.getTokenValue();
        StpKit.USER.setTokenValue(tokenValue);
        if (!StpKit.USER.isLogin()) {
            String chatUserId = IdWorker.get32UUID();
            SaLoginModel loginModel = new SaLoginModel();
            loginModel.setExtra("applicationId", accessTokenEntity.getApplicationId());
            loginModel.setExtra("chatUserType", ChatUserType.ANONYMOUS_USER.name());
            loginModel.setExtra("accessToken", accessToken);
            StpKit.USER.login(chatUserId, loginModel);
        }
        return R.success(StpKit.USER.getTokenValue());
    }


    @Operation(summary = "获取应用相关信息", description = "获取应用相关信息")
    @GetMapping("/application/profile")
    public R<ApplicationEntity> appProfile() {
        if (StpKit.USER.isLogin()) {
            String appId = (String) StpKit.USER.getExtra("applicationId");
            ApplicationAccessTokenEntity appAccessToken = accessTokenService.getById(appId);
            ApplicationVO application = applicationService.getDetail(appId);
            if (appAccessToken != null && application != null) {
                application.setLanguage(appAccessToken.getLanguage());
                application.setShowSource(appAccessToken.getShowSource());
                application.setShowExec(appAccessToken.getShowExec());
            }
            return R.success(application);
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
        params.setSink(sink);
        params.setChatUserId(userId);
        params.setChatUserType(ChatUserType.ANONYMOUS_USER.name());
        params.setDebug(false);
        if (Boolean.TRUE.equals(params.getStream())) {
            // 异步执行业务逻辑
            chatTaskExecutor.execute(() -> chatService.chatMessage(params));
            return sink.asFlux();
        } else {
            ChatResponse chatResponse = chatService.chatMessage(params);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(R.data(chatResponse));
        }
    }


    @Hidden
    @GetMapping("/historical_conversation/{current}/{size}")
    public R<Page<ApplicationChatEntity>> historicalConversation(@PathVariable int current, @PathVariable int size) {
        String tokenValue = WebUtil.getTokenValue();
        StpKit.USER.setTokenValue(tokenValue);
        String appId = (String) StpKit.USER.getExtra("applicationId");
        String userId = StpKit.USER.getLoginIdAsString();
        Page<ApplicationChatEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<ApplicationChatEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatEntity::getApplicationId, appId).eq(ApplicationChatEntity::getChatUserId, userId);
        wrapper.orderByDesc(ApplicationChatEntity::getCreateTime);
        return R.success(chatService.page(page, wrapper));
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
        String tokenValue = WebUtil.getTokenValue();
        StpKit.USER.setTokenValue(tokenValue);
        String appId = (String) StpKit.USER.getExtra("applicationId");
        String userId = StpKit.USER.getLoginIdAsString();
        LambdaQueryWrapper<ApplicationChatEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatEntity::getApplicationId, appId).eq(ApplicationChatEntity::getChatUserId, userId);
        return R.success(chatService.remove(wrapper));
    }

    @Hidden
    @GetMapping("/historical_conversation_record/{chatId}/{current}/{size}")
    public R<IPage<ApplicationChatRecordVO>> historicalConversationRecord(@PathVariable String chatId, @PathVariable int current, @PathVariable int size) {
        return R.success(chatRecordService.chatRecordPage(chatId, current, size));
    }

    @Hidden
    @PutMapping("/vote/chat/{chatId}/chat_record/{chatRecordId}")
    public R<Boolean> updateConversation(@PathVariable String chatId, @PathVariable String chatRecordId, @RequestBody ApplicationChatRecordEntity chatRecord) {
        chatRecord.setChatId(chatId);
        chatRecord.setId(chatRecordId);
        return R.success(chatRecordService.updateById(chatRecord));
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
        return ResponseEntity.ok()
                .header("Content-Type", "text/javascript; charset=utf-8")
                .body(applicationService.embed(dto));
    }


}
