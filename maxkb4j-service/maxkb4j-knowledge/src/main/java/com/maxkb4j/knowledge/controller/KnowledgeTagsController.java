package com.maxkb4j.knowledge.controller;

import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.knowledge.dto.IdListDTO;
import com.maxkb4j.knowledge.dto.TagAddDTO;
import com.maxkb4j.knowledge.dto.TagUpdateDTO;
import com.maxkb4j.knowledge.entity.TagEntity;
import com.maxkb4j.knowledge.service.ITagService;
import com.maxkb4j.knowledge.util.TagUtil;
import com.maxkb4j.knowledge.vo.TagListVO;
import com.maxkb4j.knowledge.vo.TagVO;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
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
    public R<Boolean> addTags(@PathVariable String id, @RequestBody List<TagAddDTO> tags) {
        List<TagEntity> entities = tags == null ? List.of() : tags.stream().map(dto -> {
            TagEntity entity = new TagEntity();
            entity.setKey(dto.getKey());
            entity.setValue(dto.getValue());
            return entity;
        }).toList();
        return R.status(tagService.addTags(id, entities));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_READ)
    @GetMapping("/knowledge/{id}/tags")
    public R<List<TagListVO>> listTags(@PathVariable String id, @RequestParam(required = false) String name) {
        List<TagVO> tags = tagService.listTags(id,name);
        return R.data(TagUtil.convert(tags));
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_EDIT)
    @PutMapping("/knowledge/{id}/tags/{tagId}")
    public R<Boolean> updateTagId(@PathVariable("id") String id, @PathVariable String tagId, @RequestBody TagUpdateDTO dto) {
        TagEntity tagEntity = new TagEntity();
        tagEntity.setId(tagId);
        tagEntity.setKey(dto.getKey());
        tagEntity.setValue(dto.getValue());
        return R.status(tagService.updateById(tagEntity));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DELETE)
    @DeleteMapping("/knowledge/{id}/tags/{tagId}/one")
    public R<Boolean> deleteTagId(@PathVariable("id") String id, @PathVariable String tagId) {
        return R.status(tagService.deleteTagId(tagId));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DELETE)
    @PutMapping("/knowledge/{id}/tag/{tagId}/docs_delete")
    public R<Boolean> docsDelete(@PathVariable("id") String id,@PathVariable("tagId") String tagId,  @Valid @RequestBody IdListDTO dto) {
        return R.status(tagService.docsDelete(tagId,dto.getIdList()));
    }



    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DELETE)
    @PutMapping("/knowledge/{id}/tags/batch_delete")
    public R<Boolean> batchDelete(@PathVariable("id") String id, @RequestBody List<String> tagIds) {
        return R.status(tagService.batchDelete(tagIds));
    }
}
