package com.maxkb4j.knowledge.service;

import com.maxkb4j.knowledge.dto.DocumentSimple;

import java.util.List;

public interface IDocumentService {

    boolean batchCreateDocs(String knowledgeId, int knowledgeType, List<DocumentSimple> docs);

    boolean existsByKnowledgeIdAndId(String knowledgeId, String docId);
}
