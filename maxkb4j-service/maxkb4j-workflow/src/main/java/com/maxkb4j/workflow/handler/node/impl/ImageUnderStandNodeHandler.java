package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.MessageConverter;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.common.util.MimeTypeUtils;
import com.maxkb4j.core.assistant.Assistant;
import com.maxkb4j.core.langchain4j.AiChatMemory;
import com.maxkb4j.core.langchain4j.AiServiceFactory;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.ModelConfig;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.ImageUnderstandNode;
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
import java.util.regex.Pattern;

import static org.springframework.web.util.UriUtils.extractFileExtension;

@Slf4j
@NodeHandlerType(NodeType.IMAGE_UNDERSTAND)
@Component
@RequiredArgsConstructor
public class ImageUnderStandNodeHandler extends AbsNodeHandler {

    private final IModelProviderService modelFactory;
    private final IOssService ossService;

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        throw new UnsupportedOperationException("LLM node uses async execution via doExecuteAsync");
    }

    @Override
    protected CompletableFuture<NodeResult> doExecuteAsync(Workflow workflow, AbsNode node) {
        ImageUnderstandNode.NodeParams params = parseParams(node, ImageUnderstandNode.NodeParams.class);
        String userPrompt = workflow.renderPrompt(params.getPrompt());
        String systemPrompt = workflow.renderPrompt(params.getSystem());
        List<ChatMessage> historyMessages = workflow.getHistoryMessages(
                params.getDialogueNumber(),
                params.getDialogueType(),
                node.getRuntimeNodeId()
        );
        // 构建多模态内容（如图片）
        List<Content> contents = buildImageContents(workflow, node, params.getImageList());
        // 记录上下文用于调试/追踪
        recordNodeDetails(node, systemPrompt, historyMessages, userPrompt, contents);
        String modelId = params.getModelId();
        JSONObject modelParamsSetting = params.getModelParamsSetting();
        if (params.getModelIdType() != null && params.getModelIdType().equals("reference")){
            ModelConfig modelConfig = ModelConfig.from(workflow.getReferenceField(params.getModelIdReference()));
            modelId = modelConfig.getModelId();
            modelParamsSetting = modelConfig.getModelParamsSetting();
        }
        // 构建 AI 服务
        Assistant assistant = buildAiServices(modelId, modelParamsSetting, systemPrompt, historyMessages);

        TokenStream tokenStream = assistant.chatStream(userPrompt, contents);

        return writeContextStreamAsync(params, tokenStream, workflow, node);
    }

    private Assistant buildAiServices(String modelId, JSONObject modelParamsSetting, String systemPrompt, List<ChatMessage> historyMessages) {
        AiServices<Assistant> builder = AiServiceFactory.builder(Assistant.class);

        if (StringUtils.isNotBlank(systemPrompt)) {
            builder.systemMessage(systemPrompt);
        }
        if (CollectionUtils.isNotEmpty(historyMessages)) {
            builder.chatMemory(AiChatMemory.withMessages(historyMessages));
        }
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(modelId, modelParamsSetting);
        return builder.streamingChatModel(chatModel).build();
    }

    private List<Content> buildImageContents(Workflow workflow, AbsNode node, List<String> imageFieldList) {
        List<Content> contents = new ArrayList<>();
        try {
            List<OssFile> imageFiles = getOssFiles(workflow,imageFieldList);
            for (OssFile file : imageFiles) {
                byte[] bytes = ossService.getBytes(file.getFileId());
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
                                   String userPrompt, List<Content> contents) {
        putDetails(node, Map.of(
                "system", systemPrompt,
                "historyMessage", MessageConverter.resetMessageList(historyMessages),
                "question", userPrompt,
                "hasImages", !contents.isEmpty()
        ));
    }
    /**
     * 工具渲染标签正则表达式
     */
    private static final Pattern TOOL_CALLS_RENDER_PATTERN = Pattern.compile("<tool_calls_render>(.*?)</tool_calls_render>", Pattern.DOTALL);

    private NodeResult handleChatResponse(ChatResponse response, String answer, AbsNode node, String errorMessage) {
        String reasoning = Optional.ofNullable(response.aiMessage().thinking()).orElse("");
        TokenUsage tokenUsage = response.tokenUsage();
        if (tokenUsage != null) {
            putDetails(node, Map.of(
                    "messageTokens", tokenUsage.inputTokenCount(),
                    "answerTokens", tokenUsage.outputTokenCount()
            ));
        }
        return new NodeResult(Map.of(
                "answer", TOOL_CALLS_RENDER_PATTERN.matcher(answer).replaceAll(""),
                "reasoningContent", reasoning,
                "exceptionMessage", errorMessage
        ), true);
    }

    /**
     * 异步流式写入：不阻塞线程，直接返回 CompletableFuture
     * 流式回调完成后自动 complete Future，释放线程资源
     */
    private CompletableFuture<NodeResult> writeContextStreamAsync(ImageUnderstandNode.NodeParams params, TokenStream tokenStream,
                                                                  Workflow workflow, AbsNode node) {
        List<String> answerTexts = new ArrayList<>();
        AtomicReference<String> errorMessage = new AtomicReference<>("");
        boolean isResult = Boolean.TRUE.equals(params.getIsResult());
        boolean reasoningContentEnable = Optional.ofNullable(params.getModelSetting())
                .map(setting -> setting.getBooleanValue("reasoningContentEnable"))
                .orElse(false);
        CompletableFuture<NodeResult> resultFuture = new CompletableFuture<>();
        tokenStream.onPartialThinking(thinking -> {
                    if (isResult && reasoningContentEnable) {
                        emitMessage(workflow, node, "", thinking.text());
                    }
                }).onPartialResponse(content -> {
                    if (isResult) {
                        emitMessage(workflow, node, content, "");
                        answerTexts.add(content);
                    }
                }).onCompleteResponse(response -> {
                    String answer = String.join("", answerTexts);
                    if (isResult) {
                        setAnswerText(node, answer);
                    }
                    resultFuture.complete(handleChatResponse(response, answer, node, errorMessage.get()));
                }).onError(error -> {
                    errorMessage.set(error.getMessage());
                    resultFuture.completeExceptionally(error);
                })
                .start();
        return resultFuture;
    }

    private void emitMessage(Workflow workflow, AbsNode node, String content, String reasoning) {
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