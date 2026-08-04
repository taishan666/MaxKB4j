package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.dto.KnowledgeQuery;
import com.maxkb4j.knowledge.dto.WebKnowledgeDTO;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.vo.KnowledgeListVO;
import com.maxkb4j.knowledge.vo.KnowledgeVO;

import java.util.List;

/**
 * 知识库服务「对内」接口：供 Controller 使用的完整服务契约。
 * 与 {@link IKnowledgeService}（对外跨模块契约，位于 maxkb4j-knowledge-api）区分。
 */
public interface IKnowledgeInternalService extends IKnowledgeService, IService<KnowledgeEntity> {

    IPage<KnowledgeVO> pageList(Page<KnowledgeVO> knowledgePage, KnowledgeQuery query);

    KnowledgeVO getKnowledgeById(String id);

    List<ParagraphEntity> getParagraphByProblemId(String problemId);

    Boolean deleteById(String id);

    Boolean delMulApplication(List<String> idList);

    KnowledgeEntity createKnowledge(KnowledgeEntity knowledge);

    KnowledgeEntity createKnowledgeWeb(WebKnowledgeDTO knowledge);

    List<KnowledgeListVO> listKnowledge(KnowledgeQuery query);

    void updateKnowledge(String id, KnowledgeEntity knowledge);
}