package com.maxkb4j.knowledge.service;

import com.maxkb4j.knowledge.dto.DocumentSimple;

import java.util.List;

public interface IDocumentService {

    void updateStatusById(String id, int type, int status);
    void updateStatusByIds(List<String> ids, int type, int status);
    void updateStatusMetaById(String id);
    boolean batchCreateDocs(String knowledgeId,int knowledgeType, List<DocumentSimple> docs);
    List<String> getNoActiveDocIds(List<String> knowledgeIds);
}
