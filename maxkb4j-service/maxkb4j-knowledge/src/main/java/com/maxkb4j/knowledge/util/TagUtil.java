package com.maxkb4j.knowledge.util;

import com.maxkb4j.knowledge.vo.TagListVO;
import com.maxkb4j.knowledge.vo.TagVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TagUtil {


    public static List<TagListVO> convert(List<TagVO> tags) {
        List<TagListVO> tagList = new ArrayList<>();
        Map<String, List<TagVO>> groupedTags = tags.stream().collect(Collectors.groupingBy(TagVO::getKey));
        groupedTags.forEach((key, value) -> {
            TagListVO tagListVO = new TagListVO();
            tagListVO.setKey(key);
            tagListVO.setValues(value);
            tagList.add(tagListVO);
        });
        return tagList;
    }
}
