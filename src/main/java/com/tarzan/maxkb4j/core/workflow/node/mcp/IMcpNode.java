package com.tarzan.maxkb4j.core.workflow.node.mcp;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.dto.BaseParams;

public abstract class IMcpNode extends INode {
    @Override
    public String getType() {
        return "";
    }

    @Override
    public BaseParams getNodeParamsClass(JSONObject nodeParams) {
        return null;
    }

    @Override
    public NodeResult _run() {
        return null;
    }

    @Override
    public JSONObject getDetail() {
        return null;
    }

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {

    }
}
