package com.maxkb4j.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.domain.dto.MessageConverter;
import com.maxkb4j.core.assistant.IntentClassifyAssistant;
import com.maxkb4j.core.langchain4j.AssistantServices;
import com.maxkb4j.core.util.MessageUtils;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.DialogueType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.IntentClassifyNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@NodeHandlerType(NodeType.INTENT_CLASSIFY)
@RequiredArgsConstructor
@Component
public class IntentClassifyNodeHandler extends AbstractNodeHandler {

    private final IModelProviderService modelFactory;

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        IntentClassifyNode.NodeParams params = parseParams(node, IntentClassifyNode.NodeParams.class);
        ChatModel chatModel = modelFactory.buildChatModel(params.getModelId(), params.getModelParamsSetting());
        Object query = workflow.getReferenceField(params.getContentList());
        Map<String, String> branchMap = new HashMap<>();
        List<IntentClassifyNode.Branch> branches = params.getBranch();

        for (IntentClassifyNode.Branch branch : branches) {
            branchMap.put(branch.getId(), branch.getContent());
        }

        List<ChatMessage> historyMessages = workflow.getHistoryMessages(params.getDialogueNumber(), DialogueType.WORK_FLOW.name(), node.getRuntimeNodeId());
        putDetail(node, "history_message", MessageConverter.resetMessageList(historyMessages));

        Map<Integer, String> idToClassification = new HashMap<>();
        String options = optionsFormat(idToClassification, branches);
        String chatMemory = MessageUtils.format(historyMessages);

        IntentClassifyAssistant assistant = AssistantServices.builder(IntentClassifyAssistant.class)
                .chatModel(chatModel)
                .build();

        Result<String> result = assistant.route(options, chatMemory, query.toString());

        Collection<Integer> classificationIds = parse(result.content());
        int classificationId = classificationIds.stream().findFirst().orElse(0);
        String branchId = idToClassification.get(classificationId);
        String category = branchMap.get(branchId);

        TokenUsage tokenUsage = result.tokenUsage();
        putDetails(node, Map.of(
                "system", IntentClassifyAssistant.SYSTEM_MESSAGE,
                "question", query,
                "messageTokens", tokenUsage.inputTokenCount(),
                "answerTokens", tokenUsage.outputTokenCount(),
                "answer", category
        ));

        return new NodeResult(Map.of("branchId", branchId, "category", category, "reason", ""));
    }

    protected String optionsFormat(Map<Integer, String> idToClassification, List<IntentClassifyNode.Branch> branches) {
        StringBuilder optionsBuilder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(branches)) {
            for (int i = 0; i < branches.size(); i++) {
                IntentClassifyNode.Branch branch = branches.get(i);
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
        return Arrays.stream(choices.split(",")).map(String::trim).map(Integer::parseInt).toList();
    }
}
