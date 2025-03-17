package com.tarzan.maxkb4j.module.application.chatpipeline.step.searchdatasetstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.chatpipeline.step.searchdatasetstep.ISearchDatasetStep;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.entity.DatasetSetting;
import com.tarzan.maxkb4j.module.dataset.service.RetrieveService;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@AllArgsConstructor
public class SearchDatasetStep extends ISearchDatasetStep {

    private final RetrieveService retrieveService;

    @Override
    protected List<ParagraphVO> execute(PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        JSONObject context=manage.context;
        ApplicationEntity application=(ApplicationEntity)context.get("application");
        super.context.put("model_name","test_model");
        String problemText=manage.context.getString("problem_text");
        super.context.put("problem_text",problemText);
        String paddingProblemText=context.getString("padding_problem_text");
        String execProblemText = StringUtils.isNotBlank(paddingProblemText)?paddingProblemText:problemText;
        DatasetSetting datasetSetting=application.getDatasetSetting();
        List<ParagraphVO> paragraphList= retrieveService.paragraphSearch(execProblemText,application.getDatasetIdList(), Collections.emptyList(),datasetSetting.getTopN(),datasetSetting.getSimilarity(),datasetSetting.getSearchMode());
        System.out.println("search 耗时 "+(System.currentTimeMillis()-startTime)+" ms");
        super.context.put("message_tokens",0);
        super.context.put("answer_tokens",0);
        return paragraphList;
    }

    @Override
    public JSONObject getDetails() {
        JSONObject details=new JSONObject();
        details.put("step_type","search_step");
        details.put("paragraph_list",super.context.get("paragraph_list"));
        details.put("run_time",super.context.get("run_time"));
        details.put("problem_text",super.context.get("problem_text"));
        details.put("model_name",super.context.get("model_name"));
        details.put("message_tokens",super.context.get("message_tokens"));
        details.put("answer_tokens",super.context.get("answer_tokens"));
        details.put("cost",0);
        return details;
    }
}
