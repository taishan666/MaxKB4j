package com.maxkb4j.knowledge.store.document;

import com.maxkb4j.knowledge.store.impl.FullTextStoreImpl;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TextScore;

/**
 * MongoDB 全文检索文档：仅 {@link FullTextStoreImpl} 使用的持久化形态，
 * 与纯 DTO {@link com.maxkb4j.knowledge.entity.EmbeddingEntity} 分离，
 * 避免存储（Mongo/MyBatis）注解污染上游调用方与向量后端。
 * <p>集合名 {@code embedding} 与原 {@code EmbeddingEntity} 保持一致，无需数据迁移。</p>
 */
@Data
@Document(collection = "embedding")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingDocument {

    @Id
    private String id;
    private String sourceId;
    private Integer sourceType;
    private String knowledgeId;
    private String documentId;

    /** 全文索引字段，写入前由 {@link com.maxkb4j.knowledge.util.Tokenizer#segment} 分词 */
    @TextIndexed
    private String content;

    /** MongoDB textScore 命中得分，仅在聚合检索时回填 */
    @TextScore
    private Double score;
}