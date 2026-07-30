package com.maxkb4j.application.pipeline.step.searchdatasetstep.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.application.pipeline.PipelineManage;
import com.maxkb4j.application.pipeline.step.searchdatasetstep.AbsSearchDatasetStep;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.knowledge.service.IKnowledgeService;
import com.maxkb4j.knowledge.service.IRetrieveService;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;
import com.maxkb4j.model.service.IModelProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchDatasetStep extends AbsSearchDatasetStep {

    private final IRetrieveService retrieveService;
    private final IKnowledgeService knowledgeService;
    private final IModelProviderService modelFactory;
    private final TaskExecutor taskExecutor;

    @Override
    protected List<ParagraphRagVO> execute(List<String> knowledgeIds, KnowledgeSetting datasetSetting, String problemText, String paddingProblemText, Boolean reChat, PipelineManage manage) {
        long startTime = System.currentTimeMillis();
        List<ParagraphRagVO> paragraphList= new ArrayList<>();
        if (CollectionUtils.isNotEmpty(knowledgeIds)){
    /*        if(Boolean.TRUE.equals(datasetSetting.getOnDemandEnable())){
                ApplicationVO application = manage.application;
                String modelId = application.getModelId();
                JSONObject modelParams = application.getModelParamsSetting();
                ChatModel chatModel = modelFactory.buildChatModel(modelId,modelParams);
                RouterAssistant assistant = AiServiceFactory.builder(RouterAssistant.class)
                        .chatModel(chatModel)
                        .build();
                List<String> options=new ArrayList<>();
                List<KnowledgeSimple> knowledgeList =knowledgeService.listSimpleKnowledgeByIds(knowledgeIds);
                Map<String, String> idToClassification=new HashMap<>();
                for (int i = 0; i < knowledgeList.size(); i++) {
                    KnowledgeSimple knowledge=knowledgeList.get(i);
                    int id = i + 1;
                    options.add(id+ ":" + knowledge.getName()+"("+knowledge.getDesc()+")");
                    idToClassification.put(String.valueOf(id), knowledge.getId());
                }
                Result<List<String>> result = assistant.route(String.join("\n", options), problemText);
                List<String> classificationIds = result.content();
                for (String classificationId : classificationIds) {
                    if (!idToClassification.containsKey(classificationId)){
                         String knowledgeId = idToClassification.get(classificationId);
                         if (knowledgeId != null){
                             knowledgeIds.remove(knowledgeId);
                         }
                    }
                }
                TokenUsage tokenUsage=result.tokenUsage();
                super.context.put("messageTokens", tokenUsage.inputTokenCount());
                super.context.put("answerTokens", tokenUsage.outputTokenCount());
            }*/
            if(!Boolean.TRUE.equals(datasetSetting.getOnDemandEnable())){
                List<String> excludeParagraphIds = reChat ? manage.getExcludeParagraphIds(problemText) : List.of();
                paragraphList = retrieval(knowledgeIds,datasetSetting, problemText, paddingProblemText, reChat,excludeParagraphIds);
            }
        }
        log.info("dataset search 耗时 {} ms", System.currentTimeMillis() - startTime);
        super.context.put("paragraphList", paragraphList);
        super.context.put("problemText", problemText);
        return paragraphList;
    }

    protected List<ParagraphRagVO> retrieval(List<String> knowledgeIds, KnowledgeSetting datasetSetting, String problemText, String paddingProblemText, Boolean reChat,List<String> excludeParagraphIds) {
        List<CompletableFuture<List<ParagraphRagVO>>> futureList = new ArrayList<>();
        CompletableFuture<List<ParagraphRagVO>> future = CompletableFuture.supplyAsync(() -> retrieveService.paragraphSearch(problemText, knowledgeIds, excludeParagraphIds, datasetSetting), taskExecutor);
        futureList.add(future);
        if (StringUtils.isNotBlank(paddingProblemText) && !problemText.equals(paddingProblemText)) {
            futureList.add(CompletableFuture.supplyAsync(() -> retrieveService.paragraphSearch(paddingProblemText, knowledgeIds, excludeParagraphIds, datasetSetting)));
        }
        List<ParagraphRagVO> paragraphList= futureList.stream().flatMap(f -> f.join().stream()).toList();
        //当有优化的问题时
        if (paragraphList.size() > datasetSetting.getTopN()) {
            Map<String, ParagraphRagVO> map = new LinkedHashMap<>();
            //融合排序
            for (ParagraphRagVO paragraph : paragraphList) {
                if (map.containsKey(paragraph.getId())) {
                    if (map.get(paragraph.getId()).getSimilarity() < paragraph.getSimilarity()) {
                        map.put(paragraph.getId(), paragraph);
                    }
                } else {
                    map.put(paragraph.getId(), paragraph);
                }
            }
            List<ParagraphRagVO> results = new ArrayList<>(map.values());
            results.sort(Comparator.comparing(ParagraphRagVO::getSimilarity).reversed());
            int endIndex = Math.min(datasetSetting.getTopN(), results.size());
            return results.subList(0, endIndex);
        }
        return paragraphList;
    }


    @Override
    public JSONObject getDetails() {
        JSONObject details = new JSONObject(true);
        details.put("step_type", "search_step");
        details.put("paragraphList", context.get("paragraphList"));
        details.put("runTime", context.get("runTime"));
        details.put("problemText", context.get("problemText"));
        details.put("messageTokens", context.getOrDefault("messageTokens", 0));
        details.put("answerTokens", context.getOrDefault("answerTokens", 0));
        return details;
    }
}
