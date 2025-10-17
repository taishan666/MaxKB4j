package com.tarzan.maxkb4j.core.workflow.node.intentclassify.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.core.assistant.IntentClassifyAssistant;
import com.tarzan.maxkb4j.core.tool.MessageTools;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.enums.DialogueType;
import com.tarzan.maxkb4j.core.workflow.node.intentclassify.input.IntentClassifyBranch;
import com.tarzan.maxkb4j.core.workflow.node.intentclassify.input.IntentClassifyNodeParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.INTENT_CLASSIFY;

@Slf4j
public class IntentClassifyNode extends INode {

    private final ModelFactory modelFactory;
    private final Map<Integer, String> idToClassification;

    public IntentClassifyNode(JSONObject properties) {
        super(properties);
        super.setType(INTENT_CLASSIFY.getKey());
        this.modelFactory = SpringUtil.getBean(ModelFactory.class);
        this.idToClassification = new HashMap<>();
    }


    @Override
    public NodeResult execute() throws Exception {
        IntentClassifyNodeParams nodeParams = super.getNodeData().toJavaObject(IntentClassifyNodeParams.class);
        BaseChatModel chatModel = modelFactory.build(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        Object query = super.getReferenceField(nodeParams.getContentList().get(0),nodeParams.getContentList().get(1));
        Map<String,String> branchMap = new HashMap<>();
        List<IntentClassifyBranch> branches=nodeParams.getBranch();
        for (IntentClassifyBranch branch : branches) {
            branchMap.put(branch.getId(), branch.getContent());
        }
        List<ChatMessage> historyMessages = super.getHistoryMessages(nodeParams.getDialogueNumber(), DialogueType.WORKFLOW.name(), super.getRuntimeNodeId());
        detail.put("history_message", resetMessageList(historyMessages));
        String options =optionsFormat(nodeParams.getBranch());
        String chatMemory =MessageTools.format(historyMessages);
        IntentClassifyAssistant assistant = AiServices.builder(IntentClassifyAssistant.class)
                .chatModel(chatModel.getChatModel())
                .build();
        Result<String> result = assistant.route(options,chatMemory, query.toString());
        detail.put("system", IntentClassifyAssistant.SYSTEM_MESSAGE);
        detail.put("question", query);
        Collection<Integer> classificationIds = parse(result.content());
        int classificationId=classificationIds.stream().findFirst().orElse(0);
        String branchId=idToClassification.get(classificationId);
        String category=branchMap.get(branchId);
        TokenUsage tokenUsage =  result.tokenUsage();
        detail.put("messageTokens", tokenUsage.inputTokenCount());
        detail.put("answerTokens", tokenUsage.outputTokenCount());
        detail.put("answer", category);
        return new NodeResult(Map.of("branchId",branchId,"category", category,"reason", ""),Map.of());
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
