package com.maxkb4j.knowledge.store;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.knowledge.PgVectorEmbeddingStoreProxy;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.TextChunk;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import dev.langchain4j.store.embedding.filter.comparison.IsNotIn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PostgreSQL pgvector implementation of VectorStore
 */
@Slf4j
@Component("vectorStore")
@RequiredArgsConstructor
public class VectorStoreImpl implements IDataStore {

    private final KnowledgeModelService knowledgeModelService;
    private final PgVectorEmbeddingStoreProxy embeddingStoreProxy;

    @Override
    public void upsert(EmbeddingModel model, List<TextChunk> textChunks) {
        if (textChunks == null || textChunks.isEmpty()) {
            return;
        }
        // Filter valid entities
        List<TextChunk> validChunks = textChunks.stream()
                .filter(e -> e != null && StringUtils.isNotBlank(e.getContent()))
                .toList();

        if (validChunks.isEmpty()) {
            return;
        }
        List<TextSegment> textSegments=validChunks.stream().map(e->{
            Metadata metadata=Metadata.from(Map.of(
                    "sourceId",e.getSourceId(),
                    "sourceType",e.getSourceType(),
                    "knowledgeId",e.getKnowledgeId(),
                    "documentId",e.getDocumentId(),
                    "paragraphId",e.getParagraphId()
                    ));
            return TextSegment.from("",metadata);
        }).toList();
        List<Embedding> embeddings=model.embedAll(textSegments).content();
        log.debug("Processing {} valid entities for embedding", validChunks.size());
        EmbeddingStore<TextSegment> embeddingStore = embeddingStoreProxy.get(model.dimension());
        embeddingStore.addAll(embeddings, textSegments);
    }

    @Override
    public void deleteByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId) {
        Filter filter=new IsEqualTo("knowledgeId",knowledgeId).and(new IsEqualTo("paragraphId",paragraphId)).and(new IsEqualTo("sourceId",problemId));
        embeddingStoreProxy.removeAll(filter);
    }

    @Override
    public void deleteProblemByIds(String knowledgeId, List<String> problemIds) {
        Filter filter=new IsEqualTo("knowledgeId",knowledgeId).and(new IsEqualTo("sourceType",SourceType.PROBLEM)).and(new IsIn("sourceId",problemIds));
        embeddingStoreProxy.removeAll(filter);
    }
    @Override
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        if (paragraphIds == null || paragraphIds.isEmpty()) {
            return;
        }
        Filter filter=new IsEqualTo("knowledgeId",knowledgeId).and(new IsIn("paragraphId",paragraphIds));
        embeddingStoreProxy.removeAll(filter);
        log.debug("Deleted embeddings by paragraph IDs: {}", paragraphIds);
    }

    @Override
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        Filter filter=new IsEqualTo("knowledgeId",knowledgeId).and(new IsIn("documentId",documentIds));
        embeddingStoreProxy.removeAll(filter);
        log.debug("Deleted embeddings by document IDs: {}", documentIds);
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        if (knowledgeId == null) {
            return;
        }
        Filter filter=new IsEqualTo("knowledgeId",knowledgeId);
        embeddingStoreProxy.removeAll(filter);
        log.debug("Deleted embeddings for knowledge ID: {}", knowledgeId);
    }



    @Override
    public List<TextChunkVO> search(SearchRequest request) {
        if (request.getKnowledgeIds() == null || request.getKnowledgeIds().isEmpty()) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(request.getQuery())) {
            return Collections.emptyList();
        }
        try {
            EmbeddingModel embeddingModel = getEmbeddingModel(request.getKnowledgeIds().get(0));
            if (embeddingModel == null) {
                log.warn("No embedding model found for knowledge: {}", request.getKnowledgeIds().get(0));
                return Collections.emptyList();
            }
            // Generate embedding for query
            Response<Embedding> res = embeddingModel.embed(request.getQuery());
            Filter filter=new IsIn("knowledgeId",request.getKnowledgeIds());
            if (CollectionUtils.isNotEmpty(request.getExcludeDocumentIds())){
                filter = filter.and(new IsNotIn("documentId",request.getExcludeDocumentIds()));
            }
            if (CollectionUtils.isNotEmpty(request.getExcludeParagraphIds())){
                filter = filter.and(new IsNotIn("paragraphId",request.getExcludeParagraphIds()));
            }
            EmbeddingSearchResult<TextSegment> result= embeddingStoreProxy.search(EmbeddingSearchRequest.builder()
                    .filter(filter)
                    .queryEmbedding(res.content())
                    .query(request.getQuery())
                    .minScore(request.getMinScore())
                    .maxResults(1000)
                    .build());
            List<EmbeddingMatch<TextSegment>> matches = result.matches();
            List<TextChunkVO> textChunks = matches.stream()
                    .map(match -> new TextChunkVO(match.embedded().metadata().getString("paragraphId"), match.score()))
                    .toList();
            // Deduplicate by paragraphId, keep the one with highest score
            List<TextChunkVO> distinctChunks = textChunks.stream()
                    .collect(Collectors.toMap(
                            TextChunkVO::getParagraphId,
                            t -> t,
                            (t1, t2) -> t1.getScore() >= t2.getScore() ? t1 : t2
                    ))
                    .values()
                    .stream()
                    .sorted(Comparator.comparing(TextChunkVO::getScore).reversed())
                    .toList();
            int topK = Math.min(request.getTopK(), distinctChunks.size());
            return distinctChunks.subList(0, topK);
        } catch (Exception e) {
            log.error("Vector search failed: {}", e.getMessage(), e);
            throw new RuntimeException("Vector search service error", e);
        }
    }

    /**
     * Get embedding model for a knowledge base
     */
    protected EmbeddingModel getEmbeddingModel(String knowledgeId) {
        return knowledgeModelService.getEmbeddingModel(knowledgeId);
    }


}