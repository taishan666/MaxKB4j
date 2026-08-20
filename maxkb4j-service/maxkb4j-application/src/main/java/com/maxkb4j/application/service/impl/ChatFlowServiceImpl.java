package com.maxkb4j.application.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.service.IChatService;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.workflow.builder.NodeBuilder;
import com.maxkb4j.workflow.logic.LogicFlow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.service.IWorkFlowActuator;
import com.maxkb4j.workflow.service.WorkflowFactory;
import com.maxkb4j.workflow.service.WorkflowSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class ChatFlowServiceImpl implements IChatService {

    private final IWorkFlowActuator workFlowActuator;
    private final NodeBuilder nodeBuilder;
    private final WorkflowFactory workflowFactory;

    @Override
    public ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams, ChatState chatState, Sinks.Many<ChatMessageVO> sink) {
        LogicFlow logicFlow = LogicFlow.newInstance(application.getWorkFlow());
        List<AbsNode> nodes = logicFlow.getNodes().stream().map(nodeBuilder::getNode).filter(Objects::nonNull).toList();
        IWorkflow workflow = workflowFactory.create(WorkflowSpec.application(nodes, logicFlow.getEdges())
                .chatParams(chatParams)
                .chatState(chatState)
                .sink(sink)
                .build());
        workFlowActuator.execute(workflow);
        List<Answer> answerTextList = getAnswers(workflow.output().getExecutedNodes(), chatParams.getChatRecordId());
        JSONObject details = workflow.output().runtimeDetails();
        return new ChatResponse(answerTextList, details);
    }

    public List<Answer> getAnswers(List<AbsNode> executedNodes,String chatRecordId) {
        if (executedNodes.isEmpty()) {
            return List.of();
        }
        if (chatRecordId == null) {
            return List.of();
        }
        List<Answer> answerList = new ArrayList<>(executedNodes.size());
        for (AbsNode node : executedNodes) {
            answerList.addAll(node.getAnswerList(chatRecordId));
        }
        return answerList;
    }

}