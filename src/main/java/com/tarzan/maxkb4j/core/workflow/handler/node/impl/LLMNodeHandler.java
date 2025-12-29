package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.ToolUtil;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.tool.MessageTools;
import com.tarzan.maxkb4j.core.tool.MimeTypeUtils;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.enums.WorkflowMode;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.AiChatNode;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@NodeHandlerType({NodeType.AI_CHAT, NodeType.IMAGE_UNDERSTAND})
@Component
@RequiredArgsConstructor
public class LLMNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;
    private final ToolUtil toolUtil;
    private final MongoFileService fileService;

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        AiChatNode.NodeParams nodeParams = node.getNodeData().toJavaObject(AiChatNode.NodeParams.class);
        String question = workflow.generatePrompt(nodeParams.getPrompt());
        String systemPrompt = workflow.generatePrompt(nodeParams.getSystem());
        List<ChatMessage> historyMessages = workflow.getHistoryMessages(
                nodeParams.getDialogueNumber(),
                nodeParams.getDialogueType(),
                node.getRuntimeNodeId()
        );
        List<String> toolIds = Optional.ofNullable(nodeParams.getToolIds())
                .orElse(List.of());
        // 构建 AI 服务
        AiServices<Assistant> aiServicesBuilder = buildAiServices(systemPrompt, historyMessages, toolIds);
        // 构建多模态内容（如图片）
        List<Content> contents = buildImageContents(workflow, node, nodeParams.getImageList());
        // 记录上下文用于调试/追踪
        recordNodeDetails(node, systemPrompt, historyMessages, question, contents);
        if (WorkflowMode.APPLICATION.equals(workflow.getWorkflowMode())) {
            StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(
                    nodeParams.getModelId(), nodeParams.getModelParamsSetting()
            );
            Assistant assistant = aiServicesBuilder.streamingChatModel(chatModel).build();
            TokenStream tokenStream = assistant.chatStream(question, contents);
            return writeContextStream(nodeParams, tokenStream, workflow, node);
        } else {
            ChatModel chatModel = modelFactory.buildChatModel(
                    nodeParams.getModelId(), nodeParams.getModelParamsSetting()
            );
            Assistant assistant = aiServicesBuilder.chatModel(chatModel).build();
            Result<String> result = assistant.chat(question, contents);
            return handleChatResponse(result.finalResponse(), node);
        }
    }

    private AiServices<Assistant> buildAiServices(String systemPrompt, List<ChatMessage> historyMessages, List<String> toolIds) {
        AiServices<Assistant> builder = AiServices.builder(Assistant.class);
        if (StringUtils.isNotBlank(systemPrompt)) {
            builder.systemMessageProvider(chatMemoryId -> systemPrompt);
        }
        if (CollectionUtils.isNotEmpty(historyMessages)) {
            builder.chatMemory(AppChatMemory.withMessages(historyMessages));
        }
        if (CollectionUtils.isNotEmpty(toolIds)) {
            builder.tools(toolUtil.getToolMap(toolIds));
        }
        return builder;
    }

    private List<Content> buildImageContents(Workflow workflow, INode node, List<String> imageFieldList) {
        List<Content> contents = new ArrayList<>();
        if (CollectionUtils.isEmpty(imageFieldList)) {
            return contents;
        }
        try {
            Object object = workflow.getReferenceField(imageFieldList.get(0), imageFieldList.get(1));
            if (!(object instanceof List<?> fileList)) {
                return contents;
            }
            List<SysFile> imageFiles = fileList.stream()
                    .filter(SysFile.class::isInstance)
                    .map(SysFile.class::cast)
                    .toList();
            for (SysFile file : imageFiles) {
                byte[] bytes = fileService.getBytes(file.getFileId());
                String base64Data = Base64.getEncoder().encodeToString(bytes);
                String extension = extractFileExtension(file.getName());
                ImageContent imageContent = ImageContent.from(base64Data, MimeTypeUtils.getMimeType(extension));
                contents.add(imageContent);
            }
            node.getDetail().put("imageList", imageFiles);
        } catch (Exception e) {
            log.warn("Failed to load image contents for node: {}", node.getRuntimeNodeId(), e);
        }
        return contents;
    }

    private String extractFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1).toLowerCase() : "bin";
    }

    private void recordNodeDetails(INode node, String systemPrompt, List<ChatMessage> historyMessages,
                                   String question, List<Content> contents) {
        node.getDetail().put("system", systemPrompt);
        node.getDetail().put("history_message", node.resetMessageList(historyMessages));
        node.getDetail().put("question", question);
        node.getDetail().put("hasImages", !contents.isEmpty());
    }

    private NodeResult handleChatResponse(ChatResponse response, INode node) {
        String answer = Optional.ofNullable(response.aiMessage().text()).orElse("");
        String reasoning = Optional.ofNullable(response.aiMessage().thinking()).orElse("");

        TokenUsage tokenUsage = response.tokenUsage();
        if (tokenUsage != null) {
            node.getDetail().put("messageTokens", tokenUsage.inputTokenCount());
            node.getDetail().put("answerTokens", tokenUsage.outputTokenCount());
        }

        return new NodeResult(Map.of("answer", answer, "reasoningContent", reasoning), true);
    }

    private NodeResult writeContextStream(AiChatNode.NodeParams nodeParams, TokenStream tokenStream,
                                          Workflow workflow, INode node) {
        boolean isResult = Boolean.TRUE.equals(nodeParams.getIsResult());
        boolean toolOutputEnable = Boolean.TRUE.equals(nodeParams.getToolOutputEnable());
        boolean reasoningContentEnable = Optional.ofNullable(nodeParams.getModelSetting())
                .map(setting -> setting.getBooleanValue("reasoningContentEnable"))
                .orElse(false);
        CompletableFuture<ChatResponse> chatResponseFuture = new CompletableFuture<>();
        tokenStream
                .onPartialThinking(thinking -> {
                    if (isResult && reasoningContentEnable) {
                        emitMessage(workflow, node, "", thinking.text());
                    }
                })
                .onToolExecuted(toolExecute -> {
                    if (isResult && toolOutputEnable) {
                        String toolMessage = MessageTools.getToolMessage(toolExecute);
                        emitMessage(workflow, node, toolMessage, "");
                    }
                })
                .onPartialResponse(content -> {
                    if (isResult) {
                        emitMessage(workflow, node, content, "");
                    }
                })
                .onCompleteResponse(chatResponseFuture::complete)
                .onError(error -> {
                    log.error("Streaming execution error in node: {}", node.getRuntimeNodeId(), error);
                    chatResponseFuture.completeExceptionally(error);
                })
                .start();
        ChatResponse response = chatResponseFuture.join();
        if (isResult) {
            node.setAnswerText(response.aiMessage().text());
        }
        return handleChatResponse(response, node);
    }

    private void emitMessage(Workflow workflow, INode node, String content, String reasoning) {
        ChatMessageVO vo = node.toChatMessageVO(
                workflow.getChatParams().getChatId(),
                workflow.getChatParams().getChatRecordId(),
                content,
                reasoning,
                null,
                false
        );
        workflow.getSink().tryEmitNext(vo);
    }
}