package com.tarzan.maxkb4j.core.workflow.node.application.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.core.workflow.node.application.input.ApplicationNodeParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import com.tarzan.maxkb4j.module.chat.ChatResponse;
import com.tarzan.maxkb4j.module.chat.ChildNode;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.APPLICATION;
import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.FORM;

public class ApplicationNode extends INode {
    private final ApplicationChatService chatService;

    public ApplicationNode(JSONObject properties) {
        super(properties);
        this.setType(APPLICATION.getKey());
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
        String nodeChatRecordId=null;
        String nodeRuntimeNodeId=null;
        if (chatParams.getChildNode()!=null){
            nodeChatRecordId=chatParams.getChildNode().getChatRecordId();
            nodeRuntimeNodeId=chatParams.getChildNode().getRuntimeNodeId();
        }
        ChatParams nodeChatParams = ChatParams.builder()
                .message(question)
                .appId(nodeParams.getApplicationId())
                .chatId(chatId)
                .chatRecordId(nodeChatRecordId)
                .runtimeNodeId(nodeRuntimeNodeId)
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
        CompletableFuture<ChatResponse> future = chatService.chatMessageAsync(nodeChatParams);
        // 使用原子变量或收集器来安全地累积 token
        AtomicBoolean is_interrupt_exec=new AtomicBoolean( false);
        if (nodeParams.getIsResult()) {
            // 订阅并累积 token，同时发送消息
            appNodeSink.asFlux().subscribe(e -> {
                        if(FORM.getKey().equals(e.getNodeType())){
                            is_interrupt_exec.set( true);
                        }
                        ChildNode childNode=new ChildNode(e.getChatRecordId(),e.getRuntimeNodeId());
                        ChatMessageVO vo = new ChatMessageVO(
                                chatParams.getChatId(),
                                chatParams.getChatRecordId(),
                                e.getNodeId(),
                                e.getContent(),
                                e.getReasoningContent(),
                                e.getUpNodeIdList(),
                                super.getRuntimeNodeId(),
                                super.getType(),
                                e.getViewType(),
                                childNode,
                                e.getNodeIsEnd()
                        );
                        super.getChatParams().getSink().tryEmitNext(vo);
                    });
        }
        ChatResponse chatResponse=future.join();
        detail.put("messageTokens", chatResponse.getMessageTokens());
        detail.put("answerTokens", chatResponse.getAnswerTokens());
        detail.put("question", question);
        super.setAnswerText(chatResponse.getAnswer());
        detail.put("answer", super.getAnswerText());
        detail.put("is_interrupt_exec", is_interrupt_exec.get());
        return new NodeResult(Map.of(
                "result", super.getAnswerText()
        ), Map.of(),this::writeContext,this::isInterrupt);
    }

    private void writeContext(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, INode node, WorkflowManage workflow) {
        node.getContext().putAll(nodeVariable);
        node.getDetail().putAll(nodeVariable);
        if (workflow.isResult(node, new NodeResult(nodeVariable, globalVariable))&& StringUtil.isNotBlank(node.getAnswerText())) {
            workflow.setAnswer(workflow.getAnswer()+node.getAnswerText());
        }
    }

    public boolean isInterrupt(INode node) {
        return node.getDetail().containsKey("is_interrupt_exec")&&(boolean)node.getDetail().get("is_interrupt_exec");
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