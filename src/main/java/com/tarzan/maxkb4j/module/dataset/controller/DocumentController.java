package com.tarzan.maxkb4j.module.dataset.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.core.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.dataset.dto.*;
import com.tarzan.maxkb4j.module.dataset.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.service.DocumentService;
import com.tarzan.maxkb4j.module.dataset.service.EmbedTextService;
import com.tarzan.maxkb4j.module.dataset.service.ProblemParagraphService;
import com.tarzan.maxkb4j.module.dataset.vo.DocumentVO;
import com.tarzan.maxkb4j.module.dataset.vo.TextSegmentVO;
import com.tarzan.maxkb4j.module.model.info.vo.KeyAndValueVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@RestController
@AllArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final ProblemParagraphService problemParagraphService;
    private final EmbedTextService embedTextService;


    @PostMapping("api/dataset/{id}/document/web")
    public void web(@PathVariable("id") String id, @RequestBody WebUrlDTO params) throws IOException {
        documentService.web(id,params);
    }

    @PutMapping("api/dataset/{id}/document/{docId}/sync")
    public void sync(@PathVariable("id") String id,@PathVariable("docId") String docId) throws IOException {
        documentService.sync(id,docId);
    }

    @GetMapping("api/dataset/{id}/document/{docId}/export")
    public void export(@PathVariable("id") String id, @PathVariable("docId") String docId, HttpServletResponse response) throws IOException {
        documentService.exportExcelByDocId(docId, response);
    }

    @GetMapping("api/dataset/{id}/document/{docId}/export_zip")
    public void exportZip(@PathVariable("id") String id, @PathVariable("docId") String docId, HttpServletResponse response) {
        documentService.exportExcelZipByDocId(docId, response);
    }

    @PostMapping("api/dataset/{id}/document/qa")
    public void importQa(@PathVariable("id") String id, MultipartFile[] file) throws IOException {
        documentService.importQa(id, file);
    }

    @PostMapping("api/dataset/{id}/document/table")
    public void importTable(@PathVariable("id") String id, MultipartFile[] file) throws IOException {
        documentService.importTable(id, file);
    }

    @GetMapping("api/dataset/document/table_template/export")
    public void tableTemplateExport(String type, HttpServletResponse response) throws Exception {
        documentService.tableTemplateExport(type, response);
    }

    @GetMapping("api/dataset/document/template/export")
    public void templateExport(String type, HttpServletResponse response) throws Exception {
        documentService.templateExport(type, response);
    }

    @PostMapping("api/dataset/document/split")
    public R<List<TextSegmentVO>> split(MultipartFile[] file, String[] patterns, Integer limit, Boolean with_filter) throws IOException {
        return R.success(documentService.split(file, patterns, limit, with_filter));
    }

    @GetMapping("api/dataset/document/split_pattern")
    public R<List<KeyAndValueVO>> splitPattern() {
        return R.success(documentService.splitPattern());
    }

    @GetMapping("api/dataset/{id}/document")
    public R<List<DocumentEntity>> listDocByDatasetId(@PathVariable String id) {
        return R.success(documentService.listDocByDatasetId(id));
    }

    @PutMapping("api/dataset/{id}/document/batch_generate_related")
    public R<Boolean> batchGenerateRelated(@PathVariable String id, @RequestBody GenerateProblemDTO dto) {
        return R.success(embedTextService.batchGenerateRelated(id, dto));
    }

    @PutMapping("api/dataset/{id}/document/{docId}/paragraph/batch_generate_related")
    public R<Boolean> paragraphBatchGenerateRelated(@PathVariable String id, @PathVariable String docId, @RequestBody GenerateProblemDTO dto) {
        return R.success(embedTextService.paragraphBatchGenerateRelated(id, docId, dto));
    }

    @PutMapping("api/dataset/{sourceId}/document/migrate/{targetId}")
    public R<Boolean> migrateDoc(@PathVariable("sourceId") String sourceId, @PathVariable("targetId") String targetId, @RequestBody List<String> docIds) {
        return R.success(documentService.migrateDoc(sourceId, targetId, docIds));
    }

    @PutMapping("api/dataset/{id}/document/batch_hit_handling")
    public R<Boolean> batchHitHandling(@PathVariable String id, @RequestBody DatasetBatchHitHandlingDTO dto) {
        return R.success(documentService.batchHitHandling(id, dto));
    }


    @PostMapping("api/dataset/{id}/document/_bach")
    public R<Boolean> createBatchDoc(@PathVariable("id") String id, @RequestBody List<DocumentNameDTO> docs) {
        return R.success(documentService.createBatchDoc(id, docs));
    }

    @DeleteMapping("api/dataset/{id}/document/_bach")
    public R<Boolean> deleteBatchDocByDocIds(@PathVariable("id") String id, @RequestBody DeleteDTO dto) {
        return R.success(documentService.deleteBatchDocByDocIds(dto.getIdList()));
    }

    @GetMapping("api/dataset/{id}/document/{docId}")
    public R<DocumentEntity> getDocByDocId(@PathVariable String id, @PathVariable("docId") String docId) {
        return R.success(documentService.getById(docId));
    }

    @PutMapping("api/dataset/{id}/document/{docId}/refresh")
    public R<Boolean> refresh(@PathVariable String id, @PathVariable("docId") String docId) {
        return R.success(embedTextService.refresh(id, docId));
    }
    @PutMapping("api/dataset/{id}/document/batch_refresh")
    public R<Boolean> batchRefresh(@PathVariable String id, @RequestBody DatasetBatchHitHandlingDTO dto) {
        return R.success(embedTextService.batchRefresh(id, dto));
    }

    @PutMapping("api/dataset/{id}/document/{docId}/cancel_task")
    public R<Boolean> cancelTask(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody DocumentEntity documentEntity) {
        return R.success(documentService.cancelTask(docId, documentEntity));
    }

    @PutMapping("api/dataset/{id}/document/{docId}")
    public R<DocumentEntity> updateDocByDocId(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody DocumentEntity documentEntity) {
        return R.success(documentService.updateDocByDocId(docId, documentEntity));
    }

    @DeleteMapping("api/dataset/{id}/document/{docId}")
    public R<Boolean> deleteDoc(@PathVariable("id") String id, @PathVariable("docId") String docId) {
        return R.success(documentService.deleteDoc(docId));
    }

    @GetMapping("api/dataset/{id}/document/{page}/{size}")
    public R<IPage<DocumentVO>> pageDocByDatasetId(@PathVariable String id, @PathVariable("page") int page, @PathVariable("size") int size, QueryDTO query) {
        return R.success(documentService.getDocByDatasetId(id, page, size, query));
    }

    @PostMapping("api/dataset/{id}/document/{docId}/paragraph")
    public R<Boolean> createParagraph(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody ParagraphDTO paragraph) {
        return R.success(documentService.createParagraph(id, docId, paragraph));
    }

    @GetMapping("api/dataset/{id}/document/{docId}/paragraph/{page}/{size}")
    public R<IPage<ParagraphEntity>> getParagraphByProblemId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("page") int page, @PathVariable("size") int size, String title, String content) {
        return R.success(documentService.pageParagraphByDocId(docId, page, size, title, content));
    }

    @PutMapping("api/dataset/{id}/document/{docId}/paragraph/{paragraphId}")
    public R<Boolean> updateParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId, @RequestBody ParagraphEntity paragraph) {
        return R.success(documentService.updateParagraphByParagraphId(docId,paragraphId, paragraph));
    }


    @DeleteMapping("api/dataset/{id}/document/{docId}/paragraph/{paragraphId}")
    public R<Boolean> deleteParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId) {
        return R.success(documentService.deleteParagraphByParagraphId(docId, paragraphId));
    }

    @DeleteMapping("api/dataset/{id}/document/{docId}/paragraph/_batch")
    public R<Boolean> deleteBatchParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody DeleteDTO dto) {
        return R.success(documentService.deleteBatchParagraphByParagraphIds(docId, dto.getIdList()));
    }

    @GetMapping("api/dataset/{id}/document/{docId}/paragraph/{paragraphId}/problem")
    public R<List<ProblemEntity>> getProblemsByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId) {
        return R.success(documentService.getProblemsByParagraphId(paragraphId));
    }

    @PutMapping("api/dataset/{id}/document/{docId}/paragraph/{paragraphId}/problem/{problemId}/association")
    public R<Boolean> association(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId, @PathVariable("problemId") String problemId) {
        return R.success(problemParagraphService.association(id, docId, paragraphId, problemId));
    }

    @PutMapping("api/dataset/{id}/document/{docId}/paragraph/{paragraphId}/problem/{problemId}/un_association")
    public R<Boolean> unAssociation(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId, @PathVariable("problemId") String problemId) {
        return R.success(problemParagraphService.unAssociation(id, docId, paragraphId, problemId));
    }

    @PutMapping("api/dataset/{sourceDatasetId}/document/{sourceDocId}/paragraph/migrate/dataset/{targetDatasetId}/document/{targetDocId}")
    public R<Boolean> paragraphMigrate(@PathVariable String sourceDatasetId, @PathVariable String sourceDocId, @PathVariable String targetDatasetId, @PathVariable String targetDocId, @RequestBody List<String> paragraphIds) {
        return R.success(documentService.paragraphMigrate(sourceDatasetId, sourceDocId, targetDatasetId, targetDocId, paragraphIds));
    }


}
