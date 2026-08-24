package com.maxkb4j.workflow.exception.impl;

import com.maxkb4j.workflow.exception.NodeExceptionResolver;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import static com.maxkb4j.workflow.consts.WorkflowConstants.RuntimeDetailField;

/**
 * 详情记录异常解析器
 * 责任链第二环：将错误信息记录到节点详情
 */
@Component
@Order(2)
public class DetailRecordingResolver implements NodeExceptionResolver {

    @Override
    public boolean resolve(IWorkflow workflow, AbsNode node, Exception ex) {
        node.setErrMessage(ex.getMessage());
        node.getDetail().put(RuntimeDetailField.ERROR, ex.getMessage());
        node.getDetail().put(RuntimeDetailField.ERROR_TIME, System.currentTimeMillis());
        node.getDetail().put(RuntimeDetailField.ERROR_CLASS, ex.getClass().getSimpleName());
        return true; // 继续执行下一个解析器
    }

    @Override
    public int getOrder() {
        return 2;
    }
}