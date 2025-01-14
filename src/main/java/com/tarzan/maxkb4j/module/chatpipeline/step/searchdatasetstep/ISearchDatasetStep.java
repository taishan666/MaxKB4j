package com.tarzan.maxkb4j.module.chatpipeline.step.searchdatasetstep;

import com.tarzan.maxkb4j.module.chatpipeline.IBaseChatPipelineStep;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class ISearchDatasetStep extends IBaseChatPipelineStep {
    @Override
    protected Object _run(PipelineManage manage) {
        try {
            List<ParagraphVO> paragraphList= execute(manage);
            manage.context.put("paragraph_list",paragraphList);
            super.context.put("paragraph_list",paragraphList);
            return paragraphList;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    protected abstract List<ParagraphVO> execute(PipelineManage manage) throws Exception;
}
