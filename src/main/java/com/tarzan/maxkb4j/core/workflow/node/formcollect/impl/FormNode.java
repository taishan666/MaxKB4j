package com.tarzan.maxkb4j.core.workflow.node.formcollect.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.formcollect.input.FormNodeParams;
import dev.langchain4j.model.input.PromptTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.FORM;

public class FormNode extends INode {

    public FormNode(JSONObject properties) {
        super(properties);
        super.setType(FORM.getKey());
        super.setViewType("single_view");
    }


    @Override
    public void saveContext(JSONObject detail) {
        @SuppressWarnings("unchecked")
        Map<String, Object> formData = (Map<String, Object>) detail.get("form_data");
        if (formData != null){
            context.putAll(formData);
        }
        context.put("form_data", formData);
    }


}
