package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.common.domain.dto.MessageConverter;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.core.assistant.Assistant;
import com.maxkb4j.core.langchain4j.AiChatMemory;
import com.maxkb4j.core.langchain4j.AiServiceFactory;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractChatStreamNodeHandler;
import com.maxkb4j.workflow.model.ModelConfig;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.ImageUnderstandNode;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@NodeHandlerType(NodeType.IMAGE_UNDERSTAND)
@Component
public class ImageUnderStandNodeHandler extends AbstractChatStreamNodeHandler {

    public ImageUnderStandNodeHandler(IModelProviderService modelFactory, IOssService ossService) {
        super(modelFactory, ossService);
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

        ModelConfig modelConfig = resolveModelConfig(workflow, params);
        // 构建 AI 服务
        Assistant assistant = buildAiServices(modelConfig.getModelId(), modelConfig.getModelParamsSetting(),
                systemPrompt, historyMessages);

        TokenStream tokenStream = assistant.chatStream(userPrompt, contents);

        boolean isResult = Boolean.TRUE.equals(params.getIsResult());
        boolean reasoningContentEnable = resolveReasoningContentEnable(params.getModelSetting());
        StreamOptions options = StreamOptions.withoutTools(isResult, reasoningContentEnable);
        return writeContextStreamAsync(options, tokenStream, workflow, node);
    }

    /**
     * 图片内容构建完成后的钩子：记录 imageList 到节点详情。
     */
    @Override
    protected void onImageContentsBuilt(AbsNode node, List<OssFile> imageFiles) {
        putDetail(node, "imageList", imageFiles);
    }

    private Assistant buildAiServices(String modelId, com.alibaba.fastjson.JSONObject modelParamsSetting,
                                      String systemPrompt, List<ChatMessage> historyMessages) {
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

    private void recordNodeDetails(AbsNode node, String systemPrompt, List<ChatMessage> historyMessages,
                                   String userPrompt, List<Content> contents) {
        putDetails(node, Map.of(
                "system", systemPrompt,
                "historyMessage", MessageConverter.resetMessageList(historyMessages),
                "question", userPrompt,
                "hasImages", !contents.isEmpty()
        ));
    }
}
