package com.maxkb4j.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.common.domain.dto.*;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.NodeField;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.ApplicationNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.maxkb4j.workflow.enums.NodeType.FORM;
import static com.maxkb4j.workflow.enums.NodeType.USER_SELECT;


@Slf4j
@Component
@NodeHandlerType(NodeType.APPLICATION)
@RequiredArgsConstructor
public class ApplicationNodeHandler implements INodeHandler {

    private final IApplicationChatService chatService;

    @SuppressWarnings("unchecked")
    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        ApplicationNode.NodeParams nodeParams = node.getNodeData().toJavaObject(ApplicationNode.NodeParams.class);
        List<String> questionFields = nodeParams.getQuestionReferenceAddress();
        String question = (String) workflow.getReferenceField(questionFields);
        ChatParams chatParams = workflow.getChatParams();
        String chatId = chatParams.getChatId()+"_" + nodeParams.getApplicationId();
        List<OssFile> docList = new ArrayList<>();
        List<String> docFields = nodeParams.getDocumentList();
        if (CollectionUtils.isNotEmpty(docFields)) {
            docList = (List<OssFile>) workflow.getReferenceField(docFields);
        }
        List<OssFile> imageList = new ArrayList<>();
        List<String> imageFields = nodeParams.getImageList();
        if (CollectionUtils.isNotEmpty(imageFields)) {
            imageList = (List<OssFile>) workflow.getReferenceField(imageFields);
        }
        List<OssFile> audioList = new ArrayList<>();
        List<String> audioFields = nodeParams.getAudioList();
        if (CollectionUtils.isNotEmpty(audioFields)) {
            audioList = (List<OssFile>) workflow.getReferenceField(audioFields);
        }
        List<OssFile> otherList = new ArrayList<>();
        List<String> otherFields = nodeParams.getOtherList();
        if (CollectionUtils.isNotEmpty(audioFields)) {
            otherList = (List<OssFile>) workflow.getReferenceField(otherFields);
        }
        Sinks.Many<ChatMessageVO> appNodeSink = Sinks.many().unicast().onBackpressureBuffer();
        String nodeChatRecordId=null;
        String nodeRuntimeNodeId=null;
        if (chatParams.getChildNode()!=null){
            nodeChatRecordId=chatParams.getChildNode().getChatRecordId();
            nodeRuntimeNodeId=chatParams.getChildNode().getRuntimeNodeId();
        }
        Map<String,Object> formData=new HashMap<>();
        List<NodeField> userInputFieldList= nodeParams.getUserInputFieldList();
        if (CollectionUtils.isNotEmpty(userInputFieldList)) {
            for (NodeField field : userInputFieldList) {
                Object value = workflow.getReferenceField(field.getValue());
                value=value==null?field.getDefaultValue():value;
                formData.put(field.getField(), value);
            }
        }
        List<NodeField> apiInputFieldList= nodeParams.getApiInputFieldList();
        if (CollectionUtils.isNotEmpty(apiInputFieldList)) {
            for (NodeField field : apiInputFieldList) {
                Object value = workflow.getReferenceField(field.getValue());
                value=value==null?field.getDefaultValue():value;
                formData.put(field.getField(), value);
            }
        }

        ChatParams nodeChatParams = ChatParams.builder()
                .message(question)
                .appId(nodeParams.getApplicationId())
                .chatId(chatId)
                .chatRecordId(nodeChatRecordId)
                .runtimeNodeId(nodeRuntimeNodeId)
                .reChat(chatParams.getReChat())
                .chatUserId(chatParams.getChatUserId())
                .chatUserType(chatParams.getChatUserType())
                .imageList(imageList)
                .audioList(audioList)
                .documentList(docList)
                .otherList(otherList)
                .formData(formData)
                .nodeData(chatParams.getNodeData())
                .debug(chatParams.getDebug())
                .build();
        CompletableFuture<ChatResponse> future = chatService.chatMessageAsync(nodeChatParams,appNodeSink);
        // 使用原子变量或收集器来安全地累积 token
        AtomicBoolean is_interrupt_exec=new AtomicBoolean( false);
        if (Boolean.TRUE.equals(nodeParams.getIsResult())) {
            // 订阅并累积 token，同时发送消息
            appNodeSink.asFlux().subscribe(e -> {
                if(FORM.getKey().equals(e.getNodeType())||USER_SELECT.getKey().equals(e.getNodeType())){
                    is_interrupt_exec.set(true);
                }
                ChildNode childNode=new ChildNode(e.getChatRecordId(),e.getRuntimeNodeId());
                ChatMessageVO vo = node.toChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        e.getContent(),
                        e.getReasoningContent(),
                        childNode,
                        false);
                workflow.getSink().tryEmitNext(vo);
            });
        }
        ChatResponse chatResponse=future.join();
        node.getDetail().put("messageTokens", chatResponse.getMessageTokens());
        node.getDetail().put("answerTokens", chatResponse.getAnswerTokens());
        node.getDetail().put("question", question);
        node.setAnswerText(chatResponse.getAnswer());
        node.getDetail().put("answer", node.getAnswerText());
        node.getDetail().put("is_interrupt_exec", is_interrupt_exec.get());
        return new NodeResult(Map.of(
                "result", node.getAnswerText()
        ), true,this::isInterrupt);
    }


    public boolean isInterrupt(AbsNode node) {
        return node.getDetail().containsKey("is_interrupt_exec")&&(boolean)node.getDetail().get("is_interrupt_exec");
    }

}
