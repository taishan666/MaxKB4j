package com.tarzan.maxkb4j.core.chat.actuator;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.core.chat.provider.IChatActuator;
import com.tarzan.maxkb4j.core.ragpipeline.PipelineManage;
import com.tarzan.maxkb4j.core.ragpipeline.step.chatstep.IChatStep;
import com.tarzan.maxkb4j.core.ragpipeline.step.generatehumanmessagestep.IGenerateHumanMessageStep;
import com.tarzan.maxkb4j.core.ragpipeline.step.resetproblemstep.IResetProblemStep;
import com.tarzan.maxkb4j.core.ragpipeline.step.searchdatasetstep.ISearchDatasetStep;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.chat.ChatParams;
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
    private final ApplicationChatRecordService chatRecordService;
    private final PostResponseHandler postResponseHandler;


    @Override
    public String chatMessage(ApplicationVO application, ChatParams chatParams) {
        long startTime = System.currentTimeMillis();
        ChatInfo chatInfo = ChatCache.get(chatParams.getChatId());
        boolean stream = chatParams.getStream() == null || chatParams.getStream();
        String problemText = chatParams.getMessage();
        boolean reChat = chatParams.getReChat();
        List<String> excludeParagraphIds = new ArrayList<>();
        if (reChat) {
            //todo 获取聊天记录
            String chatRecordId = chatParams.getChatRecordId();
            if (Objects.nonNull(chatRecordId)) {
                ApplicationChatRecordVO chatRecord = chatRecordService.getChatRecordInfo(chatInfo, chatRecordId);
                List<ParagraphVO> paragraphs = chatRecord.getParagraphList();
                if (!CollectionUtils.isEmpty(paragraphs)) {
                    excludeParagraphIds = paragraphs.stream().map(ParagraphVO::getId).toList();
                }
            }
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
        List<ApplicationChatRecordEntity> historyChatRecords = chatRecordService.getChatRecords(chatInfo, chatParams.getChatId());
        chatParams.setChatRecordId(chatParams.getChatRecordId() == null ? IdWorker.get32UUID() : chatParams.getChatRecordId());
        Map<String, Object> params = chatInfo.toPipelineManageParams(application, chatParams.getChatRecordId(), problemText, historyChatRecords, excludeParagraphIds, "", "", stream);
        String answer = pipelineManage.run(params, chatParams.getSink());
        JSONObject details = pipelineManage.getDetails();
        postResponseHandler.handler(chatParams.getChatId(), chatParams.getChatRecordId(), problemText, answer, null, details, startTime, chatParams.getChatUserId(), chatParams.getChatUserType(), chatParams.getDebug());
        return answer;
    }

}
