package com.tarzan.maxkb4j.core.workflow.node.aichat.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.tarzan.maxkb4j.core.assistant.Assistant;
import com.tarzan.maxkb4j.core.langchain4j.MyAiServices;
import com.tarzan.maxkb4j.core.langchain4j.MyChatMemory;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.node.aichat.input.ChatNodeParams;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.util.SpringUtil;
import com.tarzan.maxkb4j.util.StringUtil;
import com.tarzan.maxkb4j.util.ToolUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.AI_CHAT;

@Slf4j
public class BaseChatNode extends INode {

    private final ModelService modelService;
    private final ChatMemoryStore chatMemoryStore;
    private final ToolUtil toolUtil;


    public BaseChatNode(JSONObject properties) {
        super(properties);
        this.type = AI_CHAT.getKey();
        this.modelService = SpringUtil.getBean(ModelService.class);
        this.chatMemoryStore = SpringUtil.getBean(ChatMemoryStore.class);
        this.toolUtil = SpringUtil.getBean(ToolUtil.class);
    }


    @Override
    public NodeResult execute() throws Exception {
        System.out.println(AI_CHAT);
        ChatNodeParams nodeParams = super.nodeParams.toJavaObject(ChatNodeParams.class);
        BaseChatModel chatModel = modelService.getModelById(nodeParams.getModelId(), nodeParams.getModelParamsSetting());
        //   List<ChatMessage> historyContext = workflowManage.getHistoryMessage(flowParams.getHistoryChatRecord(), nodeParams.getDialogueNumber(), nodeParams.getDialogueType(), runtimeNodeId);
        List<ChatMessage> historyContext = new ArrayList<>();
        String problemText = workflowManage.generatePrompt(nodeParams.getPrompt());
        String systemPrompt = workflowManage.generatePrompt(nodeParams.getSystem());
        String system = StringUtil.isBlank(systemPrompt) ? "You're an intelligent assistant." : systemPrompt;
        String chatId = workflowManage.getFlowParams().getChatId();
        ChatMemory chatMemory = MyChatMemory.builder()
                .id(chatId)
                .maxMessages(nodeParams.getDialogueNumber())
                .chatMemoryStore(chatMemoryStore)
                .build();
        List<String> toolIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(nodeParams.getToolIds()) && nodeParams.getToolEnable()) {
            toolIds.addAll(nodeParams.getToolIds());
        }
        if (StringUtil.isNotBlank(nodeParams.getMcpToolId()) && nodeParams.getMcpEnable()) {
            toolIds.add(nodeParams.getMcpToolId());
        }
        Assistant assistant = MyAiServices.builder(Assistant.class)
                .systemMessageProvider(chatMemoryId -> system)
                .chatMemory(chatMemory)
                .tools(toolUtil.getTools(toolIds))
                .streamingChatModel(chatModel.getStreamingChatModel())
                .build();
        TokenStream tokenStream = assistant.chatStream(problemText);
        Map<String, Object> nodeVariable = Map.of(
                "result", tokenStream,
                "system", system,
                "chat_model", chatModel,
                "message_list", chatMemory.messages(),
                "history_message", historyContext,
                "question", workflowManage.getFlowParams().getQuestion()
        );
        return new NodeResult(nodeVariable, Map.of(), this::writeContextStream);
    }

    private void writeContextStream(Map<String, Object> nodeVariable, Map<String, Object> globalVariable, INode node, WorkflowManage workflow) {
        if (nodeVariable != null) {
            context.putAll(nodeVariable);
            TokenStream tokenStream = (TokenStream) nodeVariable.get("result");
            CompletableFuture<ChatResponse> futureChatResponse = new CompletableFuture<>();
            boolean isResult = workflow.isResult(node, new NodeResult(nodeVariable, globalVariable));
            tokenStream.onPartialThinking(thinking -> {
                        if (isResult) {
                            ChatMessageVO vo = new ChatMessageVO(
                                    workflowManage.getFlowParams().getChatId(),
                                    workflowManage.getFlowParams().getChatRecordId(),
                                    "",
                                    thinking.text(),
                                    runtimeNodeId,
                                    type,
                                    "many_view",
                                    false);
                            workflowManage.getSink().tryEmitNext(vo);
                        }
                    })
                    .onPartialResponse(content -> {
                        if (isResult) {
                            ChatMessageVO vo = new ChatMessageVO(
                                    workflowManage.getFlowParams().getChatId(),
                                    workflowManage.getFlowParams().getChatRecordId(),
                                    content,
                                    "",
                                    runtimeNodeId,
                                    type,
                                    "many_view",
                                    false);
                            workflowManage.getSink().tryEmitNext(vo);
                        }
                    })
                    .onCompleteResponse(response -> {
                        String answer = response.aiMessage().text();
                        String thinking = response.aiMessage().thinking();
                        TokenUsage tokenUsage = response.tokenUsage();
                        context.put("messageTokens", tokenUsage.inputTokenCount());
                        context.put("answerTokens", tokenUsage.outputTokenCount());
                        context.put("answer", answer);
                        context.put("reasoningContent", thinking);
                        ChatMessageVO vo = new ChatMessageVO(
                                workflowManage.getFlowParams().getChatId(),
                                workflowManage.getFlowParams().getChatRecordId(),
                                "",
                                "",
                                runtimeNodeId,
                                type,
                                "many_view",
                                false);
                        workflowManage.getSink().tryEmitNext(vo);
                        futureChatResponse.complete(response);// 完成后释放线程
                    })
                    .onError(error -> {
                        workflowManage.getSink().tryEmitError(error);
                        futureChatResponse.completeExceptionally(error); // 完成后释放线程
                    })
                    .start();
            futureChatResponse.join(); // 阻塞当前线程直到 futureChatResponse 完成
        }
        if (context.containsKey("start_time")) {
            long runTime = System.currentTimeMillis() - (long) context.get("start_time");
            context.put("runTime", runTime / 1000F);
        }
    }


    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("system", context.get("system"));
        List<ChatMessage> historyMessage = (List<ChatMessage>) context.get("history_message");
        detail.put("history_message", resetMessageList(historyMessage));
        detail.put("question", context.get("question"));
        detail.put("answer", context.get("answer"));
        detail.put("reasoningContent", context.get("reasoningContent"));
        detail.put("messageTokens", context.get("messageTokens"));
        detail.put("answerTokens", context.get("answerTokens"));
        return detail;
    }
}
