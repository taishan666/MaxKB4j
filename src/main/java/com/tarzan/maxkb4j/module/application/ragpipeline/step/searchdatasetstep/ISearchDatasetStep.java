package com.tarzan.maxkb4j.module.application.ragpipeline.step.searchdatasetstep;

import com.tarzan.maxkb4j.module.application.ragpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.module.application.ragpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.dataset.domain.vo.ParagraphVO;

import java.util.List;

public abstract class ISearchDatasetStep extends IBaseChatPipelineStep {
    @Override
    protected void _run(PipelineManage manage) {
        List<ParagraphVO> paragraphList = execute(manage);
        manage.context.put("paragraph_list", paragraphList);
        super.context.put("paragraph_list", paragraphList);
        //累加tokens
      //  int messageTokens=manage.context.getInteger("messageTokens");
      //  int answerTokens=manage.context.getInteger("answerTokens");
       // int thisMessageTokens=super.context.getInteger("messageTokens");
      //  int thisAnswerTokens=super.context.getInteger("answerTokens");
      //  manage.context.put("messageTokens",messageTokens+thisMessageTokens);
      //  manage.context.put("answerTokens",answerTokens+thisAnswerTokens);
    }

    protected abstract List<ParagraphVO> execute(PipelineManage manage);
}
