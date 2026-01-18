package com.tarzan.maxkb4j.core.workflow.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.module.application.domain.entity.KnowledgeSetting;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.SEARCH_KNOWLEDGE;

public class SearchKnowledgeNode extends AbsNode {



    public SearchKnowledgeNode(String id,JSONObject properties) {
        super(id,properties);
        this.setType(SEARCH_KNOWLEDGE.getKey());
    }




    @Override
    public void saveContext(Workflow workflow, Map<String, Object> detail) {
        context.put("paragraphList", detail.get("paragraphList"));
        context.put("isHitHandlingMethodList", detail.get("isHitHandlingMethodList"));
        context.put("data", detail.get("data"));
        context.put("directlyReturn", detail.get("directlyReturn"));
    }

    @Data
    public static class NodeParams {
        private List<String> knowledgeIdList;
        private KnowledgeSetting knowledgeSetting;
        private List<String> questionReferenceAddress;
        private Boolean showKnowledge;

    }



}
