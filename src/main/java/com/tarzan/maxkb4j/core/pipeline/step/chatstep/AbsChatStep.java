package com.tarzan.maxkb4j.core.pipeline.step.chatstep;

import com.alibaba.excel.util.StringUtils;
import com.tarzan.maxkb4j.core.pipeline.AbsStep;
import com.tarzan.maxkb4j.core.pipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.domain.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.application.domain.entity.NoReferencesSetting;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.enums.AIAnswerType;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import dev.langchain4j.data.message.AiMessage;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbsChatStep extends AbsStep {

    @Override
    @SuppressWarnings("unchecked")
    protected void _run(PipelineManage manage) {
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
        KnowledgeSetting knowledgeSetting = application.getKnowledgeSetting();
        NoReferencesSetting noReferencesSetting = knowledgeSetting.getNoReferencesSetting();
        String problemText = manage.chatParams.getMessage();
        boolean isAiAnswer = false;
       if (StringUtils.isBlank(problemText)) {
            answerText.set("用户消息不能为空");
        } else {
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
        manage.sink.tryEmitNext(this.toChatMessageVO(chatId, chatRecordId, isAiAnswer?"":answerText.get(), "",true));
        manage.context.put("answer", answerText.get());
    }


    protected abstract String execute(String chatId,String chatRecordId,ApplicationVO application,String userPrompt,PipelineManage manage);


    public ChatMessageVO toChatMessageVO(String chatId, String chatRecordId, String content, String reasoningContent, boolean nodeIsEnd) {
        return new ChatMessageVO(
                chatId,
                chatRecordId,
                "ai-chat-node",
                content,
                reasoningContent,
                List.of(),
                null,
                "ai-chat-node",
                "many_view",
                null,
                nodeIsEnd,
                false);
    }
}
