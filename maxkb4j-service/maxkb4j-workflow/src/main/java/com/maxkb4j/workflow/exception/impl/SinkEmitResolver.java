package com.maxkb4j.workflow.exception.impl;

import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.workflow.exception.NodeExceptionResolver;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Sink 发送异常解析器
 * 责任链第三环：向 Sink 发送错误消息（用于流式输出）
 */
@Slf4j
@Component
@Order(3)
public class SinkEmitResolver implements NodeExceptionResolver {

    @Override
    public boolean resolve(Workflow workflow, AbsNode node, Exception ex) {
        if (workflow.output().needsSink() && workflow.getChatParams() != null) {
            try {
                ChatMessageVO errVo = node.toChatMessageVO(
                        workflow.getChatParams().getChatId(),
                        workflow.getChatParams().getChatRecordId(),
                        String.format("Exception: %s", ex.getMessage()),
                        "",
                        null,
                        true);
                workflow.output().emit(errVo);
            } catch (Exception e) {
                log.warn("Failed to emit error message to sink: {}", e.getMessage());
            }
        }
        return false; // 终止责任链
    }

    @Override
    public int getOrder() {
        return 3;
    }
}