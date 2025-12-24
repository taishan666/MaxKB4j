package com.tarzan.maxkb4j.module.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.aop.SaCheckPerm;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.IdListDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.ParagraphAddDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.service.ParagraphService;
import com.tarzan.maxkb4j.module.knowledge.service.ProblemParagraphService;
import com.tarzan.maxkb4j.module.system.user.enums.PermissionEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConst.ADMIN_API + "/workspace/default")
@RequiredArgsConstructor
public class ParagraphController {

    private final ParagraphService paragraphService;
    private final ProblemParagraphService problemParagraphService;


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PostMapping("/knowledge/{id}/document/{docId}/paragraph")
    public R<Boolean> createParagraph(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody ParagraphAddDTO paragraph) {
        return R.success(paragraphService.saveParagraphAndProblem(id, docId, paragraph));
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @GetMapping("/knowledge/{id}/document/{docId}/paragraph/{current}/{size}")
    public R<IPage<ParagraphEntity>> getParagraphByProblemId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("current") int current, @PathVariable("size") int size, String title, String content) {
        return R.success(paragraphService.pageParagraphByDocId(docId, current, size, title, content));
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/{paragraphId}")
    public R<ParagraphEntity> updateParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId, @RequestBody ParagraphEntity paragraph) {
        paragraph.setId(paragraphId);
        paragraphService.updateParagraphById(id, docId, paragraph);
        return R.success(paragraphService.getById(paragraphId));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_PROBLEM_RELATE)
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/association")
    public R<Boolean> association(@PathVariable String id, @PathVariable("docId") String docId, @RequestParam String paragraphId, @RequestParam String problemId) {
        return R.status(problemParagraphService.association(id, docId, paragraphId, problemId));
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_DELETE)
    @DeleteMapping("/knowledge/{id}/document/{docId}/paragraph/{paragraphId}")
    public R<Boolean> deleteParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId) {
        return R.success(paragraphService.deleteBatchByIds(id, docId, List.of(paragraphId)));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/batch_delete")
    public R<Boolean> deleteBatchParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody IdListDTO dto) {
        return R.success(paragraphService.deleteBatchByIds(id, docId, dto.getIdList()));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_GENERATE)
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/batch_generate_related")
    public R<Boolean> paragraphBatchGenerateRelated(@PathVariable String id, @PathVariable String docId, @RequestBody GenerateProblemDTO dto) {
        return R.success(paragraphService.batchGenerateRelated(id, docId, dto));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @GetMapping("/knowledge/{id}/document/{docId}/paragraph/{paragraphId}/problem")
    public R<List<ProblemEntity>> getProblemsByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId) {
        return R.success(paragraphService.getProblemsByParagraphId(paragraphId));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/{paragraphId}/problem/{problemId}/un_association")
    public R<Boolean> unAssociation(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId, @PathVariable("problemId") String problemId) {
        return R.status(problemParagraphService.unAssociation(id, docId, paragraphId, problemId));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_MIGRATE)
    @PutMapping("/knowledge/{id}/document/{sourceDocId}/paragraph/migrate/knowledge/{targetKnowledgeId}/document/{targetDocId}")
    public R<Boolean> paragraphMigrate(@PathVariable("id") String sourceKnowledgeId, @PathVariable String sourceDocId, @PathVariable String targetKnowledgeId, @PathVariable String targetDocId, @RequestBody IdListDTO dto) {
        return R.status(paragraphService.paragraphMigrate(sourceKnowledgeId, sourceDocId, targetKnowledgeId, targetDocId, dto.getIdList()));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PutMapping("/knowledge/{id}/document/{documentId}/paragraph/adjust_position")
    public R<Boolean> adjustPosition(@PathVariable String id, @PathVariable String documentId, String paragraphId, Integer newPosition) {
        return R.status(paragraphService.adjustPosition(id, documentId, paragraphId, newPosition));
    }

}
