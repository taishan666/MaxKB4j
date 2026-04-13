package com.maxkb4j.application.pipeline.step.chatstep;

import com.alibaba.excel.util.StringUtils;
import com.maxkb4j.application.enums.AIAnswerType;
import com.maxkb4j.application.pipeline.AbsStep;
import com.maxkb4j.application.pipeline.PipelineManage;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.MessageConverter;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.common.mp.entity.NoReferencesSetting;
import com.maxkb4j.knowledge.vo.ParagraphVO;
import dev.langchain4j.data.message.AiMessage;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbsChatStep extends AbsStep {

    @Override
    @SuppressWarnings("unchecked")
    protected void _run(PipelineManage manage) throws ExecutionException, InterruptedException, TimeoutException {
        String chatId = manage.chatParams.getChatId();
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) manage.context.get("paragraphList");
        ApplicationVO application = manage.application;
        String userPrompt = (String) manage.context.get("user_prompt");
        String chatRecordId =manage.chatParams.getChatRecordId();
        AtomicReference<String> answerText = new AtomicReference<>("");
        if (CollectionUtils.isEmpty(paragraphList)) {
            paragraphList = new ArrayList<>();
        }
        List<AiMessage> directlyReturnChunkList = new ArrayList<>();
        for (ParagraphVO paragraph : paragraphList) {
            if ("directlyReturn".equals(paragraph.getHitHandlingMethod()) && paragraph.getSimilarity() >= paragraph.getDirectlyReturnSimilarity()) {
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
            NoReferencesSetting noReferencesSetting = knowledgeSetting.getNoReferencesSetting();
            String status = noReferencesSetting.getStatus();
            if (!CollectionUtils.isEmpty(directlyReturnChunkList)) {
                answerText.set(directlyReturnChunkList.get(0).text());
            } else {
                if (paragraphList.isEmpty() && AIAnswerType.designated_answer.name().equals(status)) {
                    String value = noReferencesSetting.getValue();
                    answerText.set(value.replace("{question}", problemText));
                }else {
                    String answer =execute(chatId,chatRecordId,application,userPrompt,manage);
                    answerText.set(answer);
                    isAiAnswer= true;
                }
            }
        }
        if (!isAiAnswer){
            manage.sink.tryEmitNext(this.toChatMessageVO(chatId, chatRecordId,answerText.get(), "",true));
        }
        manage.context.put("answer", answerText.get());
        manage.context.put("reasoningContent", context.get("reasoningContent"));

    }


    protected abstract String execute(String chatId,String chatRecordId,ApplicationVO application,String userPrompt,PipelineManage manage) throws ExecutionException, InterruptedException, TimeoutException;

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
