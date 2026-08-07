package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.dto.DatasetBatchHitHandlingDTO;
import com.maxkb4j.knowledge.dto.DocQuery;
import com.maxkb4j.knowledge.dto.GenerateProblemDTO;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.vo.DocumentVO;

import java.util.List;

/**
 * 文档服务「对内」接口：供 Controller 使用的完整服务契约。
 * 与 {@link IDocumentService}（对外跨模块契约，位于 maxkb4j-knowledge-api）区分。
 */
public interface IDocumentInternalService extends IService<DocumentEntity> {

    boolean batchHitHandling(String knowledgeId, DatasetBatchHitHandlingDTO dto);

    boolean deleteDocByIds(String knowledgeId, List<String> docIds);

    boolean embedByDocIds(String knowledgeId, List<String> docIds, List<String> stateList);

    boolean cancelTask(String docId, DocumentEntity doc);

    DocumentEntity updateAndGetById(String docId, DocumentEntity documentEntity);

    IPage<DocumentVO> getDocByKnowledgeId(String knowledgeId, int current, int size, DocQuery query);

    List<DocumentEntity> listDocByKnowledgeId(String id);

    boolean batchGenerateRelated(String knowledgeId, GenerateProblemDTO dto);
}