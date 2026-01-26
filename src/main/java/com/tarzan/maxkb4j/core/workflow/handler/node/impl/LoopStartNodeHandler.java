package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.LoopWorkFlow;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.module.chat.dto.LoopParams;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.LOOP_START)
@Component
public class LoopStartNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        int index=0;
        Object item = "None";
        if (workflow instanceof LoopWorkFlow loopWorkFlow) {
            LoopParams loopParams = loopWorkFlow.getLoopParams();
            index=loopParams.getIndex();
            item=loopParams.getItem();
        }
        return new NodeResult(Map.of("index",index,"item",item));
    }
}
