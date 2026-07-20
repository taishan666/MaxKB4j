package com.maxkb4j.knowledge.store;

import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.List;

/**
 * Vector store abstraction interface for storing and searching embeddings
 */
public interface IDataStore {

    /**
     * Batch insert or update embeddings
     * @param model embedding model for generating vectors
     * @param entities embedding entities to upsert
     */
    void upsert(EmbeddingModel model, List<EmbeddingEntity> entities);


    void deleteByProblemIdAndParagraphId(String knowledgeId, String problemId, String paragraphId);

    /**
     * Delete embeddings by problem IDs
     * @param knowledgeId knowledge base ID
     * @param problemIds problem IDs to delete
     */
    void deleteByProblemIds(String knowledgeId, List<String> problemIds);

    default void deleteByParagraphId(String knowledgeId, String paragraphId){
        deleteByParagraphIds(knowledgeId, List.of(paragraphId));
    }



    /**
     * Delete embeddings by paragraph IDs
     * @param knowledgeId knowledge base ID
     * @param paragraphIds paragraph IDs to delete
     */
    void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds);

    /**
     * Delete embeddings by document IDs
     * @param knowledgeId knowledge base ID
     * @param documentIds document IDs to delete
     */
    void deleteByDocumentIds(String knowledgeId, List<String> documentIds);

    /**
     * Delete all embeddings for a knowledge base
     * @param knowledgeId knowledge base ID
     */
    void deleteByKnowledgeId(String knowledgeId);

    /**
     * 批量删除多个知识库的全部向量/全文数据。
     * <p>默认实现逐个调用 {@link #deleteByKnowledgeId(String)} 兜底；各后端应 override 为
     * 单条 {@code IN (...)} 删除以避免大批量删除时的多次往返与超时。</p>
     * @param knowledgeIds 知识库 ID 列表
     */
    default void deleteByKnowledgeIds(List<String> knowledgeIds) {
        if (knowledgeIds == null || knowledgeIds.isEmpty()) {
            return;
        }
        for (String knowledgeId : knowledgeIds) {
            deleteByKnowledgeId(knowledgeId);
        }
    }

    /**
     * Perform vector similarity search
     * @param request search request parameters
     * @return list of matching text chunks
     */
    List<TextChunkVO> search(SearchRequest request);


}