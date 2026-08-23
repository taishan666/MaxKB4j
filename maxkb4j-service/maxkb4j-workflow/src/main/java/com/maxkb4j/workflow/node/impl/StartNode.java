package com.maxkb4j.workflow.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;

import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeCreatorType(NodeType.START)
public class StartNode extends AbsNode {

    public StartNode(String id,JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
        context.put(NodeField.QUESTION, detail.get(NodeField.QUESTION));
        context.put(NodeField.IMAGE, detail.get(NodeField.IMAGE_LIST));
        context.put(NodeField.DOCUMENT, detail.get(NodeField.DOCUMENT_LIST));
        context.put(NodeField.AUDIO, detail.get(NodeField.AUDIO_LIST));
        context.put(NodeField.OTHER, detail.get(NodeField.OTHER_LIST));
        JSONArray globalFields= (JSONArray) detail.get(ChatField.GLOBAL_FIELDS);
        for (int i = 0; i < globalFields.size(); i++) {
            JSONObject globalField=globalFields.getJSONObject(i);
            String key=globalField.getString(VariableField.KEY);
            Object value=globalField.get(VariableField.VALUE);
            workflow.getGlobalContext().put(key, value);
        }
        JSONArray chatFields= (JSONArray) detail.get(ChatField.CHAT_FIELDS);
        for (int i = 0; i < chatFields.size(); i++) {
            JSONObject chatField=chatFields.getJSONObject(i);
            String key=chatField.getString(VariableField.KEY);
            Object value=chatField.get(VariableField.VALUE);
            workflow.getChatContext().put(key, value);
        }
    }

    @Override
    public Map<String, Object> getDetail() {
        detail.put(NodeField.IMAGE_LIST, context.get(NodeField.IMAGE));
        detail.put(NodeField.DOCUMENT_LIST, context.get(NodeField.DOCUMENT));
        detail.put(NodeField.AUDIO_LIST, context.get(NodeField.AUDIO));
        detail.put(NodeField.OTHER_LIST, context.get(NodeField.OTHER));
        detail.remove(NodeField.IMAGE);
        detail.remove(NodeField.DOCUMENT);
        detail.remove(NodeField.AUDIO);
        detail.remove(NodeField.OTHER);
        return detail;
    }

}
