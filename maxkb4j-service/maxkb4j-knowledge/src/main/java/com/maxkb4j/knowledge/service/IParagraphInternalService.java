package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.dto.GenerateProblemDTO;
import com.maxkb4j.knowledge.dto.ParagraphAddDTO;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.vo.ProblemSimpleVO;

import java.util.List;

/**
 * 段落服务「对内」接口：供 Controller 使用的完整服务契约。
 * 与 {@link IParagraphService}（对外跨模块契约，位于 maxkb4j-knowledge-api）区分。
 */
public interface IParagraphInternalService extends IParagraphService, IService<ParagraphEntity> {

    void updateParagraphById(String knowledgeId, String docId, ParagraphEntity paragraph);

    Boolean deleteBatchByIds(String knowledgeId, String docId, List<String> paragraphIds);

    boolean saveParagraphAndProblem(String knowledgeId, String docId, ParagraphAddDTO addDTO);

    IPage<ParagraphEntity> pageParagraphByDocId(String docId, int current, int size, String title, String content);

    List<ProblemSimpleVO> getProblemsByParagraphId(String paragraphId);

    Boolean batchGenerateRelated(String knowledgeId, String docId, GenerateProblemDTO dto);

    Boolean paragraphMigrate(String sourceKnowledgeId, String sourceDocId, String targetKnowledgeId, String targetDocId, List<String> paragraphIds);

    boolean adjustPosition(String knowledgeId, String documentId, String paragraphId, Integer targetIndex);
}