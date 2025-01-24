package com.tarzan.maxkb4j.module.application.wrokflow;

import lombok.Data;

@Data
public class NodeChunkManage {
    private WorkflowManage workflowManage;

    public NodeChunkManage(WorkflowManage workflowManage) {
        this.workflowManage = workflowManage;
    }
}
