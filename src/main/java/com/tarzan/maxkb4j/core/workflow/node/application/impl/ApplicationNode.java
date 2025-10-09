package com.tarzan.maxkb4j.core.workflow.node.application.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.application.input.ApplicationNodeParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.APPLICATION;

public class ApplicationNode extends INode {
    private final ApplicationChatService chatService;

    public ApplicationNode(JSONObject properties) {
        super(properties);
        this.type = APPLICATION.getKey();
        this.chatService = SpringUtil.getBean(ApplicationChatService.class);
    }


    @SuppressWarnings("unchecked")
    @Override
    public NodeResult execute() {
        ApplicationNodeParams nodeParams = super.getNodeData().toJavaObject(ApplicationNodeParams.class);
        List<String> questionFields = nodeParams.getQuestionReferenceAddress();
        String question = (String) super.getReferenceField(questionFields.get(0), questionFields.get(1));
        ChatParams chatParams = super.getChatParams();
        String chatId = chatParams.getChatId() + nodeParams.getApplicationId();
        chatService.chatOpen(nodeParams.getApplicationId(), chatId, chatParams.getDebug());
        List<ChatFile> docList = new ArrayList<>();
        List<String> docFields = nodeParams.getDocumentList();
        if (CollectionUtils.isNotEmpty(docFields)) {
            docList = (List<ChatFile>) super.getReferenceField(docFields.get(0), docFields.get(1));
        }
        List<ChatFile> imageList = new ArrayList<>();
        List<String> imageFields = nodeParams.getImageList();
        if (CollectionUtils.isNotEmpty(imageFields)) {
            imageList = (List<ChatFile>) super.getReferenceField(imageFields.get(0), imageFields.get(1));
        }
        List<ChatFile> audioList = new ArrayList<>();
        List<String> audioFields = nodeParams.getAudioList();
        if (CollectionUtils.isNotEmpty(audioFields)) {
            audioList = (List<ChatFile>) super.getReferenceField(audioFields.get(0), audioFields.get(1));
        }
        List<ChatFile> otherList = new ArrayList<>();
        List<String> otherFields = nodeParams.getOtherList();
        if (CollectionUtils.isNotEmpty(audioFields)) {
            otherList = (List<ChatFile>) super.getReferenceField(otherFields.get(0), otherFields.get(1));
        }
        Sinks.Many<ChatMessageVO> appNodeSink = Sinks.many().unicast().onBackpressureBuffer();
        ChatParams nodeChatParams = ChatParams.builder()
                .message(question)
                .appId(nodeParams.getApplicationId())
                .chatId(chatId)
                .runtimeNodeId(super.runtimeNodeId)
                .stream(chatParams.getStream())
                .reChat(chatParams.getReChat())
                .chatUserId(chatParams.getChatUserId())
                .chatUserType(chatParams.getChatUserType())
                .sink(appNodeSink)
                .imageList(imageList)
                .audioList(audioList)
                .documentList(docList)
                .otherList(otherList)
                .formData(chatParams.getFormData())
                .nodeData(chatParams.getNodeData())
                .debug(chatParams.getDebug())
                .build();
        CompletableFuture<String> future = chatService.chatMessageAsync(nodeChatParams);
        // 使用原子变量或收集器来安全地累积 token
        AtomicInteger messageTokens = new AtomicInteger(0);
        AtomicInteger answerTokens = new AtomicInteger(0);
        if (nodeParams.getIsResult()) {
            // 订阅并累积 token，同时发送消息
            appNodeSink.asFlux()
                    .doOnNext(e -> {
                        ChatMessageVO vo = new ChatMessageVO(
                                getChatParams().getChatId(),
                                getChatParams().getChatRecordId(),
                                id,
                                e.getContent(),
                                "",
                                upNodeIdList,
                                runtimeNodeId,
                                type,
                                viewType,
                                false
                        );
                        super.getChatParams().getSink().tryEmitNext(vo);
                    })
                    .subscribe(e -> {
                        messageTokens.addAndGet(e.getMessageTokens());
                        answerTokens.addAndGet(e.getAnswerTokens());
                    });
        }
        answerText=future.join();
        detail.put("messageTokens", messageTokens.get());
        detail.put("answerTokens", answerTokens.get());
        detail.put("question", question);
        detail.put("answer", answerText);
        return new NodeResult(Map.of(
                "result", answerText
        ), Map.of());
    }


    @Override
    public void saveContext(JSONObject detail) {
        context.put("result", detail.get("result"));
    }


    @Override
    public JSONObject getRunDetail() {
/*        JSONObject detail = new JSONObject();
       // detail.put("info", properties.getString("nodeData"));
        detail.put("question", context.get("question"));
        detail.put("answer", context.get("answer"));
        *//*detail.put("messageTokens", context.get("messageTokens"));
        detail.put("answerTokens", context.get("answerTokens"));*//*
        detail.put("imageList", context.get("image"));
        detail.put("documentList", context.get("document"));
        detail.put("audioList", context.get("audio"));
        detail.put("globalFields", properties.get("globalFields"));
      //  detail.put("application_node_dict", context.get("application_node_dict"));*/
        return detail;
    }


}