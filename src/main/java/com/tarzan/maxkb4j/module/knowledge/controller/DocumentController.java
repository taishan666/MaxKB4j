package com.tarzan.maxkb4j.module.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.annotation.SaCheckPerm;
import com.tarzan.maxkb4j.common.domain.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.*;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.DocumentVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.TextSegmentVO;
import com.tarzan.maxkb4j.module.knowledge.consts.KnowledgeType;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentService;
import com.tarzan.maxkb4j.module.model.info.vo.KeyAndValueVO;
import com.tarzan.maxkb4j.module.system.user.enums.PermissionEnum;
import jakarta.servlet.http.HttpServletResponse;
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
@RequestMapping(AppConst.ADMIN_API + "/workspace/default")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PostMapping("/knowledge/{id}/document/web")
    public void web(@PathVariable("id") String id, @RequestBody WebUrlDTO params) throws IOException {
        documentService.createWebDoc(id, params.getSourceUrlList(), params.getSelector());
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_SYNC)
    @PutMapping("/knowledge/{id}/document/{docId}/sync")
    public void sync(@PathVariable("id") String id, @PathVariable("docId") String docId) throws IOException {
        documentService.syncWebDoc(id, docId);
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PostMapping("/knowledge/{id}/document/qa")
    public void importQa(@PathVariable("id") String id, MultipartFile[] file) throws IOException {
        documentService.importQa(id, file);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PostMapping("/knowledge/{id}/document/table")
    public void importTable(@PathVariable("id") String id, MultipartFile[] file) throws IOException {
        documentService.importTable(id, file);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PostMapping("/knowledge/{id}/document/split")
    public R<List<TextSegmentVO>> split(@PathVariable String id, MultipartFile[] file, String[] patterns, Integer limit, Boolean withFilter) throws IOException {
        return R.success(documentService.split(file, patterns, limit, withFilter));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PutMapping("/knowledge/{id}/document/batch_create")
    public R<Boolean> createBatchDoc(@PathVariable("id") String id, @RequestBody List<DocumentSimple> docs) {
        return R.success(documentService.batchCreateDocs(id, KnowledgeType.BASE, docs));
    }

    @GetMapping("/knowledge/{id}/document/split_pattern")
    public R<List<KeyAndValueVO>> splitPattern(@PathVariable("id") String id) {
        return R.success(documentService.splitPattern());
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @GetMapping("/knowledge/{id}/document")
    public R<List<DocumentEntity>> listDocByKnowledgeId(@PathVariable String id) {
        return R.success(documentService.listDocByKnowledgeId(id));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_GENERATE)
    @PutMapping("/knowledge/{id}/document/batch_generate_related")
    public R<Boolean> batchGenerateRelated(@PathVariable String id, @RequestBody GenerateProblemDTO dto) {
        return R.success(documentService.batchGenerateRelated(id, dto));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_MIGRATE)
    @PutMapping("/knowledge/{id}/document/migrate/{targetKnowledgeId}")
    public R<Boolean> migrateDoc(@PathVariable("id") String sourceKnowledgeId, @PathVariable("targetKnowledgeId") String targetKnowledgeId, @RequestBody List<String> docIds) {
        return R.success(documentService.migrateDoc(sourceKnowledgeId, targetKnowledgeId, docIds));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PutMapping("/knowledge/{id}/document/batch_hit_handling")
    public R<Boolean> batchHitHandling(@PathVariable String id, @RequestBody DatasetBatchHitHandlingDTO dto) {
        return R.success(documentService.batchHitHandling(id, dto));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_DELETE)
    @PutMapping("/knowledge/{id}/document/batch_delete")
    public R<Boolean> deleteBatchDocByDocIds(@PathVariable("id") String id, @RequestBody IdListDTO dto) {
        return R.success(documentService.deleteBatchDocByDocIds(id, dto.getIdList()));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @GetMapping("/knowledge/{id}/document/{docId}")
    public R<DocumentEntity> getDocByDocId(@PathVariable String id, @PathVariable("docId") String docId) {
        return R.success(documentService.getById(docId));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_VECTOR)
    @PutMapping("/knowledge/{id}/document/{docId}/refresh")
    public R<Boolean> refresh(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody DocumentEmbedDTO dto) {
        return R.success(documentService.embedByDocIds(id, List.of(docId), dto.getStateList()));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_VECTOR)
    @PutMapping("/knowledge/{id}/document/batch_refresh")
    public R<Boolean> batchRefresh(@PathVariable String id, @RequestBody DocumentEmbedDTO dto) {
        return R.success(documentService.embedByDocIds(id, dto.getIdList(), dto.getStateList()));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PutMapping("/knowledge/{id}/document/{docId}/cancel_task")
    public R<Boolean> cancelTask(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody DocumentEntity documentEntity) {
        return R.success(documentService.cancelTask(docId, documentEntity));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PutMapping("/knowledge/{id}/document/{docId}")
    public R<DocumentEntity> updateDocByDocId(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody DocumentEntity documentEntity) {
        return R.success(documentService.updateAndGetById(docId, documentEntity));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_DELETE)
    @DeleteMapping("/knowledge/{id}/document/{docId}")
    public R<Boolean> deleteDoc(@PathVariable("id") String id, @PathVariable("docId") String docId) {
        return R.success(documentService.deleteDoc(docId));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @GetMapping("/knowledge/{id}/document/{current}/{size}")
    public R<IPage<DocumentVO>> pageDocByDatasetId(@PathVariable String id, @PathVariable("current") int current, @PathVariable("size") int size, DocQuery query) {
        return R.success(documentService.getDocByKnowledgeId(id, current, size, query));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EXPORT)
    @GetMapping("/knowledge/{id}/document/{docId}/export")
    public void export(@PathVariable("id") String id, @PathVariable("docId") String docId, HttpServletResponse response) {
        documentService.exportExcelByDocId(docId, response);
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EXPORT)
    @GetMapping("/knowledge/{id}/document/{docId}/export_zip")
    public void exportZip(@PathVariable("id") String id, @PathVariable("docId") String docId, HttpServletResponse response) throws IOException {
        documentService.exportExcelZipByDocId(docId, response);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_DOWNLOAD)
    @GetMapping("/knowledge/{id}/document/{docId}/download_source_file")
    public R<String> downloadSourceFile(@PathVariable String id, @PathVariable String docId, HttpServletResponse response) throws IOException {
        boolean flag = documentService.downloadSourceFile(docId, response);
        return flag ? R.success() : R.fail("文件不存在, 仅支持手动上传的文档");
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_REPLACE)
    @PostMapping("/knowledge/{id}/document/{docId}/replace_source_file")
    public R<String> replaceSourceFile(@PathVariable String id, @PathVariable String docId, MultipartFile file) throws IOException {
        boolean flag = documentService.replaceSourceFile(id,docId,file);
        return flag ? R.success() : R.fail("文件不存在, 仅支持手动上传的文档");
    }


}
