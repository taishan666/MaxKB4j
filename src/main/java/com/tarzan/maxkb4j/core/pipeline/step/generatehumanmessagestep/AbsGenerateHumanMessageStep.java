package com.tarzan.maxkb4j.core.pipeline.step.generatehumanmessagestep;

import com.tarzan.maxkb4j.core.pipeline.AbsStep;
import com.tarzan.maxkb4j.core.pipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.domain.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.application.domain.entity.LlmModelSetting;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;

import java.util.List;

public abstract class AbsGenerateHumanMessageStep extends AbsStep {

    @Override
    @SuppressWarnings("unchecked")
    protected void _run(PipelineManage manage) {
        String problemText = manage.chatParams.getMessage();
        List<ParagraphVO> paragraphList = (List<ParagraphVO>) manage.context.get("paragraphList");
        ApplicationVO application= manage.application;
        LlmModelSetting llmModelSetting = application.getModelSetting();
        KnowledgeSetting knowledgeSetting = application.getKnowledgeSetting();
        String prompt = execute(llmModelSetting, knowledgeSetting,problemText, paragraphList);
        manage.context.put("user_prompt", prompt);
    }

    protected abstract String execute(LlmModelSetting llmModelSetting , KnowledgeSetting knowledgeSetting, String problemText, List<ParagraphVO> paragraphList);
}
