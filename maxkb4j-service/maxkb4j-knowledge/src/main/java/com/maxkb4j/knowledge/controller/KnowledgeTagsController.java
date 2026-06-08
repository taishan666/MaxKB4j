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
        List<TagVO> tags = tagService.listTags(id,name);
        return R.data(TagUtil.convert(tags));
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
    @PutMapping("/knowledge/{id}/tag/{tagId}/docs_delete")
    public R<Boolean> docsDelete(@PathVariable("id") String id,@PathVariable("tagId") String tagId,  @RequestBody IdListDTO dto) {
        return R.success(tagService.docsDelete(tagId,dto.getIdList()));
    }



    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DELETE)
    @PutMapping("/knowledge/{id}/tags/batch_delete")
    public R<Boolean> batchDelete(@PathVariable("id") String id, @RequestBody List<String> tagIds) {
        return R.success(tagService.batchDelete(tagIds));
    }
}
