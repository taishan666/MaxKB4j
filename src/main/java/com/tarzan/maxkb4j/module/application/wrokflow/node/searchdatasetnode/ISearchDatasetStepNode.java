package com.tarzan.maxkb4j.module.application.wrokflow.node.searchdatasetnode;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.wrokflow.INode;
import com.tarzan.maxkb4j.module.application.wrokflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.wrokflow.NodeResult;
import com.tarzan.maxkb4j.module.application.wrokflow.node.searchdatasetnode.dto.SearchDatasetStepNodeParams;

public abstract class ISearchDatasetStepNode extends INode {

    String type="search-dataset-node";

    @Override
    public SearchDatasetStepNodeParams getNodeParamsClass(JSONObject nodeParams) {
        return nodeParams.toJavaObject(SearchDatasetStepNodeParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams),super.workflowParams);
    }

    public abstract NodeResult execute(SearchDatasetStepNodeParams nodeParams, FlowParams workflowParams);
}
