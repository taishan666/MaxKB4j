package com.maxkb4j.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.knowledge.vo.ParagraphVO;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.RerankerNode;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@NodeHandlerType(NodeType.RERANKER)
@RequiredArgsConstructor
@Component
public class RerankerNodeHandler extends AbsNodeHandler {

    private final IModelProviderService modelFactory;

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        RerankerNode.NodeParams params = parseParams(node, RerankerNode.NodeParams.class);
        List<String> questionReferenceAddress = params.getQuestionReferenceAddress();
        String question = getReferenceFieldAsString(workflow, questionReferenceAddress);
        List<RerankerNode.RerankResult> documentList = new ArrayList<>();
        List<RerankerNode.RerankResult> resultList = new ArrayList<>();
        List<List<String>> rerankerReferenceList = params.getRerankerReferenceList();

        if (CollectionUtils.isNotEmpty(rerankerReferenceList)) {
            double similarity = params.getRerankerSetting().getSimilarity();
            List<TextSegment> textSegments = getRerankerList(workflow, rerankerReferenceList);

            // 获取重排序模型实例
            ScoringModel rerankerModel = modelFactory.buildScoringModel(params.getRerankerModelId());
            Response<List<Double>> response = rerankerModel.scoreAll(textSegments, question);
            List<Double> scores = response.content();

            for (int i = 0; i < textSegments.size(); i++) {
                Double score = scores.get(i);
                TextSegment textSegment = textSegments.get(i);
                Map<String, Object> metadata = textSegment.metadata().toMap();
                metadata.put("relevanceScore", score);
                RerankerNode.RerankResult textSegmentResult = new RerankerNode.RerankResult(textSegment.text(), metadata);
                documentList.add(textSegmentResult);
            }

            resultList = documentList.stream().filter(rerankResult -> {
                if (rerankResult.getMetadata().containsKey("relevanceScore")) {
                    Double score = (Double) rerankResult.getMetadata().get("relevanceScore");
                    return score > similarity;
                }
                return false;
            }).toList();

            int topN = params.getRerankerSetting().getTopN();
            int endListIndex = Math.min(topN, resultList.size());
            resultList = resultList.subList(0, endListIndex);
        }

        String result = String.join("", resultList.stream().map(RerankerNode.RerankResult::getPageContent).toList());
        int maxParagraphCharNumber = params.getRerankerSetting().getMaxParagraphCharNumber();
        int endIndex = Math.min(result.length(), maxParagraphCharNumber);
        result = result.substring(0, endIndex);

        // 使用辅助方法写入详情
        putDetails(node, Map.of(
                "question", question,
                "documentList", documentList
        ));

        return new NodeResult(Map.of(
                "resultList", resultList,
                "result", result
        ));
    }

    @SuppressWarnings("unchecked")
    public List<TextSegment> getRerankerList(Workflow workflow, List<List<String>> rerankerReferenceList) {
        List<TextSegment> textSegments = new ArrayList<>();
        for (List<String> reference : rerankerReferenceList) {
            Object value = workflow.getReferenceField(reference);
            List<ParagraphVO> paragraphs = (List<ParagraphVO>) value;
            textSegments.addAll(paragraphs.stream()
                    .filter(paragraph -> paragraph != null && paragraph.getContent() != null && !paragraph.getContent().isBlank())
                    .map(paragraph -> {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("title", paragraph.getTitle() != null ? paragraph.getTitle() : "");
                        metadata.put("similarity", paragraph.getSimilarity());
                        metadata.put("knowledgeType", paragraph.getKnowledgeType() != null ? paragraph.getKnowledgeType() : "");
                        metadata.put("knowledgeName", paragraph.getKnowledgeName() != null ? paragraph.getKnowledgeName() : "");
                        metadata.put("documentName", paragraph.getDocumentName() != null ? paragraph.getDocumentName() : "");
                        metadata.put("isActive", String.valueOf(paragraph.getIsActive() != null ? paragraph.getIsActive() : false));
                        return TextSegment.from(paragraph.getContent(), Metadata.from(metadata));
                    })
                    .toList());
        }
        return textSegments;
    }
}
