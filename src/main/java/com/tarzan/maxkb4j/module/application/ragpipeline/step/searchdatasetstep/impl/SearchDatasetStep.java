package com.tarzan.maxkb4j.module.application.ragpipeline.step.searchdatasetstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.domian.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.ragpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.ragpipeline.step.searchdatasetstep.ISearchDatasetStep;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.knowledge.service.RetrieveService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@AllArgsConstructor
public class SearchDatasetStep extends ISearchDatasetStep {

    private final RetrieveService retrieveService;

    @Override
    protected List<ParagraphVO> execute(PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        JSONObject context=manage.context;
        ApplicationVO application=(ApplicationVO)context.get("application");
        String problemText=manage.context.getString("problem_text");
        super.context.put("problem_text",problemText);
        String paddingProblemText=context.getString("padding_problem_text");
   //     String execProblemText = StringUtils.isNotBlank(paddingProblemText)?paddingProblemText:problemText;
        KnowledgeSetting datasetSetting=application.getKnowledgeSetting();
        List<CompletableFuture<List<ParagraphVO>>> futureList = new ArrayList<>();
        futureList.add(CompletableFuture.supplyAsync(()->retrieveService.paragraphSearch(problemText,application.getKnowledgeIdList(), List.of(),datasetSetting)));
        if(StringUtils.isNotBlank(paddingProblemText)&&!problemText.equals(paddingProblemText)){
            futureList.add(CompletableFuture.supplyAsync(()->retrieveService.paragraphSearch(paddingProblemText,application.getKnowledgeIdList(), List.of(),datasetSetting)));
        }
        List<ParagraphVO> paragraphList= futureList.stream().flatMap(future-> future.join().stream()).toList();
        if(paragraphList.size()>datasetSetting.getTopN()){
            Map<String, ParagraphVO> map = new LinkedHashMap<>();
            //融合排序
            for (ParagraphVO paragraph : paragraphList) {
                if (map.containsKey(paragraph.getId())) {
                    if (map.get(paragraph.getId()).getComprehensiveScore() < paragraph.getComprehensiveScore()) {
                        map.put(paragraph.getId(), paragraph);
                    }
                } else {
                    map.put(paragraph.getId(), paragraph);
                }
            }
            List<ParagraphVO> results=new ArrayList<>(map.values());
            results.sort(Comparator.comparing(ParagraphVO::getComprehensiveScore).reversed());
            int endIndex = Math.min(datasetSetting.getTopN(), results.size());
            paragraphList= results.subList(0, endIndex);
        }
        log.info("dataset search 耗时 {} ms", System.currentTimeMillis() - startTime);
        return paragraphList;
    }

    @Override
    public JSONObject getDetails() {
        JSONObject details=new JSONObject();
        details.put("step_type","search_step");
        details.put("paragraph_list",super.context.get("paragraph_list"));
        details.put("runTime",super.context.get("runTime"));
        details.put("problem_text",super.context.get("problem_text"));
        details.put("model_name",super.context.get("model_name"));
        details.put("messageTokens",super.context.get("messageTokens"));
        details.put("answerTokens",super.context.get("answerTokens"));
        details.put("cost",0);
        return details;
    }
}
