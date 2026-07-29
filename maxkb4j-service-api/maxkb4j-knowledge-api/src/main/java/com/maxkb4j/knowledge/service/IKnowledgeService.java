package com.maxkb4j.knowledge.service;

import com.maxkb4j.knowledge.dto.KnowledgeSimple;

import java.util.List;

public interface IKnowledgeService {

    List<KnowledgeSimple> listSimpleKnowledgeByIds(List<String> knowledgeIds);

    default KnowledgeSimple getSimpleKnowledgeById(String knowledgeId) {
        List<KnowledgeSimple> list = listSimpleKnowledgeByIds(List.of(knowledgeId));
        return list.isEmpty() ? null : list.getFirst();
    }
}
