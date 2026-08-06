package com.maxkb4j.workflow.handler.node;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.common.util.MimeTypeUtils;
import com.maxkb4j.core.assistant.Assistant;
import com.maxkb4j.core.langchain4j.AiChatMemory;
import com.maxkb4j.core.langchain4j.AiServiceFactory;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.workflow.model.ModelConfig;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.web.util.UriUtils.extractFileExtension;

/**
 * 流式聊天节点处理器的抽象基类。
 *
 * <p>提取 {@code LLMNodeHandler} 与 {@code ImageUnderStandNodeHandler} 之间高度重复的逻辑，
 * 包括：</p>
 * <ul>
 *   <li>多模态图片内容构建 {@link #buildImageContents}</li>
 *   <li>聊天响应处理 {@link #handleChatResponse}（含 tool_calls_render 标签清理）</li>
 *   <li>流式消息发送 {@link #emitMessage}</li>
 *   <li>异步流式写入 {@link #writeContextStreamAsync}（通过钩子方法支持工具处理扩展）</li>
 *   <li>reasoningContentEnable 解析 {@link #resolveReasoningContentEnable}</li>
 * </ul>
 *
 * <p>子类通过覆盖钩子方法 {@link #onBeforeToolExecution} 和 {@link #onToolExecuted}
 * 来扩展工具相关的流式行为（如 LLM 节点的工具消息输出）。</p>
 *
 * <p>子类仍需自行实现 {@link #doExecuteAsync}（构建 Assistant 并启动流）、
 */
@Slf4j
public abstract class AbstractChatStreamNodeHandler extends AbsNodeHandler {

    protected final IModelProviderService modelFactory;
    protected final IOssService ossService;

    protected AbstractChatStreamNodeHandler(IModelProviderService modelFactory, IOssService ossService) {
        this.modelFactory = modelFactory;
        this.ossService = ossService;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        throw new UnsupportedOperationException("Streaming node uses async execution via doExecuteAsync");
    }

    // ==================== Assistant construction ====================

    /**
     * Builds a streaming {@link Assistant} without tool support.
     */
    protected Assistant buildStreamingAssistant(IWorkflow workflow, ModelConfig modelConfig, String systemPrompt,
                                                List<ChatMessage> historyMessages) {
        return buildStreamingAssistant(workflow, modelConfig, systemPrompt, historyMessages, List.of());
    }

    /**
     * Builds a streaming {@link Assistant} with optional system prompt, chat memory and tool providers.
     */
    protected Assistant buildStreamingAssistant(IWorkflow workflow, ModelConfig modelConfig, String systemPrompt,
                                                List<ChatMessage> historyMessages, List<ToolProvider> toolProviders) {
        AiServices<Assistant> builder = AiServiceFactory.builder(Assistant.class);
        if (StringUtils.isNotBlank(systemPrompt)) {
            builder.systemMessage(systemPrompt);
        }
        if (CollectionUtils.isNotEmpty(historyMessages)) {
            builder.chatMemory(AiChatMemory.withMessages(workflow.getChatParams().getChatId(), historyMessages));
        }
        if (CollectionUtils.isNotEmpty(toolProviders)) {
            builder.toolProviders(toolProviders);
        }
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(
                modelConfig.getModelId(), modelConfig.getModelParamsSetting());
        return builder.streamingChatModel(chatModel).build();
    }

    // ==================== 多模态图片内容构建 ====================

    /**
     * 根据图片字段引用列表，从 OSS 加载图片并构建为 {@link ImageContent} 列表。
     *
     * <p>构建完成后会调用 {@link #onImageContentsBuilt} 钩子，子类可覆盖该钩子
     * 记录额外的节点详情（如 ImageUnderstand 节点记录 imageList）。</p>
     *
     * @param workflow        工作流上下文
     * @param node            节点实例
     * @param imageFieldList  图片字段引用路径列表
     * @return 图片内容列表，加载失败时返回空列表
     */
    protected List<Content> buildImageContents(IWorkflow workflow, AbsNode node, List<String> imageFieldList) {
        List<Content> contents = new ArrayList<>();
        try {
            List<OssFile> imageFiles = getOssFiles(workflow, imageFieldList);
            for (OssFile file : imageFiles) {
                byte[] bytes = ossService.getBytes(file.getFileId());
                String base64Data = Base64.getEncoder().encodeToString(bytes);
                String extension = extractFileExtension(file.getName());
                ImageContent imageContent = ImageContent.from(base64Data, MimeTypeUtils.getMimeType(extension));
                contents.add(imageContent);
            }
            onImageContentsBuilt(node, imageFiles);
        } catch (Exception e) {
            log.warn("Failed to load image contents for node: {}", node.getRuntimeNodeId(), e);
        }
        return contents;
    }

    /**
     * 图片内容构建完成后的钩子，默认空实现。
     * 子类可覆盖以记录额外详情（如 ImageUnderstand 节点写入 imageList）。
     *
     * @param node        节点实例
     * @param imageFiles  已加载的图片文件列表
     */
    protected void onImageContentsBuilt(AbsNode node, List<OssFile> imageFiles) {
        // 默认空实现
    }

    // ==================== 聊天响应处理 ====================

    /**
     * 处理聊天响应：提取推理内容、记录 Token 使用情况、清理工具渲染标签，
     * 并构造 {@link NodeResult}。
     *
     * @param response      聊天响应
     * @param answer        累积的答案文本
     * @param node          节点实例
     * @param errorMessage  错误信息（可为空）
     * @return 节点执行结果
     */
    protected NodeResult handleChatResponse(ChatResponse response, String answer, AbsNode node, String errorMessage) {
        String reasoning = Optional.ofNullable(response.aiMessage().thinking()).orElse("");
        recordTokenUsage(node, response.tokenUsage());
        return new NodeResult(Map.of(
                "answer", answer,
                "reasoningContent", reasoning,
                "exceptionMessage", errorMessage
        ), true);
    }

    // ==================== 流式消息发送 ====================

    /**
     * 构造 {@link ChatMessageVO} 并通过工作流输出流发送。
     *
     * @param workflow  工作流上下文
     * @param node      节点实例
     * @param content   消息内容
     * @param reasoning 推理内容
     */
    protected void emitMessage(IWorkflow workflow, AbsNode node, String content, String reasoning) {
        ChatParams chatParams=workflow.getChatParams();
        ChatMessageVO vo = node.toChatMessageVO(
                chatParams.getChatId(),
                chatParams.getChatRecordId(),
                content,
                reasoning,
                null,
                false
        );
        workflow.output().emit(vo);
    }

    // ==================== 异步流式写入 ====================

    /**
     * 解析 modelSetting 中的 reasoningContentEnable 开关。
     *
     * @param modelSetting 模型设置 JSONObject，可为 null
     * @return 是否启用推理内容输出，默认 false
     */
    protected boolean resolveReasoningContentEnable(JSONObject modelSetting) {
        return Optional.ofNullable(modelSetting)
                .map(setting -> setting.getBooleanValue("reasoningContentEnable"))
                .orElse(false);
    }

    /**
     * 通用异步流式写入逻辑。
     *
     * <p>封装 onPartialThinking / onPartialResponse / onCompleteResponse / onError 的标准处理流程，
     * 并通过 {@link #onBeforeToolExecution} 和 {@link #onToolExecuted} 钩子支持工具处理扩展。
     * 无工具的节点（如 ImageUnderstand）只需传入 {@code toolOutputEnable=false}，
     * 钩子方法将不会被触发。</p>
     *
     * @param options                流式选项（isResult、reasoningContentEnable、toolOutputEnable）
     * @param tokenStream            Token 流
     * @param workflow               工作流上下文
     * @param node                   节点实例
     * @return 节点执行结果的 CompletableFuture
     */
    protected CompletableFuture<NodeResult> writeContextStreamAsync(
            StreamOptions options, TokenStream tokenStream, IWorkflow workflow, AbsNode node) {
        List<String> answerTexts = new ArrayList<>();
        AtomicReference<String> errorMessage = new AtomicReference<>("");
        boolean isResult = options.isResult();
        CompletableFuture<NodeResult> resultFuture = new CompletableFuture<>();
        tokenStream.onPartialThinking(thinking -> {
                    if (isResult && options.reasoningContentEnable()) {
                        emitMessage(workflow, node, "", thinking.text());
                    }
                }).beforeToolExecution(toolExecute -> {
                    if (isResult && options.toolOutputEnable()) {
                        onBeforeToolExecution(toolExecute, workflow, node);
                    }
                }).onToolExecuted(toolExecute -> {
                    if (isResult && options.toolOutputEnable()) {
                        String toolMessage = onToolExecuted(toolExecute, workflow, node);
                        if (toolMessage != null && !toolMessage.isEmpty()) {
                            answerTexts.add(toolMessage);
                        }
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

    /**
     * 工具执行前的钩子，默认空实现。
     * LLM 节点覆盖此方法以输出工具执行前的格式化消息。
     *
     * @param toolExecute 工具执行上下文
     * @param workflow    工作流上下文
     * @param node        节点实例
     */
    protected void onBeforeToolExecution(BeforeToolExecution toolExecute, IWorkflow workflow, AbsNode node) {
        // 默认空实现
    }

    /**
     * 工具执行后的钩子，默认返回空字符串。
     * LLM 节点覆盖此方法以输出工具执行后的格式化消息，并返回需累加到答案的文本。
     *
     * @param toolExecute 工具执行结果
     * @param workflow    工作流上下文
     * @param node        节点实例
     * @return 需要累加到答案的文本，默认空字符串（表示不累加）
     */
    protected String onToolExecuted(ToolExecution toolExecute, IWorkflow workflow, AbsNode node) {
        return "";
    }

    /**
     * 流式执行选项，封装 {@link #writeContextStreamAsync} 所需的开关参数。
     *
     * @param isResult                当前节点是否为结果节点
     * @param reasoningContentEnable  是否输出推理内容
     * @param toolOutputEnable        是否输出工具执行消息
     */
    protected record StreamOptions(boolean isResult, boolean reasoningContentEnable, boolean toolOutputEnable) {
        /**
         * 便捷工厂方法：无工具的流式节点（如 ImageUnderstand）使用，toolOutputEnable 固定为 false。
         */
        public static StreamOptions withoutTools(boolean isResult, boolean reasoningContentEnable) {
            return new StreamOptions(isResult, reasoningContentEnable, false);
        }

        /**
         * 通用工厂方法：支持工具输出的流式节点（如 LLM）使用。
         */
        public static StreamOptions of(boolean isResult, boolean reasoningContentEnable, boolean toolOutputEnable) {
            return new StreamOptions(isResult, reasoningContentEnable, toolOutputEnable);
        }
    }
}
