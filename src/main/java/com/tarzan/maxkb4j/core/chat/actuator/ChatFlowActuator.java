package com.tarzan.maxkb4j.core.chat.actuator;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.core.chat.provider.IChatActuator;
import com.tarzan.maxkb4j.core.workflow.builder.NodeBuilder;
import com.tarzan.maxkb4j.core.workflow.handler.WorkflowHandler;
import com.tarzan.maxkb4j.core.workflow.logic.LogicFlow;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class ChatFlowActuator implements IChatActuator {

    private final WorkflowHandler workflowHandler;

    @Override
    public ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams, Sinks.Many<ChatMessageVO> sink) {
        chatParams.setChatRecordId(chatParams.getChatRecordId() == null ? IdWorker.get32UUID() : chatParams.getChatRecordId());
        LogicFlow logicFlow = LogicFlow.newInstance(application.getWorkFlow());
        List<INode> nodes = logicFlow.getNodes().stream().map(NodeBuilder::getNode).filter(Objects::nonNull).toList();
        Workflow workflow = new Workflow(
                nodes,
                logicFlow.getEdges(),
                chatParams,
                sink);
        String answer = workflowHandler.execute(workflow);
        JSONObject details = workflow.getRuntimeDetails();
        return new ChatResponse(answer, details);
    }

}
