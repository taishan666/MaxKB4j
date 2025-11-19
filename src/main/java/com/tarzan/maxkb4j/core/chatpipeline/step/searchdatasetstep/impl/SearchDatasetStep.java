package com.tarzan.maxkb4j.core.chatpipeline.step.searchdatasetstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.core.chatpipeline.step.searchdatasetstep.ISearchDatasetStep;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.knowledge.service.RetrieveService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
        ApplicationVO application=(ApplicationVO)manage.context.get("application");
        String problemText= (String) manage.context.get("problemText");
        String paddingProblemText= (String) manage.context.get("paddingProblemText");
        Boolean reChat=(Boolean)manage.context.get("reChat");
        List<String> excludeParagraphIds;
        if (reChat) {
            @SuppressWarnings("unchecked")
            List<ApplicationChatRecordEntity> historyChatRecords= (List<ApplicationChatRecordEntity>) manage.context.get("chatRecordList");
            excludeParagraphIds=getExcludeParagraphIds(historyChatRecords, problemText);
        } else {
            excludeParagraphIds = new ArrayList<>();
        }
        KnowledgeSetting datasetSetting=application.getKnowledgeSetting();
        List<CompletableFuture<List<ParagraphVO>>> futureList = new ArrayList<>();
        futureList.add(CompletableFuture.supplyAsync(()->retrieveService.paragraphSearch(problemText,application.getKnowledgeIdList(), excludeParagraphIds,datasetSetting)));
        if(StringUtils.isNotBlank(paddingProblemText)&&!problemText.equals(paddingProblemText)){
            futureList.add(CompletableFuture.supplyAsync(()->retrieveService.paragraphSearch(paddingProblemText,application.getKnowledgeIdList(), excludeParagraphIds,datasetSetting)));
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
        context.put("paragraphList",paragraphList);
        context.put("problemText",problemText);
        return paragraphList;
    }

    private List<String> getExcludeParagraphIds(List<ApplicationChatRecordEntity> chatRecordList, String problemText){
        List<String> excludeParagraphIds=new ArrayList<>();
        if (!CollectionUtils.isEmpty(chatRecordList)){
            for (ApplicationChatRecordEntity chatRecord : chatRecordList) {
                JSONObject details=chatRecord.getDetails();
                if (!details.isEmpty()){
                    if (problemText.equals(chatRecord.getProblemText())&&details.containsKey("search_step")){
                        JSONObject searchStep=details.getJSONObject("search_step");
                        @SuppressWarnings("unchecked")
                        List<ParagraphVO> paragraphList= (List<ParagraphVO>) searchStep.get("paragraphList");
                        if (!CollectionUtils.isEmpty(paragraphList)){
                            excludeParagraphIds.addAll(paragraphList.stream().map(ParagraphVO::getId).toList());
                        }
                    }
                }
            }
        }
        return excludeParagraphIds;
    }

    @Override
    public JSONObject getDetails() {
        JSONObject details=new JSONObject();
        details.put("step_type","search_step");
        details.put("paragraphList",context.get("paragraphList"));
        details.put("runTime",context.get("runTime"));
        details.put("problemText",context.get("problemText"));
        details.put("messageTokens",context.getOrDefault("messageTokens",0));
        details.put("answerTokens",context.getOrDefault("answerTokens",0));
        return details;
    }
}
