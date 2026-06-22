package com.maxkb4j.knowledge.controller;

import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.knowledge.dto.IdListDTO;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.service.ITagService;
import com.maxkb4j.knowledge.util.TagUtil;
import com.maxkb4j.knowledge.vo.TagListVO;
import com.maxkb4j.knowledge.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        if (tags.isEmpty()){
            return R.success();
        }
        Set<String> keys = tags.stream().map(TagEntity::getKey).collect(Collectors.toSet());
        Set<String> values = tags.stream().map(TagEntity::getValue).collect(Collectors.toSet());
        Set<String> existTags = tagService.lambdaQuery()
                .select(TagEntity::getKey, TagEntity::getValue)
                .eq(TagEntity::getKnowledgeId, id)
                .in(TagEntity::getKey, keys)
                .in(TagEntity::getValue, values)
                .list()
                .stream()
                .map(tag -> tag.getKey() + ":" + tag.getValue())
                .collect(Collectors.toSet());
        Set<String> addTags = new HashSet<>();
        List<TagEntity> saveTags = tags.stream()
                .filter(tag -> !existTags.contains(tag.getKey() + ":" + tag.getValue()) && addTags.add(tag.getKey() + ":" + tag.getValue()))
                .peek(tag -> tag.setKnowledgeId(id))
                .toList();
        if (saveTags.isEmpty()){
            return R.success();
        }
        return R.status(tagService.saveBatch(saveTags));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_READ)
    @GetMapping("/knowledge/{id}/tags")
    public R<List<TagListVO>> listTags(@PathVariable String id, @RequestParam(required = false) String name) {
        List<TagVO> tags = tagService.listTags(id,name);
        return R.data(TagUtil.convert(tags));
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_EDIT)
    @PutMapping("/knowledge/{id}/tags/{tagId}")
    public R<Boolean> updateTagId(@PathVariable("id") String id, @PathVariable String tagId, @RequestBody TagEntity tagEntity) {
        return R.status(tagService.updateById(tagEntity));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DELETE)
    @DeleteMapping("/knowledge/{id}/tags/{tagId}/one")
    public R<Boolean> deleteTagId(@PathVariable("id") String id, @PathVariable String tagId) {
        return R.status(tagService.deleteTagId(tagId));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DELETE)
    @PutMapping("/knowledge/{id}/tag/{tagId}/docs_delete")
    public R<Boolean> docsDelete(@PathVariable("id") String id,@PathVariable("tagId") String tagId,  @RequestBody IdListDTO dto) {
        return R.status(tagService.docsDelete(tagId,dto.getIdList()));
    }



    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DELETE)
    @PutMapping("/knowledge/{id}/tags/batch_delete")
    public R<Boolean> batchDelete(@PathVariable("id") String id, @RequestBody List<String> tagIds) {
        return R.status(tagService.batchDelete(tagIds));
    }
}
