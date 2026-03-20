package com.maxkb4j.application.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.application.service.IChatService;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatResponse;
import com.maxkb4j.workflow.builder.NodeBuilder;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.logic.LogicFlow;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;
import com.maxkb4j.workflow.service.IWorkFlowActuator;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class ChatFlowServiceImpl implements IChatService {

    private final IWorkFlowActuator workFlowActuator;

    @Override
    public ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams, Sinks.Many<ChatMessageVO> sink) {
        chatParams.setChatRecordId(chatParams.getChatRecordId() == null ? IdWorker.get32UUID() : chatParams.getChatRecordId());
        LogicFlow logicFlow = LogicFlow.newInstance(application.getWorkFlow());
        List<AbsNode> nodes = logicFlow.getNodes().stream().map(NodeBuilder::getNode).filter(Objects::nonNull).toList();
        Workflow workflow = new Workflow(
                WorkflowMode.APPLICATION,
                nodes,
                logicFlow.getEdges(),
                chatParams,
                sink);
        workFlowActuator.execute(workflow);
        List<Answer> answerTextList =workflow.getAnswerTextList();
        JSONObject details = workflow.getRuntimeDetails();
        return new ChatResponse(answerTextList, details);
    }

}
