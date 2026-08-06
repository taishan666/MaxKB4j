package com.maxkb4j.workflow.model;

import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatState;

/**
 * 聊天系工作流契约
 * 聊天工作流（含聊天循环工作流）在完整工作流能力（{@link IWorkflow}）之上，
 * 额外提供聊天参数与对话执行上下文访问。
 */
public interface IChatWorkflow extends IWorkflow {

    /**
     * 获取聊天参数
     */
    ChatParams getChatParams();

    /**
     * 获取对话执行上下文（服务端解析的身份信息与历史记录）
     */
    ChatState getChatState();
}
