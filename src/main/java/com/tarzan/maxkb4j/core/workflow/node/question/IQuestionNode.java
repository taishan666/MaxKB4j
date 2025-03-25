package com.tarzan.maxkb4j.core.workflow.node.question;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.start.input.FlowParams;
import com.tarzan.maxkb4j.core.workflow.node.question.input.QuestionParams;

import java.util.Objects;

public abstract class IQuestionNode extends INode {

    @Override
    public String getType() {
        return "question-node";
    }
    @Override
    public QuestionParams getNodeParamsClass(JSONObject nodeParams) {
        if(Objects.isNull(nodeParams)){
            return new QuestionParams();
        }
        return nodeParams.toJavaObject(QuestionParams.class);
    }

    @Override
    public NodeResult _run() {
        return this.execute(getNodeParamsClass(super.nodeParams),super.workflowParams);
    }

    public abstract NodeResult execute(QuestionParams nodeParams, FlowParams workflowParams);


}
