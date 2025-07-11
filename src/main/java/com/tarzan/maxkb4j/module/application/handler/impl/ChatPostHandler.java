package com.tarzan.maxkb4j.module.application.handler.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationPublicAccessClientEntity;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatRecordMapper;
import com.tarzan.maxkb4j.module.application.service.ApplicationPublicAccessClientService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@AllArgsConstructor
@Component
public class ChatPostHandler extends PostResponseHandler {

    private final ApplicationPublicAccessClientService publicAccessClientService;
    private final ApplicationChatMapper chatMapper;
    private final ApplicationChatRecordMapper chatRecordMapper;
 //   private final ChatMemoryStore chatMemoryStore;


    @Override
    public void handler(String chatId, String chatRecordId, String problemText, String answerText, ApplicationChatRecordEntity chatRecord, JSONObject details,long startTime, String clientId, String clientType) {
        ChatInfo chatInfo = ChatCache.get(chatId);
        int messageTokens = details.values().stream()
                .map(row -> (JSONObject) row)
                .filter(row -> row.containsKey("messageTokens") && row.get("messageTokens") != null)
                .mapToInt(row -> row.getIntValue("messageTokens"))
                .sum();
        int answerTokens = details.values().stream()
                .map(row -> (JSONObject) row)
                .filter(row -> row.containsKey("answerTokens") && row.get("answerTokens") != null)
                .mapToInt(row -> row.getIntValue("answerTokens"))
                .sum();
        if (chatRecord != null) {
            chatRecord.setAnswerText(answerText);
            chatRecord.setDetails(new JSONObject(details));
            chatRecord.setMessageTokens(messageTokens);
            chatRecord.setAnswerTokens(answerTokens);
            chatRecord.setCost(messageTokens+answerTokens);
            chatRecord.setAnswerTextList(Set.of(answerText));
            chatRecord.setRunTime((System.currentTimeMillis() - startTime) / 1000F);
        } else {
            chatRecord = new ApplicationChatRecordEntity();
            chatRecord.setId(chatRecordId);
            chatRecord.setChatId(chatId);
            chatRecord.setProblemText(problemText);
            chatRecord.setAnswerText(answerText);
            chatRecord.setAnswerTextList(Set.of(answerText));
            chatRecord.setIndex(chatInfo.getChatRecordList().size() + 1);
            chatRecord.setMessageTokens(messageTokens);
            chatRecord.setAnswerTokens(answerTokens);
            chatRecord.setRunTime((System.currentTimeMillis() - startTime) / 1000F);
            chatRecord.setVoteStatus("-1");
            chatRecord.setCost(messageTokens+answerTokens);
            chatRecord.setDetails(details);
            chatRecord.setImproveParagraphIdList(new HashSet<>());
        }
        chatInfo.addChatRecord(chatRecord);
   /*     List<ChatMessage> messages=new ArrayList<>();
        chatInfo.getChatRecordList().forEach(record -> {
            messages.add(UserMessage.from(problemText));
            messages.add(AiMessage.from(answerText));
        });*/
       // System.err.println("chatMemory:" + chatId+"  messages size"+ messages.size());
        // 重新设置缓存
        ChatCache.put(chatId, chatInfo);
       // chatMemoryStore.updateMessages(chatId, messages);
        if (clientType!=null&&clientType.equals(AuthType.ACCESS_TOKEN.name())) {
            ApplicationPublicAccessClientEntity applicationPublicAccessClient = publicAccessClientService.getById(clientId);
            if (applicationPublicAccessClient != null) {
                applicationPublicAccessClient.setAccessNum(applicationPublicAccessClient.getAccessNum() + 1);
                applicationPublicAccessClient.setIntraDayAccessNum(applicationPublicAccessClient.getIntraDayAccessNum() + 1);
                publicAccessClientService.updateById(applicationPublicAccessClient);
            }
            String appId = chatInfo.getApplication().getId();
            if (Objects.nonNull(appId)) {
                if(chatInfo.getChatRecordList().size()==1){
                    ApplicationChatEntity chatEntity = new ApplicationChatEntity();
                    chatEntity.setId(chatId);
                    chatEntity.setApplicationId(appId);
                    String problemOverview=problemText.length()>50?problemText.substring(0,50):problemText;
                    chatEntity.setOverview(problemOverview);
                    chatEntity.setClientId(clientId);
                    chatEntity.setIsDeleted(false);
                    chatMapper.insertOrUpdate(chatEntity);
                }
                chatRecordMapper.insertOrUpdate(chatRecord);
            }
        }
    }
}

