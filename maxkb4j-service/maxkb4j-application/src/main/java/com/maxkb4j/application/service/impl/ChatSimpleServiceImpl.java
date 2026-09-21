package com.maxkb4j.application.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.common.domain.vo.ResultCallback;
import com.maxkb4j.application.pipeline.PipelineManage;
import com.maxkb4j.application.pipeline.step.chatstep.AbsChatStep;
import com.maxkb4j.application.pipeline.step.generatehumanmessagestep.AbsGenerateHumanMessageStep;
import com.maxkb4j.application.pipeline.step.rerankstep.AbsRerankStep;
import com.maxkb4j.application.pipeline.step.resetproblemstep.AbsResetProblemStep;
import com.maxkb4j.application.pipeline.step.searchdatasetstep.AbsSearchDatasetStep;
import com.maxkb4j.application.service.IChatService;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.vo.Answer;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.application.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class ChatSimpleServiceImpl implements IChatService {

    private final AbsResetProblemStep resetProblemStep;
    private final AbsSearchDatasetStep searchDatasetStep;
    private final AbsRerankStep rerankStep;
    private final AbsGenerateHumanMessageStep generateHumanMessageStep;
    private final AbsChatStep chatStep;

    @Override
    public ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams, ChatState chatState, ResultCallback<ChatMessageVO> callback) {
        PipelineManage.Builder pipelineManageBuilder = new PipelineManage.Builder();
        Boolean problemOptimization = application.getProblemOptimization();
        if (!CollectionUtils.isEmpty(application.getKnowledgeIds())) {
            if (Objects.nonNull(problemOptimization) && problemOptimization) {
                pipelineManageBuilder.addStep(resetProblemStep);
            }
            pipelineManageBuilder.addStep(searchDatasetStep);
            // rerank 未启用时该 step 为空操作，直接注册不影响既有行为
            pipelineManageBuilder.addStep(rerankStep);
        }
        pipelineManageBuilder.addStep(generateHumanMessageStep);
        pipelineManageBuilder.addStep(chatStep);
        PipelineManage pipelineManage = pipelineManageBuilder.build();
        chatParams.setChatRecordId(chatParams.getChatRecordId() == null ? IdWorker.get32UUID() : chatParams.getChatRecordId());
        Answer answer = pipelineManage.run(application, chatParams, chatState,  callback);
        JSONObject details = pipelineManage.getDetails();
        return new ChatResponse(List.of(answer), details);
    }


}
