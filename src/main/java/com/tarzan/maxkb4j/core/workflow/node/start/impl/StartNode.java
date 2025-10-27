package com.tarzan.maxkb4j.core.workflow.node.start.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;

import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.START;

public class StartNode extends INode {


    public StartNode(JSONObject properties) {
        super(properties);
        super.setType(START.getKey());
    }


    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("question", detail.get("question"));
        context.put("image", detail.get("image"));
        context.put("document", detail.get("document"));
        context.put("audio", detail.get("audio"));
        context.put("other", detail.get("other"));
        JSONArray globalFields= (JSONArray) detail.get("globalFields");
        for (int i = 0; i < globalFields.size(); i++) {
            JSONObject globalField=globalFields.getJSONObject(i);
            String key=globalField.getString("key");
            Object value=globalField.get("value");
            workflow.getContext().put(key, value);
        }
        JSONArray chatFields= (JSONArray) detail.get("chatFields");
        for (int i = 0; i < chatFields.size(); i++) {
            JSONObject chatField=chatFields.getJSONObject(i);
            String key=chatField.getString("key");
            Object value=chatField.get("value");
            workflow.getChatContext().put(key, value);
        }
    }

    @Override
    public Map<String, Object> executeDetail() {
        JSONObject config=super.getProperties().getJSONObject("config");
        JSONArray globalFields=config.getJSONArray("globalFields");
        for (int i = 0; i < globalFields.size(); i++) {
            JSONObject globalField=globalFields.getJSONObject(i);
            String key=globalField.getString("value");
            globalField.put("key",key);
            globalField.put("value", detail.get(key));
        }
        detail.put("globalFields",globalFields);
        JSONArray chatFields=config.getJSONArray("chatFields");
        for (int i = 0; i < chatFields.size(); i++) {
            JSONObject chatField=chatFields.getJSONObject(i);
            String key=chatField.getString("value");
            chatField.put("key",key);
            chatField.put("value",detail.get(key));
        }
        detail.put("chatFields",globalFields);
        return detail;
    }


}
