package com.tarzan.maxkb4j.core.chatpipeline.step.searchdatasetstep;

import com.tarzan.maxkb4j.core.chatpipeline.IChatPipelineStep;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.domian.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;

import java.util.List;

public abstract class ISearchDatasetStep extends IChatPipelineStep {
    @Override
    protected void _run(PipelineManage manage) {
        ApplicationVO application = manage.application;
        String problemText = manage.chatParams.getMessage();
        String paddingProblemText = (String) manage.context.get("paddingProblemText");
        Boolean reChat =  manage.chatParams.getReChat();
        List<String> knowledgeIdList = application.getKnowledgeIdList();
        KnowledgeSetting datasetSetting = application.getKnowledgeSetting();
        List<ParagraphVO> paragraphList = execute(knowledgeIdList,datasetSetting, problemText, paddingProblemText, reChat,manage);
        manage.context.put("paragraphList", paragraphList);
    }

    protected abstract List<ParagraphVO> execute(List<String> knowledgeIdList,KnowledgeSetting datasetSetting,String problemText,String paddingProblemText,Boolean reChat,PipelineManage manage);
}
