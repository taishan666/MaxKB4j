package com.maxkb4j.application.pipeline.step.chatstep;

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
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

public abstract class AbsChatStep extends AbsStep {

    @Override
    @SuppressWarnings("unchecked")
    protected void _run(PipelineManage manage) throws Exception {
        String chatId = manage.chatParams.getChatId();
        String chatRecordId = manage.chatParams.getChatRecordId();
        ApplicationVO application = manage.application;
        List<ChatMessage> historyMessages = manage.getHistoryMessages(application.getDialogueNumber());
        List<ParagraphRagVO> paragraphList = Optional
                .ofNullable((List<ParagraphRagVO>) manage.context.get("paragraphList"))
                .orElse(List.of());

        AnswerResult result = resolveAnswer(manage, application, paragraphList, historyMessages);
        // AI 流式回答已在 execute 内部实时推送，此处仅补发非 AI 回答的结束消息
        if (!result.fromAi()) {
            manage.sink.tryEmitNext(toChatMessageVO(chatId, chatRecordId, result.text(), "", true));
        }
        recordResult(manage, result.text(), historyMessages);
    }

    // ==================== 答案解析 ====================

    /**
     * 按优先级解析本轮回答：参数校验 → 直接返回分段 → 知识库兜底 → AI 流式对话。
     */
    private AnswerResult resolveAnswer(PipelineManage manage, ApplicationVO application,
                                       List<ParagraphRagVO> paragraphList, List<ChatMessage> historyMessages) throws Exception {
        String invalidMessage = validate(application, manage.chatParams.getMessage());
        if (invalidMessage != null) {
            return AnswerResult.ofText(invalidMessage);
        }
        String directAnswer = findDirectReturnAnswer(paragraphList);
        if (directAnswer != null) {
            return AnswerResult.ofText(directAnswer);
        }
        AnswerResult fallbackAnswer = resolveFallbackAnswer(application, paragraphList);
        if (fallbackAnswer != null) {
            return fallbackAnswer;
        }
        String answer = execute(manage.chatParams.getChatId(), manage.chatParams.getChatRecordId(),
                application, historyMessages, (String) manage.context.get("userPrompt"), manage);
        return AnswerResult.ofAi(answer);
    }

    /**
     * 前置校验：模型未配置或用户消息为空时返回提示文案，校验通过返回 null。
     */
    private String validate(ApplicationVO application, String problemText) {
        if (StringUtils.isBlank(application.getModelId())) {
            return "抱歉，AI 模型未配置，请先前往智能体设置 AI 模型。";
        }
        if (StringUtils.isBlank(problemText)) {
            return "用户消息不能为空";
        }
        return null;
    }

    /**
     * 查找命中"直接返回"条件的分段内容，无命中返回 null。
     */
    private String findDirectReturnAnswer(List<ParagraphRagVO> paragraphList) {
        return paragraphList.stream()
                .filter(ParagraphRagVO::returnIfSatisfied)
                .map(ParagraphRagVO::getContent)
                .findFirst()
                .orElse(null);
    }

    /**
     * 检索结果为空且启用兜底时返回兜底回答，否则返回 null（继续走 AI 对话）。
     */
    private AnswerResult resolveFallbackAnswer(ApplicationVO application, List<ParagraphRagVO> paragraphList) {
        if (!paragraphList.isEmpty()) {
            return null;
        }
        KnowledgeSetting knowledgeSetting = Optional.ofNullable(application.getKnowledgeSetting())
                .orElseGet(KnowledgeSetting::new);
        if (!Boolean.TRUE.equals(knowledgeSetting.getFallbackEnable())) {
            return null;
        }
        return AnswerResult.ofText(knowledgeSetting.getFallbackResponse());
    }

    // ==================== 结果落库 ====================

    /**
     * 追加本轮问答到历史消息，并写入 messageList / answer / reasoningContent 上下文。
     */
    private void recordResult(PipelineManage manage, String answerText, List<ChatMessage> historyMessages) {
        historyMessages.add(new UserMessage(manage.chatParams.getMessage()));
        historyMessages.add(new AiMessage(answerText));
        context.put("messageList", manage.formatHistoryMessages(historyMessages));
        manage.context.put("answer", answerText);
        manage.context.put("reasoningContent", context.get("reasoningContent"));
    }

    /**
     * 答案解析结果。
     *
     * @param text   回答文本
     * @param fromAi 是否来自 AI 流式对话（true 时消息已在 execute 内实时推送，无需补发）
     */
    protected record AnswerResult(String text, boolean fromAi) {

        static AnswerResult ofText(String text) {
            return new AnswerResult(text, false);
        }

        static AnswerResult ofAi(String text) {
            return new AnswerResult(text, true);
        }
    }


    protected abstract String execute(String chatId, String chatRecordId, ApplicationVO application, List<ChatMessage> historyMessages, String userPrompt, PipelineManage manage) throws Exception;


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
