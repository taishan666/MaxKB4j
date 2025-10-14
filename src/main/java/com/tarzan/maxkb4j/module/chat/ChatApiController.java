package com.tarzan.maxkb4j.module.chat;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;

@RestController
@RequestMapping(AppConst.CHAT_API)
@AllArgsConstructor
public class ChatApiController {

    private final ApplicationAccessTokenService accessTokenService;
    private final ApplicationService applicationService;
    private final ApplicationChatService chatService;
    private final TaskExecutor chatTaskExecutor;
    private final ApplicationChatRecordService chatRecordService;


    @GetMapping("/profile")
    public R<JSONObject> profile(String accessToken) {
        ApplicationAccessTokenEntity appAccessToken = accessTokenService.getByToken(accessToken);
        if (appAccessToken == null){
            return R.fail("未找到应用");
        }
        JSONObject result = new JSONObject();
        result.put("authentication", appAccessToken.getAuthentication());
        return R.success(result);
    }


    @PostMapping("/auth/anonymous")
    public R<String> auth(@RequestBody JSONObject params) {
        ApplicationAccessTokenEntity accessToken = accessTokenService.getByToken(params.getString("accessToken"));
        String chatUserId = IdWorker.get32UUID();
        String loginId =chatUserId+"-"+accessToken.getApplicationId();
        if (StpUtil.isLogin()) {
            loginId = StpUtil.getLoginIdAsString();
            if (!loginId.contains("-")){
                loginId =loginId+"-"+accessToken.getApplicationId();
                StpUtil.login(loginId);
            }
        }else {
            StpUtil.login(loginId);
        }
        return R.success(StpUtil.getTokenValue());
    }


    @GetMapping("/application/profile")
    public R<ApplicationEntity> appProfile(HttpServletRequest  request) {
        String authorization=request.getHeader("Authorization");
        String tokenValue = authorization.substring(7);
        StpUtil.setTokenValue(tokenValue);
        if (StpUtil.isLogin()) {
            String appId =StpUtil.getLoginIdAsString().split("-")[1];
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


    @GetMapping("/open")
    public R<String> chatOpen(HttpServletRequest  request) {
        String authorization=request.getHeader("Authorization");
        String tokenValue = authorization.substring(7);
        StpUtil.setTokenValue(tokenValue);
        String appId = StpUtil.getLoginIdAsString().split("-")[1];
        return R.success(chatService.chatOpen(appId, false));
    }


    @PostMapping(path = "/chat_message/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatMessageVO> chatMessage(@PathVariable String chatId, @RequestBody ChatParams params,HttpServletRequest  request) {
        String authorization=request.getHeader("Authorization");
        String tokenValue = authorization.substring(7);
        StpUtil.setTokenValue(tokenValue);
        String userId = StpUtil.getLoginIdAsString().split("-")[0];
        Sinks.Many<ChatMessageVO> sink = Sinks.many().unicast().onBackpressureBuffer();
        params.setChatId(chatId);
        params.setSink(sink);
        params.setChatUserId(userId);
        params.setChatUserType(ChatUserType.ANONYMOUS_USER.name());
        params.setDebug(false);
        // 异步执行业务逻辑
        chatTaskExecutor.execute(() -> chatService.chatMessage(params));
        return sink.asFlux();
    }


    @GetMapping("/historical_conversation/{current}/{size}")
    public R<Page<ApplicationChatEntity>> historicalConversation(@PathVariable int current, @PathVariable int size,HttpServletRequest  request) {
        String authorization=request.getHeader("Authorization");
        String tokenValue = authorization.substring(7);
        StpUtil.setTokenValue(tokenValue);
        String userId = StpUtil.getLoginIdAsString().split("-")[0];
        String appId = StpUtil.getLoginIdAsString().split("-")[1];
        Page<ApplicationChatEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<ApplicationChatEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatEntity::getApplicationId, appId).eq(ApplicationChatEntity::getChatUserId, userId);
        wrapper.orderByDesc(ApplicationChatEntity::getCreateTime);
        return R.success(chatService.page(page, wrapper));
    }

    @GetMapping("/historical_conversation/{chatId}/record/{chatRecordId}")
    public R<ApplicationChatRecordVO> historicalConversation(@PathVariable String chatId, @PathVariable String chatRecordId) {
        return R.success(chatRecordService.getChatRecordInfo(chatId, chatRecordId));
    }


    @PutMapping("/historical_conversation/{chatId}")
    public R<Boolean> updateConversation(@PathVariable String chatId, @RequestBody ApplicationChatEntity chatEntity) {
        chatEntity.setId(chatId);
        return R.success(chatService.updateById(chatEntity));
    }


    @DeleteMapping("/historical_conversation/{chatId}")
    public R<Boolean> deleteConversation(@PathVariable String chatId) {
        return R.success(chatService.deleteById(chatId));
    }

    @DeleteMapping("/historical_conversation/clear")
    public R<Boolean> historicalConversationClear() {
        String userId = StpUtil.getLoginIdAsString().split("-")[0];
        String appId = StpUtil.getLoginIdAsString().split("-")[1];
        LambdaQueryWrapper<ApplicationChatEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatEntity::getApplicationId, appId).eq(ApplicationChatEntity::getChatUserId, userId);
        return R.success(chatService.remove(wrapper));
    }


    @GetMapping("/historical_conversation_record/{chatId}/{current}/{size}")
    public R<IPage<ApplicationChatRecordVO>> historicalConversationRecord(@PathVariable String chatId, @PathVariable int current, @PathVariable int size) {
        return R.success(chatRecordService.chatRecordPage(chatId, current, size));
    }

    @PutMapping("/vote/chat/{chatId}/chat_record/{chatRecordId}")
    public R<Boolean> updateConversation(@PathVariable String chatId, @PathVariable String chatRecordId, @RequestBody ApplicationChatRecordEntity chatRecord) {
        chatRecord.setChatId(chatId);
        chatRecord.setId(chatRecordId);
        return R.success(chatRecordService.updateById(chatRecord));
    }

    /**
     * 嵌入第三方
     *
     * @param dto      dto
     */
    @GetMapping("/embed")
    @SaIgnore
    public ResponseEntity<String> embed(EmbedDTO dto) throws IOException {
        return ResponseEntity.ok()
                .header("Content-Type", "text/javascript; charset=utf-8")
                .body(applicationService.embed(dto));
    }



}
