package com.maxkb4j.workflow.handler.node.impl;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.knowledge.service.IRetrieveService;
import com.maxkb4j.knowledge.vo.ParagraphVO;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.SearchKnowledgeNode;
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
public class SearchKnowledgeNodeHandler extends AbsNodeHandler {

    private final IRetrieveService retrieveService;

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        SearchKnowledgeNode.NodeParams params = parseParams(node, SearchKnowledgeNode.NodeParams.class);
        KnowledgeSetting knowledgeSetting = params.getKnowledgeSetting();
        List<String> fields = params.getQuestionReferenceAddress();
        String question = getReferenceFieldAsString(workflow, fields);

        List<String> excludeParagraphIds = new ArrayList<>();
        if (workflow.getChatParams().getReChat()) {
            excludeParagraphIds = getExcludeParagraphIds(workflow, node.getRuntimeNodeId(), question);
        }

        List<ParagraphVO> paragraphList = retrieveService.paragraphSearch(
                question, params.getKnowledgeIds(), excludeParagraphIds, knowledgeSetting);
        List<ParagraphVO> isHitHandlingMethodList = paragraphList.stream()
                .filter(ParagraphVO::isHitHandlingMethod)
                .toList();

        // 使用辅助方法写入详情
        putDetails(node, Map.of(
                "question", question,
                "showKnowledge", params.getShowKnowledge()
        ));

        return new NodeResult(Map.of(
                "paragraphList", paragraphList,
                "isHitHandlingMethodList", isHitHandlingMethodList,
                "data", processParagraphs(paragraphList, knowledgeSetting.getMaxParagraphCharNumber()),
                "directlyReturn", directlyReturns(isHitHandlingMethodList)
        ));
    }

    @SuppressWarnings("unchecked")
    private List<String> getExcludeParagraphIds(Workflow workflow, String runtimeNodeId, String question) {
        List<String> excludeParagraphIds = new ArrayList<>();
        for (ChatRecordDTO chatRecord : workflow.getHistoryChatRecords()) {
            if (chatRecord.getProblemText().equals(workflow.getChatParams().getMessage())) {
                JSONObject details = chatRecord.getDetails();
                if (!details.isEmpty()) {
                    JSONObject detail = details.getJSONObject(runtimeNodeId);
                    if (question.equals(detail.getString("question"))) {
                        List<ParagraphVO> paragraphList = (List<ParagraphVO>) detail.get("paragraphList");
                        if (!CollectionUtils.isEmpty(paragraphList)) {
                            excludeParagraphIds.addAll(paragraphList.stream().map(ParagraphVO::getId).toList());
                        }
                    }
                }
            }
        }
        return excludeParagraphIds;
    }

    public String resetTitle(String title) {
        if (StringUtils.isNotBlank(title)) {
            return title;
        }
        return "";
    }

    public String directlyReturns(List<ParagraphVO> isHitHandlingMethodList) {
        StringBuilder result = new StringBuilder();
        for (ParagraphVO paragraph : isHitHandlingMethodList) {
            String content = paragraph.getContent();
            if (content != null && !content.isEmpty()) {
                result.append("\n").append(content);
            }
        }
        return result.toString();
    }

    public String processParagraphs(List<ParagraphVO> paragraphList, int maxParagraphCharNumber) {
        StringBuilder result = new StringBuilder();
        for (ParagraphVO paragraph : paragraphList) {
            String title = resetTitle(paragraph.getTitle());
            String content = paragraph.getContent() != null ? paragraph.getContent() : "";
            // 拼接标题和内容
            if ((title != null && !title.isEmpty()) || !content.isEmpty()) {
                result.append(title != null ? title : "").append(content).append("\n");
                // 如果超出最大字符数限制，截断并返回
                if (result.length() > maxParagraphCharNumber) {
                    return result.substring(0, maxParagraphCharNumber);
                }
            }
        }
        if (!result.isEmpty()) {
            result.deleteCharAt(result.length() - 1);
        }
        // 如果未超出限制，直接返回结果
        return result.toString();
    }

}
