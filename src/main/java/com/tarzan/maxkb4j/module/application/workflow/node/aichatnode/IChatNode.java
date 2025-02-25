package com.tarzan.maxkb4j.module.application.workflow.node.aichatnode;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.aichatnode.dto.ChatNodeParams;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

public abstract class IChatNode extends INode {

    @Override
    public String getType() {
        return "ai-chat-node";
    }

    @Override
    public ChatNodeParams getNodeParamsClass(JSONObject nodeParams) {
        if(Objects.isNull(nodeParams)){
            return new ChatNodeParams();
        }
        return nodeParams.toJavaObject(ChatNodeParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams),super.workflowParams);
    }

    public abstract NodeResult execute(ChatNodeParams nodeParams, FlowParams workflowParams);


}
