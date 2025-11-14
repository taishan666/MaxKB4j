package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;

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
        context.put("image", detail.get("imageList"));
        context.put("document", detail.get("documentList"));
        context.put("audio", detail.get("audioList"));
        context.put("other", detail.get("otherList"));
        JSONArray globalFields= (JSONArray) detail.get("globalFields");
        for (int i = 0; i < globalFields.size(); i++) {
            JSONObject globalField=globalFields.getJSONObject(i);
            String key=globalField.getString("key");
            Object value=globalField.get("value");
            workflow.getContext().put(key, value);
        }
        JSONArray chatFields= (JSONArray) detail.get("chatFields");
        if (chatFields!=null){
            for (int i = 0; i < chatFields.size(); i++) {
                JSONObject chatField=chatFields.getJSONObject(i);
                String key=chatField.getString("key");
                Object value=chatField.get("value");
                workflow.getChatContext().put(key, value);
            }
        }
    }

    @Override
    public Map<String, Object> executeDetail() {
        detail.put("imageList", detail.get("image"));
        detail.put("documentList", detail.get("document"));
        detail.put("audioList", detail.get("audio"));
        detail.put("otherList", detail.get("other"));
        detail.remove("image");
        detail.remove("document");
        detail.remove("audio");
        detail.remove("other");
        JSONObject config=super.getProperties().getJSONObject("config");
        JSONArray globalFields=config.getJSONArray("globalFields");
        for (int i = 0; i < globalFields.size(); i++) {
            JSONObject globalField=globalFields.getJSONObject(i);
            String key=globalField.getString("value");
            globalField.put("key",key);
            globalField.put("value", detail.get(key));
        }
        detail.put("globalFields",globalFields);
        return detail;
    }


}
