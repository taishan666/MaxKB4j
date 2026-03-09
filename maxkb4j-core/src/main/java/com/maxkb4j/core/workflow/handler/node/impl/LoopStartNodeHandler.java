package com.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.chat.dto.LoopParams;
import com.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.maxkb4j.core.workflow.enums.NodeType;
import com.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.maxkb4j.core.workflow.model.LoopWorkFlow;
import com.maxkb4j.core.workflow.model.NodeResult;
import com.maxkb4j.core.workflow.model.Workflow;
import com.maxkb4j.core.workflow.node.AbsNode;
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
            JSONArray loopInputFieldList=node.getProperties().getJSONArray("loopInputFieldList");
            for (int i = 0; i < loopInputFieldList.size(); i++) {
                JSONObject loopInputField=loopInputFieldList.getJSONObject(i);
                String key=loopInputField.getString("field");
                loopWorkFlow.getLoopContext().put(key,"");
            }
        }
        return new NodeResult(Map.of("index",index,"item",item));
    }
}
