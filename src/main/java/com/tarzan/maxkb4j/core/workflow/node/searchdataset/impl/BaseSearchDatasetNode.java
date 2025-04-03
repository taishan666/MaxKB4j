package com.tarzan.maxkb4j.core.workflow.node.searchdataset.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.searchdataset.input.SearchDatasetStepNodeParams;
import com.tarzan.maxkb4j.module.application.entity.DatasetSetting;
import com.tarzan.maxkb4j.module.dataset.service.RetrieveService;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.util.SpringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BaseSearchDatasetNode extends INode {


    private final RetrieveService retrieveService;

    public BaseSearchDatasetNode() {
        this.retrieveService = SpringUtil.getBean(RetrieveService.class);
    }

    @Override
    public String getType() {
        return "search-dataset-node";
    }

    @Override
    public NodeResult execute() {
        SearchDatasetStepNodeParams nodeParams=super.nodeParams.toJavaObject(SearchDatasetStepNodeParams.class);
        DatasetSetting datasetSetting=nodeParams.getDatasetSetting();
        List<String> fields=nodeParams.getQuestionReferenceAddress();
        String question= (String)workflowManage.getReferenceField(fields.get(0),fields.subList(1, fields.size()));
        List<ParagraphVO> paragraphList= retrieveService.paragraphSearch(question,nodeParams.getDatasetIdList(), Collections.emptyList(),datasetSetting.getTopN(),datasetSetting.getSimilarity(),datasetSetting.getSearchMode());
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
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("question", context.get("question"));
        detail.put("paragraph_list", context.get("paragraph_list"));
        return detail;
    }

}
