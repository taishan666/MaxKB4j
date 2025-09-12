package com.tarzan.maxkb4j.core.workflow.node.classification.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.classification.input.ClassificationBranch;
import com.tarzan.maxkb4j.core.workflow.node.classification.input.ClassificationNodeParams;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.core.langchain4j.MyChatMemory;
import com.tarzan.maxkb4j.core.langchain4j.MyQueryClassifier;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.rag.query.Metadata;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.*;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.AI_CHAT;
import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.CLASSIFICATION;

public class BaseClassificationNode extends INode {

    private final ModelService modelService;
    private final ChatMemoryStore chatMemoryStore;

    public BaseClassificationNode(JSONObject properties) {
        super(properties);
        this.type = AI_CHAT.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
        this.chatMemoryStore = SpringUtil.getBean(ChatMemoryStore.class);
    }

    @Override
    public NodeResult execute() {
        System.out.println(CLASSIFICATION);
        ClassificationNodeParams nodeParams = super.nodeParams.toJavaObject(ClassificationNodeParams.class);
        if (Objects.isNull(nodeParams.getDialogueType())) {
            nodeParams.setDialogueType("WORK_FLOW");
        }
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        List<String> questionFields = nodeParams.getQuestionReferenceAddress();
        String question = (String) workflowManage.getReferenceField(questionFields.get(0), questionFields.subList(1, questionFields.size()));
        Map<String, String> questionMap = new HashMap<>();
        for (ClassificationBranch branch : nodeParams.getBranch()) {
            questionMap.put(branch.getId(), branch.getCondition());
        }
        MyQueryClassifier queryClassifier = new MyQueryClassifier(chatModel.getChatModel(), questionMap);
        String chatId = workflowManage.getContext().getString("chatId");
        ChatMemory chatMemory = MyChatMemory.builder()
                .id(chatId + "_" + runtimeNodeId)
                .maxMessages(nodeParams.getDialogueNumber())
                .chatMemoryStore(chatMemoryStore)
                .build();
        Metadata metadata = new Metadata(UserMessage.from(question), chatMemory.id(), chatMemory.messages());
        Query query = new Query(question, metadata);
        Collection<String> route = queryClassifier.route(query);
        String branchId = nodeParams.getBranch().get(0).getId();
        if (!route.isEmpty()) {
            branchId = route.stream().findFirst().get();
        }
        chatMemory.add(UserMessage.from(question));
        chatMemory.add(AiMessage.from(questionMap.get(branchId)));
        return new NodeResult(Map.of("branch_id", branchId, "branch_name", questionMap.get(branchId)), Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("answer", context.get("answer"));
        detail.put("branch_id", context.get("branch_id"));
        detail.put("branch_name", context.get("branch_name"));
        return detail;
    }
}
