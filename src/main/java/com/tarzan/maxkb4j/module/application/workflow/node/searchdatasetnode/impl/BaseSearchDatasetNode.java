package com.tarzan.maxkb4j.module.application.workflow.node.searchdatasetnode.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.workflow.NodeResult;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.node.searchdatasetnode.ISearchDatasetStepNode;
import com.tarzan.maxkb4j.module.application.workflow.node.searchdatasetnode.dto.DatasetSetting;
import com.tarzan.maxkb4j.module.application.workflow.node.searchdatasetnode.dto.SearchDatasetStepNodeParams;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.embedding.service.EmbeddingService;
import com.tarzan.maxkb4j.util.SpringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BaseSearchDatasetNode extends ISearchDatasetStepNode {


    private final EmbeddingService embeddingService;

    public BaseSearchDatasetNode() {
        this.embeddingService = SpringUtil.getBean(EmbeddingService.class);
    }
    @Override
    public NodeResult execute(SearchDatasetStepNodeParams nodeParams, FlowParams workflowParams) {
        DatasetSetting datasetSetting=nodeParams.getDatasetSetting();
        List<ParagraphVO> paragraphList= embeddingService.paragraphSearch(workflowParams.getQuestion(),nodeParams.getDatasetIdList(), Collections.emptyList(),datasetSetting.getTopN(),datasetSetting.getSimilarity(),datasetSetting.getSearchMode());
        List<ParagraphVO> isHitHandlingMethodList=paragraphList.stream().filter(ParagraphVO::isHitHandlingMethod).toList();
        Map<String, Object> nodeVariable = Map.of(
                "paragraph_list", paragraphList,
                "is_hit_handling_method_list", isHitHandlingMethodList,
                "data", processParagraphs(paragraphList, datasetSetting.getMaxParagraphCharNumber()),
                "directly_return", directlyReturns(isHitHandlingMethodList),
                "question", workflowParams.getQuestion()
        );
        return new NodeResult(nodeVariable, Map.of());
    }


    public static String resetTitle(String title) {
        if(StringUtils.isNotBlank(title)){
            return "### "+title+"\n";
        }
        return "";
    }


    public  String directlyReturns(List<ParagraphVO> isHitHandlingMethodList) {
        StringBuilder result = new StringBuilder();
        for (ParagraphVO paragraph : isHitHandlingMethodList) {
            String content =paragraph.getContent();
            if (!content.isEmpty()) {
                result.append("\n").append(content);
            }
        }
        return result.toString();
    }

    public  String processParagraphs(List<ParagraphVO> paragraphList, int maxParagraphCharNumber) {
        StringBuilder result = new StringBuilder();
        for (ParagraphVO paragraph : paragraphList) {
            String title = resetTitle(paragraph.getTitle());
            String content =paragraph.getContent();
            // 拼接标题和内容
            if (!title.isEmpty() || !content.isEmpty()) {
                result.append("\n").append(title).append(content);
                // 如果超出最大字符数限制，截断并返回
                if (result.length() > maxParagraphCharNumber) {
                    return result.substring(0, maxParagraphCharNumber);
                }
            }
        }
        // 如果未超出限制，直接返回结果
        return result.toString();
    }

    @Override
    public JSONObject getDetail(int index) {
        JSONObject detail = super.getDetail(index);
        detail.put("question", context.getString("question"));
        detail.put("paragraph_list", context.get("paragraph_list"));
        return detail;
    }

    @Override
    public void saveContext(JSONObject detail, WorkflowManage workflowManage) {
        List<ParagraphVO> result = (List<ParagraphVO>) detail.get("paragraph_list");
        DatasetSetting datasetSetting = super.getNodeParamsClass(nodeParams).getDatasetSetting();

        String directlyReturn = result.stream()
                .filter(paragraph -> "directly_return".equals(paragraph.getHitHandlingMethod()))
                .map(paragraph -> String.format("%s:%s",
                        paragraph.getTitle(),
                        paragraph.getContent()))
                .collect(Collectors.joining("\n"));

        context.put("paragraph_list", result);
        context.put("question", detail.get("question"));
        context.put("run_time", detail.get("run_time"));

/*        List<Map<String, Object>> isHitHandlingMethodList = result.stream()
                .filter(row -> "true".equals(row.get("is_hit_handling_method")))
                .collect(Collectors.toList());
        context.put("is_hit_handling_method_list", isHitHandlingMethodList);*/

        String data = result.stream()
                .map(paragraph -> String.format("%s:%s",
                        paragraph.getTitle(),
                        paragraph.getContent()))
                .collect(Collectors.joining("\n"))
                .substring(0, Math.min(datasetSetting.getMaxParagraphCharNumber(), 5000));

        context.put("data", data);
        context.put("directly_return", directlyReturn);
    }
}
