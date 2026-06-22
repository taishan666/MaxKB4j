package com.maxkb4j.knowledge.controller;

import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.knowledge.dto.DocumentTagAddDTO;
import com.maxkb4j.knowledge.entity.DocumentTagEntity;
import com.maxkb4j.knowledge.service.IDocumentTagService;
import com.maxkb4j.knowledge.vo.TagListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class DocumentTagController {

    private final IDocumentTagService documentTagService;

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @GetMapping("/knowledge/{id}/document/{docId}/tags")
    public R<List<TagListVO>> listTags(@PathVariable("id") String id, @PathVariable String docId, @RequestParam(required = false) String name) {
        return R.data(documentTagService.listTags(docId,name));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @PostMapping("/knowledge/{id}/document/{docId}/tags")
    public R<Boolean> addTags(@PathVariable("id") String id, @PathVariable String docId, @RequestBody List<String> tagIds) {
        List<DocumentTagEntity> documentTags=new ArrayList<>();
        if (tagIds.isEmpty()){
            return R.status(true);
        }
        Set<String> existTagIds=documentTagService.lambdaQuery()
                .select(DocumentTagEntity::getTagId)
                .eq(DocumentTagEntity::getDocumentId,docId)
                .in(DocumentTagEntity::getTagId,tagIds)
                .list()
                .stream()
                .map(DocumentTagEntity::getTagId)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> addTagIds=new HashSet<>();
        for (String tagId : tagIds) {
            if (!existTagIds.contains(tagId)&&addTagIds.add(tagId)){
                DocumentTagEntity documentTag=new DocumentTagEntity();
                documentTag.setDocumentId(docId);
                documentTag.setTagId(tagId);
                documentTags.add(documentTag);
            }
        }
        return R.status(documentTags.isEmpty()||documentTagService.saveBatch(documentTags));
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PostMapping("/knowledge/{id}/document/batch_add_tag")
    public R<Boolean> batchAddTags(@PathVariable("id") String id, @RequestBody DocumentTagAddDTO dto) {
        List<DocumentTagEntity> documentTags=new ArrayList<>();
        List<String> documentIds=dto.getDocumentIds();
        List<String> tagIds=dto.getTagIds();
        if (documentIds.isEmpty()||tagIds.isEmpty()){
            return R.status(true);
        }
        Set<String> existDocumentTags=documentTagService.lambdaQuery()
                .select(DocumentTagEntity::getDocumentId,DocumentTagEntity::getTagId)
                .in(DocumentTagEntity::getDocumentId,documentIds)
                .in(DocumentTagEntity::getTagId,tagIds)
                .list()
                .stream()
                .map(documentTag -> documentTag.getDocumentId()+":"+documentTag.getTagId())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> addDocumentTags=new HashSet<>();
        for (String documentId : documentIds) {
            for (String tagId : tagIds) {
                String key=documentId+":"+tagId;
                if (!existDocumentTags.contains(key)&&addDocumentTags.add(key)){
                    DocumentTagEntity documentTag=new DocumentTagEntity();
                    documentTag.setDocumentId(documentId);
                    documentTag.setTagId(tagId);
                    documentTags.add(documentTag);
                }
            }
        }
        return R.status(documentTags.isEmpty()||documentTagService.saveBatch(documentTags));
    }



    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_DELETE)
    @PutMapping("/knowledge/{id}/document/{docId}/tags/batch_delete")
    public R<Boolean> batchDeleteTags(@PathVariable("id") String id,  @PathVariable String docId,@RequestBody List<String> tagIds) {
        return R.status(documentTagService.removeByIds(tagIds));
    }


}
