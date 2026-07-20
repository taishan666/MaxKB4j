package com.maxkb4j.knowledge.service;

import com.maxkb4j.knowledge.dto.KnowledgeSimple;

import java.util.List;

public interface IKnowledgeService {

    List<KnowledgeSimple> listNameAndDescByIds(List<String> knowledgeIds);
}
