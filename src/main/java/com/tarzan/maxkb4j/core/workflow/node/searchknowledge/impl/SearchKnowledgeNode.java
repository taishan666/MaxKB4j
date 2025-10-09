package com.tarzan.maxkb4j.core.workflow.node.searchknowledge.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.util.SpringUtil;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.node.searchknowledge.input.SearchKnowledgeNodeParams;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.knowledge.service.RetrieveService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.SEARCH_KNOWLEDGE;

public class SearchKnowledgeNode extends INode {


    private final RetrieveService retrieveService;

    public SearchKnowledgeNode(JSONObject properties) {
        super(properties);
        this.type = SEARCH_KNOWLEDGE.getKey();
        this.retrieveService = SpringUtil.getBean(RetrieveService.class);
    }


    @Override
    public NodeResult execute() {
        SearchKnowledgeNodeParams nodeParams=super.getNodeData().toJavaObject(SearchKnowledgeNodeParams.class);
        KnowledgeSetting knowledgeSetting=nodeParams.getKnowledgeSetting();
        List<String> fields=nodeParams.getQuestionReferenceAddress();
        String question= (String)super.getReferenceField(fields.get(0),fields.get(1));
        List<String> excludeParagraphIds=new ArrayList<>();
        if (super.getChatParams().getReChat()){
            excludeParagraphIds=getExcludeParagraphIds(question);
        }
        List<ParagraphVO> paragraphList= retrieveService.paragraphSearch(question,nodeParams.getKnowledgeIdList(),excludeParagraphIds,knowledgeSetting);
        List<ParagraphVO> isHitHandlingMethodList=paragraphList.stream().filter(ParagraphVO::isHitHandlingMethod).toList();
        detail.put("question",question);
        detail.put("showKnowledge", nodeParams.getShowKnowledge());//todo 获取对话记录时会用
        return new NodeResult(Map.of(
                "paragraphList", paragraphList,
                "isHitHandlingMethodList", isHitHandlingMethodList,
                "data", processParagraphs(paragraphList, knowledgeSetting.getMaxParagraphCharNumber()),
                "directlyReturn", directlyReturns(isHitHandlingMethodList)
        ), Map.of());
    }

    private List<String> getExcludeParagraphIds(String question){
        List<String> excludeParagraphIds=new ArrayList<>();
        for (ApplicationChatRecordEntity chatRecord : super.getHistoryChatRecords()) {
            JSONObject details=chatRecord.getDetails();
            if (!details.isEmpty()){
                for (String key : details.keySet()) {
                    JSONObject detail= details.getJSONObject(key);
                    if (question.equals(detail.getString("question"))&&type.equals(detail.getString("type"))){
                        @SuppressWarnings("unchecked")
                        List<ParagraphVO> paragraphList= (List<ParagraphVO>) detail.get("paragraphList");
                        if (!CollectionUtils.isEmpty(paragraphList)){
                            excludeParagraphIds.addAll(paragraphList.stream().map(ParagraphVO::getId).toList());
                        }
                    }
                }
            }
        }
        return excludeParagraphIds;
    }



    public static String resetTitle(String title) {
        if(StringUtils.isNotBlank(title)){
            return title;
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
                result.append(title).append(content).append("\n");
                // 如果超出最大字符数限制，截断并返回
                if (result.length() > maxParagraphCharNumber) {
                    return result.substring(0, maxParagraphCharNumber);
                }
            }
        }
        if (!result.isEmpty()){
            result.deleteCharAt(result.length() - 1);
        }
        // 如果未超出限制，直接返回结果
        return result.toString();
    }


    @Override
    public void saveContext(JSONObject detail) {
        context.put("paragraphList", detail.get("paragraphList"));
        context.put("isHitHandlingMethodList", detail.get("isHitHandlingMethodList"));
        context.put("data", detail.get("data"));
        context.put("directlyReturn", detail.get("directlyReturn"));
    }

    @Override
    public JSONObject getRunDetail() {
        return detail;
    }

}
