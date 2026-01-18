package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.ApplicationNode;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import com.tarzan.maxkb4j.module.chat.dto.ChildNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.FORM;
import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.USER_SELECT;

@Slf4j
@Component
@NodeHandlerType(NodeType.APPLICATION)
@RequiredArgsConstructor
public class ApplicationNodeHandler implements INodeHandler {

    private final ApplicationChatService chatService;

    @SuppressWarnings("unchecked")
    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        ApplicationNode.NodeParams nodeParams = node.getNodeData().toJavaObject(ApplicationNode.NodeParams.class);
        List<String> questionFields = nodeParams.getQuestionReferenceAddress();
        String question = (String) workflow.getReferenceField(questionFields.get(0), questionFields.get(1));
        ChatParams chatParams = workflow.getChatParams();
        String chatId = chatParams.getChatId() + nodeParams.getApplicationId();
        List<SysFile> docList = new ArrayList<>();
        List<String> docFields = nodeParams.getDocumentList();
        if (CollectionUtils.isNotEmpty(docFields)) {
            docList = (List<SysFile>) workflow.getReferenceField(docFields.get(0), docFields.get(1));
        }
        List<SysFile> imageList = new ArrayList<>();
        List<String> imageFields = nodeParams.getImageList();
        if (CollectionUtils.isNotEmpty(imageFields)) {
            imageList = (List<SysFile>) workflow.getReferenceField(imageFields.get(0), imageFields.get(1));
        }
        List<SysFile> audioList = new ArrayList<>();
        List<String> audioFields = nodeParams.getAudioList();
        if (CollectionUtils.isNotEmpty(audioFields)) {
            audioList = (List<SysFile>) workflow.getReferenceField(audioFields.get(0), audioFields.get(1));
        }
        List<SysFile> otherList = new ArrayList<>();
        List<String> otherFields = nodeParams.getOtherList();
        if (CollectionUtils.isNotEmpty(audioFields)) {
            otherList = (List<SysFile>) workflow.getReferenceField(otherFields.get(0), otherFields.get(1));
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
                .reChat(chatParams.getReChat())
                .chatUserId(chatParams.getChatUserId())
                .chatUserType(chatParams.getChatUserType())
                .imageList(imageList)
                .audioList(audioList)
                .documentList(docList)
                .otherList(otherList)
                .formData(chatParams.getFormData())
                .nodeData(chatParams.getNodeData())
                .debug(chatParams.getDebug())
                .build();
        CompletableFuture<ChatResponse> future = chatService.chatMessageAsync(nodeChatParams,appNodeSink);
        // 使用原子变量或收集器来安全地累积 token
        AtomicBoolean is_interrupt_exec=new AtomicBoolean( false);
        if (nodeParams.getIsResult()) {
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
