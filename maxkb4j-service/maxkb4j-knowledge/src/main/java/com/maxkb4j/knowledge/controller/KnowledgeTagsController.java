package com.maxkb4j.knowledge.controller;

import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.service.ITagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        tags.forEach(tag -> tag.setKnowledgeId(id));
        return R.success(tagService.saveBatch(tags));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_READ)
    @GetMapping("/knowledge/{id}/tags")
    public R<List<TagEntity>> listTags(@PathVariable String id) {
        return R.success(tagService.list());
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_EDIT)
    @PutMapping("/knowledge/{id}/tags/{tagId}")
    public R<Boolean> updateTagId(@PathVariable("id") String id, @PathVariable String tagId, @RequestBody TagEntity tagEntity) {
        return R.success(tagService.updateById(tagEntity));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DELETE)
    @DeleteMapping("/knowledge/{id}/tags/{tagId}/one")
    public R<Boolean> deleteTagId(@PathVariable("id") String id, @PathVariable String tagId) {
        return R.success(tagService.removeById(tagId));
    }
}
