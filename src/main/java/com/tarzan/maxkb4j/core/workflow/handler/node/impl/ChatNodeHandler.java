/*
package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.common.util.ToolUtil;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.workflow.node.aichat.input.ChatNodeParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.TokenStream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
@AllArgsConstructor
public class ChatNodeHandler {

    private final ModelFactory modelFactory;
    private final ToolUtil toolUtil;



    @Override
    public NodeResult execute() throws Exception {
        ChatNodeParams nodeParams = super.getNodeData().toJavaObject(ChatNodeParams.class);
        BaseChatModel chatModel = modelFactory.build(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        String question = super.generatePrompt(nodeParams.getPrompt());
        String systemPrompt = super.generatePrompt(nodeParams.getSystem());
        List<String> toolIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nodeParams.getToolIds()) && nodeParams.getToolEnable()) {
            toolIds.addAll(nodeParams.getToolIds());
        }
        if (StringUtil.isNotBlank(nodeParams.getMcpToolId()) && nodeParams.getMcpEnable()) {
            toolIds.add(nodeParams.getMcpToolId());
        }
        List<ChatMessage> historyMessages = super.getHistoryMessages(nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), getRuntimeNodeId());
        if (StringUtil.isNotBlank(systemPrompt)) {
            aiServicesBuilder.systemMessageProvider(chatMemoryId -> systemPrompt);
        }
        if (CollectionUtils.isNotEmpty(historyMessages)) {
            aiServicesBuilder.chatMemory(AppChatMemory.withMessages(historyMessages));
        }
        if (CollectionUtils.isNotEmpty(toolIds)) {
            aiServicesBuilder.tools(toolUtil.getToolMap(toolIds));
        }
        Assistant assistant = aiServicesBuilder
                .streamingChatModel(chatModel.getStreamingChatModel())
                .build();
        TokenStream tokenStream = assistant.chatStream(question);
        detail.put("system", systemPrompt);
        detail.put("history_message", resetMessageList(historyMessages));
        detail.put("question", question);
        return writeContextStream(nodeParams,tokenStream);
    }


}
*/
