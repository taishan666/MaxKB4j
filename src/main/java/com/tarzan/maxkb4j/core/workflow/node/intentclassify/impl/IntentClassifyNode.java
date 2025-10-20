package com.tarzan.maxkb4j.core.workflow.node.intentclassify.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.node.intentclassify.input.IntentClassifyBranch;
import dev.langchain4j.internal.ValidationUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.INTENT_CLASSIFY;

@Slf4j
public class IntentClassifyNode extends INode {

    private final Map<Integer, String> idToClassification;

    public IntentClassifyNode(JSONObject properties) {
        super(properties);
        super.setType(INTENT_CLASSIFY.getKey());
        this.idToClassification = new HashMap<>();
    }



    protected Collection<Integer> parse(String choices) {
        return  Arrays.stream(choices.split(",")).map(String::trim).map(Integer::parseInt).toList();
    }

    protected String optionsFormat(List<IntentClassifyBranch> branches) {
        StringBuilder optionsBuilder = new StringBuilder();
        if (CollectionUtils.isNotEmpty( branches)){
            Collections.reverse(branches);
            for (int i = 0; i < branches.size(); i++) {
                IntentClassifyBranch branch=branches.get(i);
                idToClassification.put(i, ValidationUtils.ensureNotNull(branch.getId(), "Classification"));
                if (i > 0) {
                    optionsBuilder.append("\n");
                }
                optionsBuilder.append(i);
                optionsBuilder.append(": ");
                optionsBuilder.append(ValidationUtils.ensureNotBlank(branch.getContent(), "Classification description"));
            }
        }
        return optionsBuilder.toString();
    }



    @Override
    public void saveContext(JSONObject detail) {
        context.put("answer", detail.get("answer"));
        context.put("reasoningContent", detail.get("reasoningContent"));
    }

}
