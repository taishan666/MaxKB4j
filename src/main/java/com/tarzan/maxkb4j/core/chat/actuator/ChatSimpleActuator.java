package com.tarzan.maxkb4j.core.chat.actuator;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.core.chat.provider.IChatActuator;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.core.chatpipeline.step.chatstep.IChatStep;
import com.tarzan.maxkb4j.core.chatpipeline.step.generatehumanmessagestep.IGenerateHumanMessageStep;
import com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep.IResetProblemStep;
import com.tarzan.maxkb4j.core.chatpipeline.step.searchdatasetstep.ISearchDatasetStep;
import com.tarzan.maxkb4j.module.chat.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@AllArgsConstructor
@Component
public class ChatSimpleActuator implements IChatActuator {

    private final IResetProblemStep resetProblemStep;
    private final ISearchDatasetStep searchDatasetStep;
    private final IGenerateHumanMessageStep generateHumanMessageStep;
    private final IChatStep chatStep;
    private final PostResponseHandler postResponseHandler;


    @Override
    public ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams) {
        long startTime = System.currentTimeMillis();
        ChatInfo chatInfo = ChatCache.get(chatParams.getChatId());
        String problemText = chatParams.getMessage();
        boolean reChat = chatParams.getReChat() != null && chatParams.getReChat();
        List<String> excludeParagraphIds = new ArrayList<>();
        if (reChat) {
            excludeParagraphIds=getExcludeParagraphIds(chatInfo, chatParams);
        }
        PipelineManage.Builder pipelineManageBuilder = new PipelineManage.Builder();
        Boolean problemOptimization = application.getProblemOptimization();
        if (!CollectionUtils.isEmpty(application.getKnowledgeIdList())) {
            if (Objects.nonNull(problemOptimization) && problemOptimization) {
                pipelineManageBuilder.addStep(resetProblemStep);
            }
            pipelineManageBuilder.addStep(searchDatasetStep);
        }
        pipelineManageBuilder.addStep(generateHumanMessageStep);
        pipelineManageBuilder.addStep(chatStep);
        PipelineManage pipelineManage = pipelineManageBuilder.build();
        chatParams.setChatRecordId(chatParams.getChatRecordId() == null ? IdWorker.get32UUID() : chatParams.getChatRecordId());
        Map<String, Object> params = chatInfo.toPipelineManageParams(application, chatParams.getChatRecordId(), problemText, excludeParagraphIds, chatParams.getChatUserId(), chatParams.getChatUserType());
        String answer = pipelineManage.run(params, chatParams.getSink());
        JSONObject details = pipelineManage.getDetails();
        ChatResponse chatResponse = new ChatResponse(answer, details);
        postResponseHandler.handler(chatParams, chatResponse, null,  startTime);
        return chatResponse;
    }

    private List<String> getExcludeParagraphIds(ChatInfo chatInfo, ChatParams chatParams){
        List<String> excludeParagraphIds=new ArrayList<>();
        List<ApplicationChatRecordEntity> chatRecordList=chatInfo.getChatRecordList();
        if (!CollectionUtils.isEmpty(chatRecordList)){
            for (ApplicationChatRecordEntity chatRecord : chatRecordList) {
                JSONObject details=chatRecord.getDetails();
                if (!details.isEmpty()){
                    if (chatParams.getMessage().equals(chatRecord.getProblemText())&&details.containsKey("search_step")){
                        JSONObject searchStep=details.getJSONObject("search_step");
                        @SuppressWarnings("unchecked")
                        List<ParagraphVO> paragraphList= (List<ParagraphVO>) searchStep.get("paragraphList");
                        if (!CollectionUtils.isEmpty(paragraphList)){
                            excludeParagraphIds.addAll(paragraphList.stream().map(ParagraphVO::getId).toList());
                        }
                    }
                }
            }
        }
        return excludeParagraphIds;
    }

}
