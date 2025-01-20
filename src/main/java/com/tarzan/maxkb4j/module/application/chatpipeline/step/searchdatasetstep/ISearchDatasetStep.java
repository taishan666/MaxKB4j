package com.tarzan.maxkb4j.module.application.chatpipeline.step.searchdatasetstep;

import com.tarzan.maxkb4j.module.application.chatpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.module.application.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;

import java.util.List;

public abstract class ISearchDatasetStep extends IBaseChatPipelineStep {
    @Override
    protected void _run(PipelineManage manage) {
        List<ParagraphVO> paragraphList = execute(manage);
        manage.context.put("paragraph_list", paragraphList);
        super.context.put("paragraph_list", paragraphList);
        //累加tokens
        int messageTokens=manage.context.getInteger("message_tokens");
        int answerTokens=manage.context.getInteger("answer_tokens");
        int thisMessageTokens=super.context.getInteger("message_tokens");
        int thisAnswerTokens=super.context.getInteger("answer_tokens");
        manage.context.put("message_tokens",messageTokens+thisMessageTokens);
        manage.context.put("answer_tokens",answerTokens+thisAnswerTokens);
    }

    protected abstract List<ParagraphVO> execute(PipelineManage manage);
}
