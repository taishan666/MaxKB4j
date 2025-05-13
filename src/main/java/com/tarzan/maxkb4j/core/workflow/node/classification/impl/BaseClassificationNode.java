package com.tarzan.maxkb4j.core.workflow.node.classification.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.classification.input.ClassificationBranch;
import com.tarzan.maxkb4j.core.workflow.node.classification.input.ClassificationNodeParams;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.rag.MyQueryClassifier;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.rag.query.Query;

import java.util.*;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.AI_CHAT;
import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.CONDITION;

public class BaseClassificationNode extends INode {

    private final ModelService modelService;

    public BaseClassificationNode() {
        this.type = AI_CHAT.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
    }

    @Override
    public NodeResult execute() {
        System.out.println(CONDITION);
        ClassificationNodeParams nodeParams = super.nodeParams.toJavaObject(ClassificationNodeParams.class);
        if (Objects.isNull(nodeParams.getDialogueType())) {
            nodeParams.setDialogueType("WORKFLOW");
        }
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
      //  List<ChatMessage> historyMessage = workflowManage.getHistoryMessage(super.workflowParams.getHistoryChatRecord(), nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), super.runtimeNodeId);
        List<String> questionFields=nodeParams.getQuestionReferenceAddress();
        String question= (String)workflowManage.getReferenceField(questionFields.get(0),questionFields.subList(1, questionFields.size()));
        Map<String, String> questionMap = new HashMap<>();
        for (ClassificationBranch branch : nodeParams.getBranch()) {
            questionMap.put(branch.getId(), branch.getCondition());
        }
        MyQueryClassifier queryClassifier = new MyQueryClassifier(chatModel.getChatModel(),questionMap);
        Collection<String> route = queryClassifier.route(new Query(question));
        String branchId=route.stream().findFirst().get();
        return new NodeResult(Map.of("branch_id", branchId, "branch_name", questionMap.get(branchId)), Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("answer",context.get("answer"));
        detail.put("branch_id",context.get("branch_id"));
        detail.put("branch_name",context.get("branch_name"));
        return detail;
    }
}
