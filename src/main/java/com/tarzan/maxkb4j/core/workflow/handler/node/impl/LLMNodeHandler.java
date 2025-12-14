package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.ToolUtil;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.tool.MessageTools;
import com.tarzan.maxkb4j.core.tool.MimeTypeUtils;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.AiChatNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Slf4j
@NodeHandlerType({NodeType.AI_CHAT, NodeType.IMAGE_UNDERSTAND})
@Component
@AllArgsConstructor
public class LLMNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;
    private final ToolUtil toolUtil;
    private final MongoFileService fileService;

    @SuppressWarnings("unchecked")
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        AiChatNode.NodeParams nodeParams = node.getNodeData().toJavaObject(AiChatNode.NodeParams.class);
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        String question = workflow.generatePrompt(nodeParams.getPrompt());
        String systemPrompt = workflow.generatePrompt(nodeParams.getSystem());
        List<String> toolIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nodeParams.getToolIds())) {
            toolIds.addAll(nodeParams.getToolIds());
        }
        List<ChatMessage> historyMessages = workflow.getHistoryMessages(nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), node.getRuntimeNodeId());
        AiServices<Assistant> aiServicesBuilder = AiServices.builder(Assistant.class);
        if (StringUtils.isNotBlank(systemPrompt)) {
            aiServicesBuilder.systemMessageProvider(chatMemoryId -> systemPrompt);
        }
        if (CollectionUtils.isNotEmpty(historyMessages)) {
            aiServicesBuilder.chatMemory(AppChatMemory.withMessages(historyMessages));
        }
        if (CollectionUtils.isNotEmpty(toolIds)) {
            aiServicesBuilder.tools(toolUtil.getToolMap(toolIds));
        }
        List<Content> contents = new ArrayList<>();
        List<String> imageFieldList = nodeParams.getImageList();
        if (CollectionUtils.isNotEmpty(imageFieldList)){
            Object object = workflow.getReferenceField(imageFieldList.get(0), imageFieldList.get(1));
            if (object != null){
                List<ChatFile> ImageFiles = (List<ChatFile>) object;
                for (ChatFile file : ImageFiles) {
                    byte[] bytes = fileService.getBytes(file.getFileId());
                    String base64Data = Base64.getEncoder().encodeToString(bytes);
                    String fileName = file.getName();
                    String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
                    ImageContent imageContent = ImageContent.from(base64Data, MimeTypeUtils.getMimeType(extension));
                    contents.add(imageContent);
                }
                node.getDetail().put("imageList", ImageFiles);
            }
        }
        Assistant assistant = aiServicesBuilder.streamingChatModel(chatModel).build();
        TokenStream tokenStream = assistant.chatStream(question,contents);
        node.getDetail().put("system", systemPrompt);
        node.getDetail().put("history_message", node.resetMessageList(historyMessages));
        node.getDetail().put("question", question);
        return writeContextStream(nodeParams, tokenStream, workflow, node);
    }

    private NodeResult writeContextStream(AiChatNode.NodeParams nodeParams, TokenStream tokenStream, Workflow workflow, INode node) {
        boolean isResult = Boolean.TRUE.equals(nodeParams.getIsResult());
        boolean toolOutputEnable =Boolean.TRUE.equals(nodeParams.getToolOutputEnable());
        boolean reasoningContentEnable = nodeParams.getModelSetting() != null && nodeParams.getModelSetting().getBooleanValue("reasoningContentEnable");
        CompletableFuture<ChatResponse> chatResponseFuture = new CompletableFuture<>();
        // 完成后释放线程
        tokenStream.onPartialThinking(thinking -> {
                    if (isResult && reasoningContentEnable) {
                        ChatMessageVO vo = node.toChatMessageVO(
                                workflow.getChatParams().getChatId(),
                                workflow.getChatParams().getChatRecordId(),
                                "",
                                thinking.text(),
                                false);
                        workflow.getSink().tryEmitNext(vo);
                    }
                })
                .onToolExecuted(toolExecute -> {
                    if (isResult && toolOutputEnable) {
                        ChatMessageVO vo = node.toChatMessageVO(
                                workflow.getChatParams().getChatId(),
                                workflow.getChatParams().getChatRecordId(),
                                MessageTools.getToolMessage(toolExecute),
                                "",
                                false);
                        workflow.getSink().tryEmitNext(vo);
                    }
                })
                .onPartialResponse(content -> {
                    if (isResult) {
                        ChatMessageVO vo = node.toChatMessageVO(
                                workflow.getChatParams().getChatId(),
                                workflow.getChatParams().getChatRecordId(),
                                content,
                                "",
                                false);
                        workflow.getSink().tryEmitNext(vo);
                    }
                })
                .onCompleteResponse(chatResponseFuture::complete)
                .onError(error -> {
                    log.error("执行错误", error);
                    chatResponseFuture.completeExceptionally(error); // 完成后释放线程
                })
                .start();
        ChatResponse response = chatResponseFuture.join();
        if (isResult){
            node.setAnswerText(response.aiMessage().text());
        }
        String thinking = response.aiMessage().thinking()==null? "" : response.aiMessage().thinking();
        TokenUsage tokenUsage = response.tokenUsage();
        node.getDetail().put("messageTokens", tokenUsage.inputTokenCount());
        node.getDetail().put("answerTokens", tokenUsage.outputTokenCount());
        return new NodeResult(Map.of("answer", response.aiMessage().text(), "reasoningContent", thinking),  true);

    }


}
