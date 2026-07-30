package com.maxkb4j.application.pipeline.step.chatstep;

import com.alibaba.excel.util.StringUtils;
import com.maxkb4j.application.pipeline.AbsStep;
import com.maxkb4j.application.pipeline.PipelineManage;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.common.util.MessageConverter;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbsChatStep extends AbsStep {

    @Override
    @SuppressWarnings("unchecked")
    protected void _run(PipelineManage manage) throws Exception {
        String chatId = manage.chatParams.getChatId();
        List<ParagraphRagVO> paragraphList = (List<ParagraphRagVO>) manage.context.get("paragraphList");
        ApplicationVO application = manage.application;
        String userPrompt = (String) manage.context.get("userPrompt");
        String chatRecordId =manage.chatParams.getChatRecordId();
        int dialogueNumber = application.getDialogueNumber();
        List<ChatMessage> historyMessages = manage.getHistoryMessages(dialogueNumber);
        AtomicReference<String> answerText = new AtomicReference<>("");
        if (CollectionUtils.isEmpty(paragraphList)) {
            paragraphList = new ArrayList<>();
        }
        List<AiMessage> directlyReturnChunkList = new ArrayList<>();
        for (ParagraphRagVO paragraph : paragraphList) {
            if (paragraph.returnIfSatisfied()) {
                directlyReturnChunkList.add(AiMessage.from(paragraph.getContent()));
            }
        }
        String problemText = manage.chatParams.getMessage();
        String modelId = application.getModelId();
        boolean isAiAnswer = false;
        if (StringUtils.isBlank(modelId)) {
            answerText.set("抱歉，AI 模型未配置，请先前往智能体设置 AI 模型。");
        }else if (StringUtils.isBlank(problemText)) {
            answerText.set("用户消息不能为空");
        } else {
            KnowledgeSetting knowledgeSetting = application.getKnowledgeSetting();
            Boolean fallbackEnable = knowledgeSetting.getFallbackEnable();
            if (!CollectionUtils.isEmpty(directlyReturnChunkList)) {
                answerText.set(directlyReturnChunkList.getFirst().text());
            } else {
                if (paragraphList.isEmpty() && Boolean.TRUE.equals(fallbackEnable)) {
                    String fallbackResponse = knowledgeSetting.getFallbackResponse();
                    answerText.set(fallbackResponse);
                }else {
                    String answer =execute(chatId,chatRecordId,application,historyMessages,userPrompt,manage);
                    answerText.set(answer);
                    isAiAnswer= true;
                }
            }
        }
        if (!isAiAnswer){
            manage.sink.tryEmitNext(this.toChatMessageVO(chatId, chatRecordId,answerText.get(), "",true));
        }
        historyMessages.add(new UserMessage(problemText));
        historyMessages.add(new AiMessage(answerText.get()));
        context.put("messageList", manage.formatHistoryMessages(historyMessages));
        manage.context.put("answer", answerText.get());
        manage.context.put("reasoningContent", context.get("reasoningContent"));
    }


    protected abstract String execute(String chatId,String chatRecordId,ApplicationVO application,List<ChatMessage> historyMessages,String userPrompt,PipelineManage manage) throws Exception;



    /**
     * 转换为聊天消息VO
     * 使用 MessageConverter 工具类
     *
     * @param chatId           聊天ID
     * @param chatRecordId     聊天记录ID
     * @param content          消息内容
     * @param reasoningContent 推理内容
     * @param nodeIsEnd        节点是否结束
     * @return 聊天消息VO
     */
    public ChatMessageVO toChatMessageVO(String chatId, String chatRecordId, String content, String reasoningContent, boolean nodeIsEnd) {
        return MessageConverter.toChatMessageVO(
                chatId,
                chatRecordId,
                "ai-chat-node",
                 "",
                content,
                reasoningContent,
                List.of(),
                "",
                "",
                "ai-chat-node",
                "many_view",
                null,
                nodeIsEnd,
                false);
    }

}
