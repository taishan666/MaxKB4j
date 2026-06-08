package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.entity.DocumentTagEntity;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.vo.TagListVO;

import java.util.List;
import java.util.Map;

public interface IDocumentTagService extends IService<DocumentTagEntity> {
    List<TagListVO> listTags(String docId,String name);

    Map<String, List<TagEntity>> listTagsByDocIds(List<String> docIds);
}
