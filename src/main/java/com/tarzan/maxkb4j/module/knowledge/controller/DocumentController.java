package com.tarzan.maxkb4j.module.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.base.dto.BaseQuery;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.*;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.DocumentVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.TextSegmentVO;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentService;
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
@RequestMapping(AppConst.ADMIN_API+"/workspace/default")
@AllArgsConstructor
public class DocumentController {

    private final DocumentService documentService;


 
    @PostMapping("/knowledge/{id}/document/web")
    public void web(@PathVariable("id") String id, @RequestBody WebUrlDTO params) throws IOException {
        documentService.web(id,params);
    }
 
    @PutMapping("/knowledge/{id}/document/{docId}/sync")
    public void sync(@PathVariable("id") String id,@PathVariable("docId") String docId) throws IOException {
        documentService.sync(id,docId);
    }
  
    @GetMapping("/knowledge/{knowledgeId}/document/{docId}/export")
    public void export(@PathVariable("knowledgeId") String knowledgeId, @PathVariable("docId") String docId, HttpServletResponse response) throws IOException {
        documentService.exportExcelByDocId(docId, response);
    }

  
    @GetMapping("/knowledge/{id}/document/{docId}/export_zip")
    public void exportZip(@PathVariable("id") String id, @PathVariable("docId") String docId, HttpServletResponse response) {
        documentService.exportExcelZipByDocId(docId, response);
    }

  
    @PostMapping("/knowledge/{id}/document/qa")
    public void importQa(@PathVariable("id") String id, MultipartFile[] file) throws IOException {
        documentService.importQa(id, file);
    }

   
    @PostMapping("/knowledge/{id}/document/table")
    public void importTable(@PathVariable("id") String id, MultipartFile[] file) throws IOException {
        documentService.importTable(id, file);
    }


    @PostMapping("/knowledge/{knowledgeId}/document/split")
    public R<List<TextSegmentVO>> split(@PathVariable String knowledgeId, MultipartFile[] file, String[] patterns, Integer limit, Boolean with_filter) throws IOException {
        return R.success(documentService.split(file, patterns, limit, with_filter));
    }
  
    @GetMapping("/knowledge/{knowledgeId}/document/split_pattern")
    public R<List<KeyAndValueVO>> splitPattern(@PathVariable String knowledgeId) {
        return R.success(documentService.splitPattern());
    }

  
    @GetMapping("/knowledge/{id}/document")
    public R<List<DocumentEntity>> listDocByDatasetId(@PathVariable String id) {
        return R.success(documentService.listDocByKnowledgeId(id));
    }

 
    @PutMapping("/knowledge/{id}/document/batch_generate_related")
    public R<Boolean> batchGenerateRelated(@PathVariable String id, @RequestBody GenerateProblemDTO dto) {
        return R.success(documentService.batchGenerateRelated(id, dto));
    }


 
    @PutMapping("/knowledge/{sourceKnowledgeId}/document/migrate/{targetKnowledgeId}")
    public R<Boolean> migrateDoc(@PathVariable("sourceKnowledgeId") String sourceKnowledgeId, @PathVariable("targetKnowledgeId") String targetKnowledgeId, @RequestBody List<String> docIds) {
        return R.success(documentService.migrateDoc(sourceKnowledgeId, targetKnowledgeId, docIds));
    }

    @PutMapping("/knowledge/{id}/document/batch_hit_handling")
    public R<Boolean> batchHitHandling(@PathVariable String id, @RequestBody DatasetBatchHitHandlingDTO dto) {
        return R.success(documentService.batchHitHandling(id, dto));
    }

 
    @PutMapping("/knowledge/{id}/document/batch_create")
    public R<Boolean> createBatchDoc(@PathVariable("id") String id, @RequestBody List<DocumentNameDTO> docs) {
        return R.success(documentService.createBatchDoc(id, docs));
    }

  
    @PutMapping("/knowledge/{id}/document/batch_delete")
    public R<Boolean> deleteBatchDocByDocIds(@PathVariable("id") String id, @RequestBody IdListDTO dto) {
        return R.success(documentService.deleteBatchDocByDocIds(id,dto.getIdList()));
    }

   
    @GetMapping("/knowledge/{id}/document/{docId}")
    public R<DocumentEntity> getDocByDocId(@PathVariable String id, @PathVariable("docId") String docId) {
        return R.success(documentService.getById(docId));
    }

   
    @PutMapping("/knowledge/{id}/document/{docId}/refresh")
    public R<Boolean> refresh(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody DocumentEmbedDTO dto) {
        return R.success(documentService.embedByDocIds(id,List.of(docId),dto.getStateList()));
    }

   
    @PutMapping("/knowledge/{id}/document/batch_refresh")
    public R<Boolean> batchRefresh(@PathVariable String id, @RequestBody DocumentEmbedDTO dto) {
        return R.success(documentService.embedByDocIds(id,dto.getIdList(),dto.getStateList()));
    }

  
    @PutMapping("/knowledge/{id}/document/{docId}/cancel_task")
    public R<Boolean> cancelTask(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody DocumentEntity documentEntity) {
        return R.success(documentService.cancelTask(docId, documentEntity));
    }

  
    @PutMapping("/knowledge/{id}/document/{docId}")
    public R<DocumentEntity> updateDocByDocId(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody DocumentEntity documentEntity) {
        return R.success(documentService.updateAndGetById(docId, documentEntity));
    }

  
    @DeleteMapping("/knowledge/{id}/document/{docId}")
    public R<Boolean> deleteDoc(@PathVariable("id") String id, @PathVariable("docId") String docId) {
        return R.success(documentService.deleteDoc(docId));
    }

  
    @GetMapping("/knowledge/{knowledgeId}/document/{current}/{size}")
    public R<IPage<DocumentVO>> pageDocByDatasetId(@PathVariable String knowledgeId, @PathVariable("current") int current, @PathVariable("size") int size, BaseQuery query) {
        return R.success(documentService.getDocByKnowledgeId(knowledgeId, current, size, query));
    }

    @GetMapping("/knowledge/{knowledgeId}/document/{docId}/download_source_file")
    public R<String>  downloadSourceFile(@PathVariable String knowledgeId, @PathVariable String docId, HttpServletResponse response) throws IOException {
         boolean flag=documentService.downloadSourceFile(docId,response);
         return flag?R.success():R.fail("文件不存在, 仅支持手动上传的文档");
    }


}
