package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import com.maxkb4j.knowledge.dto.ProblemDTO;
import com.maxkb4j.knowledge.vo.ProblemVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.List;

public interface IProblemService extends IService<ProblemEntity> {
    void generateRelated(ChatModel chatModel, EmbeddingModel embeddingModel, String knowledgeId, String docId, ParagraphEntity paragraph, List<ProblemEntity> existingProblems, int problemNumber);
    IPage<ProblemVO> pageByDatasetId(String knowledgeId, int page, int size, String content);
    boolean createProblemsByDatasetId(String knowledgeId, List<String> problems);
    boolean createProblemsByParagraphId(String knowledgeId, String docId, String paragraphId, ProblemDTO dto);
    boolean deleteProblemByIds(String knowledgeId, List<String> problemIds);
    boolean updateProblemById(ProblemEntity problem);
}
