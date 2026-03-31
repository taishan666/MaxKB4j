package com.maxkb4j.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.application.service.IApplicationChatService;
import com.maxkb4j.common.domain.dto.*;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
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
public class ApplicationNodeHandler extends AbstractNodeHandler<ApplicationNode.NodeParams> {

    private final IApplicationChatService chatService;

    @Override
    protected Class<ApplicationNode.NodeParams> getParamsClass() {
        return ApplicationNode.NodeParams.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node, ApplicationNode.NodeParams params) throws Exception {
        List<String> questionFields = params.getQuestionReferenceAddress();
        String question = getReferenceFieldAsString(workflow, questionFields);
        ChatParams chatParams = workflow.getConfiguration().getChatParams();
        String chatId = chatParams.getChatId() + "_" + params.getApplicationId();

        // 获取各种文件列表
        List<OssFile> docList = getFileList(workflow, params.getDocumentList());
        List<OssFile> imageList = getFileList(workflow, params.getImageList());
        List<OssFile> audioList = getFileList(workflow, params.getAudioList());
        List<OssFile> otherList = getFileList(workflow, params.getOtherList());

        Sinks.Many<ChatMessageVO> appNodeSink = Sinks.many().unicast().onBackpressureBuffer();

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
                .appId(params.getApplicationId())
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

        CompletableFuture<ChatResponse> future = chatService.chatMessageAsync(nodeChatParams, appNodeSink);

        AtomicBoolean isInterruptExec = new AtomicBoolean(false);

        if (Boolean.TRUE.equals(params.getIsResult())) {
            // 订阅并累积 token，同时发送消息
            appNodeSink.asFlux().subscribe(e -> {
                if (FORM.getKey().equals(e.getNodeType()) || USER_SELECT.getKey().equals(e.getNodeType())) {
                    isInterruptExec.set(true);
                }
                ChildNode childNode = new ChildNode(e.getChatRecordId(), e.getRuntimeNodeId());
                ChatMessageVO vo = node.toChatMessageVO(
                        workflow.getConfiguration().getChatParams().getChatId(),
                        workflow.getConfiguration().getChatParams().getChatRecordId(),
                        e.getContent(),
                        e.getReasoningContent(),
                        childNode,
                        false);
                workflow.output().emit(vo);
            });
        }

        ChatResponse chatResponse = future.join();

        // 写入详情
        putDetails(node, Map.of(
                "messageTokens", chatResponse.getMessageTokens(),
                "answerTokens", chatResponse.getAnswerTokens(),
                "question", question,
                "answer", chatResponse.getAnswer(),
                "is_interrupt_exec", isInterruptExec.get()
        ));

        if (Boolean.TRUE.equals(params.getIsResult())) {
            setAnswer(node, chatResponse.getAnswer());
        }

        return new NodeResult(Map.of("result", node.getAnswerText()), true, this::shouldInterrupt);
    }

    @Override
    public boolean shouldInterrupt(AbsNode node) {
        return getInterruptFlag(node);
    }

    /**
     * 获取文件列表
     */
    @SuppressWarnings("unchecked")
    private List<OssFile> getFileList(Workflow workflow, List<String> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return new ArrayList<>();
        }
        Object result = workflow.getReferenceField(fields);
        if (result instanceof List<?>) {
            return (List<OssFile>) result;
        }
        return new ArrayList<>();
    }

    /**
     * 构建 formData
     */
    private Map<String, Object> buildFormData(Workflow workflow, List<NodeField> fieldList) {
        Map<String, Object> formData = new HashMap<>();
        if (CollectionUtils.isNotEmpty(fieldList)) {
            for (NodeField field : fieldList) {
                Object value = workflow.getReferenceField(field.getValue());
                value = value == null ? field.getDefaultValue() : value;
                formData.put(field.getField(), value);
            }
        }
        return formData;
    }
}