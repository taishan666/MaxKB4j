package com.maxkb4j.knowledge.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 嵌入写入/检索用的纯数据载体（DTO）：仅承载业务字段，不绑定任何存储技术
 * （MyBatis / Mongo / langchain4j），由各 store 自行转换为对应后端的持久化形态：
 * <ul>
 *   <li>{@link com.maxkb4j.knowledge.store.impl.FullTextStoreImpl} 转为 Mongo 文档 {@code EmbeddingDocument}</li>
 *   <li>{@link com.maxkb4j.knowledge.store.impl.PgVectorEmbeddingStoreImpl} 转为 langchain4j {@code TextSegment}</li>
 * </ul>
 * 原先同时挂载 MyBatis-Plus 与 Mongo 注解、并带 {@code id}/{@code score} 存储字段，
 * 现已剥离为纯 DTO，避免存储注解污染上游调用方。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingEntity {
    private String sourceId;
    private Integer sourceType;
    private String knowledgeId;
    private String documentId;
    private String content;
}