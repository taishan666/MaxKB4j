package com.tarzan.maxkb4j.core.workflow.node.aichat.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.common.util.ToolUtil;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.node.aichat.input.ChatNodeParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
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
import java.util.concurrent.ExecutionException;

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
        String question = super.generatePrompt(nodeParams.getPrompt());
        String systemPrompt = super.generatePrompt(nodeParams.getSystem());
        List<String> toolIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nodeParams.getToolIds()) && nodeParams.getToolEnable()) {
            toolIds.addAll(nodeParams.getToolIds());
        }
        if (StringUtil.isNotBlank(nodeParams.getMcpToolId()) && nodeParams.getMcpEnable()) {
            toolIds.add(nodeParams.getMcpToolId());
        }
        List<ChatMessage> historyMessages = super.getHistoryMessages(nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), runtimeNodeId);
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
        Map<String, Object> nodeVariable = new HashMap<>(Map.of(
                "system", systemPrompt,
                "history_message", resetMessageList(historyMessages),
                "question", question
        ));
        return writeContextStream(nodeVariable, Map.of(),nodeParams,tokenStream);
    }




    private NodeResult writeContextStream(Map<String, Object> nodeVariable, Map<String, Object> globalVariable,ChatNodeParams nodeParams,TokenStream tokenStream) {
        boolean isResult = nodeParams.getIsResult();
        boolean reasoningContentEnable = nodeParams.getModelSetting().getBooleanValue("reasoningContentEnable");
        CompletableFuture<ChatResponse> chatResponseFuture  = new CompletableFuture<>();
        // 完成后释放线程
        tokenStream.onPartialThinking(thinking -> {
                    if (isResult && reasoningContentEnable) {
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
                .onCompleteResponse(chatResponseFuture::complete)
                .onError(error -> {
                    super.getChatParams().getSink().tryEmitError(error);
                    chatResponseFuture.completeExceptionally(error); // 完成后释放线程
                })
                .start();
        try {
            // 阻塞等待 answer
            ChatResponse response = chatResponseFuture.get(); // 可设置超时：get(30, TimeUnit.SECONDS)
            String answer = response.aiMessage().text();
            String thinking = response.aiMessage().thinking();
            TokenUsage tokenUsage = response.tokenUsage();
            nodeVariable.put("messageTokens", tokenUsage.inputTokenCount());
            nodeVariable.put("answerTokens", tokenUsage.outputTokenCount());
            nodeVariable.put("answer", answer);
            nodeVariable.put("reasoningContent", thinking);
            return new NodeResult(nodeVariable,globalVariable, this::writeContext);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error waiting for TokenStream completion", e);
            Thread.currentThread().interrupt(); // 恢复中断状态
            return new NodeResult(nodeVariable, globalVariable);
        }

    }

    private void writeContext(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, INode node, WorkflowManage workflow) {
        if (nodeVariable != null) {
            node.getContext().putAll(nodeVariable);
            if (workflow.isResult(node, new NodeResult(nodeVariable, globalVariable)) && nodeVariable.containsKey("answer")) {
                String answer = (String) nodeVariable.get("answer");
                workflow.setAnswer(answer);
                ChatMessageVO endVo = new ChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        node.getId(),
                        "\n",
                        "",
                        node.getRuntimeNodeId(),
                        node.getType(),
                        node.getViewType(),
                        true);
                node.getChatParams().getSink().tryEmitNext(endVo);
            }
        }
    }



    @Override
    public void saveContext(JSONObject detail) {
        context.put("system", detail.get("system"));
        context.put("history_message", detail.get("history_message"));
        context.put("question", detail.get("question"));
        context.put("answer", detail.get("answer"));
        context.put("reasoningContent", detail.get("reasoningContent"));
        context.put("messageTokens", detail.get("messageTokens"));
        context.put("answerTokens", detail.get("answerTokens"));
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("system", context.get("system"));
        detail.put("history_message", context.get("history_message"));
        detail.put("question", context.get("question"));
        detail.put("answer", context.get("answer"));
        detail.put("reasoningContent", context.get("reasoningContent"));
        detail.put("messageTokens", context.get("messageTokens"));
        detail.put("answerTokens", context.get("answerTokens"));
        return detail;
    }
}
