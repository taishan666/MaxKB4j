package com.maxkb4j.application.pipeline.step.searchdatasetstep;


import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.base.entity.KnowledgeSetting;
import com.maxkb4j.application.pipeline.AbsStep;
import com.maxkb4j.application.pipeline.PipelineManage;
import com.maxkb4j.knowledge.vo.ParagraphVO;

import java.util.List;

public abstract class AbsSearchDatasetStep extends AbsStep {
    @Override
    protected void _run(PipelineManage manage) {
        ApplicationVO application = manage.application;
        String problemText = manage.chatParams.getMessage();
        String paddingProblemText = (String) manage.context.get("paddingProblemText");
        Boolean reChat =  manage.chatParams.getReChat();
        List<String> knowledgeIds= application.getKnowledgeIds();
        KnowledgeSetting datasetSetting = application.getKnowledgeSetting();
        List<ParagraphVO> paragraphList = execute(knowledgeIds,datasetSetting, problemText, paddingProblemText, reChat,manage);
        manage.context.put("paragraphList", paragraphList);
    }

    protected abstract List<ParagraphVO> execute(List<String> knowledgeIds, KnowledgeSetting datasetSetting, String problemText, String paddingProblemText, Boolean reChat, PipelineManage manage);
}
