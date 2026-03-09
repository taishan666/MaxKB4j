package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.entity.DocumentEntity;

import java.util.List;

public interface IDocumentService extends IService<DocumentEntity> {

    boolean batchCreateDocs(String knowledgeId,int knowledgeType, List<DocumentSimple> docs);
}
