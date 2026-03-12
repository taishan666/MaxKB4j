package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;

import java.util.List;

public interface IKnowledgeService extends IService<KnowledgeEntity> {

    List<KnowledgeEntity> listNameAndDescByIds(List<String> knowledgeIds);
}
