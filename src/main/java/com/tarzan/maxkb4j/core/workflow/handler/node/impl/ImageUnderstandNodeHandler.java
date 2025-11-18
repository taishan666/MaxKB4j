package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.tool.MimeTypeUtils;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ImageUnderstandNode;
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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@AllArgsConstructor
@Component
public class ImageUnderstandNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;
    private final MongoFileService fileService;


    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        ImageUnderstandNode.NodeParams nodeParams=node.getNodeData().toJavaObject(ImageUnderstandNode.NodeParams.class);
        List<String> imageFieldList = nodeParams.getImageList();
        Object object = workflow.getReferenceField(imageFieldList.get(0), imageFieldList.get(1));
        @SuppressWarnings("unchecked")
        List<ChatFile> ImageFiles = (List<ChatFile>) object;
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        String question =  workflow.generatePrompt(nodeParams.getPrompt());
        String systemPrompt =workflow.generatePrompt(nodeParams.getSystem());
        List<ChatMessage> historyMessages=workflow.getHistoryMessages(nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), node.getRuntimeNodeId());
        List<Content> contents=new ArrayList<>();
        for (ChatFile file : ImageFiles) {
            byte[] bytes = fileService.getBytes(file.getFileId());
            String base64Data = Base64.getEncoder().encodeToString(bytes);
            String fileName=file.getName();
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            ImageContent imageContent=ImageContent.from(base64Data, MimeTypeUtils.getMimeType( extension));
            contents.add(imageContent);
        }
        AiServices<Assistant> aiServicesBuilder=AiServices.builder(Assistant.class);
        if (StringUtil.isNotBlank(systemPrompt)){
            aiServicesBuilder.systemMessageProvider(chatMemoryId -> systemPrompt);
        }
        if (CollectionUtils.isNotEmpty(historyMessages)){
            aiServicesBuilder.chatMemory(AppChatMemory.withMessages(historyMessages));
        }
        Assistant assistant = aiServicesBuilder.streamingChatModel(chatModel).build();
        TokenStream tokenStream = assistant.chatStream(question,contents);
        node.getDetail().put("system", systemPrompt);
        node.getDetail().put("history_message", node.resetMessageList(historyMessages));
        node.getDetail().put("question", question);
        node.getDetail().put("imageList", ImageFiles);
        return writeContextStream(nodeParams,tokenStream,node,workflow);
    }

    private NodeResult writeContextStream(ImageUnderstandNode.NodeParams nodeParams, TokenStream tokenStream,INode  node,Workflow workflow) {
        boolean isResult = nodeParams.getIsResult();
        CompletableFuture<ChatResponse> chatResponseFuture  = new CompletableFuture<>();
        // 完成后释放线程
        tokenStream.onPartialResponse(content -> {
                    if (isResult) {
                        ChatMessageVO vo = node.toChatMessageVO(
                                workflow.getChatParams().getChatId(),
                                workflow.getChatParams().getChatRecordId(),
                                content,
                                "",
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
            // 阻塞等待 answer
            ChatResponse response = chatResponseFuture.get(); // 可设置超时：get(30, TimeUnit.SECONDS)
            node.setAnswerText(response.aiMessage().text());
            TokenUsage tokenUsage = response.tokenUsage();
            node.getDetail().put("messageTokens", tokenUsage.inputTokenCount());
            node.getDetail().put("answerTokens", tokenUsage.outputTokenCount());
            return new NodeResult(Map.of("answer", node.getAnswerText()),Map.of(), this::writeContext);
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
            if (workflow.isResult(node, new NodeResult(nodeVariable, globalVariable))&& StringUtil.isNotBlank(node.getAnswerText())) {
                workflow.setAnswer(workflow.getAnswer()+node.getAnswerText());
                ChatMessageVO endVo = node.toChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        "",
                        "",
                        true);
                workflow.getChatParams().getSink().tryEmitNext(endVo);
            }
        }
    }
}
