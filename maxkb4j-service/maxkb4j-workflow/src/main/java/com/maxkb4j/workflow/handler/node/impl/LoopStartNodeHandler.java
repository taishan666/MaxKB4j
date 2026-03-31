package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.LoopParams;
import com.maxkb4j.workflow.model.LoopWorkFlow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.LOOP_START)
@Component
public class LoopStartNodeHandler extends AbstractNodeHandler<Object> {

    @Override
    protected Class<Object> getParamsClass() {
        return Object.class;
    }

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node, Object params) throws Exception {
        int index = 0;
        Object item = "None";

        if (workflow instanceof LoopWorkFlow loopWorkFlow) {
            LoopParams loopParams = loopWorkFlow.getLoopParams();
            index = loopParams.getIndex();
            item = loopParams.getItem();

            JSONArray loopInputFieldList = node.getProperties().getJSONArray("loopInputFieldList");
            if (loopInputFieldList != null) {
                for (int i = 0; i < loopInputFieldList.size(); i++) {
                    JSONObject loopInputField = loopInputFieldList.getJSONObject(i);
                    String key = loopInputField.getString("field");
                    loopWorkFlow.getLoopContext().put(key, "");
                }
            }
        }

        return buildResult(Map.of("index", index, "item", item));
    }
}
