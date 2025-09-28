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
        List<String> imageList = nodeParams.getImageList();
        Object object = super.getReferenceField(imageList.get(0), imageList.get(1));
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
        Map<String, Object> nodeVariable = new HashMap<>(Map.of(
                "system", systemPrompt,
                "history_message", resetMessageList(historyMessages),
                "question", question
        ));
        return writeContextStream(nodeVariable, Map.of(),nodeParams,tokenStream);
    }

    private NodeResult writeContextStream(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, ImageUnderstandParams nodeParams, TokenStream tokenStream) {
        boolean isResult = nodeParams.getIsResult();
        CompletableFuture<ChatResponse> chatResponseFuture  = new CompletableFuture<>();
        tokenStream.onPartialResponse(content -> {
                    if (isResult) {
                        ChatMessageVO vo = new ChatMessageVO(
                                getChatParams().getChatId(),
                                getChatParams().getChatRecordId(),
                                id,
                                content,
                                "",
                                runtimeNodeId,
                                type,
                                viewType,
                                false);
                        super.getChatParams().getSink().tryEmitNext(vo);
                    }
                })
                .onCompleteResponse(response -> {
                    ChatMessageVO vo = new ChatMessageVO(
                            getChatParams().getChatId(),
                            getChatParams().getChatRecordId(),
                            type,
                            "",
                            "",
                            runtimeNodeId,
                            type,
                            viewType,
                            true);
                    super.getChatParams().getSink().tryEmitNext(vo);
                    chatResponseFuture.complete(response);// 完成后释放线程
                })
                .onError(error -> {
                    super.getChatParams().getSink().tryEmitError(error);
                    chatResponseFuture.completeExceptionally(error); // 完成后释放线程
                })
                .start();
        try {
            // 阻塞等待 answer
            ChatResponse response = chatResponseFuture.get(); // 可设置超时：get(30, TimeUnit.SECONDS)
            String answer = response.aiMessage().text();
            String thinking = response.aiMessage().thinking();
            TokenUsage tokenUsage = response.tokenUsage();
            nodeVariable.put("messageTokens", tokenUsage.inputTokenCount());
            nodeVariable.put("answerTokens", tokenUsage.outputTokenCount());
            nodeVariable.put("answer", answer);
            nodeVariable.put("reasoningContent", thinking);
            return new NodeResult(nodeVariable,globalVariable, this::writeContext);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error waiting for TokenStream completion", e);
            Thread.currentThread().interrupt(); // 恢复中断状态
            return new NodeResult(nodeVariable, globalVariable);
        }

    }


    private void writeContext(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, INode node, WorkflowManage workflow) {
        if (nodeVariable != null) {
            node.getContext().putAll(nodeVariable);
            String answer = (String) nodeVariable.get("answer");
            workflow.setAnswer(answer);
        }
    }


    @Override
    public void saveContext(JSONObject detail) {
        context.put("system", detail.get("system"));
        context.put("question", detail.get("question"));
        context.put("answer", detail.get("answer"));
        context.put("history_message",detail.get("history_message"));
        context.put("imageLst",detail.get("imageLst"));
        context.put("messageTokens", detail.get("messageTokens"));
        context.put("answerTokens", detail.get("answerTokens"));
    }


    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("system",context.get("system"));
        detail.put("question",context.get("question"));
        detail.put("answer",context.get("answer"));
        detail.put("history_message",context.get("history_message"));
        detail.put("imageLst",context.get("imageLst"));
        detail.put("messageTokens", context.get("messageTokens"));
        detail.put("answerTokens", context.get("answerTokens"));
        return detail;
    }

}
