package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.vo.TagVO;

import java.util.List;

public interface ITagService extends IService<TagEntity> {
    Boolean deleteTagId(String tagId);

    Boolean batchDelete(List<String> tagIds);

    List<TagVO> listTags(String id, String name);

    Boolean docsDelete(String tagId, List<String> docIds);

    /**
     * 为知识库批量添加标签，自动去重已存在的标签。
     *
     * @param knowledgeId 知识库 ID
     * @param tags        待添加标签列表
     * @return 是否添加成功
     */
    Boolean addTags(String knowledgeId, List<TagEntity> tags);
}
