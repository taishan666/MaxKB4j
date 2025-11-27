package com.tarzan.maxkb4j.core.chatpipeline.step.chatstep;

import com.alibaba.excel.util.StringUtils;
import com.tarzan.maxkb4j.core.chatpipeline.IChatPipelineStep;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.domian.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.application.domian.entity.NoReferencesSetting;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.enums.AIAnswerType;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import dev.langchain4j.data.message.AiMessage;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class IChatStep extends IChatPipelineStep {

    @Override
    @SuppressWarnings("unchecked")
    protected void _run(PipelineManage manage) {
        String chatId = (String) manage.context.get("chatId");
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) manage.context.get("paragraphList");
        ApplicationVO application = (ApplicationVO) manage.context.get("application");
        String userPrompt = (String) manage.context.get("user_prompt");
        String chatRecordId = (String) manage.context.get("chatRecordId");
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
        String problemText = (String) manage.context.get("problemText");
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
       if (isAiAnswer){
           manage.sink.tryEmitNext(this.toChatMessageVO(chatId, chatRecordId, "", "",true));
       }else {
           manage.sink.tryEmitNext(this.toChatMessageVO(chatId, chatRecordId, answerText.get(), "",true));
       }
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
                nodeIsEnd);
    }
}
