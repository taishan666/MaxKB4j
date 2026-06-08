package com.maxkb4j.knowledge.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.service.ITagService;
import com.maxkb4j.knowledge.vo.TagListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class KnowledgeTagsController {

    private final ITagService tagService;

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_CREATE)
    @PostMapping("/knowledge/{id}/tags")
    public R<Boolean> addTags(@PathVariable String id, @RequestBody List<TagEntity> tags) {
        List<TagEntity> saveTags = tags.stream().filter(tag -> {
                    long count = tagService.lambdaQuery().eq(TagEntity::getKey, tag.getKey()).eq(TagEntity::getValue, tag.getValue()).eq(TagEntity::getKnowledgeId, id).count();
                    return count == 0;
                }).peek(tag -> tag.setKnowledgeId(id))
                .toList();
        if (saveTags.isEmpty()){
            return R.success();
        }
        return R.status(tagService.saveBatch(saveTags));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_READ)
    @GetMapping("/knowledge/{id}/tags")
    public R<List<TagListVO>> listTags(@PathVariable String id, @RequestParam(required = false) String name) {
        List<TagListVO> tagList = new ArrayList<>();
        LambdaQueryWrapper<TagEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TagEntity::getKnowledgeId, id);
        if (StringUtils.isNotBlank(name)){
            wrapper.like(TagEntity::getKey, name);
            wrapper.or().like(TagEntity::getValue, name);
        }
        List<TagEntity> tags = tagService.list(wrapper);
        Map<String, List<TagEntity>> groupedTags = tags.stream().collect(Collectors.groupingBy(TagEntity::getKey));
        groupedTags.forEach((key, value) -> {
            TagListVO tagListVO = new TagListVO();
            tagListVO.setKey(key);
            tagListVO.setValues(value);
            tagList.add(tagListVO);
        });
        return R.data(tagList);
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_EDIT)
    @PutMapping("/knowledge/{id}/tags/{tagId}")
    public R<Boolean> updateTagId(@PathVariable("id") String id, @PathVariable String tagId, @RequestBody TagEntity tagEntity) {
        return R.success(tagService.updateById(tagEntity));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DELETE)
    @DeleteMapping("/knowledge/{id}/tags/{tagId}/one")
    public R<Boolean> deleteTagId(@PathVariable("id") String id, @PathVariable String tagId) {
        return R.success(tagService.deleteTagId(tagId));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DELETE)
    @DeleteMapping("/knowledge/{id}/tags/batch_delete")
    public R<Boolean> batchDelete(@PathVariable("id") String id, @RequestBody List<String> tagIds) {
        return R.success(tagService.batchDelete(tagIds));
    }
}
