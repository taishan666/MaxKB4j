package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.common.domain.dto.KeyAndValue;
import com.maxkb4j.knowledge.dto.DatasetBatchHitHandlingDTO;
import com.maxkb4j.knowledge.dto.DocQuery;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.dto.GenerateProblemDTO;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.vo.DocumentVO;
import com.maxkb4j.knowledge.vo.TextSegmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 文档服务「对内」接口：供 Controller 使用的完整服务契约。
 * 与 {@link IDocumentService}（对外跨模块契约，位于 maxkb4j-knowledge-api）区分。
 */
public interface IDocumentInternalService extends IDocumentService, IService<DocumentEntity> {

    void createWebDoc(String knowledgeId, List<String> sourceUrlList, String selector);

    void syncWebDoc(String knowledgeId, String docId);

    void importQa(String knowledgeId, MultipartFile[] files) throws IOException;

    void importTable(String knowledgeId, MultipartFile[] files) throws IOException;

    List<TextSegmentVO> split(String knowledgeId, MultipartFile[] files, String[] patterns, Integer limit, Boolean withFilter) throws IOException;

    boolean migrateDoc(String sourceKnowledgeId, String targetKnowledgeId, List<String> docIds);

    boolean batchHitHandling(String knowledgeId, DatasetBatchHitHandlingDTO dto);

    boolean deleteDocByIds(String knowledgeId, List<String> docIds);

    boolean embedByDocIds(String knowledgeId, List<String> docIds, List<String> stateList);

    boolean cancelTask(String docId, DocumentEntity doc);

    DocumentEntity updateAndGetById(String docId, DocumentEntity documentEntity);

    IPage<DocumentVO> getDocByKnowledgeId(String knowledgeId, int current, int size, DocQuery query);

    List<KeyAndValue> splitPattern();

    List<DocumentEntity> listDocByKnowledgeId(String id);

    boolean batchGenerateRelated(String knowledgeId, GenerateProblemDTO dto);
}