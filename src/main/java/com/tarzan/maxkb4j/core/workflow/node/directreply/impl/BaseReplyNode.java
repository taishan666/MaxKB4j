package com.tarzan.maxkb4j.core.workflow.node.directreply.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.node.directreply.IReplyNode;
import com.tarzan.maxkb4j.core.workflow.node.directreply.input.ReplyNodeParams;

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
        Object res=this.workflowManage.getReferenceField(fields.get(0),fields.subList(1, fields.size()));
        return res==null?"":res.toString();
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("answer",context.get("answer"));
        return detail;
    }

    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {
        this.context.put("answer",nodeDetail.get("answer"));
        this.answerText=nodeDetail.getString("answer");
    }
}
