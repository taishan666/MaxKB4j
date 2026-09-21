package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.maxkb4j.common.util.BatchUtil;
import com.maxkb4j.knowledge.listener.DocumentIndexListener;
import com.maxkb4j.knowledge.listener.ParagraphIndexListener;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.store.IDataStore;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
 * <p>段落较多时，查询、状态更新与旧向量删除均按 {@link BatchUtil#NUMBER_BACH_PROTECT}
 * 分批执行，避免单条 SQL 或存储过滤条件的 IN 列表超出数据库参数上限。
 *
 * @author tarzan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParagraphIndexBatchExecutor {

    /**
     * status 状态串的索引维度
     */
    private static final int INDEX_TYPE = 1;
    /**
     * 待处理（可被重试流程重新拾起）
     */
    private static final int STATUS_PENDING = 0;
    /**
     * 处理中
     */
    private static final int STATUS_PROCESSING = 1;
    /**
     * 完成
     */
    private static final int STATUS_COMPLETED = 2;

    private final IDocumentInternalService documentService;
    private final IParagraphInternalService paragraphService;
    private final IDataStore compositeStore;

    /**
     * 批量对文档下的段落进行向量化索引，并维护文档/段落状态流转。
     *
     * <p>调用方负责按需解析一次 {@code embeddingModel}（多文档场景避免重复构建），
     * 并在捕获异常时记录日志。索引失败时段落与文档均回退为待处理状态，以便重试。
     *
     * @param embeddingModel 嵌入模型
     * @param knowledgeId    知识库 ID
     * @param docId          文档 ID
     * @param paragraphIds   待索引段落 ID 列表
     */
    public void indexBatch(EmbeddingModel embeddingModel, String knowledgeId, String docId, List<String> paragraphIds) {
        if (CollectionUtils.isEmpty(paragraphIds)) {
            // 无可索引段落，直接收敛到完成态，省去中间态写库
            documentService.updateStatusById(docId, INDEX_TYPE, STATUS_COMPLETED);
            return;
        }
        if (embeddingModel == null) {
            // 未配置嵌入模型：文档回退待处理状态，交由重试流程在配置修复后拾起
            documentService.updateStatusById(docId, INDEX_TYPE, STATUS_PENDING);
            log.warn("知识库 {} 未配置嵌入模型，文档 {} 索引跳过", knowledgeId, docId);
            return;
        }

        log.info("开始--->文档索引: {}", docId);
        documentService.updateStatusById(docId, INDEX_TYPE, STATUS_PROCESSING);
        // 段落先统一置为待处理：中途失败或进程崩溃后仍能被重试流程拾起
        updateParagraphStatus(paragraphIds, STATUS_PENDING);

        try {
            int indexedCount = createIndexBatch(knowledgeId, docId, paragraphIds, embeddingModel);
            updateParagraphStatus(paragraphIds, STATUS_COMPLETED);
            // status_meta 聚合段落状态，必须在段落置为完成之后重算
            documentService.updateStatusMetaById(docId);
            documentService.updateStatusById(docId, INDEX_TYPE, STATUS_COMPLETED);
            log.info("结束--->文档索引: {} (处理了 {} 个段落)", docId, indexedCount);
        } catch (Exception e) {
            // 段落与文档均保留为待处理状态以便重试，交由调用方记录日志
            documentService.updateStatusById(docId, INDEX_TYPE, STATUS_PENDING);
            throw new RuntimeException("文档索引失败: " + docId, e);
        }
    }

    /**
     * 批量构建段落嵌入并写入向量/全文索引。
     *
     * @return 实际写入的嵌入实体数量（标题与内容均为空白的段落会被跳过）
     */
    private int createIndexBatch(String knowledgeId, String docId, List<String> paragraphIds, EmbeddingModel embeddingModel) {
        List<ParagraphEntity> paragraphs = new ArrayList<>(paragraphIds.size());
        BatchUtil.protectBach(paragraphIds, ids -> {
            paragraphs.addAll(paragraphService.listByIds(ids));
        });
        if (CollectionUtils.isEmpty(paragraphs)) {
            return 0;
        }

        log.info("开始批量索引 {} 个段落", paragraphs.size());

        // 清理旧向量，分批执行防止存储过滤条件的 IN 列表过长
        BatchUtil.protectBach(paragraphIds, ids -> {
            compositeStore.deleteByParagraphIds(knowledgeId, ids);
        });

        // 上下文增强嵌入：短块脱离文档语境后向量区分度低，前缀「文档名 | 标题路径」
        // 为每块补足来源语境，避免不同文档中的同名术语相互混淆
        String documentName = resolveDocumentName(docId);
        List<EmbeddingEntity> embeddingEntities = new ArrayList<>(paragraphs.size());
        for (ParagraphEntity paragraph : paragraphs) {
            String title = StringUtils.trimToEmpty(paragraph.getTitle());
            String content = StringUtils.defaultString(paragraph.getContent());
            // 标题与内容均为空白的段落没有可检索信号，跳过以省去无效的向量化与全文写入
            if (StringUtils.isBlank(title) && StringUtils.isBlank(content)) {
                continue;
            }
            embeddingEntities.add(EmbeddingEntity.builder()
                    .knowledgeId(knowledgeId)
                    .documentId(docId)
                    .sourceId(paragraph.getId())
                    .sourceType(SourceType.PARAGRAPH)
                    .content(buildEmbeddingText(documentName, title, content))
                    .build());
        }

        if (embeddingEntities.isEmpty()) {
            return 0;
        }
        compositeStore.upsert(embeddingModel, embeddingEntities);
        log.info("批量索引完成，共处理 {} 个嵌入实体", embeddingEntities.size());
        return embeddingEntities.size();
    }

    /**
     * 组装上下文增强的嵌入输入：{@code 文档名 | 标题路径\n标题+正文}。
     * <p>文档名或标题缺失时退化为 {@code 标题+正文}，与旧格式兼容。</p>
     */
    private static String buildEmbeddingText(String documentName, String title, String content) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(documentName)) {
            sb.append(documentName);
        }
        if (StringUtils.isNotBlank(title)) {
            if (!sb.isEmpty()) {
                sb.append(" | ");
            }
            sb.append(title);
        }
        if (!sb.isEmpty()) {
            sb.append("\n");
        }
        return sb.append(title).append(content).toString();
    }

    /**
     * 查询文档名用于嵌入前缀；文档已删除时返回空串（段落残留的重建索引场景）。
     */
    private String resolveDocumentName(String docId) {
        try {
            DocumentEntity document = documentService.getById(docId);
            return document == null ? "" : StringUtils.trimToEmpty(document.getName());
        } catch (Exception e) {
            log.warn("查询文档名失败，嵌入输入退化为标题+正文: docId={}, cause: {}", docId, e.getMessage());
            return "";
        }
    }

    /**
     * 分批更新段落索引状态，避免段落过多时单条 UPDATE 的 IN 列表超限。
     */
    private void updateParagraphStatus(List<String> paragraphIds, int status) {
        BatchUtil.protectBach(paragraphIds, ids -> {
            paragraphService.updateStatusByIds(ids, INDEX_TYPE, status);
        });
    }
}
