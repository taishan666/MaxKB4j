package com.tarzan.maxkb4j.core.workflow.node.directreply.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.directreply.input.ReplyNodeParams;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.REPLY;

public class BaseReplyNode extends INode {

    public BaseReplyNode(JSONObject properties) {
        super(properties);
        this.type = REPLY.getKey();
    }


    @Override
    public NodeResult execute() {
        System.out.println(REPLY);
        ReplyNodeParams nodeParams= super.getNodeData().toJavaObject(ReplyNodeParams.class);
        String result;
        if ("referencing".equals(nodeParams.getReplyType())){
            result=getReferenceContent(nodeParams.getFields());
        }else {
            result=this.workflowManage.generatePrompt(nodeParams.getContent());
        }
        return new NodeResult(Map.of("answer",result),Map.of());
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

}
