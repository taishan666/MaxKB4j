package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.dto.ParagraphAddDTO;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.List;

public interface IParagraphService extends IService<ParagraphEntity> {
    ParagraphEntity createParagraph(String knowledgeId, String docId, String title, String content,Integer  position);
    boolean saveParagraphAndProblem(String knowledgeId, String docId, ParagraphAddDTO addDTO);
    boolean saveParagraphAndProblem(ParagraphEntity paragraph, List<String> problems);
    boolean deleteById(String knowledgeId,String paragraphId);
    List<ParagraphEntity> listByStateIds(String docId,int type, List<String> stateList);
    void updateStatusById(String id, int type, int status);
    void updateStatusByIds(List<String> ids, int type, int status);
    void createIndexBatch(List<ParagraphEntity> paragraphs, EmbeddingModel embeddingModel);
}
