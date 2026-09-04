package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.engine.graph.AbstractLoopWorkflow;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.LoopParams;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import org.springframework.stereotype.Component;

import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeHandlerType(NodeType.LOOP_START)
@Component
public class LoopStartNodeHandler extends AbsNodeHandler {

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        int index = 0;
        Object item = null;
        if (workflow instanceof AbstractLoopWorkflow loopWorkFlow) {
            LoopParams loopParams = loopWorkFlow.getLoopParams();
            index = loopParams.getIndex();
            item = loopParams.getItem();
            JSONObject properties = node.getProperties();
            if (properties != null){
                JSONArray loopInputFieldList = properties.getJSONArray(LoopField.LOOP_INPUT_FIELD_LIST);
                if (loopInputFieldList != null) {
                    for (int i = 0; i < loopInputFieldList.size(); i++) {
                        JSONObject loopInputField = loopInputFieldList.getJSONObject(i);
                        String key = loopInputField.getString(NodeField.FIELD);
                        Object value=Defaults.NONE;
                        if (workflow.getLoopContext().containsKey(key)){
                            value=workflow.getLoopContext().get(key);
                        }else {
                            workflow.getLoopContext().put(key,value);
                        }
                        loopInputField.put(VariableField.VALUE, value);
                    }
                    putDetail(node, LoopField.LOOP_INPUT_FIELD_LIST, loopInputFieldList);
                }
            }
        }
        item=item==null?Defaults.NONE:item.toString();
        return new NodeResult(Map.of(RuntimeDetailField.INDEX, index, LoopField.ITEM, item));
    }
}
