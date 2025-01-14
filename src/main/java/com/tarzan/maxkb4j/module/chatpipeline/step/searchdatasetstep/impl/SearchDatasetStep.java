package com.tarzan.maxkb4j.module.chatpipeline.step.searchdatasetstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.chatpipeline.step.searchdatasetstep.ISearchDatasetStep;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.embedding.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class SearchDatasetStep extends ISearchDatasetStep {

    @Autowired
    private EmbeddingService embeddingService;

    @Override
    protected List<ParagraphVO> execute(PipelineManage manage) throws Exception {
        JSONObject context=manage.context;
        ApplicationEntity application=(ApplicationEntity)context.get("application");
        super.context.put("model_name","test_model");
        UUID datasetId=application.getDatasetIdList().get(0);
        String problem=manage.context.getString("problem_text");
        super.context.put("problem_text",problem);
        JSONObject datasetSetting=application.getDatasetSetting();
        HitTestDTO hitTestDTO=new HitTestDTO();
        hitTestDTO.setQuery_text(problem);
        hitTestDTO.setSearch_mode(datasetSetting.getString("search_mode"));
        hitTestDTO.setSimilarity(datasetSetting.getDouble("similarity"));
        hitTestDTO.setTop_number(datasetSetting.getInteger("top_n"));
       // EmbeddingService embeddingService=SpringUtil.getBean(EmbeddingService.class);
        return embeddingService.paragraphSearch(datasetId,hitTestDTO);
    }

    @Override
    public JSONObject getDetails() {
        JSONObject details=new JSONObject();
        details.put("step_type","search_step");
        details.put("paragraph_list",super.context.getJSONArray("paragraph_list"));
        details.put("run_time",super.context.getLong("run_time"));
        details.put("problem_text",super.context.getString("problem_text"));
        details.put("model_name",super.context.get("model_name"));
        details.put("message_tokens",0);
        details.put("answer_tokens",0);
        details.put("cost",0);
        return details;
    }
}
