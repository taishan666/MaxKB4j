package com.tarzan.maxkb4j.core.workflow.node.documentextract;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.documentextract.input.DocumentExtractParams;

public abstract class IDocumentExtractNode extends INode {
    @Override
    public String getType() {
        return "document-extract-node";
    }

    @Override
    public DocumentExtractParams getNodeParamsClass(JSONObject nodeParams) {
        return nodeParams.toJavaObject(DocumentExtractParams.class);
    }


    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams));
    }

    public abstract NodeResult execute(DocumentExtractParams nodeParams);

}
