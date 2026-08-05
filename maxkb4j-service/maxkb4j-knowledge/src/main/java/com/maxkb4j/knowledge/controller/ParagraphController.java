package com.maxkb4j.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.knowledge.dto.GenerateProblemDTO;
import com.maxkb4j.knowledge.dto.IdListDTO;
import com.maxkb4j.knowledge.dto.ParagraphAddDTO;
import com.maxkb4j.knowledge.entity.ParagraphEntity;
import com.maxkb4j.knowledge.service.IParagraphInternalService;
import com.maxkb4j.knowledge.service.IProblemParagraphService;
import com.maxkb4j.knowledge.vo.ParagraphBaseVO;
import com.maxkb4j.knowledge.vo.ParagraphPageVO;
import com.maxkb4j.knowledge.vo.ProblemSimpleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class ParagraphController {

    private final IParagraphInternalService paragraphService;
    private final IProblemParagraphService problemParagraphService;


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PostMapping("/knowledge/{id}/document/{docId}/paragraph")
    public R<Boolean> createParagraph(@PathVariable String id, @PathVariable("docId") String docId, @Valid @RequestBody ParagraphAddDTO paragraph) {
        return R.status(paragraphService.saveParagraphAndProblem(id, docId, paragraph));
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @GetMapping("/knowledge/{id}/document/{docId}/paragraph/{current}/{size}")
    public R<IPage<ParagraphPageVO>> getParagraphByProblemId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("current") int current, @PathVariable("size") int size, String title, String content) {
        return R.data(BeanUtil.copyPage(paragraphService.pageParagraphByDocId(docId, current, size, title, content), ParagraphPageVO.class));
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/{paragraphId}")
    public R<ParagraphBaseVO> updateParagraphById(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId, @RequestBody ParagraphEntity paragraph) {
        paragraph.setId(paragraphId);
        paragraphService.updateParagraphById(id, docId, paragraph);
        ParagraphEntity e = paragraphService.getById(paragraphId);
        return R.data(e == null ? null : BeanUtil.copy(e, ParagraphBaseVO.class));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_PROBLEM_RELATE)
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/association")
    public R<Boolean> association(@PathVariable String id, @PathVariable("docId") String docId, @RequestParam String paragraphId, @RequestParam String problemId) {
        return R.status(problemParagraphService.association(id, docId, paragraphId, problemId));
    }


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_DELETE)
    @DeleteMapping("/knowledge/{id}/document/{docId}/paragraph/{paragraphId}")
    public R<Boolean> deleteParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId) {
        return R.status(paragraphService.deleteBatchByIds(id, docId, List.of(paragraphId)));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/batch_delete")
    public R<Boolean> deleteBatchParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @Valid @RequestBody IdListDTO dto) {
        return R.status(paragraphService.deleteBatchByIds(id, docId, dto.getIdList()));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_GENERATE)
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/batch_generate_related")
    public R<Boolean> paragraphBatchGenerateRelated(@PathVariable String id, @PathVariable String docId, @Valid @RequestBody GenerateProblemDTO dto) {
        return R.status(paragraphService.batchGenerateRelated(id, docId, dto));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @GetMapping("/knowledge/{id}/document/{docId}/paragraph/{paragraphId}/problem")
    public R<List<ProblemSimpleVO>> getProblemsByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId) {
        return R.data(paragraphService.getProblemsByParagraphId(paragraphId));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_READ)
    @PutMapping("/knowledge/{id}/document/{docId}/paragraph/un_association")
    public R<Boolean> unAssociation(@PathVariable String id, @PathVariable("docId") String docId, @RequestParam("paragraphId") String paragraphId, @RequestParam("problemId") String problemId) {
        return R.status(problemParagraphService.unAssociation(id, docId, paragraphId, problemId));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_MIGRATE)
    @PutMapping("/knowledge/{id}/document/{sourceDocId}/paragraph/migrate/knowledge/{targetKnowledgeId}/document/{targetDocId}")
    public R<Boolean> paragraphMigrate(@PathVariable("id") String sourceKnowledgeId, @PathVariable String sourceDocId, @PathVariable String targetKnowledgeId, @PathVariable String targetDocId, @Valid @RequestBody IdListDTO dto) {
        return R.status(paragraphService.paragraphMigrate(sourceKnowledgeId, sourceDocId, targetKnowledgeId, targetDocId, dto.getIdList()));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_EDIT)
    @PutMapping("/knowledge/{id}/document/{documentId}/paragraph/adjust_position")
    public R<Boolean> adjustPosition(@PathVariable String id, @PathVariable String documentId, String paragraphId, Integer newPosition, Integer targetIndex) {
        return R.status(paragraphService.adjustPosition(id, documentId, paragraphId,newPosition,targetIndex));
    }

}
