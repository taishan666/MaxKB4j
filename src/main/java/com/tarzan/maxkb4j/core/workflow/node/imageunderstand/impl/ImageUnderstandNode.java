package com.tarzan.maxkb4j.core.workflow.node.imageunderstand.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.imageunderstand.input.ImageUnderstandParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.IMAGE_UNDERSTAND;

@Slf4j
public class ImageUnderstandNode extends INode {

    private final ModelService modelService;
    private final MongoFileService fileService;
    private final AiServices<Assistant> aiServicesBuilder;

    public ImageUnderstandNode(JSONObject properties) {
        super(properties);
        this.type = IMAGE_UNDERSTAND.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
        this.fileService = SpringUtil.getBean(MongoFileService.class);
        this.aiServicesBuilder = AiServices.builder(Assistant.class);
    }

    private static final Map<String, String> mimeTypeMap = new HashMap<>();
    static {
        mimeTypeMap.put("jpg", "image/jpeg");
        mimeTypeMap.put("jpeg", "image/jpeg");
        mimeTypeMap.put("png", "image/png");
        mimeTypeMap.put("gif", "image/gif");
    }

    @Override
    public NodeResult execute() {
        ImageUnderstandParams nodeParams=super.getNodeData().toJavaObject(ImageUnderstandParams.class);
        List<String> imageFieldList = nodeParams.getImageList();
        Object object = super.getReferenceField(imageFieldList.get(0), imageFieldList.get(1));
        @SuppressWarnings("unchecked")
        List<ChatFile> ImageFiles = (List<ChatFile>) object;
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        String question =  super.generatePrompt(nodeParams.getPrompt());
        String systemPrompt =super.generatePrompt(nodeParams.getSystem());
        List<ChatMessage> historyMessages=super.getHistoryMessages(nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), runtimeNodeId);
        List<Content> contents=new ArrayList<>();
        contents.add(TextContent.from(question));
        for (ChatFile file : ImageFiles) {
            byte[] bytes = fileService.getBytes(file.getFileId());
            String base64Data = Base64.getEncoder().encodeToString(bytes);
            String fileName=file.getName();
            String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            ImageContent imageContent=ImageContent.from(base64Data,mimeTypeMap.getOrDefault(extension, "image/jpeg"));
            contents.add(imageContent);
        }
        if (StringUtil.isNotBlank(systemPrompt)){
            aiServicesBuilder.systemMessageProvider(chatMemoryId -> systemPrompt);
        }
        if (CollectionUtils.isNotEmpty(historyMessages)){
            aiServicesBuilder.chatMemory(AppChatMemory.withMessages(historyMessages));
        }
        Assistant assistant = aiServicesBuilder.streamingChatModel(chatModel.getStreamingChatModel()).build();
        TokenStream tokenStream = assistant.chatStream(question,contents);
        detail.put("system", systemPrompt);
        detail.put("history_message", resetMessageList(historyMessages));
        detail.put("question", question);
        detail.put("imageList", ImageFiles);
        return writeContextStream(nodeParams,tokenStream);
    }

    private NodeResult writeContextStream(ImageUnderstandParams nodeParams, TokenStream tokenStream) {
        boolean isResult = nodeParams.getIsResult();
        CompletableFuture<ChatResponse> chatResponseFuture  = new CompletableFuture<>();
        // 完成后释放线程
        tokenStream.onPartialResponse(content -> {
                    if (isResult) {
                        ChatMessageVO vo = new ChatMessageVO(
                                getChatParams().getChatId(),
                                getChatParams().getChatRecordId(),
                                id,
                                content,
                                "",
                                upNodeIdList,
                                runtimeNodeId,
                                type,
                                viewType,
                                false);
                        super.getChatParams().getSink().tryEmitNext(vo);
                    }
                })
                .onCompleteResponse(chatResponseFuture::complete)
                .onError(error -> {
                    super.getChatParams().getSink().tryEmitError(error);
                    chatResponseFuture.completeExceptionally(error); // 完成后释放线程
                })
                .start();
        try {
            // 阻塞等待 answer
            ChatResponse response = chatResponseFuture.get(); // 可设置超时：get(30, TimeUnit.SECONDS)
            answerText = response.aiMessage().text();
            TokenUsage tokenUsage = response.tokenUsage();
            detail.put("messageTokens", tokenUsage.inputTokenCount());
            detail.put("answerTokens", tokenUsage.outputTokenCount());
            return new NodeResult(Map.of("answer", answerText),Map.of(), this::writeContext);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error waiting for TokenStream completion", e);
            Thread.currentThread().interrupt(); // 恢复中断状态
            return new NodeResult(null, null);
        }

    }


    private void writeContext(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, INode node, WorkflowManage workflow) {
        if (nodeVariable != null) {
            node.getContext().putAll(nodeVariable);
            node.getDetail().putAll(nodeVariable);
            if (workflow.isResult(node, new NodeResult(nodeVariable, globalVariable))&& StringUtil.isNotBlank(node.getAnswerText())) {
                workflow.setAnswer(workflow.getAnswer()+node.getAnswerText());
                ChatMessageVO vo = new ChatMessageVO(
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
                node.getChatParams().getSink().tryEmitNext(vo);
            }
        }
    }


    @Override
    public void saveContext(JSONObject detail) {
        context.put("answer", detail.get("answer"));
    }


    @Override
    public JSONObject getRunDetail() {
        return detail;
    }

}
