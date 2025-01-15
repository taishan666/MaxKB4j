package com.tarzan.maxkb4j.module.chatpipeline.step.resetproblemstep;

import com.tarzan.maxkb4j.module.chatpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;

public abstract class IResetProblemStep extends IBaseChatPipelineStep {

    @Override
    protected void _run(PipelineManage manage) {
        String paddingProblemText = execute(manage);
        manage.context.put("padding_problem_text", paddingProblemText);
        //累加tokens
        int messageTokens=manage.context.getInteger("message_tokens");
        int answerTokens=manage.context.getInteger("answer_tokens");
        int thisMessageTokens=super.context.getInteger("message_tokens");
        int thisAnswerTokens=super.context.getInteger("answer_tokens");
        manage.context.put("message_tokens",messageTokens+thisMessageTokens);
        manage.context.put("answer_tokens",answerTokens+thisAnswerTokens);
    }

    protected abstract String execute(PipelineManage manage);
}
