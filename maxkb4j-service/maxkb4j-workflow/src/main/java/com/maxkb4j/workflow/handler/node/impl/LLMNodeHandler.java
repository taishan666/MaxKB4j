package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.domain.dto.MessageConverter;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.core.assistant.Assistant;
import com.maxkb4j.core.langchain4j.AiChatMemory;
import com.maxkb4j.core.langchain4j.AiServiceFactory;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.tool.service.IToolFormatterService;
import com.maxkb4j.tool.service.IToolProviderService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractChatStreamNodeHandler;
import com.maxkb4j.workflow.model.ModelConfig;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.AiChatNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@NodeHandlerType(NodeType.AI_CHAT)
@Component
public class LLMNodeHandler extends AbstractChatStreamNodeHandler {

    private final IToolProviderService toolProviderService;
    private final IToolFormatterService toolFormatterService;

    public LLMNodeHandler(IModelProviderService modelFactory,
                          IToolProviderService toolProviderService,
                          IToolFormatterService toolFormatterService,
                          IOssService ossService) {
        super(modelFactory, ossService);
        this.toolProviderService = toolProviderService;
        this.toolFormatterService = toolFormatterService;
    }

    @Override
    protected CompletableFuture<NodeResult> doExecuteAsync(Workflow workflow, AbsNode node) {
        AiChatNode.NodeParams params = parseParams(node, AiChatNode.NodeParams.class);
        String userPrompt = workflow.renderPrompt(params.getPrompt());
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
        recordNodeDetails(node, systemPrompt, historyMessages, userPrompt, contents);

        ModelConfig modelConfig = resolveModelConfig(workflow, params);
        // 构建 AI 服务
        Assistant assistant = buildAiServices(modelConfig.getModelId(), modelConfig.getModelParamsSetting(),
                workflow, systemPrompt, historyMessages, toolIds, applicationIds);

        TokenStream tokenStream = assistant.chatStream(userPrompt, contents);

        boolean isResult = Boolean.TRUE.equals(params.getIsResult());
        boolean toolOutputEnable = Boolean.TRUE.equals(params.getToolOutputEnable());
        boolean reasoningContentEnable = resolveReasoningContentEnable(params.getModelSetting());
        StreamOptions options = StreamOptions.of(isResult, reasoningContentEnable, toolOutputEnable);
        return writeContextStreamAsync(options, tokenStream, workflow, node);
    }

    /**
     * 工具执行前钩子：输出工具执行前的格式化消息。
     */
    @Override
    protected void onBeforeToolExecution(BeforeToolExecution toolExecute, Workflow workflow, AbsNode node) {
        String toolMessage = toolFormatterService.format(toolExecute);
        emitMessage(workflow, node, toolMessage, "");
    }

    /**
     * 工具执行后钩子：输出工具执行后的格式化消息，并返回需累加到答案的文本。
     */
    @Override
    protected String onToolExecuted(ToolExecution toolExecute, Workflow workflow, AbsNode node) {
        String toolMessage = toolFormatterService.format(toolExecute);
        emitMessage(workflow, node, toolMessage, "");
        return toolMessage;
    }

    private Assistant buildAiServices(String modelId, JSONObject modelParamsSetting, Workflow workflow,
                                      String systemPrompt, List<ChatMessage> historyMessages,
                                      List<String> toolIds, List<String> applicationIds) {
        AiServices<Assistant> builder = AiServiceFactory.builder(Assistant.class);

        if (StringUtils.isNotBlank(systemPrompt)) {
            builder.systemMessage(systemPrompt);
        }
        if (CollectionUtils.isNotEmpty(historyMessages)) {
            builder.chatMemory(AiChatMemory.withMessages(historyMessages));
        }
        try {
            builder.toolProviders(toolProviderService.getToolProviders(toolIds, applicationIds));
        } catch (ApiException e) {
            workflow.output().emit(null); // Error will be propagated differently
        }
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(modelId, modelParamsSetting);
        return builder.streamingChatModel(chatModel).build();
    }

    private void recordNodeDetails(AbsNode node, String systemPrompt, List<ChatMessage> historyMessages,
                                   String textMassage, List<Content> contents) {
        List<JSONObject> question = MessageConverter.resetContents(contents);
        question.add(new JSONObject(Map.of("type", "text", "text", textMassage)));
        putDetails(node, Map.of(
                "system", systemPrompt,
                "historyMessage", MessageConverter.resetMessageList(historyMessages),
                "question", question
        ));
    }
}
