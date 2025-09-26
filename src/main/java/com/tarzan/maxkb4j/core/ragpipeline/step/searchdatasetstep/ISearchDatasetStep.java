package com.tarzan.maxkb4j.core.ragpipeline.step.searchdatasetstep;

import com.tarzan.maxkb4j.core.ragpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.core.ragpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;

import java.util.List;

public abstract class ISearchDatasetStep extends IBaseChatPipelineStep {
    @Override
    protected void _run(PipelineManage manage) {
        List<ParagraphVO> paragraphList = execute(manage);
        manage.context.put("paragraphList", paragraphList);
        super.context.put("paragraphList", paragraphList);
    }

    protected abstract List<ParagraphVO> execute(PipelineManage manage);
}
