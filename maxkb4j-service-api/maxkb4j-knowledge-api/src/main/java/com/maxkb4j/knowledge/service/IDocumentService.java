package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.dto.DocumentSimple;
import com.maxkb4j.knowledge.entity.DocumentEntity;

import java.util.List;

public interface IDocumentService extends IService<DocumentEntity> {

    void updateStatusById(String id, int type, int status);
    void updateStatusByIds(List<String> ids, int type, int status);
    void updateStatusMetaById(String id);
    boolean batchCreateDocs(String knowledgeId,int knowledgeType, List<DocumentSimple> docs);

}
