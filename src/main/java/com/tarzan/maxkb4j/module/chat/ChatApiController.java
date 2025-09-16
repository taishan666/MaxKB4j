package com.tarzan.maxkb4j.module.chat;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationAccessTokenEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.application.service.ApplicationAccessTokenService;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import lombok.AllArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

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
        JSONObject result = new JSONObject();
        result.put("authentication", false);
        return R.success(result);
    }

    @GetMapping("/application/profile")
    public R<ApplicationEntity> appProfile() {
        String appId = (String) StpUtil.getExtra("application_id");
        return R.success(applicationService.getById(appId));
    }

    @GetMapping("/open")
    public R<String> chatOpen() {
        String appId = (String) StpUtil.getExtra("application_id");
        return R.success(chatService.chatOpen(appId));
    }

    //todo
    @PostMapping("/auth/anonymous")
    public R<String> auth(@RequestBody JSONObject params) {
        ApplicationAccessTokenEntity accessToken = accessTokenService.getByToken(params.getString("accessToken"));
        SaLoginModel loginModel = new SaLoginModel();
        loginModel.setExtra("username", "anonymous");
        loginModel.setExtra("language", "");
        loginModel.setExtra("client_id", IdWorker.get32UUID());
        loginModel.setExtra("client_type", AuthType.ACCESS_TOKEN.name());
        loginModel.setExtra("application_id", accessToken.getApplicationId());
        loginModel.setExtra(AuthType.ACCESS_TOKEN.name(), accessToken);
        loginModel.setDevice(AuthType.ACCESS_TOKEN.name());
        StpUtil.login(StpUtil.getLoginId(), loginModel);
        return R.success(StpUtil.getTokenValue());
    }

    //todo
    @PostMapping("/chat_message/{chatId}")
    public Flux<ChatMessageVO> chatMessage(@PathVariable String chatId, @RequestBody ChatMessageDTO params) {
        Sinks.Many<ChatMessageVO> sink = Sinks.many().multicast().onBackpressureBuffer();
        String clientId = (String) StpUtil.getExtra("client_id");
        String clientType = (String) StpUtil.getExtra("client_type");
        params.setChatId(chatId);
        params.setClientId(clientId);
        params.setClientType(clientType);
        params.setSink(sink);
        // 异步执行业务逻辑
        chatTaskExecutor.execute(() -> chatService.chatMessage(params));
        return sink.asFlux();
    }

    @GetMapping("/historical_conversation/{current}/{size}")
    public R<Page<ApplicationChatEntity>> historicalConversation(@PathVariable int current, @PathVariable int size) {
        String appId = (String) StpUtil.getExtra("application_id");
        String userId = StpUtil.getLoginIdAsString();
        Page<ApplicationChatEntity> page = new Page<>(current, size);
        LambdaQueryWrapper<ApplicationChatEntity> wrapper= Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatEntity::getApplicationId, appId).eq(ApplicationChatEntity::getChatUserId, userId);
        return R.success(chatService.page(page,wrapper));
    }

    @DeleteMapping("/historical_conversation/clear")
    public R<Boolean> historicalConversationClear() {
        String appId = (String) StpUtil.getExtra("application_id");
        String userId = StpUtil.getLoginIdAsString();
        LambdaQueryWrapper<ApplicationChatEntity> wrapper= Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatEntity::getApplicationId, appId).eq(ApplicationChatEntity::getChatUserId, userId);
        return R.success(chatService.remove(wrapper));
    }

    @GetMapping("/historical_conversation_record/{chatId}/{current}/{size}")
    public R<Page<ApplicationChatRecordEntity>> historicalConversationRecord(@PathVariable String chatId,@PathVariable int current, @PathVariable int size) {
        Page<ApplicationChatRecordEntity> page = new Page<>(current, size);
        return R.success(chatRecordService.page(page,Wrappers.<ApplicationChatRecordEntity>lambdaQuery().eq(ApplicationChatRecordEntity::getChatId, chatId)));
    }
}
