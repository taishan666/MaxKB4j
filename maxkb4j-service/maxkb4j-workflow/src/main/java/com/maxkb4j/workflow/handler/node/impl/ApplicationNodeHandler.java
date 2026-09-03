package com.maxkb4j.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.maxkb4j.application.dto.ChatResponse;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.common.domain.dto.*;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.IChatWorkflow;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.InputField;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.ApplicationNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.maxkb4j.workflow.consts.WorkflowConstants.NodeField;
import static com.maxkb4j.workflow.enums.NodeType.FORM;
import static com.maxkb4j.workflow.enums.NodeType.USER_SELECT;


@Slf4j
@Component
@NodeHandlerType(NodeType.APPLICATION)
@RequiredArgsConstructor
public class ApplicationNodeHandler extends AbsNodeHandler {

    private final IApplicationChatService chatService;

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        if (workflow instanceof IChatWorkflow chatWorkflow) {
            ApplicationNode.NodeParams params = parseParams(node, ApplicationNode.NodeParams.class);
            List<String> questionFields = params.getQuestionReferenceAddress();
            String question = getReferenceFieldAsString(workflow, questionFields);
            ChatParams chatParams = chatWorkflow.getChatParams();
            ChatState chatState= chatWorkflow.getChatState();
            String chatId = chatParams.getChatId() + "_" + params.getApplicationId();
            // 获取各种文件列表
            List<OssFile> docList = getOssFiles(workflow, params.getDocumentList());
            List<OssFile> imageList = getOssFiles(workflow, params.getImageList());
            List<OssFile> audioList = getOssFiles(workflow, params.getAudioList());
            List<OssFile> otherList = getOssFiles(workflow, params.getOtherList());
            String nodeChatRecordId = null;
            String nodeRuntimeNodeId = null;
            if (chatParams.getChildNode() != null) {
                nodeChatRecordId = chatParams.getChildNode().getChatRecordId();
                nodeRuntimeNodeId = chatParams.getChildNode().getRuntimeNodeId();
            }
            // 构建 formData
            Map<String, Object> formData = buildFormData(workflow, params.getUserInputFieldList());
            formData.putAll(buildFormData(workflow, params.getApiInputFieldList()));
            ChatParams nodeChatParams = ChatParams.builder()
                    .message(question)
                    .chatId(chatId)
                    .chatRecordId(nodeChatRecordId)
                    .runtimeNodeId(nodeRuntimeNodeId)
                    .reChat(chatParams.getReChat())
                    .imageList(imageList)
                    .audioList(audioList)
                    .documentList(docList)
                    .otherList(otherList)
                    .formData(formData)
                    .nodeData(chatParams.getNodeData())
                    .build();
            ChatState nodeContext = ChatState.builder()
                    .appId(params.getApplicationId())
                    .chatUserId(chatState.getChatUserId())
                    .chatUserType(chatState.getChatUserType())
                    .debug(chatState.getDebug())
                    .build();
            Sinks.Many<ChatMessageVO> appNodeSink = Sinks.many().unicast().onBackpressureBuffer();
            AtomicBoolean isInterruptExec = new AtomicBoolean(false);
            if (Boolean.TRUE.equals(params.getIsResult())) {
                // 订阅并累积 token，同时发送消息
                appNodeSink.asFlux().subscribe(e -> {
                    if (FORM.getKey().equals(e.getNodeType()) || USER_SELECT.getKey().equals(e.getNodeType())) {
                        isInterruptExec.set(StringUtils.isNotEmpty(e.getContent()));
                    }
                    ChildNode childNode = new ChildNode(e.getChatRecordId(), e.getRuntimeNodeId());
                    ChatMessageVO vo = node.toChatMessageVO(
                            chatParams.getChatId(),
                            chatParams.getChatRecordId(),
                            e.getNodeName(),
                            e.getContent(),
                            e.getReasoningContent(),
                            childNode,
                            e.getNodeIsEnd());
                    workflow.output().emit(vo);
                });
            }
            ChatResponse chatResponse = chatService.chatMessage(nodeChatParams, nodeContext, appNodeSink);
            // 写入详情
            putDetails(node, Map.of(
                    NodeField.MESSAGE_TOKENS, chatResponse.getMessageTokens(),
                    NodeField.ANSWER_TOKENS, chatResponse.getAnswerTokens(),
                    NodeField.QUESTION, question,
                    NodeField.ANSWER, chatResponse.getAnswer(),
                    NodeField.IS_INTERRUPT_EXEC, isInterruptExec.get()
            ));
            return new NodeResult(Map.of(NodeField.RESULT, chatResponse.getAnswer()), true, this::shouldInterrupt);
        }
        return new NodeResult(Map.of(NodeField.RESULT, ""));
    }


    @Override
    public boolean shouldInterrupt(AbsNode node) {
        return getInterruptFlag(node);
    }

    /**
     * 构建 formData
     */
    private Map<String, Object> buildFormData(IWorkflow workflow, List<InputField> fieldList) {
        Map<String, Object> formData = new HashMap<>();
        if (CollectionUtils.isNotEmpty(fieldList)) {
            for (InputField field : fieldList) {
                Object value = workflow.getReferenceField(field.getValue());
                value = value == null ? field.getDefault_value() : value;
                formData.put(field.getField(), value);
            }
        }
        return formData;
    }
}
