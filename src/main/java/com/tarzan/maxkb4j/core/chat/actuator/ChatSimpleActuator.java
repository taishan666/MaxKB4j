package com.tarzan.maxkb4j.core.chat.actuator;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.core.chat.provider.IChatActuator;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.core.chatpipeline.step.chatstep.IChatStep;
import com.tarzan.maxkb4j.core.chatpipeline.step.generatehumanmessagestep.IGenerateHumanMessageStep;
import com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep.IResetProblemStep;
import com.tarzan.maxkb4j.core.chatpipeline.step.searchdatasetstep.ISearchDatasetStep;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Sinks;

import java.util.Objects;

@AllArgsConstructor
@Component
public class ChatSimpleActuator implements IChatActuator {

    private final IResetProblemStep resetProblemStep;
    private final ISearchDatasetStep searchDatasetStep;
    private final IGenerateHumanMessageStep generateHumanMessageStep;
    private final IChatStep chatStep;

    @Override
    public ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams, Sinks.Many<ChatMessageVO> sink) {
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
        String answer = pipelineManage.run(application,chatParams, sink);
        JSONObject details = pipelineManage.getDetails();
        return new ChatResponse(answer, details);
    }


}
