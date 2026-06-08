package com.maxkb4j.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.knowledge.entity.TagEntity;

import java.util.List;

public interface ITagService extends IService<TagEntity> {
    Boolean deleteTagId(String tagId);

    Boolean batchDelete(List<String> tagIds);
}
