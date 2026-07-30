package com.maxkb4j.application.pipeline.step.generatehumanmessagestep;


import com.maxkb4j.application.pipeline.AbsStep;
import com.maxkb4j.application.pipeline.PipelineManage;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.common.mp.entity.LlmModelSetting;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;

import java.util.List;

public abstract class AbsGenerateHumanMessageStep extends AbsStep {

    @Override
    @SuppressWarnings("unchecked")
    protected void _run(PipelineManage manage) {
        String problemText = manage.chatParams.getMessage();
        List<ParagraphRagVO> paragraphList = (List<ParagraphRagVO>) manage.context.get("paragraphList");
        ApplicationVO application= manage.application;
        LlmModelSetting llmModelSetting = application.getModelSetting();
        KnowledgeSetting knowledgeSetting = application.getKnowledgeSetting();
        String prompt = execute(llmModelSetting, knowledgeSetting,problemText, paragraphList);
        manage.context.put("userPrompt", prompt);
    }

    protected abstract String execute(LlmModelSetting llmModelSetting , KnowledgeSetting knowledgeSetting, String problemText, List<ParagraphRagVO> paragraphList);
}
