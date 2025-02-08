package com.tarzan.maxkb4j.module.application.workflow.node.directreplynode.impl;

import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.node.NodeDetail;
import com.tarzan.maxkb4j.module.application.workflow.node.directreplynode.IReplyNode;
import com.tarzan.maxkb4j.module.application.workflow.node.directreplynode.dto.ReplyNodeParams;

import java.util.List;
import java.util.Map;

public class BaseReplyNode extends IReplyNode {
    @Override
    public NodeResult execute(ReplyNodeParams nodeParams) {
        String result;
        if ("referencing".equals(nodeParams.getReplyType())){
            result=getReferenceContent(nodeParams.getFields());
        }else {
            result=generateReplyContent(nodeParams.getContent());
        }
        return new NodeResult(Map.of("answer",result),Map.of());
    }

    private String generateReplyContent(String prompt){
        return this.workflowManage.generatePrompt(prompt);
    }

    private String getReferenceContent(List<String> fields){
        return (String)this.workflowManage.getReferenceField(fields.get(0),fields.subList(1, fields.size()));
    }

    @Override
    public void saveContext(NodeDetail nodeDetail, WorkflowManage workflowManage) {
        this.context.put("answer",nodeDetail.getAnswer());
        this.answerText=nodeDetail.getAnswer();
    }
}
