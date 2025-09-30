package com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep;

import com.tarzan.maxkb4j.core.chatpipeline.IChatPipelineStep;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;

public abstract class IResetProblemStep extends IChatPipelineStep {

    @Override
    protected void _run(PipelineManage manage) {
        String paddingProblemText = execute(manage);
        manage.context.put("padding_problem_text", paddingProblemText);
        //累加tokens
        int messageTokens=manage.context.getInteger("messageTokens");
        int answerTokens=manage.context.getInteger("answerTokens");
        int thisMessageTokens=super.context.getInteger("messageTokens");
        int thisAnswerTokens=super.context.getInteger("answerTokens");
        manage.context.put("messageTokens",messageTokens+thisMessageTokens);
        manage.context.put("answerTokens",answerTokens+thisAnswerTokens);
    }

    protected abstract String execute(PipelineManage manage);
}
