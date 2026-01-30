package com.tarzan.maxkb4j.core.pipeline.step.searchdatasetstep;

import com.tarzan.maxkb4j.core.pipeline.AbsStep;
import com.tarzan.maxkb4j.core.pipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.domain.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;

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

    protected abstract List<ParagraphVO> execute(List<String> knowledgeIds,KnowledgeSetting datasetSetting,String problemText,String paddingProblemText,Boolean reChat,PipelineManage manage);
}
