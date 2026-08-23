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
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;


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
                NodeField.QUESTION, question,
                KnowledgeField.SHOW_KNOWLEDGE, params.getShowKnowledge()
        ));
        int maxParagraphCharNumber = knowledgeSetting.getMaxParagraphCharNumber();
        return new NodeResult(Map.of(
                NodeField.PARAGRAPH_LIST, paragraphList,
                NodeField.IS_HIT_HANDLING_METHOD_LIST, isHitHandlingMethodList,
                NodeField.DATA, contentInjector.format(paragraphList, maxParagraphCharNumber),
                NodeField.DIRECTLY_RETURN, directlyReturns(isHitHandlingMethodList)
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
                    if (detail != null && Objects.equals(question, detail.getString(NodeField.QUESTION))) {
                        List<ParagraphRagVO> paragraphList = (List<ParagraphRagVO>) detail.get(NodeField.PARAGRAPH_LIST);
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
