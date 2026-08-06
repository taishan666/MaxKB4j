package com.maxkb4j.workflow.node.impl;
import com.maxkb4j.workflow.annotation.NodeCreatorType;
import com.maxkb4j.workflow.enums.NodeType;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.cache.ChatCache;
import com.maxkb4j.common.domain.dto.ChatInfo;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;

import java.util.Map;

@NodeCreatorType(NodeType.START)
public class StartNode extends AbsNode {

    public StartNode(String id,JSONObject properties) {
        super(id,properties);
    }

    @Override
    public void saveContext(IWorkflow workflow, Map<String, Object> detail) {
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
            workflow.getGlobalContext().put(key, value);
        }
        String chatId = (String) workflow.getGlobalContext().get("chatId");
        ChatInfo chatInfo = ChatCache.get(chatId);
        if (chatInfo != null){
            workflow.getChatContext().putAll(chatInfo.getChatVariables());
        }
    }

    @Override
    public Map<String, Object> getDetail() {
        detail.put("imageList", context.get("image"));
        detail.put("documentList", context.get("document"));
        detail.put("audioList", context.get("audio"));
        detail.put("otherList", context.get("other"));
        detail.remove("image");
        detail.remove("document");
        detail.remove("audio");
        detail.remove("other");
        return detail;
    }

}
