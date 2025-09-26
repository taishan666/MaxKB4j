package com.tarzan.maxkb4j.core.workflow.node.imageunderstand.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.domain.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.imageunderstand.input.ImageUnderstandParams;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import com.tarzan.maxkb4j.util.SpringUtil;
import com.tarzan.maxkb4j.util.StringUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.IMAGE_UNDERSTAND;

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
        Object object = workflowManage.getReferenceField(imageList.get(0), imageList.get(1));
        @SuppressWarnings("unchecked")
        List<ChatFile> ImageFiles = (List<ChatFile>) object;
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        String question =  workflowManage.generatePrompt(nodeParams.getPrompt());
        String systemPrompt =workflowManage.generatePrompt(nodeParams.getSystem());
        List<ChatMessage> historyMessages=workflowManage.getHistoryMessages(nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), runtimeNodeId);
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
        Map<String, Object> nodeVariable = new HashMap<>(Map.of(
                "system", systemPrompt,
                "history_message", resetMessageList(historyMessages),
                "question", question,
                "answer", ""
        ));
        TokenStream tokenStream = assistant.chatStream(question,contents);
        CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();
        boolean isResult = nodeParams.getIsResult();
        tokenStream.onPartialResponse(content -> {
                    if (isResult) {
                        ChatMessageVO vo = new ChatMessageVO(
                                workflowManage.getChatParams().getChatId(),
                                workflowManage.getChatParams().getChatRecordId(),
                                id,
                                content,
                                "",
                                runtimeNodeId,
                                type,
                                viewType,
                                false);
                        workflowManage.getSink().tryEmitNext(vo);
                    }
                })
                .onCompleteResponse(response -> {
                    String answer = response.aiMessage().text();
                    TokenUsage tokenUsage = response.tokenUsage();
                    nodeVariable.put("messageTokens", tokenUsage.inputTokenCount());
                    nodeVariable.put("answerTokens", tokenUsage.outputTokenCount());
                    nodeVariable.put("answer", answer);
                    ChatMessageVO vo = new ChatMessageVO(
                            workflowManage.getChatParams().getChatId(),
                            workflowManage.getChatParams().getChatRecordId(),
                            id,
                            "",
                            "",
                            runtimeNodeId,
                            type,
                            viewType,
                            false);
                    workflowManage.getSink().tryEmitNext(vo);
                    futureChatResponse.complete(response);// 完成后释放线程
                })
                .onError(error -> {
                    workflowManage.getSink().tryEmitError(error);
                    futureChatResponse.completeExceptionally(error); // 完成后释放线程
                })
                .start();
        futureChatResponse.join(); // 阻塞当前线程直到 futureChatResponse 完成
        return new NodeResult(nodeVariable, Map.of(),this::writeContext);
    }

    private void writeContext(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, INode node, WorkflowManage workflow) {
        if (nodeVariable != null) {
            node.getContext().putAll(nodeVariable);
            String answer = (String) nodeVariable.get("answer");
            workflow.setAnswer(answer);
        }
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("answer",context.get("answer"));
        detail.put("messageTokens", context.get("messageTokens"));
        detail.put("answerTokens", context.get("answerTokens"));
        return detail;
    }

}
