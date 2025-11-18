package com.tarzan.maxkb4j.module.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.IdListDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.ParagraphAddDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.service.ParagraphService;
import com.tarzan.maxkb4j.module.knowledge.service.ProblemParagraphService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConst.ADMIN_API+"/workspace/default")
@AllArgsConstructor
public class ParagraphController {

    private final ParagraphService paragraphService;
    private final ProblemParagraphService problemParagraphService;

  
    @PostMapping("/knowledge/{id}/document/{docId}/paragraph")
    public R<Boolean> createParagraph(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody ParagraphAddDTO paragraph) {
        return R.success(paragraphService.saveParagraphAndProblem(id, docId, paragraph));
    }

  
    @GetMapping("/knowledge/{id}/document/{docId}/paragraph/{current}/{size}")
    public R<IPage<ParagraphEntity>> getParagraphByProblemId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("current") int current, @PathVariable("size") int size, String title, String content) {
        return R.success(paragraphService.pageParagraphByDocId(docId, current, size, title, content));
    }

  
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/{paragraphId}")
    public R<ParagraphEntity> updateParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId, @RequestBody ParagraphEntity paragraph) {
        paragraph.setId(paragraphId);
        paragraphService.updateParagraphById(id,docId,paragraph);
        return R.success(paragraphService.getById(paragraphId));
    }

  
    @DeleteMapping("/knowledge/{knowledgeId}/document/{docId}/paragraph/{paragraphId}")
    public R<Boolean> deleteParagraphByParagraphId(@PathVariable String knowledgeId, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId) {
        return R.success(paragraphService.deleteBatchByIds(knowledgeId, docId,List.of(paragraphId)));
    }

 
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/batch_delete")
    public R<Boolean> deleteBatchParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody IdListDTO dto) {
        return R.success(paragraphService.deleteBatchByIds(id,docId,dto.getIdList()));
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

   
    @PutMapping("/knowledge/{sourceKnowledgeId}/document/{sourceDocId}/paragraph/migrate/knowledge/{targetKnowledgeId}/document/{targetDocId}")
    public R<Boolean> paragraphMigrate(@PathVariable String sourceKnowledgeId, @PathVariable String sourceDocId, @PathVariable String targetKnowledgeId, @PathVariable String targetDocId, @RequestBody IdListDTO dto) {
        return R.success(paragraphService.paragraphMigrate(sourceKnowledgeId, sourceDocId, targetKnowledgeId, targetDocId, dto.getIdList()));
    }
    @PutMapping("/knowledge/{KnowledgeId}/document/{documentId}/paragraph/adjust_position")
    public R<Boolean> adjustPosition(@PathVariable String KnowledgeId, @PathVariable String documentId, String paragraphId, Integer newPosition) {
        return R.success(paragraphService.adjustPosition(KnowledgeId, documentId, paragraphId, newPosition));
    }

}
