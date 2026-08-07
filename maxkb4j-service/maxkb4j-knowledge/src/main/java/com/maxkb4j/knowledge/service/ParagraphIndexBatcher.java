package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.knowledge.listener.DocumentIndexListener;
import com.maxkb4j.knowledge.listener.ParagraphIndexListener;
import com.maxkb4j.knowledge.service.impl.DocumentServiceImpl;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.service.impl.ParagraphServiceImpl;
import com.maxkb4j.knowledge.store.IDataStore;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 段落索引批处理器
 *
 * <p>封装文档/段落向量化索引时的状态流转与批量索引逻辑，
 * 供 {@link DocumentIndexListener} 与 {@link ParagraphIndexListener} 复用，避免重复代码。
 *
 * <p>状态约定（type=1 表示索引维度）：
 * <ul>
 *   <li>0 - 待处理</li>
 *   <li>1 - 处理中</li>
 *   <li>2 - 完成</li>
 * </ul>
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParagraphIndexBatcher {

    private final DocumentServiceImpl documentService;
    private final ParagraphServiceImpl paragraphService;
    private final IDataStore compositeStore;

    /**
     * 批量对文档下的段落进行向量化索引，并维护文档/段落状态流转。
     *
     * <p>调用方负责按需解析一次 {@code embeddingModel}（多文档场景避免重复构建），
     * 并在捕获异常时记录日志。
     *
     * @param embeddingModel 嵌入模型
     * @param knowledgeId     知识库 ID
     * @param docId           文档 ID
     * @param paragraphIds    待索引段落 ID 列表
     */
    public void indexBatch(EmbeddingModel embeddingModel, String knowledgeId, String docId, List<String> paragraphIds) {
        documentService.updateStatusById(docId, 1, 0);

        if (CollectionUtils.isNotEmpty(paragraphIds)) {
            log.info("开始--->文档索引: {}", docId);
            documentService.updateStatusById(docId, 1, 1);
            paragraphService.updateStatusByIds(paragraphIds, 1, 0);

            try {
                createIndexBatch(knowledgeId, paragraphIds, embeddingModel);
                paragraphService.updateStatusByIds(paragraphIds, 1, 2);
                documentService.updateStatusMetaById(docId);
                log.info("结束--->文档索引: {} (处理了 {} 个段落)", docId, paragraphIds.size());
            } catch (Exception e) {
                // 段落保留为待处理状态以便重试，交由调用方记录日志
                throw new RuntimeException("文档索引失败: " + docId, e);
            }
        }

        documentService.updateStatusById(docId, 1, 2);
    }

    /**
     * Batch create index for multiple paragraphs with optimized processing
     */
    private void createIndexBatch(String knowledgeId,List<String> paragraphIds, EmbeddingModel embeddingModel) {
        List<ParagraphEntity> paragraphs = paragraphService.listByIds(paragraphIds);
        if (CollectionUtils.isEmpty(paragraphs)) {
            return;
        }

        log.info("开始批量索引 {} 个段落", paragraphs.size());

        // Collect all embedding entities for batch processing
        List<EmbeddingEntity> allEmbeddingEntities = new ArrayList<>();
        // Clear previous vectors
        compositeStore.deleteByParagraphIds(knowledgeId, paragraphIds);
        for (ParagraphEntity paragraph : paragraphs) {
            if (paragraph == null) continue;

            String title = paragraph.getTitle() != null ? paragraph.getTitle() : "";
            String content = paragraph.getContent() != null ? paragraph.getContent() : "";

            // Create paragraph embedding
            EmbeddingEntity paragraphEmbed = EmbeddingEntity.builder()
                    .knowledgeId(paragraph.getKnowledgeId())
                    .documentId(paragraph.getDocumentId())
                    .sourceId(paragraph.getId())
                    .sourceType(SourceType.PARAGRAPH)
                    .content(title + content)
                    .build();
            allEmbeddingEntities.add(paragraphEmbed);
        }

        // Batch insert all embeddings
        if (!allEmbeddingEntities.isEmpty()) {
            compositeStore.upsert(embeddingModel, allEmbeddingEntities);
            log.info("批量索引完成，共处理 {} 个嵌入实体", allEmbeddingEntities.size());
        }
    }
}
