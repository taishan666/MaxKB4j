package com.tarzan.maxkb4j.module.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DeleteDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.ParagraphDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.service.DocumentService;
import com.tarzan.maxkb4j.module.knowledge.service.ParagraphService;
import com.tarzan.maxkb4j.module.knowledge.service.ProblemParagraphService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConst.ADMIN_API+"/workspace/default")
@AllArgsConstructor
public class ParagraphController {

    private final DocumentService documentService;
    private final ParagraphService paragraphService;
    private final ProblemParagraphService problemParagraphService;

  
    @PostMapping("/knowledge/{id}/document/{docId}/paragraph")
    public R<Boolean> createParagraph(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody ParagraphDTO paragraph) {
        return R.success(paragraphService.createParagraph(id, docId, paragraph));
    }

  
    @GetMapping("/knowledge/{id}/document/{docId}/paragraph/{page}/{size}")
    public R<IPage<ParagraphEntity>> getParagraphByProblemId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("page") int page, @PathVariable("size") int size, String title, String content) {
        return R.success(paragraphService.pageParagraphByDocId(docId, page, size, title, content));
    }

  
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/{paragraphId}")
    public R<ParagraphEntity> updateParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId, @RequestBody ParagraphEntity paragraph) {
        paragraph.setId(paragraphId);
        paragraphService.updateParagraphById(docId,paragraph);
        return R.success(paragraphService.getById(paragraphId));
    }

  
    @DeleteMapping("/knowledge/{id}/document/{docId}/paragraph/{paragraphId}")
    public R<Boolean> deleteParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId) {
        return R.success(paragraphService.deleteBatchByIds(docId,List.of(paragraphId)));
    }

 
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/batch_delete")
    public R<Boolean> deleteBatchParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody DeleteDTO dto) {
        return R.success(paragraphService.deleteBatchByIds(docId,dto.getIdList()));
    }

  
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/batch_generate_related")
    public R<Boolean> paragraphBatchGenerateRelated(@PathVariable String id, @PathVariable String docId, @RequestBody GenerateProblemDTO dto) {
        return R.success(paragraphService.batchGenerateRelated(id, docId, dto));
    }

   
    @GetMapping("/knowledge/{id}/document/{docId}/paragraph/{paragraphId}/problem")
    public R<List<ProblemEntity>> getProblemsByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId) {
        return R.success(paragraphService.getProblemsByParagraphId(paragraphId));
    }

  
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/{paragraphId}/problem/{problemId}/association")
    public R<Boolean> association(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId, @PathVariable("problemId") String problemId) {
        return R.success(problemParagraphService.association(id, docId, paragraphId, problemId));
    }

 
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/{paragraphId}/problem/{problemId}/un_association")
    public R<Boolean> unAssociation(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId, @PathVariable("problemId") String problemId) {
        return R.success(problemParagraphService.unAssociation(id, docId, paragraphId, problemId));
    }

   
    @PutMapping("/knowledge/{sourceDatasetId}/document/{sourceDocId}/paragraph/migrate/dataset/{targetDatasetId}/document/{targetDocId}")
    public R<Boolean> paragraphMigrate(@PathVariable String sourceDatasetId, @PathVariable String sourceDocId, @PathVariable String targetDatasetId, @PathVariable String targetDocId, @RequestBody List<String> paragraphIds) {
        return R.success(documentService.paragraphMigrate(sourceDatasetId, sourceDocId, targetDatasetId, targetDocId, paragraphIds));
    }
}
