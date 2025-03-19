package com.tarzan.maxkb4j.module.application.workflow.node.searchdataset;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.node.start.input.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.searchdataset.input.SearchDatasetStepNodeParams;

import java.util.Objects;

public abstract class ISearchDatasetStepNode extends INode {

    @Override
    public String getType() {
        return "search-dataset-node";
    }
    @Override
    public SearchDatasetStepNodeParams getNodeParamsClass(JSONObject nodeParams) {
        if(Objects.isNull(nodeParams)){
            return new SearchDatasetStepNodeParams();
        }
        return nodeParams.toJavaObject(SearchDatasetStepNodeParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams),super.workflowParams);
    }

    public abstract NodeResult execute(SearchDatasetStepNodeParams nodeParams, FlowParams workflowParams);
}
