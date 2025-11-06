package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.common.util.ToolUtil;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.tool.MessageTools;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.AiChatNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


@Slf4j
@Component
@AllArgsConstructor
public class AiChatNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;
    private final ToolUtil toolUtil;

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        AiChatNode.NodeParams nodeParams = node.getNodeData().toJavaObject(AiChatNode.NodeParams.class);
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        String question = workflow.generatePrompt(nodeParams.getPrompt());
        String systemPrompt = workflow.generatePrompt(nodeParams.getSystem());
        List<String> toolIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nodeParams.getToolIds()) && nodeParams.getToolEnable()) {
            toolIds.addAll(nodeParams.getToolIds());
        }
        if (StringUtil.isNotBlank(nodeParams.getMcpToolId()) && nodeParams.getMcpEnable()) {
            toolIds.add(nodeParams.getMcpToolId());
        }
        List<ChatMessage> historyMessages = workflow.getHistoryMessages(nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), node.getRuntimeNodeId());
        AiServices<Assistant> aiServicesBuilder = AiServices.builder(Assistant.class);
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
                .streamingChatModel(chatModel)
                .build();
        TokenStream tokenStream = assistant.chatStream(question);
        node.getDetail().put("system", systemPrompt);
        node.getDetail().put("history_message", node.resetMessageList(historyMessages));
        node.getDetail().put("question", question);
        return writeContextStream(nodeParams, tokenStream,workflow, node);
    }

    private NodeResult writeContextStream(AiChatNode.NodeParams nodeParams, TokenStream tokenStream, Workflow workflow,INode node) {
        boolean isResult = nodeParams.getIsResult();
        boolean mcpOutputEnable = nodeParams.getMcpOutputEnable();
        boolean reasoningContentEnable = nodeParams.getModelSetting().getBooleanValue("reasoningContentEnable");
        CompletableFuture<ChatResponse> chatResponseFuture = new CompletableFuture<>();
        // 完成后释放线程
        tokenStream.onPartialThinking(thinking -> {
                    if (isResult && reasoningContentEnable) {
                        ChatMessageVO vo = new ChatMessageVO(
                                workflow.getChatParams().getChatId(),
                                workflow.getChatParams().getChatRecordId(),
                                node.getId(),
                                "",
                                thinking.text(),
                                node.getUpNodeIdList(),
                                node.getRuntimeNodeId(),
                                node.getType(),
                                node.getViewType(),
                                false);
                        workflow.getChatParams().getSink().tryEmitNext(vo);
                    }
                })
                .onPartialResponse(content -> {
                    if (isResult) {
                        ChatMessageVO vo = new ChatMessageVO(
                                workflow.getChatParams().getChatId(),
                                workflow.getChatParams().getChatRecordId(),
                                node.getId(),
                                content,
                                "",
                                node.getUpNodeIdList(),
                                node.getRuntimeNodeId(),
                                node.getType(),
                                node.getViewType(),
                                false);
                        workflow.getChatParams().getSink().tryEmitNext(vo);
                    }
                })
                .onToolExecuted(toolExecute -> {
                    if (isResult && mcpOutputEnable) {
                        ChatMessageVO vo = new ChatMessageVO(
                                workflow.getChatParams().getChatId(),
                                workflow.getChatParams().getChatRecordId(),
                                node.getId(),
                                MessageTools.getToolMessage(toolExecute.request().name(), toolExecute.result()),
                                "",
                                node.getUpNodeIdList(),
                                node.getRuntimeNodeId(),
                                node.getType(),
                                node.getViewType(),
                                false);
                        workflow.getChatParams().getSink().tryEmitNext(vo);
                    }
                })
                .onCompleteResponse(chatResponseFuture::complete)
                .onError(error -> {
                    workflow.getChatParams().getSink().tryEmitError(error);
                    chatResponseFuture.completeExceptionally(error); // 完成后释放线程
                })
                .start();
        try {
            // 阻塞等待 answer 可设置超时：get(30, TimeUnit.SECONDS)
            ChatResponse response = chatResponseFuture.get();
            node.setAnswerText(response.aiMessage().text());
            String thinking = response.aiMessage().thinking();
            thinking = thinking == null ? "" : thinking;
            TokenUsage tokenUsage = response.tokenUsage();
            node.getDetail().put("messageTokens", tokenUsage.inputTokenCount());
            node.getDetail().put("answerTokens", tokenUsage.outputTokenCount());
            return new NodeResult(Map.of("answer", node.getAnswerText(), "reasoningContent", thinking), Map.of(), this::writeContext);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error waiting for TokenStream completion", e);
            Thread.currentThread().interrupt(); // 恢复中断状态
            return new NodeResult(null, null);
        }

    }

    private void writeContext(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, INode node, Workflow workflow) {
        if (nodeVariable != null) {
            node.getContext().putAll(nodeVariable);
            node.getDetail().putAll(nodeVariable);
            if (workflow.isResult(node, new NodeResult(nodeVariable, globalVariable)) && StringUtil.isNotBlank(node.getAnswerText())) {
                workflow.setAnswer(workflow.getAnswer() + node.getAnswerText());
                ChatMessageVO endVo = new ChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        node.getId(),
                        "\n",
                        "",
                        node.getUpNodeIdList(),
                        node.getRuntimeNodeId(),
                        node.getType(),
                        node.getViewType(),
                        true);
                workflow.getChatParams().getSink().tryEmitNext(endVo);
            }
        }
    }


}
