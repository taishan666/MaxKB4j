package com.maxkb4j.application.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.common.domain.vo.ResultCallback;
import com.maxkb4j.application.service.IChatService;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.vo.Answer;
import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.workflow.logic.LogicFlow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.WorkflowSpec;
import com.maxkb4j.workflow.service.IWorkFlowActuator;
import com.maxkb4j.workflow.service.WorkflowFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ChatFlowServiceImpl implements IChatService {

    private final IWorkFlowActuator workFlowActuator;
    private final WorkflowFactory workflowFactory;

    @Override
    public ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams, ChatState chatState, ResultCallback<ChatMessageVO> callback) {
        LogicFlow logicFlow = LogicFlow.newInstance(application.getWorkFlow());
        IWorkflow workflow = workflowFactory.create(WorkflowSpec.application(logicFlow)
                .chatParams(chatParams)
                .chatState(chatState)
                .callback(callback)
                .build());
        workFlowActuator.execute(workflow);
        JSONObject details = workflow.output().runtimeDetails();
        List<Answer> answerTextList = workflow.output().getAnswers(chatParams.getChatRecordId());
        return new ChatResponse(answerTextList, details);
    }

}
