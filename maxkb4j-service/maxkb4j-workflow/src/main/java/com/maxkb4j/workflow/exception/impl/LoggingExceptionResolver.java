package com.maxkb4j.workflow.exception.impl;

import com.maxkb4j.workflow.exception.NodeExceptionResolver;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 日志记录异常解析器
 * 责任链第一环：记录错误日志
 */
@Slf4j
@Component
@Order(1)
public class LoggingExceptionResolver implements NodeExceptionResolver {

    @Override
    public boolean resolve(Workflow workflow, AbsNode node, Exception ex) {
        log.error("Node execution failed - Type: {}, Id: {}, Error: {}",
                node.getType(), node.getId(), ex.getMessage(), ex);
        return true; // 继续执行下一个解析器
    }

    @Override
    public int getOrder() {
        return 1;
    }
}