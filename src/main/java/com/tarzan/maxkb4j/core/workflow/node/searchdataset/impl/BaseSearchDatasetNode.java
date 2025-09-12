package com.tarzan.maxkb4j.core.workflow.node.searchdataset.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.searchdataset.input.SearchDatasetStepNodeParams;
import com.tarzan.maxkb4j.module.application.domian.entity.DatasetSetting;
import com.tarzan.maxkb4j.module.knowledge.service.RetrieveService;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.util.SpringUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.SEARCH_KNOWLEDGE;

public class BaseSearchDatasetNode extends INode {


    private final RetrieveService retrieveService;

    public BaseSearchDatasetNode(JSONObject properties) {
        super(properties);
        this.type = SEARCH_KNOWLEDGE.getKey();
        this.retrieveService = SpringUtil.getBean(RetrieveService.class);
    }


    @Override
    public NodeResult execute() {
        System.out.println(SEARCH_KNOWLEDGE);
        SearchDatasetStepNodeParams nodeParams=super.nodeParams.toJavaObject(SearchDatasetStepNodeParams.class);
        DatasetSetting knowledgeSetting=nodeParams.getKnowledgeSetting();
        List<String> fields=nodeParams.getQuestionReferenceAddress();
        String question= (String)workflowManage.getReferenceField(fields.get(0),fields.subList(1, fields.size()));
        List<ParagraphVO> paragraphList= retrieveService.paragraphSearch(question,nodeParams.getKnowledgeIdList(), Collections.emptyList(),knowledgeSetting);
        List<ParagraphVO> isHitHandlingMethodList=paragraphList.stream().filter(ParagraphVO::isHitHandlingMethod).toList();
        Map<String, Object> nodeVariable = Map.of(
                "paragraphList", paragraphList,
                "isHitHandlingMethodList", isHitHandlingMethodList,
                "data", processParagraphs(paragraphList, knowledgeSetting.getMaxParagraphCharNumber()),
                "directlyReturn", directlyReturns(isHitHandlingMethodList),
                "question", flowParams.getQuestion()
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
