package com.maxkb4j.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.domain.dto.KeyAndValue;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.knowledge.consts.KnowledgeType;
import com.maxkb4j.knowledge.dto.*;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.knowledge.service.DocumentWriteService;
import com.maxkb4j.knowledge.service.IDocumentInternalService;
import com.maxkb4j.knowledge.service.impl.DocumentImportService;
import com.maxkb4j.knowledge.service.impl.DocumentMigrationService;
import com.maxkb4j.knowledge.service.IDocumentSplitService;
import com.maxkb4j.knowledge.vo.DocumentVO;
import com.maxkb4j.knowledge.vo.TextSegmentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class DocumentController {

    private final IDocumentInternalService documentService;
    private final DocumentImportService documentImportService;
    private final DocumentMigrationService documentMigrationService;
    private final IDocumentSplitService documentSplitService;
    private final DocumentWriteService documentWriteService;

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PostMapping("/knowledge/{id}/document/web")
    public void web(@PathVariable("id") String id, @Valid @RequestBody WebUrlDTO params) throws IOException {
        documentImportService.createWebDoc(id, params.getSourceUrlList(), params.getSelector());
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_SYNC)
    @PutMapping("/knowledge/{id}/document/{docId}/sync")
    public void sync(@PathVariable("id") String id, @PathVariable("docId") String docId) {
        documentImportService.syncWebDoc(id, docId);
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PostMapping("/knowledge/{id}/document/qa")
    public void importQa(@PathVariable("id") String id, MultipartFile[] file) throws IOException {
        documentImportService.importQa(id, file);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PostMapping("/knowledge/{id}/document/table")
    public void importTable(@PathVariable("id") String id, MultipartFile[] file) throws IOException {
        documentImportService.importTable(id, file);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PostMapping("/knowledge/{id}/document/split")
    public R<List<TextSegmentVO>> split(@PathVariable String id, MultipartFile[] file, String[] patterns, Integer limit, Boolean withFilter) throws IOException {
        return R.data(documentImportService.split(id,file, patterns, limit, withFilter));
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PutMapping("/knowledge/{id}/document/batch_create")
    public R<Boolean> createBatchDoc(@PathVariable("id") String id, @RequestBody List<DocumentSimple> docs) {
        return R.status(documentWriteService.batchCreateDocs(id, KnowledgeType.BASE, docs));
    }

    @GetMapping("/knowledge/{id}/document/split_pattern")
    public R<List<KeyAndValue>> splitPattern(@PathVariable("id") String id) {
        return R.data(documentSplitService.splitPattern());
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @GetMapping("/knowledge/{id}/document")
    public R<List<DocumentVO>> listDocByKnowledgeId(@PathVariable String id) {
        return R.data(BeanUtil.copyList(documentService.listDocByKnowledgeId(id), DocumentVO.class));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_GENERATE)
    @PutMapping("/knowledge/{id}/document/batch_generate_related")
    public R<Boolean> batchGenerateRelated(@PathVariable String id, @Valid @RequestBody GenerateProblemDTO dto) {
        return R.status(documentService.batchGenerateRelated(id, dto));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_MIGRATE)
    @PutMapping("/knowledge/{id}/document/migrate/{targetKnowledgeId}")
    public R<Boolean> migrateDoc(@PathVariable("id") String sourceKnowledgeId, @PathVariable("targetKnowledgeId") String targetKnowledgeId, @RequestBody List<String> docIds) {
        return R.status(documentMigrationService.migrateDoc(sourceKnowledgeId, targetKnowledgeId, docIds));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PutMapping("/knowledge/{id}/document/batch_hit_handling")
    public R<Boolean> batchHitHandling(@PathVariable String id, @Valid @RequestBody DatasetBatchHitHandlingDTO dto) {
        return R.status(documentService.batchHitHandling(id, dto));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_DELETE)
    @PutMapping("/knowledge/{id}/document/batch_delete")
    public R<Boolean> deleteBatchDocByDocIds(@PathVariable("id") String id, @Valid @RequestBody IdListDTO dto) {
        return R.status(documentService.deleteDocByIds(id, dto.getIdList()));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @GetMapping("/knowledge/{id}/document/{docId}")
    public R<DocumentVO> getDocByDocId(@PathVariable String id, @PathVariable("docId") String docId) {
        DocumentEntity e = documentService.getById(docId);
        return R.data(e == null ? null : BeanUtil.copy(e, DocumentVO.class));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_VECTOR)
    @PutMapping("/knowledge/{id}/document/{docId}/refresh")
    public R<Boolean> refresh(@PathVariable String id, @PathVariable("docId") String docId, @Valid @RequestBody DocumentEmbedDTO dto) {
        return R.status(documentService.embedByDocIds(id, List.of(docId), dto.getStateList()));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_VECTOR)
    @PutMapping("/knowledge/{id}/document/batch_refresh")
    public R<Boolean> batchRefresh(@PathVariable String id, @Valid @RequestBody DocumentEmbedDTO dto) {
        return R.status(documentService.embedByDocIds(id, dto.getIdList(), dto.getStateList()));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PutMapping("/knowledge/{id}/document/{docId}/cancel_task")
    public R<Boolean> cancelTask(@PathVariable String id, @PathVariable("docId") String docId, @Valid @RequestBody DocumentTaskCancelDTO dto) {
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setType(dto.getType());
        return R.status(documentService.cancelTask(docId, documentEntity));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PutMapping("/knowledge/{id}/document/{docId}")
    public R<DocumentVO> updateDocByDocId(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody DocumentUpdateDTO dto) {
        DocumentEntity documentEntity = BeanUtil.copy(dto, DocumentEntity.class);
        DocumentEntity e = documentService.updateAndGetById(docId, documentEntity);
        return R.data(e == null ? null : BeanUtil.copy(e, DocumentVO.class));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_DELETE)
    @DeleteMapping("/knowledge/{id}/document/{docId}")
    public R<Boolean> deleteDoc(@PathVariable("id") String id, @PathVariable("docId") String docId) {
        return R.status(documentService.deleteDocByIds(id,List.of(docId)));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @GetMapping("/knowledge/{id}/document/{current}/{size}")
    public R<IPage<DocumentVO>> pageDocByDatasetId(@PathVariable String id, @PathVariable("current") int current, @PathVariable("size") int size, DocQuery query) {
        return R.data(documentService.getDocByKnowledgeId(id, current, size, query));
    }


}
