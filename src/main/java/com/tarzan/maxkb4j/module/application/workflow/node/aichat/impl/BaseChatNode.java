package com.tarzan.maxkb4j.module.application.workflow.node.aichat.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.MyChatMemory;
import com.tarzan.maxkb4j.module.application.workflow.INode;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.node.aichat.IChatNode;
import com.tarzan.maxkb4j.module.application.workflow.node.aichat.input.ChatNodeParams;
import com.tarzan.maxkb4j.module.application.workflow.node.start.input.FlowParams;
import com.tarzan.maxkb4j.module.assistant.Assistant;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.model.provider.out.ChatStream;
import com.tarzan.maxkb4j.util.SpringUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public class BaseChatNode extends IChatNode {

    private final ModelService modelService;

    public BaseChatNode() {
        this.modelService = SpringUtil.getBean(ModelService.class);
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

    private Iterator<String> writeContextStream1(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode currentNode, WorkflowManage workflow) {
        ChatStream chatStream = (ChatStream) nodeVariable.get("result");
        chatStream.setCallback((response) -> {
            String answer = response.aiMessage().text();
            workflow.setAnswer(answer);
            TokenUsage tokenUsage = response.tokenUsage();
            context.put("messageTokens", tokenUsage.inputTokenCount());
            context.put("answerTokens", tokenUsage.outputTokenCount());
            context.put("answer", answer);
            context.put("question", nodeVariable.get("question"));
            context.put("history_message", nodeVariable.get("history_message"));
            long runTime = System.currentTimeMillis() - (long) context.get("start_time");
            context.put("runTime", runTime / 1000F);
        });
        return chatStream.getIterator();
    }

    private Stream<String> writeContextStream(Map<String, Object> nodeVariable, Map<String, Object> workflowVariable, INode currentNode, WorkflowManage workflow) {
        TokenStream tokenStream = (TokenStream) nodeVariable.get("result");
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        tokenStream.onPartialResponse(sink::tryEmitNext)
                .onCompleteResponse(response -> {
                    sink.tryEmitComplete();
                    String answer = response.aiMessage().text();
                    workflow.setAnswer(answer);
                    TokenUsage tokenUsage = response.tokenUsage();
                    context.put("messageTokens", tokenUsage.inputTokenCount());
                    context.put("answerTokens", tokenUsage.outputTokenCount());
                    context.put("answer", answer);
                    context.put("question", nodeVariable.get("question"));
                    context.put("history_message", nodeVariable.get("history_message"));
                    long runTime = System.currentTimeMillis() - (long) context.get("start_time");
                    context.put("runTime", runTime / 1000F);
                })
                .onError(error -> {
                    sink.tryEmitNext(error.getMessage());
                    sink.tryEmitComplete();
                })
                .start();
        return sink.asFlux().toStream();
    }

    @Override
    public NodeResult execute(ChatNodeParams nodeParams, FlowParams flowParams) {
        if (Objects.isNull(nodeParams.getDialogueType())) {
            nodeParams.setDialogueType("WORKFLOW");
        }
     /*   if (Objects.isNull(nodeParams.getModelParamsSetting())) {
            nodeParams.setModelParamsSetting(getDefaultModelParamsSetting(nodeParams.getModelId()));
        }*/
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        List<ChatMessage> historyMessage = workflowManage.getHistoryMessage(flowParams.getHistoryChatRecord(), nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), super.runtimeNodeId);
        UserMessage question = workflowManage.generatePromptQuestion(nodeParams.getPrompt());
        String system = workflowManage.generatePrompt(nodeParams.getSystem());
        List<ChatMessage> messageList = super.workflowManage.generateMessageList(system, question, historyMessage);
        if (nodeParams.getIsResult()) {
            //   ChatStream chatStream = chatModel.stream(messageList);
            MyChatMemory chatMemory = new MyChatMemory(5, 5000);
            chatMemory.add1(messageList);
            Assistant assistant = AiServices.builder(Assistant.class)
                    //     .systemMessageProvider(chatMemoryId ->system)
                    //    .chatLanguageModel(chatModel.getChatModel())
                    .streamingChatLanguageModel(chatModel.getStreamingChatModel())
                    // .chatMemory(chatMemory)
                    // .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                    //   .contentRetriever(new MyContentRetriever(paragraphList))
                    .build();
            TokenStream tokenStream = assistant.chatStream(question.singleText());

            Map<String, Object> nodeVariable = Map.of(
                    "result", tokenStream,
                    "system", system,
                    "chat_model", chatModel,
                    "message_list", messageList,
                    "history_message", historyMessage,
                    "question", question.singleText()
            );
            return new NodeResult(nodeVariable, Map.of(), this::writeContextStream);
        } else {
            ChatResponse res = chatModel.generate(messageList);
            AiMessage aiMessage = res.aiMessage();
            TokenUsage tokenUsage = res.tokenUsage();
            Map<String, Object> nodeVariable = Map.of(
                    "system", system,
                    "chat_model", chatModel,
                    "message_list", messageList,
                    "history_message", historyMessage,
                    "question", question.singleText(),
                    "answer", aiMessage.text(),
                    "messageTokens", tokenUsage.inputTokenCount(),
                    "answerTokens", tokenUsage.outputTokenCount()
            );
            return new NodeResult(nodeVariable, Map.of());
        }

    }


    @Override
    public void saveContext(JSONObject detail, WorkflowManage workflowManage) {
        this.context.put("question", detail.get("question"));
        this.context.put("answer", detail.get("answer"));
        this.answerText = detail.getString("answer");
    }
}
