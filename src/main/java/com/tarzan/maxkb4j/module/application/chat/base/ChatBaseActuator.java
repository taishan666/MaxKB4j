package com.tarzan.maxkb4j.module.application.chat.base;

import com.tarzan.maxkb4j.core.exception.ApiException;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.chat.provider.IChatActuator;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationAccessTokenEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatUserStatsEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.application.service.ApplicationAccessTokenService;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatUserStatsService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

public abstract class ChatBaseActuator implements IChatActuator {

    @Autowired
    private ApplicationChatUserStatsService publicAccessClientService;
    @Autowired
    private ApplicationAccessTokenService accessTokenService;

    public abstract ChatInfo reChatOpen(String chatId);

    public void chatCheck(ChatInfo chatInfo,ChatMessageDTO dto) {
        if (chatInfo == null) {
            dto.getSink().tryEmitNext(new ChatMessageVO());
            throw new ApiException("会话不存在");
        }
        String appId = chatInfo.getApplication().getId();
        if (AuthType.ACCESS_TOKEN.name().equals(dto.getClientType())) {
            if (Objects.nonNull(appId)) {
                ApplicationChatUserStatsEntity accessClient = publicAccessClientService.getById(dto.getClientId());
                if (Objects.isNull(accessClient)) {
                    accessClient = new ApplicationChatUserStatsEntity();
                    accessClient.setId(dto.getClientId());
                    accessClient.setApplicationId(appId);
                    accessClient.setAccessNum(0);
                    accessClient.setIntraDayAccessNum(0);
                    publicAccessClientService.save(accessClient);
                }
                ApplicationAccessTokenEntity appAccessToken = accessTokenService.lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId).one();
                if (appAccessToken.getAccessNum() < accessClient.getIntraDayAccessNum()) {
                    throw new ApiException("访问次数超过今日访问量");
                }
            }
        }
    }


    public ChatInfo getChatInfo(String chatId) {
        ChatInfo chatInfo = ChatCache.get(chatId);
        if (chatInfo == null) {
            return reChatOpen(chatId);
        }
        return chatInfo;
    }
}
