package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.SearchKnowledgeNode;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domain.entity.KnowledgeSetting;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.knowledge.service.RetrieveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@NodeHandlerType(NodeType.SEARCH_KNOWLEDGE)
@RequiredArgsConstructor
@Component
public class SearchKnowledgeNodeHandler implements INodeHandler {

    private final RetrieveService retrieveService;
    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        SearchKnowledgeNode.NodeParams nodeParams=node.getNodeData().toJavaObject(SearchKnowledgeNode.NodeParams .class);
        KnowledgeSetting knowledgeSetting=nodeParams.getKnowledgeSetting();
        List<String> fields=nodeParams.getQuestionReferenceAddress();
        String question= (String)workflow.getReferenceField(fields);
        List<String> excludeParagraphIds=new ArrayList<>();
        if (workflow.getChatParams().getReChat()){
            excludeParagraphIds=getExcludeParagraphIds(workflow,node.getRuntimeNodeId(),question);
        }
        List<ParagraphVO> paragraphList= retrieveService.paragraphSearch(question,nodeParams.getKnowledgeIds(),excludeParagraphIds,knowledgeSetting);
        List<ParagraphVO> isHitHandlingMethodList=paragraphList.stream().filter(ParagraphVO::isHitHandlingMethod).toList();
        node.getDetail().put("question",question);
        node.getDetail().put("showKnowledge", nodeParams.getShowKnowledge());
        return new NodeResult(Map.of(
                "paragraphList", paragraphList,
                "isHitHandlingMethodList", isHitHandlingMethodList,
                "data", processParagraphs(paragraphList, knowledgeSetting.getMaxParagraphCharNumber()),
                "directlyReturn", directlyReturns(isHitHandlingMethodList)
        ));
    }

    @SuppressWarnings("unchecked")
    private List<String> getExcludeParagraphIds(Workflow workflow, String runtimeNodeId,String question){
        List<String> excludeParagraphIds=new ArrayList<>();
        for (ApplicationChatRecordEntity chatRecord : workflow.getHistoryChatRecords()) {
            if (chatRecord.getProblemText().equals(workflow.getChatParams().getMessage())){
                JSONObject details=chatRecord.getDetails();
                if (!details.isEmpty()){
                    JSONObject detail= details.getJSONObject(runtimeNodeId);
                    if (question.equals(detail.getString("question"))){
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


    public  String resetTitle(String title) {
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



}
