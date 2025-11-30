package com.tarzan.maxkb4j.core.chatpipeline.step.resetproblemstep;

import com.tarzan.maxkb4j.core.chatpipeline.IChatPipelineStep;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;

public abstract class IResetProblemStep extends IChatPipelineStep {

    @Override
    protected void _run(PipelineManage manage) {
        ApplicationEntity application = (ApplicationEntity) manage.context.get("application");
        String question = (String) manage.context.get("problemText");
        String paddingProblemText = execute(application, question,manage);
        manage.context.put("paddingProblemText", paddingProblemText);
    }


    protected abstract String execute(ApplicationEntity application,String question,PipelineManage manage);
}
