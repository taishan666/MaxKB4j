package com.maxkb4j.workflow.handler.node.impl;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.ModelConfig;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.RerankerNode;
import com.maxkb4j.workflow.util.ContextValueUtil;
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

import static com.maxkb4j.workflow.consts.WorkflowConstants.*;


@Slf4j
@NodeHandlerType(NodeType.RERANKER)
@RequiredArgsConstructor
@Component
public class RerankerNodeHandler extends AbsNodeHandler {

    private final IModelProviderService modelFactory;

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        RerankerNode.NodeParams params = parseParams(node, RerankerNode.NodeParams.class);
        List<String> questionReferenceAddress = params.getQuestionReferenceAddress();
        String question = getReferenceFieldAsString(workflow, questionReferenceAddress);
        List<RerankerNode.RerankResult> documentList = new ArrayList<>();
        List<RerankerNode.RerankResult> resultList = new ArrayList<>();
        List<List<String>> rerankerReferenceList = params.getRerankerReferenceList();

        if (CollectionUtils.isNotEmpty(rerankerReferenceList)) {
            double similarity = params.getRerankerSetting().getSimilarity();
            List<TextSegment> textSegments = getRerankerList(workflow, rerankerReferenceList);
            if (question == null || question.isBlank() || textSegments.isEmpty()) {
                // 问题解析为 null/空，或所有引用路径均无可用段落（上游被 SKIP/配置错误/恢复后 JSON 类型已由
                // ContextValueUtil.toList 兜底为空列表）时，跳过远程重排序调用，返回空结果，
                // 避免打分模型对空输入抛出与真实原因无关的异常
                log.warn("Reranker node {} skips scoring: question blank = {}, usable paragraphs = {}",
                        node.getId(), question == null || question.isBlank(), textSegments.size());
                return new NodeResult(Map.of(NodeField.RESULT_LIST, resultList, NodeField.RESULT, ""));
            }
            ModelConfig modelConfig = resolveModelConfig(workflow, params);
            // 获取重排序模型实例
            ScoringModel rerankerModel = modelFactory.buildScoringModel(modelConfig.getModelId());
            Response<List<Double>> response = rerankerModel.scoreAll(textSegments, question);
            List<Double> scores = response.content();

            if (scores == null || scores.isEmpty()) {
                log.warn("Reranker returned no scores");
                return new NodeResult(Map.of(NodeField.RESULT_LIST, resultList, NodeField.RESULT, ""));
            }
            int scoreSize = scores.size();
            for (int i = 0; i < textSegments.size(); i++) {
                if (i >= scoreSize) {
                    break; // scores 与 textSegments 长度不一致时安全截断
                }
                Double score = scores.get(i);
                TextSegment textSegment = textSegments.get(i);
                Map<String, Object> metadata = textSegment.metadata().toMap();
                metadata.put(NodeField.RELEVANCE_SCORE, score);
                RerankerNode.RerankResult textSegmentResult = new RerankerNode.RerankResult(textSegment.text(), metadata);
                documentList.add(textSegmentResult);
            }

            resultList = documentList.stream().filter(rerankResult -> {
                if (rerankResult.getMetadata().containsKey(NodeField.RELEVANCE_SCORE)) {
                    Double score = (Double) rerankResult.getMetadata().get(NodeField.RELEVANCE_SCORE);
                    return score > similarity;
                }
                return false;
            }).sorted((left, right) -> Double.compare(
                    (Double) right.getMetadata().get(NodeField.RELEVANCE_SCORE),
                    (Double) left.getMetadata().get(NodeField.RELEVANCE_SCORE)
            )).toList();

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
                NodeField.QUESTION, question,
                NodeField.DOCUMENT_LIST, documentList
        ));

        return new NodeResult(Map.of(
                NodeField.RESULT_LIST, resultList,
                NodeField.RESULT, result
        ));
    }

    public List<TextSegment> getRerankerList(IWorkflow workflow, List<List<String>> rerankerReferenceList) {
        List<TextSegment> textSegments = new ArrayList<>();
        for (List<String> reference : rerankerReferenceList) {
            // 引用值可能来自恢复的上下文（JSONArray/JSONObject）或为 null（上游被 SKIP 等），需安全重建
            List<ParagraphRagVO> paragraphs = ContextValueUtil.toList(workflow.getReferenceField(reference), ParagraphRagVO.class);
            textSegments.addAll(paragraphs.stream()
                    .filter(paragraph -> paragraph != null && paragraph.getContent() != null && !paragraph.getContent().isBlank())
                    .map(paragraph -> {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put(MetadataField.TITLE, paragraph.getTitle() != null ? paragraph.getTitle() : "");
                        metadata.put(MetadataField.SIMILARITY, paragraph.getSimilarity() != null ? paragraph.getSimilarity() : 0D);
                        metadata.put(MetadataField.KNOWLEDGE_TYPE, paragraph.getKnowledgeType() != null ? paragraph.getKnowledgeType() : "");
                        metadata.put(MetadataField.KNOWLEDGE_NAME, paragraph.getKnowledgeName() != null ? paragraph.getKnowledgeName() : "");
                        metadata.put(MetadataField.DOCUMENT_NAME, paragraph.getDocumentName() != null ? paragraph.getDocumentName() : "");
                        metadata.put(MetadataField.IS_ACTIVE, String.valueOf(paragraph.getIsActive() != null ? paragraph.getIsActive() : false));
                        return TextSegment.from(paragraph.getContent(), Metadata.from(metadata));
                    })
                    .toList());
        }
        return textSegments;
    }
}
