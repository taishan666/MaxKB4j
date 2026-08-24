package com.maxkb4j.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.util.MessageConverter;
import com.maxkb4j.core.assistant.IntentClassifyAssistant;
import com.maxkb4j.core.langchain4j.AiServiceFactory;
import com.maxkb4j.core.util.MessageUtils;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.DialogueType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.ModelConfig;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.IntentClassifyNode;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@Slf4j
@NodeHandlerType(NodeType.INTENT_CLASSIFY)
@RequiredArgsConstructor
@Component
public class IntentClassifyNodeHandler extends AbsNodeHandler {

    private final IModelProviderService modelFactory;

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        IntentClassifyNode.NodeParams params = parseParams(node, IntentClassifyNode.NodeParams.class);
        ModelConfig modelConfig = resolveModelConfig(workflow, params);
        ChatModel chatModel = modelFactory.buildChatModel(modelConfig.getModelId(), modelConfig.getModelParamsSetting());
        Object query = workflow.getReferenceField(params.getContentList());
        String queryStr = query == null ? "" : query.toString();
        Map<String, String> branchMap = new HashMap<>();
        List<IntentClassifyNode.Branch> branches = params.getBranch();

        for (IntentClassifyNode.Branch branch : branches) {
            branchMap.put(branch.getId(), branch.getContent());
        }

        List<ChatMessage> historyMessages = workflow.getHistoryMessages(params.getDialogueNumber(), DialogueType.WORK_FLOW.name(), node.getRuntimeNodeId());
        putDetail(node, ChatField.HISTORY_MESSAGE, MessageConverter.formatHistoryMessages(historyMessages));

        Map<Integer, String> idToClassification = new HashMap<>();
        String options = optionsFormat(idToClassification, branches);
        String chatMemory = MessageUtils.format(historyMessages);

        IntentClassifyAssistant assistant = AiServiceFactory.builder(IntentClassifyAssistant.class)
                .chatModel(chatModel)
                .build();

        Result<String> result = assistant.route(options, chatMemory, queryStr);

        Collection<Integer> classificationIds = parse(result.content());
        int classificationId = classificationIds.stream().findFirst().orElse(0);
        String branchId = idToClassification.get(classificationId);
        String category = branchMap.get(branchId);

        Map<String, Object> details = new HashMap<>();
        details.put(ChatField.SYSTEM, IntentClassifyAssistant.SYSTEM_MESSAGE);
        details.put(NodeField.QUESTION, queryStr);
        details.put(NodeField.ANSWER, category);
        putDetails(node, details);
        recordTokenUsage(node, result.tokenUsage());

        Map<String, Object> nodeVariable = new HashMap<>();
        nodeVariable.put(NodeField.BRANCH_ID, branchId);
        nodeVariable.put(NodeField.CATEGORY, category);
        nodeVariable.put(NodeField.REASON, "");
        return new NodeResult(nodeVariable);
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
        if (choices == null) {
            return List.of();
        }
        return Arrays.stream(choices.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
