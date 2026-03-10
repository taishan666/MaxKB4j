package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.entity.ProblemEntity;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.List;

public interface IProblemService extends IService<ProblemEntity> {
    void generateRelated(ChatModel chatModel, EmbeddingModel embeddingModel, String knowledgeId, String docId, ParagraphEntity paragraph, List<ProblemEntity> existingProblems, String promptTemplate);
}
