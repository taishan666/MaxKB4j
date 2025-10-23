package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.core.assistant.IntentClassifyAssistant;
import com.tarzan.maxkb4j.core.tool.MessageTools;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.enums.DialogueType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.intentclassify.impl.IntentClassifyNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.model.provider.service.impl.BaseChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@AllArgsConstructor
@Component
public class IntentClassifyNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;
    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        IntentClassifyNode.NodeParams nodeParams = node.getNodeData().toJavaObject(IntentClassifyNode.NodeParams.class);
        BaseChatModel chatModel = modelFactory.build(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        Object query = workflow.getReferenceField(nodeParams.getContentList().get(0),nodeParams.getContentList().get(1));
        Map<String,String> branchMap = new HashMap<>();
        List<IntentClassifyNode.Branch> branches=nodeParams.getBranch();
        for (IntentClassifyNode.Branch branch : branches) {
            branchMap.put(branch.getId(), branch.getContent());
        }
        List<ChatMessage> historyMessages = workflow.getHistoryMessages(nodeParams.getDialogueNumber(), DialogueType.WORKFLOW.name(), node.getRuntimeNodeId());
        node.getDetail().put("history_message", node.resetMessageList(historyMessages));
        Map<Integer, String> idToClassification=new HashMap<>();
        String options =optionsFormat(idToClassification,nodeParams.getBranch());
        String chatMemory = MessageTools.format(historyMessages);
        IntentClassifyAssistant assistant = AiServices.builder(IntentClassifyAssistant.class)
                .chatModel(chatModel.getChatModel())
                .build();
        Result<String> result = assistant.route(options,chatMemory, query.toString());
        node.getDetail().put("system", IntentClassifyAssistant.SYSTEM_MESSAGE);
        node.getDetail().put("question", query);
        Collection<Integer> classificationIds = parse(result.content());
        int classificationId=classificationIds.stream().findFirst().orElse(0);
        String branchId=idToClassification.get(classificationId);
        String category=branchMap.get(branchId);
        TokenUsage tokenUsage =  result.tokenUsage();
        node.getDetail().put("messageTokens", tokenUsage.inputTokenCount());
        node.getDetail().put("answerTokens", tokenUsage.outputTokenCount());
        node.getDetail().put("answer", category);
        return new NodeResult(Map.of("branchId",branchId,"category", category,"reason", ""),Map.of());
    }

    protected String optionsFormat(Map<Integer, String> idToClassification,List<IntentClassifyNode.Branch> branches) {
        StringBuilder optionsBuilder = new StringBuilder();
        if (CollectionUtils.isNotEmpty( branches)){
            Collections.reverse(branches);
            for (int i = 0; i < branches.size(); i++) {
                IntentClassifyNode.Branch branch=branches.get(i);
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

    protected Collection<Integer> parse(String choices) {
        return  Arrays.stream(choices.split(",")).map(String::trim).map(Integer::parseInt).toList();
    }


}
