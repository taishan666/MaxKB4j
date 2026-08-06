package com.maxkb4j.application.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.service.IChatService;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.common.domain.dto.ChatContext;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.workflow.builder.NodeBuilder;
import com.maxkb4j.workflow.logic.LogicFlow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.service.IWorkFlowActuator;
import com.maxkb4j.workflow.service.WorkflowFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class ChatFlowServiceImpl implements IChatService {

    private final IWorkFlowActuator workFlowActuator;
    private final NodeBuilder nodeBuilder;
    private final WorkflowFactory workflowFactory;

    @Override
    public ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams, ChatContext chatContext, Sinks.Many<ChatMessageVO> sink) {
        LogicFlow logicFlow = LogicFlow.newInstance(application.getWorkFlow());
        List<AbsNode> nodes = logicFlow.getNodes().stream().map(nodeBuilder::getNode).filter(Objects::nonNull).toList();
        IWorkflow workflow = workflowFactory.createApplication(nodes, logicFlow.getEdges(), chatParams, chatContext, sink);
        workFlowActuator.execute(workflow);
        List<Answer> answerTextList = workflow.output().answers();
        JSONObject details = workflow.output().runtimeDetails();
        return new ChatResponse(answerTextList, details);
    }

}