package com.tarzan.maxkb4j.core.workflow.node.intentclassify.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.enums.DialogueType;
import com.tarzan.maxkb4j.core.workflow.node.intentclassify.input.IntentClassifyBranch;
import com.tarzan.maxkb4j.core.workflow.node.intentclassify.input.IntentClassifyNodeParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.INTENT_CLASSIFY;

@Slf4j
public class IntentClassifyNode extends INode {

    private final ModelService modelService;
    private final Map<Integer, String> idToClassification;
    private static final String DEFAULT_PROMPT_TEMPLATE = """
            Based on the user query, \
            determine the most suitable option(s) to retrieve relevant information from the following options:
            {{options}}
            It is very important that your answer consists of a single number and nothing else!
            Conversation: {{chatMemory}}
            User query: {{query}}""";

    public IntentClassifyNode(JSONObject properties) {
        super(properties);
        super.setType(INTENT_CLASSIFY.getKey());
        this.modelService = SpringUtil.getBean(ModelService.class);
        this.idToClassification = new HashMap<>();
    }


    @Override
    public NodeResult execute() throws Exception {
        IntentClassifyNodeParams nodeParams = super.getNodeData().toJavaObject(IntentClassifyNodeParams.class);
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        Object query = super.getReferenceField(nodeParams.getContentList().get(0),nodeParams.getContentList().get(1));
        Map<String,String> branchMap = new HashMap<>();
        List<IntentClassifyBranch> branches=nodeParams.getBranch();
        for (IntentClassifyBranch branch : branches) {
            branchMap.put(branch.getId(), branch.getContent());
        }
        List<ChatMessage> historyMessages = super.getHistoryMessages(nodeParams.getDialogueNumber(), DialogueType.WORKFLOW.name(), super.getRuntimeNodeId());
        detail.put("history_message", resetMessageList(historyMessages));
        PromptTemplate promptTemplate = PromptTemplate.from(DEFAULT_PROMPT_TEMPLATE);
        Map<String, Object> variables = new HashMap<>();
        variables.put("query", query);
        variables.put("options", optionsFormat(nodeParams.getBranch()));
        variables.put("chatMemory", format(historyMessages));
        String question = promptTemplate.apply(variables).text();
        String systemPrompt = "You are a professional intent recognition assistant. Please accurately identify the user's true intent based on the user's input and intent options.";
        Assistant assistant = AiServices.builder(Assistant.class)
                .systemMessageProvider(chatMemoryId -> systemPrompt)
                .chatModel(chatModel.getChatModel())
                .build();
        Result<String> result = assistant.chat(question);
        detail.put("system", systemPrompt);
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
