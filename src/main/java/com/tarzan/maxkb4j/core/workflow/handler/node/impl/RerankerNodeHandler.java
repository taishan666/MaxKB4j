package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.node.impl.RerankerNode;
import com.tarzan.maxkb4j.core.workflow.result.NodeResult;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.model.scoring.ScoringModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@AllArgsConstructor
@Component
public class RerankerNodeHandler implements INodeHandler {

    private final ModelFactory modelFactory;

    @Override
    public NodeResult execute(Workflow workflow, INode node) throws Exception {
        RerankerNode.NodeParams nodeParams = node.getNodeData().toJavaObject(RerankerNode.NodeParams.class);
        List<String> questionReferenceAddress = nodeParams.getQuestionReferenceAddress();
        String question = (String) workflow.getReferenceField(questionReferenceAddress.get(0), questionReferenceAddress.get(1));
        List<List<String>> rerankerReferenceList = nodeParams.getRerankerReferenceList();
        List<TextSegment> textSegments = getRerankerList(workflow,rerankerReferenceList);
        // 获取重排序模型实例
        ScoringModel rerankerModel = modelFactory.build(nodeParams.getRerankerModelId());
        double similarity = nodeParams.getRerankerSetting().getSimilarity();
        Response<List<Double>> response = rerankerModel.scoreAll(textSegments, question);
        List<RerankerNode.RerankResult> documentList = new ArrayList<>();
        List<Double> scores = response.content();
        for (int i = 0; i < textSegments.size(); i++) {
            Double score = scores.get(i);
            TextSegment textSegment = textSegments.get(i);
            Map<String, Object> metadata = textSegment.metadata().toMap();
            metadata.put("relevance_score", score);
            RerankerNode.RerankResult textSegmentResult = new RerankerNode.RerankResult(textSegment.text(), metadata);
            documentList.add(textSegmentResult);
        }
        List<RerankerNode.RerankResult> resultList =documentList.stream().filter(rerankResult -> {
            if (rerankResult.getMetadata().containsKey("relevance_score")){
                Double score = (Double) rerankResult.getMetadata().get("relevance_score");
                return score > similarity;
            }
            return false;
        }).toList();
        int topN = nodeParams.getRerankerSetting().getTopN();
        int endListIndex = Math.min(topN, resultList.size());
        resultList=resultList.subList(0, endListIndex);
        String result = String.join("", resultList.stream().map(RerankerNode.RerankResult::getPageContent).toList());
        int maxParagraphCharNumber = nodeParams.getRerankerSetting().getMaxParagraphCharNumber();
        int endIndex = Math.min(result.length(), maxParagraphCharNumber);
        result = result.substring(0, endIndex);
        node.getDetail().put("question", question);
        node.getDetail().put("documentList", documentList);
        TokenUsage tokenUsage =  response.tokenUsage();
        assert tokenUsage != null;
        node.getDetail().put("documentList", documentList);
        return new NodeResult(Map.of("resultList", resultList,
                "result", result), Map.of());
    }

    @SuppressWarnings("unchecked")
    public List<TextSegment> getRerankerList(Workflow workflow,List<List<String>> rerankerReferenceList) {
        List<TextSegment> textSegments=new ArrayList<>();
        for (List<String> reference : rerankerReferenceList) {
            Object value = workflow.getReferenceField(
                    reference.get(0), // 第1个元素
                    reference.get(1)// 剩余部分
            );
            List<ParagraphVO> paragraphs = (List<ParagraphVO>) value;
            textSegments.addAll(paragraphs.stream()
                    .map(paragraph ->{
                        Map<String,Object> metadata = new HashMap<>();
                        metadata.put("title", paragraph.getTitle());
                        metadata.put("similarity", paragraph.getSimilarity());
                        metadata.put("knowledgeType", paragraph.getKnowledgeType());
                        metadata.put("knowledgeName", paragraph.getKnowledgeName());
                        metadata.put("documentName", paragraph.getDocumentName());
                        metadata.put("isActive", String.valueOf(paragraph.getIsActive()));
                        return TextSegment.from(paragraph.getContent(), Metadata.from(metadata));
                    })
                    .toList());
        }
        return textSegments;
    }
}
