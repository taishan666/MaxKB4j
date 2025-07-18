package com.tarzan.maxkb4j.module.dataset.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.dataset.domain.dto.DeleteDTO;
import com.tarzan.maxkb4j.module.dataset.domain.dto.ParagraphDTO;
import com.tarzan.maxkb4j.module.dataset.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.service.DocumentService;
import com.tarzan.maxkb4j.module.dataset.service.ParagraphService;
import com.tarzan.maxkb4j.module.dataset.service.ProblemParagraphService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(AppConst.BASE_PATH)
@AllArgsConstructor
public class ParagraphController {

    private final DocumentService documentService;
    private final ParagraphService paragraphService;
    private final ProblemParagraphService problemParagraphService;

    @SaCheckPermission("DATASET:EDIT")
    @PostMapping("/dataset/{id}/document/{docId}/paragraph")
    public R<Boolean> createParagraph(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody ParagraphDTO paragraph) {
        return R.success(paragraphService.createParagraph(id, docId, paragraph));
    }

    @SaCheckPermission("DATASET:READ")
    @GetMapping("/dataset/{id}/document/{docId}/paragraph/{page}/{size}")
    public R<IPage<ParagraphEntity>> getParagraphByProblemId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("page") int page, @PathVariable("size") int size, String title, String content) {
        return R.success(paragraphService.pageParagraphByDocId(docId, page, size, title, content));
    }

    @SaCheckPermission("DATASET:EDIT")
    @PutMapping("/dataset/{id}/document/{docId}/paragraph/{paragraphId}")
    public R<Boolean> updateParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId, @RequestBody ParagraphEntity paragraph) {
        paragraph.setId(paragraphId);
        return R.success(paragraphService.updateParagraphById(docId,paragraph));
    }

    @SaCheckPermission("DATASET:DELETE")
    @DeleteMapping("/dataset/{id}/document/{docId}/paragraph/{paragraphId}")
    public R<Boolean> deleteParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId) {
        return R.success(paragraphService.deleteBatchByIds(docId,List.of(paragraphId)));
    }

    @SaCheckPermission("DATASET:DELETE")
    @DeleteMapping("/dataset/{id}/document/{docId}/paragraph/_batch")
    public R<Boolean> deleteBatchParagraphByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @RequestBody DeleteDTO dto) {
        return R.success(paragraphService.deleteBatchByIds(docId,dto.getIdList()));
    }

    @SaCheckPermission("DATASET:READ")
    @GetMapping("/dataset/{id}/document/{docId}/paragraph/{paragraphId}/problem")
    public R<List<ProblemEntity>> getProblemsByParagraphId(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId) {
        return R.success(paragraphService.getProblemsByParagraphId(paragraphId));
    }

    @SaCheckPermission("DATASET:EDIT")
    @PutMapping("/dataset/{id}/document/{docId}/paragraph/{paragraphId}/problem/{problemId}/association")
    public R<Boolean> association(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId, @PathVariable("problemId") String problemId) {
        return R.success(problemParagraphService.association(id, docId, paragraphId, problemId));
    }

    @SaCheckPermission("DATASET:EDIT")
    @PutMapping("/dataset/{id}/document/{docId}/paragraph/{paragraphId}/problem/{problemId}/un_association")
    public R<Boolean> unAssociation(@PathVariable String id, @PathVariable("docId") String docId, @PathVariable("paragraphId") String paragraphId, @PathVariable("problemId") String problemId) {
        return R.success(problemParagraphService.unAssociation(id, docId, paragraphId, problemId));
    }

    @SaCheckPermission("DATASET:EDIT")
    @PutMapping("/dataset/{sourceDatasetId}/document/{sourceDocId}/paragraph/migrate/dataset/{targetDatasetId}/document/{targetDocId}")
    public R<Boolean> paragraphMigrate(@PathVariable String sourceDatasetId, @PathVariable String sourceDocId, @PathVariable String targetDatasetId, @PathVariable String targetDocId, @RequestBody List<String> paragraphIds) {
        return R.success(documentService.paragraphMigrate(sourceDatasetId, sourceDocId, targetDatasetId, targetDocId, paragraphIds));
    }
}
