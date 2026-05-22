package com.maxkb4j.knowledge.store;

import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.entity.ParagraphKeywords;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.GraphKeywordService;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component("graphStore")
@RequiredArgsConstructor
public class GraphStoreImpl implements IDataStore {

    private final GraphKeywordService graphKeywordService;
    private final MongoTemplate mongoTemplate;

    @Override
    public void upsert(EmbeddingModel model, List<EmbeddingEntity> entities) {
        log.debug("GraphStore upsert called - graph data is managed through GraphExtractionService");
    }

    @Override
    public void deleteByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId) {
        // Graph store doesn't store problem-based data
    }

    @Override
    public void deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        // Graph store doesn't store problem-based data
    }

    @Override
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
    }

    @Override
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
    }

    @Override
    public void updateActiveStatus(String knowledgeId, String paragraphId, boolean isActive) {
    }

    @Override
    public List<TextChunkVO> search(SearchRequest request) {
        if (CollectionUtils.isEmpty(request.getKnowledgeIds())) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(request.getQuery())) {
            return Collections.emptyList();
        }
        // Step 1: Extract dual-level keywords
        request.setChatModelId("296a3398e194eb8843f59930cb0f9308");
        List<String> keywords = graphKeywordService.extractKeywords(request.getChatModelId(), request.getQuery());
        List<Criteria> criteriaList = new ArrayList<>();
        //  criteriaList.add(Criteria.where("name").in(keywords));
        for (String keyword : keywords) {
            criteriaList.add(Criteria.where("keywords").regex(Pattern.compile(keyword, Pattern.CASE_INSENSITIVE)));
        }
        List<ParagraphKeywords> paragraphs = mongoTemplate.find(
                Query.query(Criteria.where("knowledgeId").in(request.getKnowledgeIds())
                        .and("isActive").is(true)
                        .orOperator(criteriaList.toArray(new Criteria[0]))),
                ParagraphKeywords.class
        );


        return paragraphs.stream().map(paragraph -> new TextChunkVO(paragraph.getParagraphId(), 0.8f)).toList();
    }

}