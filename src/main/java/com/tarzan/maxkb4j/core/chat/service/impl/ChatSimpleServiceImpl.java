package com.tarzan.maxkb4j.core.chat.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.core.chat.service.IChatService;
import com.tarzan.maxkb4j.core.pipeline.PipelineManage;
import com.tarzan.maxkb4j.core.pipeline.step.chatstep.AbsChatStep;
import com.tarzan.maxkb4j.core.pipeline.step.generatehumanmessagestep.AbsGenerateHumanMessageStep;
import com.tarzan.maxkb4j.core.pipeline.step.resetproblemstep.AbsResetProblemStep;
import com.tarzan.maxkb4j.core.pipeline.step.searchdatasetstep.AbsSearchDatasetStep;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Sinks;

import java.util.Objects;

@RequiredArgsConstructor
@Component
public class ChatSimpleServiceImpl implements IChatService {

    private final AbsResetProblemStep resetProblemStep;
    private final AbsSearchDatasetStep searchDatasetStep;
    private final AbsGenerateHumanMessageStep generateHumanMessageStep;
    private final AbsChatStep chatStep;

    @Override
    public ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams, Sinks.Many<ChatMessageVO> sink) {
        PipelineManage.Builder pipelineManageBuilder = new PipelineManage.Builder();
        Boolean problemOptimization = application.getProblemOptimization();
        if (!CollectionUtils.isEmpty(application.getKnowledgeIds())) {
            if (Objects.nonNull(problemOptimization) && problemOptimization) {
                pipelineManageBuilder.addStep(resetProblemStep);
            }
            pipelineManageBuilder.addStep(searchDatasetStep);
        }
        pipelineManageBuilder.addStep(generateHumanMessageStep);
        pipelineManageBuilder.addStep(chatStep);
        PipelineManage pipelineManage = pipelineManageBuilder.build();
        chatParams.setChatRecordId(chatParams.getChatRecordId() == null ? IdWorker.get32UUID() : chatParams.getChatRecordId());
        String answer = pipelineManage.run(application,chatParams, sink);
        JSONObject details = pipelineManage.getDetails();
        return new ChatResponse(answer, details);
    }


}
