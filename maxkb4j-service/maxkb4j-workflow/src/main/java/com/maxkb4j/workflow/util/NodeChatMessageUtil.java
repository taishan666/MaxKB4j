package com.maxkb4j.workflow.util;

import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.common.domain.vo.ChildNode;
import com.maxkb4j.common.util.MessageConverter;
import com.maxkb4j.workflow.node.AbsNode;
import org.apache.commons.lang3.StringUtils;

import static com.maxkb4j.workflow.enums.NodeType.FORM;
import static com.maxkb4j.workflow.enums.NodeType.USER_SELECT;

public class NodeChatMessageUtil {

    /**
     * 判定消息是否为交互中断信号：表单（FORM）或用户选择（USER_SELECT）节点
     * 且已携带有效内容时，需要中断执行等待用户输入。
     *
     * @param message 待判定的消息
     * @return 是否需要中断
     */
    public static boolean isInterruptMessage(ChatMessageVO message) {
        String nodeType = message.getNodeType();
        if (FORM.getKey().equals(nodeType) || USER_SELECT.getKey().equals(nodeType)) {
            return StringUtils.isNotBlank(message.getContent());
        }
        return false;
    }


    /**
     * 以当前节点身份统一构造聊天消息 VO（两个发送路径的公共构建逻辑）。
     */
    public static ChatMessageVO buildChatMessage(ChatParams chatParams, String content, String reasoningContent, AbsNode node, ChildNode childNode, boolean nodeIsEnd) {
        String realNodeId = node.getRuntimeNodeId();
        if (childNode != null) {
            realNodeId = childNode.getRuntimeNodeId();
        }
        return MessageConverter.toChatMessageVO(
                chatParams.getChatId(),
                chatParams.getChatRecordId(),
                node.getId(),
                node.getNodeName(),
                content,
                reasoningContent,
                node.getUpNodeIdList(),
                node.getRuntimeNodeId(),
                realNodeId,
                node.getType(),
                node.getViewType(),
                childNode,
                nodeIsEnd,
                false);
    }
}
