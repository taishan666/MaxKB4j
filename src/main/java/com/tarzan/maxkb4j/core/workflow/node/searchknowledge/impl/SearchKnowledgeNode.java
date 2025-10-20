package com.tarzan.maxkb4j.core.workflow.node.searchknowledge.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.SEARCH_KNOWLEDGE;

public class SearchKnowledgeNode extends INode {



    public SearchKnowledgeNode(JSONObject properties) {
        super(properties);
        this.setType(SEARCH_KNOWLEDGE.getKey());
    }




    @Override
    public void saveContext(Workflow workflow, JSONObject detail) {
        context.put("paragraphList", detail.get("paragraphList"));
        context.put("isHitHandlingMethodList", detail.get("isHitHandlingMethodList"));
        context.put("data", detail.get("data"));
        context.put("directlyReturn", detail.get("directlyReturn"));
    }


}
