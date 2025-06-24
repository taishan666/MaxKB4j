package com.tarzan.maxkb4j.core.workflow.node.application.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.node.application.input.ApplicationNodeParams;
import com.tarzan.maxkb4j.module.application.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.application.vo.ChatMessageVO;
import com.tarzan.maxkb4j.util.SpringUtil;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.APPLICATION;

public class BaseApplicationNode extends INode {
    private final ApplicationChatService chatService;

    public BaseApplicationNode() {
        this.type = APPLICATION.getKey();
        this.chatService = SpringUtil.getBean(ApplicationChatService.class);
    }


    @Override
    public NodeResult execute() {
        System.out.println(APPLICATION);
        ApplicationNodeParams nodeParams= super.nodeParams.toJavaObject(ApplicationNodeParams.class);
        WorkflowManage workflowManage=super.getWorkflowManage();
        String chatId=chatService.chatOpen(nodeParams.getApplicationId(),runtimeNodeId);
        List<String> questionFields=nodeParams.getQuestionReferenceAddress();
        String question= (String)workflowManage.getReferenceField(questionFields.get(0),questionFields.subList(1, questionFields.size()));
        ChatMessageDTO messageDto = ChatMessageDTO.builder()
                .message(question)
                .clientId(nodeParams.getApplicationId())
                .clientType(AuthType.APPLICATION.name())
                .reChat(false).build();
        Flux<ChatMessageVO> chatMessageVo = chatService.chatMessage(chatId,messageDto);
        return new NodeResult(Map.of(
                "result", chatMessageVo,
                "question", question
        ), Map.of(), this::writeContextStream);
    }

    private Stream<String> writeContextStream(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode currentNode, WorkflowManage workflow) {
        Flux<ChatMessageVO> chatMessageVo= (Flux<ChatMessageVO>) nodeVariable.get("result");
        StringBuilder answerSB=new StringBuilder();
        AtomicInteger messageTokens= new AtomicInteger();
        AtomicInteger answerTokens= new AtomicInteger();
        return chatMessageVo.map(vo->{
            answerSB.append(vo.getContent());
            messageTokens.addAndGet(vo.getMessageTokens());
            answerTokens.addAndGet(vo.getAnswerTokens());
            return vo.getContent();
        }).doOnComplete(()->{
            context.put("messageTokens", messageTokens.get());
            context.put("answerTokens", answerTokens.get());
            context.put("question", nodeVariable.get("question"));
            context.put("result", answerSB.toString());
            context.put("answer", answerSB.toString());
        }).toStream();
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("info", lfNode.getProperties().getString("nodeData"));
        detail.put("question", context.get("question"));
        detail.put("answer", context.get("answer"));
        detail.put("messageTokens", context.get("messageTokens"));
        detail.put("answerTokens", context.get("answerTokens"));
        detail.put("image_list", context.get("image"));
        detail.put("document_list", context.get("document"));
        detail.put("audio_list", context.get("audio"));
        detail.put("global_fields", lfNode.getProperties().get("globalFields"));
        detail.put("application_node_dict", context.get("application_node_dict"));
        return  detail;
    }


}