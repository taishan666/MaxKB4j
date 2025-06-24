package com.tarzan.maxkb4j.core.workflow.node.aichat.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.node.aichat.input.ChatNodeParams;
import com.tarzan.maxkb4j.module.assistant.Assistant;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.rag.MyAiServices;
import com.tarzan.maxkb4j.module.rag.MyChatMemory;
import com.tarzan.maxkb4j.module.rag.MyContentRetriever;
import com.tarzan.maxkb4j.util.SpringUtil;
import com.tarzan.maxkb4j.util.StringUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.DefaultContentAggregator;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.tarzan.maxkb4j.core.constant.PromptTemplates.RAG_PROMPT_TEMPLATE;
import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.AI_CHAT;

@Slf4j
public class BaseChatNode extends INode {

    private final ModelService modelService;
    private final ChatMemoryStore chatMemoryStore;

    public BaseChatNode() {
        this.type=AI_CHAT.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
        this.chatMemoryStore = SpringUtil.getBean(ChatMemoryStore.class);
    }



    private Stream<String> writeContextStream(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode currentNode, WorkflowManage workflow) {
        TokenStream tokenStream = (TokenStream) nodeVariable.get("result");
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        tokenStream.onPartialResponse(sink::tryEmitNext)
                .onCompleteResponse(response -> {
                    String answer = response.aiMessage().text();
                    workflow.setAnswer(answer);
                    TokenUsage tokenUsage = response.tokenUsage();
                    context.put("messageTokens", tokenUsage.inputTokenCount());
                    context.put("answerTokens", tokenUsage.outputTokenCount());
                    context.put("answer", answer);
                    context.put("system", nodeVariable.get("system"));
                    context.put("question", nodeVariable.get("question"));
                    context.put("history_message", nodeVariable.get("history_message"));
                    long runTime = System.currentTimeMillis() - (long) context.get("start_time");
                    context.put("runTime", runTime / 1000F);
                    sink.tryEmitComplete();
                })
                .onError(error -> sink.tryEmitNext(error.getMessage()))
                .start();
        return sink.asFlux().toStream();
    }

    @Override
    public NodeResult execute() throws Exception {
        System.out.println(AI_CHAT);
        ChatNodeParams nodeParams= super.nodeParams.toJavaObject(ChatNodeParams.class);
        if (Objects.isNull(nodeParams.getDialogueType())) {
            nodeParams.setDialogueType("WORKFLOW");
        }
        List<String> fields=nodeParams.getDatasetReferenceAddress();
        List<ParagraphVO> paragraphList =new ArrayList<>();
        if(!CollectionUtils.isEmpty(fields)&&fields.size()>1){
            Object res=workflowManage.getReferenceField(fields.get(0),fields.subList(1, fields.size()));
            paragraphList = (List<ParagraphVO>) res;
        }
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        List<ChatMessage> historyMessage = workflowManage.getHistoryMessage(flowParams.getHistoryChatRecord(), nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), runtimeNodeId);
        List<String> questionFields=nodeParams.getQuestionReferenceAddress();
        String problemText= (String)workflowManage.getReferenceField(questionFields.get(0),questionFields.subList(1, questionFields.size()));
        String systemPrompt = workflowManage.generatePrompt(nodeParams.getSystem());
        String system= StringUtil.isBlank(systemPrompt)?"You're an intelligent assistant.":systemPrompt;
        ContentInjector contentInjector = DefaultContentInjector.builder()
                .promptTemplate(RAG_PROMPT_TEMPLATE)
                .build();
        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(new DefaultQueryRouter(new MyContentRetriever(paragraphList)))
                .contentAggregator(new DefaultContentAggregator())
                .contentInjector(contentInjector)
                .build();
        String chatId = super.flowParams.getChatId();
        ChatMemory chatMemory = MyChatMemory.builder()
                .id(chatId)
                .maxMessages(nodeParams.getDialogueNumber())
                .chatMemoryStore(chatMemoryStore)
                .build();
        Assistant assistant = MyAiServices.builder(Assistant.class)
                .systemMessageProvider(chatMemoryId ->system)
                .chatMemory(chatMemory)
                .retrievalAugmentor(retrievalAugmentor)
                .streamingChatModel(chatModel.getStreamingChatModel())
                .build();
        TokenStream tokenStream = assistant.chatStream(problemText);
        Map<String, Object> nodeVariable = Map.of(
                "result", tokenStream,
                "system", system,
                "chat_model", chatModel,
                "message_list", chatMemory.messages(),
                "history_message", historyMessage,
                "question", problemText
        );
        return new NodeResult(nodeVariable, Map.of(), this::writeContextStream);
    }


    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("system", context.get("system"));
        List<ChatMessage> historyMessage = (List<ChatMessage>) context.get("history_message");
        detail.put("history_message", resetMessageList(historyMessage));
        detail.put("question", context.get("question"));
        detail.put("answer", context.get("answer"));
        detail.put("messageTokens", context.get("messageTokens"));
        detail.put("answerTokens", context.get("answerTokens"));
        return detail;
    }
}
