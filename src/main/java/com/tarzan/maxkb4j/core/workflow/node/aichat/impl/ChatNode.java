package com.tarzan.maxkb4j.core.workflow.node.aichat.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.node.aichat.input.ChatNodeParams;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.common.util.ToolUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.AI_CHAT;

@Slf4j
public class ChatNode extends INode {

    private final ModelService modelService;
    private final ToolUtil toolUtil;
    private final AiServices<Assistant> aiServicesBuilder;


    public ChatNode(JSONObject properties) {
        super(properties);
        super.type = AI_CHAT.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
        this.toolUtil = SpringUtil.getBean(ToolUtil.class);
        this.aiServicesBuilder = AiServices.builder(Assistant.class);
    }


    @Override
    public NodeResult execute() throws Exception {
        ChatNodeParams nodeParams = super.getNodeData().toJavaObject(ChatNodeParams.class);
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        String problemText = super.generatePrompt(nodeParams.getPrompt());
        String systemPrompt= super.generatePrompt(nodeParams.getSystem());
        List<String> toolIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nodeParams.getToolIds()) && nodeParams.getToolEnable()) {
            toolIds.addAll(nodeParams.getToolIds());
        }
        if (StringUtil.isNotBlank(nodeParams.getMcpToolId()) && nodeParams.getMcpEnable()) {
            toolIds.add(nodeParams.getMcpToolId());
        }
        List<ChatMessage> historyMessages=super.getHistoryMessages(nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), runtimeNodeId);
        if (StringUtil.isNotBlank(systemPrompt)){
            aiServicesBuilder.systemMessageProvider(chatMemoryId -> systemPrompt);
        }
        if (CollectionUtils.isNotEmpty(historyMessages)){
            aiServicesBuilder.chatMemory(AppChatMemory.withMessages(historyMessages));
        }
        if (CollectionUtils.isNotEmpty(toolIds)){
            aiServicesBuilder.tools(toolUtil.getTools(toolIds));
        }
        Map<String, Object> nodeVariable = new HashMap<>(Map.of(
                "system", systemPrompt,
                "history_message", resetMessageList(historyMessages),
                "question", problemText,
                "answer", ""
        ));
        Assistant assistant = aiServicesBuilder
                .streamingChatModel(chatModel.getStreamingChatModel())
                .build();
        TokenStream tokenStream = assistant.chatStream(problemText);
        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();
        boolean isResult = nodeParams.getIsResult();
        boolean reasoningContentEnable = nodeParams.getModelSetting().getBooleanValue("reasoningContentEnable");
        tokenStream.onPartialThinking(thinking -> {
                    if (isResult&&reasoningContentEnable) {
                        ChatMessageVO vo = new ChatMessageVO(
                                getChatParams().getChatId(),
                                getChatParams().getChatRecordId(),
                                this.getId(),
                                "",
                                thinking.text(),
                                runtimeNodeId,
                                type,
                                viewType,
                                false);
                        super.getChatParams().getSink().tryEmitNext(vo);
                    }
                })
                .onPartialResponse(content -> {
                    if (isResult) {
                        ChatMessageVO vo = new ChatMessageVO(
                                getChatParams().getChatId(),
                                getChatParams().getChatRecordId(),
                                id,
                                content,
                                "",
                                runtimeNodeId,
                                type,
                                viewType,
                                false);
                        super.getChatParams().getSink().tryEmitNext(vo);
                    }
                })
                .onCompleteResponse(response -> {
                    String answer = response.aiMessage().text();
                    String thinking = response.aiMessage().thinking();
                    TokenUsage tokenUsage = response.tokenUsage();
                    nodeVariable.put("messageTokens", tokenUsage.inputTokenCount());
                    nodeVariable.put("answerTokens", tokenUsage.outputTokenCount());
                    nodeVariable.put("answer", answer);
                    nodeVariable.put("reasoningContent", thinking);
                    ChatMessageVO vo = new ChatMessageVO(
                            getChatParams().getChatId(),
                            getChatParams().getChatRecordId(),
                            type,
                            "",
                            "",
                            runtimeNodeId,
                            type,
                            viewType,
                            false);
                    super.getChatParams().getSink().tryEmitNext(vo);
                    futureChatResponse.complete(response);// 完成后释放线程
                })
                .onError(error -> {
                    super.getChatParams().getSink().tryEmitError(error);
                    futureChatResponse.completeExceptionally(error); // 完成后释放线程
                })
                .start();
        futureChatResponse.join(); // 阻塞当前线程直到 futureChatResponse 完成
        return new NodeResult(nodeVariable, Map.of(),this::writeContext);
    }

    private void writeContext(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, INode node, WorkflowManage workflow) {
        if (nodeVariable != null) {
            node.getContext().putAll(nodeVariable);
            String answer = (String) nodeVariable.get("answer");
            workflow.setAnswer(answer);
        }
    }



    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("system", context.get("system"));
        detail.put("history_message",  context.get("history_message"));
        detail.put("question", context.get("question"));
        detail.put("answer", context.get("answer"));
        detail.put("reasoningContent", context.get("reasoningContent"));
        detail.put("messageTokens", context.get("messageTokens"));
        detail.put("answerTokens", context.get("answerTokens"));
        return detail;
    }
}
