package com.tarzan.maxkb4j.core.workflow.node.imageunderstand.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.dto.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.imageunderstand.input.ImageUnderstandParams;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.resource.service.MongoFileService;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

import java.util.*;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.IMAGE_UNDERSTAND;

public class BaseImageUnderstandNode extends INode {

    private final ModelService modelService;
    private final MongoFileService fileService;

    public BaseImageUnderstandNode() {
        super();
        this.type = IMAGE_UNDERSTAND.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
        this.fileService = SpringUtil.getBean(MongoFileService.class);
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
        ImageUnderstandParams nodeParams=super.nodeParams.toJavaObject(ImageUnderstandParams.class);
        List<String> imageList = nodeParams.getImageList();
        Object object = super.getWorkflowManage().getReferenceField(imageList.get(0), imageList.subList(1, imageList.size()));
        List<ChatFile> ImageFiles = (List<ChatFile>) object;
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        String question =  workflowManage.generatePrompt(nodeParams.getPrompt());
        String system =workflowManage.generatePrompt(nodeParams.getSystem());
        List<ChatMessage> historyMessage = workflowManage.getHistoryMessage(super.flowParams.getHistoryChatRecord(), nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), super.runtimeNodeId);
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
        UserMessage userMessage = new UserMessage(contents);
        List<ChatMessage> messageList =  super.workflowManage.generateMessageList(system, userMessage, historyMessage);
        ChatResponse res=chatModel.generate(messageList);
        AiMessage aiMessage=res.aiMessage();
        TokenUsage tokenUsage=res.tokenUsage();
        Map<String, Object> nodeVariable = Map.of(
                "system", system,
                "chat_model", chatModel,
                "message_list", messageList,
                "history_message", historyMessage,
                "question", question,
                "answer", aiMessage.text(),
                "messageTokens", tokenUsage.inputTokenCount(),
                "answerTokens", tokenUsage.outputTokenCount()
        );
        return new NodeResult(nodeVariable, Map.of());
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
