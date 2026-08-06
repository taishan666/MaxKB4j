package com.maxkb4j.workflow.handler.node.impl;

import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.core.support.RagContentInjector;
import com.maxkb4j.knowledge.service.IRetrieveService;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.enums.ValueType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.SearchKnowledgeNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@NodeHandlerType(NodeType.SEARCH_KNOWLEDGE)
@RequiredArgsConstructor
@Component
public class SearchKnowledgeNodeHandler extends AbsNodeHandler {

    private final IRetrieveService retrieveService;
    public static final RagContentInjector contentInjector = new RagContentInjector();

    @Override
    @SuppressWarnings("unchecked")
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        SearchKnowledgeNode.NodeParams params = parseParams(node, SearchKnowledgeNode.NodeParams.class);
        List<String> knowledgeIds = params.getKnowledgeIds();
        if (ValueType.referencing.name().equals(params.getSearchScopeType())) {
            List<String> fields = params.getSearchScopeReference();
            Object value = workflow.getReferenceField(fields);
            if (value instanceof List) {
                knowledgeIds= (List<String>) value;
            }
        }
        KnowledgeSetting knowledgeSetting = params.getKnowledgeSetting();
        List<String> fields = params.getQuestionReferenceAddress();
        String question = getReferenceFieldAsString(workflow, fields);
        List<String> excludeParagraphIds = new ArrayList<>();
      /*  if (workflow.getChatParams().getReChat()) {
            excludeParagraphIds = getExcludeParagraphIds(workflow, node.getRuntimeNodeId(), question);
        }*/
        List<ParagraphRagVO> paragraphList = retrieveService.paragraphSearch(
                question, knowledgeIds, excludeParagraphIds, knowledgeSetting);
        List<ParagraphRagVO> isHitHandlingMethodList = paragraphList.stream()
                .filter(ParagraphRagVO::returnIfSatisfied)
                .toList();
        // 使用辅助方法写入详情
        putDetails(node, Map.of(
                "question", question,
                "showKnowledge", params.getShowKnowledge()
        ));
        int maxParagraphCharNumber = knowledgeSetting.getMaxParagraphCharNumber();
        return new NodeResult(Map.of(
                "paragraphList", paragraphList,
                "isHitHandlingMethodList", isHitHandlingMethodList,
                "data", contentInjector.format(paragraphList, maxParagraphCharNumber),
                "directlyReturn", directlyReturns(isHitHandlingMethodList)
        ));
    }

/*    @SuppressWarnings("unchecked")
    private List<String> getExcludeParagraphIds(IWorkflow workflow, String runtimeNodeId, String question) {
        List<String> excludeParagraphIds = new ArrayList<>();
        for (ChatRecordDTO chatRecord : workflow.getHistoryChatRecords()) {
            if (Objects.equals(chatRecord.getProblemText(), workflow.getChatParams().getMessage())) {
                JSONObject details = chatRecord.getDetails();
                if (details != null && !details.isEmpty()) {
                    JSONObject detail = details.getJSONObject(runtimeNodeId);
                    if (detail != null && Objects.equals(question, detail.getString("question"))) {
                        List<ParagraphRagVO> paragraphList = (List<ParagraphRagVO>) detail.get("paragraphList");
                        if (!CollectionUtils.isEmpty(paragraphList)) {
                            excludeParagraphIds.addAll(paragraphList.stream().map(ParagraphRagVO::getId).toList());
                        }
                    }
                }
            }
        }
        return excludeParagraphIds;
    }*/

    public String directlyReturns(List<ParagraphRagVO> isHitHandlingMethodList) {
        StringBuilder result = new StringBuilder();
        for (ParagraphRagVO paragraph : isHitHandlingMethodList) {
            String content = paragraph.getContent();
            if (content != null && !content.isEmpty()) {
                result.append("\n").append(content);
            }
        }
        return result.toString();
    }

}
