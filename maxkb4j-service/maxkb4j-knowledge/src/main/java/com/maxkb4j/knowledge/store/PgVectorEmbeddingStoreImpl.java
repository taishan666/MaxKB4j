package com.maxkb4j.knowledge.store;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.IParagraphService;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.pgvector.DefaultMetadataStorageConfig;
import dev.langchain4j.store.embedding.pgvector.MetadataStorageMode;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Slf4j
@Component("PgVectorEmbeddingStore")
@RequiredArgsConstructor
public class PgVectorEmbeddingStoreImpl implements IDataStore {

    private final KnowledgeModelService knowledgeModelService;
    private final DataSource dataSource;
    private final IParagraphService paragraphService;

    public static Map<Integer, EmbeddingStore<TextSegment>> proxy = new HashMap<>();


    private EmbeddingStore<TextSegment> build(int dimension) {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("embedding_" + dimension)
                .dimension(dimension)
                .useIndex(true)
                .indexListSize(1000)
                .metadataStorageConfig(DefaultMetadataStorageConfig.builder()
                        .storageMode(MetadataStorageMode.COMBINED_JSONB)
                        .columnDefinitions(Collections.singletonList("metadata JSON NULL"))
                        .build())
                .build();
    }

    public EmbeddingStore<TextSegment> get(int dimension) {
        return proxy.getOrDefault(dimension, build(dimension));
    }

    @Override
    public void upsert(EmbeddingModel model, List<EmbeddingEntity> entities) {
        EmbeddingStore<TextSegment> store = get(model.dimension());
        List<TextSegment> textSegments = entities.stream().map(entity -> {
            Metadata metadata = new Metadata();
            metadata.put("knowledgeId", entity.getKnowledgeId());
            metadata.put("documentId", entity.getDocumentId());
            metadata.put("paragraphId", entity.getParagraphId());
            metadata.put("sourceId", entity.getSourceId());
            metadata.put("sourceType", entity.getSourceType());
            return TextSegment.from(entity.getContent(), metadata);
        }).toList();
        Response<List<Embedding>> response = model.embedAll(textSegments);
        List<Embedding> embeddings = response.content();
        store.addAll(embeddings, textSegments);
    }

    @Override
    public void deleteByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId) {
        Filter filter = metadataKey("knowledgeId").isEqualTo(knowledgeId)
                .and(metadataKey("paragraphId").isEqualTo(paragraphId))
                .and(metadataKey("sourceId").isEqualTo(problemId));
        proxy.forEach((dimension, store) -> store.removeAll(filter));
    }

    @Override
    public void deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        Filter filter = metadataKey("knowledgeId").isEqualTo(knowledgeId)
                .and(metadataKey("sourceType").isEqualTo(SourceType.PROBLEM))
                .and(metadataKey("sourceId").isIn(problemIds));
        proxy.forEach((dimension, store) -> store.removeAll(filter));
    }

    @Override
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        Filter filter = metadataKey("knowledgeId").isEqualTo(knowledgeId)
                .and(metadataKey("paragraphId").isIn(paragraphIds));
        proxy.forEach((dimension, store) -> store.removeAll(filter));
    }

    @Override
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        Filter filter = metadataKey("knowledgeId").isEqualTo(knowledgeId)
                .and(metadataKey("documentId").isIn(documentIds));
        proxy.forEach((dimension, store) -> store.removeAll(filter));
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        Filter filter = metadataKey("knowledgeId").isEqualTo(knowledgeId);
        proxy.forEach((dimension, store) -> store.removeAll(filter));
    }

    @Override
    public void updateActiveStatus(String knowledgeId, String paragraphId, boolean isActive) {
    }

    @Override
    public List<TextChunkVO> search(SearchRequest request) {
        if (request.getKnowledgeIds() == null || request.getKnowledgeIds().isEmpty()) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(request.getQuery())) {
            return Collections.emptyList();
        }
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(request.getKnowledgeIds().getFirst());
        if (embeddingModel == null) {
            log.warn("No embedding model found for knowledge: {}", request.getKnowledgeIds().getFirst());
            return Collections.emptyList();
        }
        List<String> excludeParagraphIds = new ArrayList<>();
        List<String> noActiveParagraphIds = paragraphService.noActiveList(request.getKnowledgeIds(), request.getExcludeDocumentIds());
        if (CollectionUtils.isNotEmpty(noActiveParagraphIds)) {
            excludeParagraphIds.addAll(noActiveParagraphIds);
        }
        if (CollectionUtils.isNotEmpty(request.getExcludeParagraphIds())) {
            excludeParagraphIds.addAll(request.getExcludeParagraphIds());
        }
        Response<Embedding> res = embeddingModel.embed(request.getQuery());
        Embedding queryEmbedding = res.content();
        EmbeddingStore<TextSegment> store = get(queryEmbedding.dimension());
        Filter filter = metadataKey("knowledgeId").isIn(request.getKnowledgeIds())
                .and(metadataKey("documentId").isNotIn(request.getExcludeDocumentIds()))
                .and(metadataKey("paragraphId").isNotIn(excludeParagraphIds));
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .filter(filter)
                .queryEmbedding(res.content())
                .maxResults(request.getTopK() * 10)
                .minScore(request.getMinScore())
                .build();
        EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);
        List<TextChunkVO> results = new ArrayList<>(searchResult.matches().stream().map(match -> {
            TextSegment segment = match.embedded();
            return new TextChunkVO(segment.metadata().getString("paragraphId"), match.score());
        }).toList());
        if (results.isEmpty()) {
            return Collections.emptyList();
        }
        // 1. 计算每个 paragraphId 在原始结果中的总分
        Map<String, Double> paragraphIdTotalScoreMap = new HashMap<>();
        for (TextChunkVO result : results) {
            paragraphIdTotalScoreMap.merge(result.getParagraphId(), result.getScore(), Double::sum);
        }
        // 2. 按 score 降序排序
        results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // 3. 去重：每个 paragraphId 只保留 score 最大的（即排序后第一个出现的）
        List<TextChunkVO> distinctResults = new ArrayList<>();
        Set<String> seenParagraphIds = new HashSet<>();
        for (TextChunkVO result : results) {
            if (!seenParagraphIds.contains(result.getParagraphId())) {
                seenParagraphIds.add(result.getParagraphId());
                distinctResults.add(result);
            }
        }
        // 4. 排序：score 降序 -> paragraphId 总分降序
        distinctResults.sort((a, b) -> {
            int scoreCompare = Double.compare(b.getScore(), a.getScore());
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            // score 相同时，按 paragraphId 总分降序
            Double totalScoreA = paragraphIdTotalScoreMap.getOrDefault(a.getParagraphId(), 0.0);
            Double totalScoreB = paragraphIdTotalScoreMap.getOrDefault(b.getParagraphId(), 0.0);
            return Double.compare(totalScoreB, totalScoreA);
        });
        int end = Math.min(request.getTopK(), distinctResults.size());
        return distinctResults.subList(0, end);
    }
}
