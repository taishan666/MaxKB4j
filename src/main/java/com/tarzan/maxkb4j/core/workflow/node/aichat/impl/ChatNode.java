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
        detail.put("system", systemPrompt);
        detail.put("history_message", resetMessageList(historyMessages));
        detail.put("question", question);
        return writeContextStream(nodeParams,tokenStream);
    }




    private NodeResult writeContextStream(ChatNodeParams nodeParams,TokenStream tokenStream) {
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
                                upNodeIdList,
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
                                upNodeIdList,
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
            // 阻塞等待 answer 可设置超时：get(30, TimeUnit.SECONDS)
            ChatResponse response = chatResponseFuture.get();
            String answer = response.aiMessage().text();
            String thinking = response.aiMessage().thinking();
            TokenUsage tokenUsage = response.tokenUsage();
            detail.put("messageTokens", tokenUsage.inputTokenCount());
            detail.put("answerTokens", tokenUsage.outputTokenCount());
            return new NodeResult(Map.of("answer", answer,"reasoningContent", thinking),Map.of(), this::writeContext);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error waiting for TokenStream completion", e);
            Thread.currentThread().interrupt(); // 恢复中断状态
            return new NodeResult(null, null);
        }

    }

    private void writeContext(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, INode node, WorkflowManage workflow) {
        if (nodeVariable != null) {
            node.getContext().putAll(nodeVariable);
            if (workflow.isResult(node, new NodeResult(nodeVariable, globalVariable)) && nodeVariable.containsKey("answer")) {
                String answer = (String) nodeVariable.get("answer");
                workflow.setAnswer(workflow.getAnswer()+answer);
                ChatMessageVO endVo = new ChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        node.getId(),
                        "\n",
                        "",
                        upNodeIdList,
                        runtimeNodeId,
                        node.getType(),
                        node.getViewType(),
                        true);
                node.getChatParams().getSink().tryEmitNext(endVo);
            }
        }
    }



    @Override
    public void saveContext(JSONObject detail) {
        context.put("answer", detail.get("answer"));
        context.put("reasoningContent", detail.get("reasoningContent"));
    }

    @Override
    public JSONObject getRunDetail() {
        return detail;
    }
}
