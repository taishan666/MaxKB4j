package com.tarzan.maxkb4j.module.chatpipeline.searchdatasetstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.chatpipeline.searchdatasetstep.ISearchDatasetStep;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.embedding.service.EmbeddingService;
import com.tarzan.maxkb4j.util.SpringUtil;

import java.util.List;
import java.util.UUID;

public class SearchDatasetStep extends ISearchDatasetStep {


    @Override
    protected List<ParagraphVO> execute(PipelineManage manage) throws Exception {
        System.out.println("SearchDatasetStep: "+manage.getContext());
        JSONObject context=manage.getContext();
        ApplicationEntity application=(ApplicationEntity)context.get("application");
        UUID datasetId=application.getDatasetIdList().get(0);
        String message=(String)manage.getContext().get("message");
        JSONObject datasetSetting=application.getDatasetSetting();
        HitTestDTO hitTestDTO=new HitTestDTO();
        hitTestDTO.setQuery_text(message);
        hitTestDTO.setSearch_mode(datasetSetting.getString("search_mode"));
        hitTestDTO.setSimilarity(datasetSetting.getDouble("similarity"));
        hitTestDTO.setTop_number(datasetSetting.getInteger("top_n"));
        EmbeddingService embeddingService=SpringUtil.getBean(EmbeddingService.class);
        return embeddingService.paragraphSearch(datasetId,hitTestDTO);
    }

    @Override
    public JSONObject getDetails() {
        return new JSONObject();
    }
}
