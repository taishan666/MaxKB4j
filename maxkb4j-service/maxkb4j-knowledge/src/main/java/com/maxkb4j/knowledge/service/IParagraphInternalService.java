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

    /**
     * 查询指定知识库下 isActive=false 的段落 ID，供检索时排除。
     * @param knowledgeIds 知识库 ID 列表
     * @param excludeDocIds 需要排除的文档 ID（可选）
     * @return 非激活段落 ID 列表
     */
    List<String> getNoActiveParagraphIds(List<String> knowledgeIds, List<String> excludeDocIds);

    boolean saveParagraphAndProblem(String knowledgeId, String docId, ParagraphAddDTO addDTO);

    IPage<ParagraphEntity> pageParagraphByDocId(String docId, int current, int size, String title, String content);

    List<ProblemSimpleVO> getProblemsByParagraphId(String paragraphId);

    Boolean batchGenerateRelated(String knowledgeId, String docId, GenerateProblemDTO dto);

    Boolean paragraphMigrate(String sourceKnowledgeId, String sourceDocId, String targetKnowledgeId, String targetDocId, List<String> paragraphIds);

    boolean adjustPosition(String knowledgeId, String documentId, String paragraphId, Integer targetIndex);
}