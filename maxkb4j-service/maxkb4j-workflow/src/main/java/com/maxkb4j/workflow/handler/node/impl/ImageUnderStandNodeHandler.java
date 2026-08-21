package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.common.util.MessageConverter;
import com.maxkb4j.core.assistant.Assistant;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractChatStreamNodeHandler;
import com.maxkb4j.workflow.model.ModelConfig;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.ImageUnderstandNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@Slf4j
@NodeHandlerType(NodeType.IMAGE_UNDERSTAND)
@Component
public class ImageUnderStandNodeHandler extends AbstractChatStreamNodeHandler {

    public ImageUnderStandNodeHandler(IModelProviderService modelFactory, IOssService ossService) {
        super(modelFactory, ossService);
    }

    @Override
    protected CompletableFuture<NodeResult> doExecuteAsync(IWorkflow workflow, AbsNode node) {
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
        Assistant assistant = buildStreamingAssistant(workflow, modelConfig, systemPrompt, historyMessages);

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
        putDetail(node, NodeField.IMAGE_LIST, imageFiles);
    }

    private void recordNodeDetails(AbsNode node, String systemPrompt, List<ChatMessage> historyMessages,
                                   String userPrompt, List<Content> contents) {
        putDetails(node, Map.of(
                ChatField.SYSTEM, systemPrompt,
                ChatField.HISTORY_MESSAGE, MessageConverter.formatHistoryMessages(historyMessages),
                NodeField.QUESTION, userPrompt,
                "hasImages", !contents.isEmpty()
        ));
    }
}
