package com.maxkb4j.workflow.handler.node;

import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import lombok.extern.slf4j.Slf4j;

/**
 * 子类仍需自行实现 {@link #doExecuteAsync}（构建 Assistant 并启动流）、
 */
@Slf4j
public abstract class StreamNodeHandler extends AbsNodeHandler {

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        throw new UnsupportedOperationException("Streaming node uses async execution via doExecuteAsync");
    }

}
