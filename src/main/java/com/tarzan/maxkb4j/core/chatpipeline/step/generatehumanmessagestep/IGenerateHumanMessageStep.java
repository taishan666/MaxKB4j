package com.tarzan.maxkb4j.core.chatpipeline.step.generatehumanmessagestep;

import com.tarzan.maxkb4j.core.chatpipeline.IChatPipelineStep;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;

import java.util.List;

public abstract class IGenerateHumanMessageStep extends IChatPipelineStep {

    @Override
    @SuppressWarnings("unchecked")
    protected void _run(PipelineManage manage) {
        String problemText = (String) manage.context.get("problemText");
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) manage.context.get("paragraphList");
        ApplicationVO application = (ApplicationVO) manage.context.get("application");
        String prompt = execute(application, problemText, paragraphList);
        manage.context.put("user_prompt", prompt);
    }

    protected abstract String execute(ApplicationVO application, String problemText, List<ParagraphVO> paragraphList);
}
