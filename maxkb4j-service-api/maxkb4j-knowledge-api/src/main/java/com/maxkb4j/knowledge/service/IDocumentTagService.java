package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.entity.DocumentTagEntity;
import com.maxkb4j.knowledge.entity.TagEntity;

import java.util.List;

public interface IDocumentTagService extends IService<DocumentTagEntity> {
    List<TagEntity> listTagsByDocId(String docId);
}
