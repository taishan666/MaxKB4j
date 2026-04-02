package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.MessageConverter;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.MimeTypeUtils;
import com.maxkb4j.core.assistant.Assistant;
import com.maxkb4j.core.langchain4j.AppChatMemory;
import com.maxkb4j.core.langchain4j.AssistantServices;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.tool.service.IToolProviderService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.enums.WorkflowMode;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.model.params.AiChatNodeParams;
import com.maxkb4j.workflow.node.AbsNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.web.util.UriUtils.extractFileExtension;

@Slf4j
@NodeHandlerType({NodeType.AI_CHAT, NodeType.IMAGE_UNDERSTAND})
@Component
@RequiredArgsConstructor
public class LLMNodeHandler extends AbsNodeHandler {

    private final IModelProviderService modelFactory;
    private final IToolProviderService toolProviderService;
    private final IOssService fileService;


    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        AiChatNodeParams params = parseParams(node, AiChatNodeParams.class);
        String question = workflow.renderPrompt(params.getPrompt());
        String systemPrompt = workflow.renderPrompt(params.getSystem());
        List<ChatMessage> historyMessages = workflow.getHistoryMessages(
                params.getDialogueNumber(),
                params.getDialogueType(),
                node.getRuntimeNodeId()
        );
        List<String> toolIds = Optional.ofNullable(params.getToolIds()).orElse(List.of());
        List<String> applicationIds = Optional.ofNullable(params.getApplicationIds()).orElse(List.of());

        // 构建多模态内容（如图片）
        List<Content> contents = buildImageContents(workflow, node, params.getImageList());

        // 记录上下文用于调试/追踪
        recordNodeDetails(node, systemPrompt, historyMessages, question, contents);

        // 构建 AI 服务
        Assistant assistant = buildAiServices(params.getModelId(), params.getModelParamsSetting(),
                workflow, systemPrompt, historyMessages, toolIds, applicationIds);

        TokenStream tokenStream = assistant.chatStream(question, contents);

        return writeContextStream(params, tokenStream, workflow, node);
    }

    private Assistant buildAiServices(String modelId, JSONObject modelParamsSetting, Workflow workflow,
                                      String systemPrompt, List<ChatMessage> historyMessages,
                                      List<String> toolIds, List<String> applicationIds) {
        AiServices<Assistant> builder = AssistantServices.builder(Assistant.class);

        if (StringUtils.isNotBlank(systemPrompt)) {
            builder.systemMessage(systemPrompt);
        }
        if (CollectionUtils.isNotEmpty(historyMessages)) {
            builder.chatMemory(AppChatMemory.withMessages(historyMessages));
        }
        if (CollectionUtils.isNotEmpty(toolIds) || CollectionUtils.isNotEmpty(applicationIds)) {
            try {
                builder.toolProvider(toolProviderService.getSkillsProvider(modelId, toolIds));
                builder.tools(toolProviderService.getToolMap(toolIds, applicationIds));
            } catch (ApiException e) {
                workflow.output().emit(null); // Error will be propagated differently
            }
        }

        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(modelId, modelParamsSetting);
        return builder.streamingChatModel(chatModel).build();
    }

    private List<Content> buildImageContents(Workflow workflow, AbsNode node, List<String> imageFieldList) {
        List<Content> contents = new ArrayList<>();
        if (CollectionUtils.isEmpty(imageFieldList)) {
            return contents;
        }
        try {
            Object object = workflow.getReferenceField(imageFieldList);
            if (!(object instanceof List<?> fileList)) {
                return contents;
            }
            List<OssFile> imageFiles = fileList.stream()
                    .filter(OssFile.class::isInstance)
                    .map(OssFile.class::cast)
                    .toList();
            for (OssFile file : imageFiles) {
                byte[] bytes = fileService.getBytes(file.getFileId());
                String base64Data = Base64.getEncoder().encodeToString(bytes);
                String extension = extractFileExtension(file.getName());
                ImageContent imageContent = ImageContent.from(base64Data, MimeTypeUtils.getMimeType(extension));
                contents.add(imageContent);
            }
            putDetail(node, "imageList", imageFiles);
        } catch (Exception e) {
            log.warn("Failed to load image contents for node: {}", node.getRuntimeNodeId(), e);
        }
        return contents;
    }

    private void recordNodeDetails(AbsNode node, String systemPrompt, List<ChatMessage> historyMessages,
                                   String question, List<Content> contents) {
        putDetails(node, Map.of(
                "system", systemPrompt,
                "history_message", MessageConverter.resetMessageList(historyMessages),
                "question", question,
                "hasImages", !contents.isEmpty()
        ));
    }

    private NodeResult handleChatResponse(ChatResponse response, AbsNode node, String errorMessage) {
        String answer = Optional.ofNullable(response.aiMessage().text()).orElse("");
        String reasoning = Optional.ofNullable(response.aiMessage().thinking()).orElse("");
        TokenUsage tokenUsage = response.tokenUsage();

        if (tokenUsage != null) {
            putDetails(node, Map.of(
                    "messageTokens", tokenUsage.inputTokenCount(),
                    "answerTokens", tokenUsage.outputTokenCount()
            ));
        }

        return new NodeResult(Map.of(
                "answer", answer,
                "reasoningContent", reasoning,
                "exceptionMessage", errorMessage
        ), true);
    }

    private NodeResult writeContextStream(AiChatNodeParams params, TokenStream tokenStream,
                                          Workflow workflow, AbsNode node) {
        AtomicReference<String> errorMessage = new AtomicReference<>("");
        boolean isResult = Boolean.TRUE.equals(params.getIsResult());
        boolean toolOutputEnable = Boolean.TRUE.equals(params.getToolOutputEnable());
        boolean reasoningContentEnable = Optional.ofNullable(params.getModelSetting())
                .map(setting -> setting.getBooleanValue("reasoningContentEnable"))
                .orElse(false);

        CompletableFuture<ChatResponse> chatResponseFuture = new CompletableFuture<>();

        tokenStream.onPartialThinking(thinking -> {
                    if (isResult && reasoningContentEnable) {
                        emitMessage(workflow, node, "", thinking.text());
                    }
                }).onToolExecuted(toolExecute -> {
                    if (isResult && toolOutputEnable) {
                        String toolMessage = toolProviderService.format(toolExecute);
                        emitMessage(workflow, node, toolMessage, "");
                    }
                }).onPartialResponse(content -> {
                    if (isResult) {
                        emitMessage(workflow, node, content, "");
                    }
                }).onCompleteResponse(chatResponseFuture::complete)
                .onError(error -> {
                    errorMessage.set(error.getMessage());
                    chatResponseFuture.complete(ChatResponse.builder().build());
                })
                .start();

        ChatResponse response = chatResponseFuture.join();

        if (isResult) {
            setAnswer(node, response.aiMessage().text());
        }

        return handleChatResponse(response, node, errorMessage.get());
    }

    private void emitMessage(Workflow workflow, AbsNode node, String content, String reasoning) {
        if (WorkflowMode.APPLICATION.equals(workflow.getWorkflowMode())) {
            ChatMessageVO vo = node.toChatMessageVO(
                    workflow.getChatParams().getChatId(),
                    workflow.getChatParams().getChatRecordId(),
                    content,
                    reasoning,
                    null,
                    false
            );
            workflow.output().emit(vo);
        }
    }
}