package com.tarzan.maxkb4j.core.workflow.node.intentclassify.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.AppChatMemory;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.node.intentclassify.input.IntentClassifyBranch;
import com.tarzan.maxkb4j.core.workflow.node.intentclassify.input.IntentClassifyNodeParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.INTENT_CLASSIFY;

@Slf4j
public class IntentClassifyNode extends INode {

    private final ModelService modelService;
    private final AiServices<Assistant> aiServicesBuilder;

    String PROMPT_TEMPLATE = """
    # Role
    You are an intention classification expert, good at being able to judge which classification the user's input belongs to.

    ## Skills
    Skill 1: Clearly determine which of the following intention classifications the user's input belongs to.
    Intention classification list:
    {{classification_list}}

    Note:
    - Please determine the match only between the user's input content and the Intention classification list content, without judging or categorizing the match with the classification ID.
    - **When classifying, you must give higher weight to the context and intent continuity shown in the historical conversation. Do not rely solely on the literal meaning of the current input; instead, prioritize the most consistent classification with the previous dialogue flow.**

    ## User Input
    {{user_input}}

    ## Reply requirements
    - The answer must be returned in JSON format.
    - Strictly ensure that the output is in a valid JSON format.
    - Do not add prefix ```json or suffix ```
    - The answer needs to include the following fields such as:
    {{
    "classificationId": 0,
    "reason": ""
    }}

    ## Limit
    - Please do not reply in text.""";


    public IntentClassifyNode(JSONObject properties) {
        super(properties);
        super.type = INTENT_CLASSIFY.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
        this.aiServicesBuilder = AiServices.builder(Assistant.class);
    }


    @Override
    public NodeResult execute() throws Exception {
        IntentClassifyNodeParams nodeParams = super.getNodeData().toJavaObject(IntentClassifyNodeParams.class);
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        Object content = super.getReferenceField(nodeParams.getContentList().get(0),nodeParams.getContentList().get(1));
        Map<String,String> branchMap = new HashMap<>();
        List<IntentClassifyBranch> branches=nodeParams.getBranch();
        for (IntentClassifyBranch branch : branches) {
            branchMap.put(branch.getId(), branch.getContent());
        }
        PromptTemplate promptTemplate = PromptTemplate.from(PROMPT_TEMPLATE);
        Map<String, Object> variables = new HashMap<>();
        variables.put("user_input", content);
        variables.put("classification_list", JSONArray.toJSONString(nodeParams.getBranch()));
        String question = promptTemplate.apply(variables).text();
        String systemPrompt = "你是一个专业的意图识别助手，请根据用户输入和意图选项，准确识别用户的真实意图。";
        List<ChatMessage> historyMessages = super.getHistoryMessages(nodeParams.getDialogueNumber(), "WORK_FLOW", runtimeNodeId);
        detail.put("history_message", resetMessageList(historyMessages));
        aiServicesBuilder.chatMemory(AppChatMemory.withMessages(historyMessages));
        Assistant assistant = aiServicesBuilder
                .systemMessageProvider(chatMemoryId -> systemPrompt)
                .chatMemory(AppChatMemory.withMessages(historyMessages))
                .chatModel(chatModel.getChatModel())
                .build();
        Result<String> result = assistant.chat(question);
        detail.put("system", systemPrompt);
        detail.put("question", content);
        JSONObject json = JSONObject.parseObject(result.content());
        TokenUsage tokenUsage =  result.tokenUsage();
        detail.put("messageTokens", tokenUsage.inputTokenCount());
        detail.put("answerTokens", tokenUsage.outputTokenCount());
        detail.put("answer", result.content());
        String classificationId=json.getString("classificationId");
        return new NodeResult(Map.of("branchId",classificationId,"category", branchMap.get(classificationId),"reason", json.getString("reason")),Map.of());
    }


    @Override
    public void saveContext(JSONObject detail) {
        context.put("answer", detail.get("answer"));
        context.put("reasoningContent", detail.get("reasoningContent"));
    }

    @Override
    public JSONObject getRunDetail() {
        return detail;
    }
}
