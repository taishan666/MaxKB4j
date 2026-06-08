package com.maxkb4j.knowledge.util;

import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.vo.TagListVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TagUtil {

    public static List<TagListVO> convert(List<TagEntity> tags) {
        List<TagListVO> tagList = new ArrayList<>();
        Map<String, List<TagEntity>> groupedTags = tags.stream().collect(Collectors.groupingBy(TagEntity::getKey));
        groupedTags.forEach((key, value) -> {
            TagListVO tagListVO = new TagListVO();
            tagListVO.setKey(key);
            tagListVO.setValues(value);
            tagList.add(tagListVO);
        });
        return tagList;
    }
}
