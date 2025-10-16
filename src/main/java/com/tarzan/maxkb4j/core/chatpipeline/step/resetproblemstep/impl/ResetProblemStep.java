package com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.assistant.CompressingQueryTransformer;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep.IResetProblemStep;
import com.tarzan.maxkb4j.core.tool.MessageTools;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class ResetProblemStep extends IResetProblemStep {

    private final ModelService modelService;
    private final ChatMemoryStore chatMemoryStore;

    @Override
    protected String execute(PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        JSONObject context = manage.context;
        ApplicationEntity application = (ApplicationEntity) context.get("application");
        String modelId = application.getModelId();
        JSONObject modelParams = application.getModelParamsSetting();
        BaseChatModel chatModel = modelService.getModelById(modelId,modelParams);
        String question = context.getString("problemText");
        String chatId = context.getString("chatId");
       // String systemText = application.getModelSetting().getSystem();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(chatId)
                .maxMessages(application.getDialogueNumber())
                .chatMemoryStore(chatMemoryStore)
                .build();
        CompressingQueryTransformer queryTransformer = AiServices.builder(CompressingQueryTransformer.class)
            //    .systemMessageProvider(chatMemoryId -> systemPrompt)
                .chatModel(chatModel.getChatModel())
                .build();
        Result<String> result= queryTransformer.transform(MessageTools.format(chatMemory.messages()),question);
        String paddingProblem=result.content();
        super.context.put("modelId", modelId);
        super.context.put("problemText", question);
        TokenUsage tokenUsage=result.tokenUsage();
        super.context.put("messageTokens", tokenUsage.inputTokenCount());
        super.context.put("answerTokens", tokenUsage.inputTokenCount());
        super.context.put("paddingProblemText", paddingProblem);
        log.info("BaseResetProblemStep 耗时 {} ms", System.currentTimeMillis() - startTime);
        return paddingProblem;
    }

/*    protected String execute1(PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        JSONObject context = manage.context;
        ApplicationEntity application = (ApplicationEntity) context.get("application");
        String modelId = application.getModelId();
        BaseChatModel chatModel = modelService.getModelById(modelId);
        QueryTransformer queryTransformer=new MyCompressingQueryTransformer(chatModel.getChatModel());
        String question = context.getString("problemText");
        String chatId = context.getString("chatId");
        String systemText = application.getModelSetting().getSystem();

        List<ChatMessage> chatMessages=chatMemory.messages();
        chatMessages.add(0, SystemMessage.from(systemText));
        Metadata metadata=new Metadata(UserMessage.from(question), chatMemory.id(), chatMessages);
        Query query=new Query(question,metadata);
        Collection<Query> list= queryTransformer.transform(query);
        StringBuilder answerSb=new StringBuilder();
        for (Query queryResult : list) {
            answerSb.append(queryResult.text());
        }
        String paddingProblem=answerSb.toString();
        super.context.put("modelId", modelId);
        super.context.put("problemText", question);
        super.context.put("messageTokens", TokenUtil.countTokens(chatMemory.messages()));
        super.context.put("answerTokens", TokenUtil.countTokens(paddingProblem));
        super.context.put("paddingProblemText", paddingProblem);
        log.info("BaseResetProblemStep 耗时 {} ms", System.currentTimeMillis() - startTime);
        return paddingProblem;
    }*/



    @Override
    public JSONObject getDetails() {
        JSONObject details=new JSONObject();
        details.put("step_type","problem_padding");
        details.put("problemText",context.get("problemText"));
        details.put("paddingProblemText",context.get("paddingProblemText"));
        details.put("messageTokens", context.get("messageTokens"));
        details.put("answerTokens", context.get("answerTokens"));
        return details;
    }
}
