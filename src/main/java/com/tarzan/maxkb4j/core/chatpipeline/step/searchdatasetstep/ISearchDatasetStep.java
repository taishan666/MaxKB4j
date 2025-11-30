package com.tarzan.maxkb4j.core.chatpipeline.step.searchdatasetstep;

import com.tarzan.maxkb4j.core.chatpipeline.IChatPipelineStep;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;

import java.util.List;

public abstract class ISearchDatasetStep extends IChatPipelineStep {
    @Override
    protected void _run(PipelineManage manage) {
        ApplicationVO application = (ApplicationVO) manage.context.get("application");
        String problemText = (String) manage.context.get("problemText");
        String paddingProblemText = (String) manage.context.get("paddingProblemText");
        Boolean reChat = (Boolean) manage.context.get("reChat");
        List<ParagraphVO> paragraphList = execute(application, problemText, paddingProblemText, reChat,manage);
        manage.context.put("paragraphList", paragraphList);
    }

    protected abstract List<ParagraphVO> execute(ApplicationVO application,String problemText,String paddingProblemText,Boolean reChat,PipelineManage manage);
}
