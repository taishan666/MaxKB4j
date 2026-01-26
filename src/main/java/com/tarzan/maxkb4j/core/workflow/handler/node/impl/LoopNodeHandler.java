package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.builder.NodeBuilder;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.WorkflowHandler;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.logic.LogicFlow;
import com.tarzan.maxkb4j.core.workflow.model.LoopWorkFlow;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.LoopNode;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.dto.LoopParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@NodeHandlerType(NodeType.LOOP_NODE)
@Component
@RequiredArgsConstructor
public class LoopNodeHandler implements INodeHandler {

    private final WorkflowHandler workflowHandler;

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        LoopNode.NodeParams nodeParams = node.getNodeData().toJavaObject(LoopNode.NodeParams.class);
        String loopType = nodeParams.getLoopType();
        List<String> array = nodeParams.getArray();
        Integer number = nodeParams.getNumber();
        JSONObject loopBody = nodeParams.getLoopBody();
        if ("ARRAY".equals(loopType)) {
            generateLoopArray(array,workflow, loopBody, node);
        } else if ("LOOP".equals(loopType)) {
            generateWhileLoop(workflow, loopBody,node);
        } else {
            generateLoopNumber(number,workflow, loopBody,node);
        }
        return  new NodeResult(Map.of());
    }


    private void loop(Workflow workflow, JSONObject loopBody,LoopParams loopParams, AbsNode node) {
        LogicFlow logicFlow = LogicFlow.newInstance(loopBody);
        List<AbsNode> nodes = logicFlow.getNodes().stream().map(NodeBuilder::getNode).filter(Objects::nonNull).toList();
        LoopWorkFlow  loopWorkflow= new LoopWorkFlow(
                nodes,
                logicFlow.getEdges(),
                workflow.getChatParams(),
                loopParams,
                workflow.getSink());
        workflowHandler.execute(loopWorkflow);
        ChatMessageVO vo = node.toChatMessageVO(
                loopParams.getIndex(),
                workflow.getChatParams().getChatId(),
                workflow.getChatParams().getChatRecordId(),
                "",
                "",
                null,
                false);
        workflow.getSink().tryEmitNext(vo);
    }

    private void generateLoopArray(List<String> array, Workflow workflow,JSONObject loopBody, AbsNode node) {
        int startIndex = (int) node.getContext().getOrDefault("current_index", 0);
        for (int i = startIndex; i < array.size(); i++) {
            Object item = array.get(i);
            loop(workflow,loopBody,new LoopParams(i,item), node);
        }
    }

    private void generateLoopNumber(Integer number, Workflow workflow,  JSONObject loopBody, AbsNode node) {
        int startIndex = (int) node.getContext().getOrDefault("current_index", 0);
        for (int i = startIndex; i < number; i++) {
            loop(workflow,loopBody,new LoopParams(i,i), node);
        }
    }

    private void generateWhileLoop(Workflow workflow,  JSONObject loopBody,AbsNode node) {
        int i = (int) node.getContext().getOrDefault("current_index", 0);
        do {
            loop(workflow,loopBody,new LoopParams(i,i), node);
            i++;
        } while (i <= 500);
    }

}
