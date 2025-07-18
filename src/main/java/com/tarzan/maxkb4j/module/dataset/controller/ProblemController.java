package com.tarzan.maxkb4j.module.dataset.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.dataset.domain.dto.ProblemDTO;
import com.tarzan.maxkb4j.module.dataset.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.domain.vo.ProblemVO;
import com.tarzan.maxkb4j.module.dataset.service.DatasetService;
import com.tarzan.maxkb4j.module.dataset.service.ProblemService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2025-03-18 16:00:15
 */
@RestController
@RequestMapping(AppConst.BASE_PATH)
@AllArgsConstructor
public class ProblemController {

    private final DatasetService datasetService;
    private final ProblemService problemService;

    @SaCheckPermission("DATASET:EDIT")
    @PostMapping("/dataset/{id}/problem")
    public R<Boolean> createProblemsByDatasetId(@PathVariable String id, @RequestBody List<String> problems) {
        return R.status(datasetService.createProblemsByDatasetId(id, problems));
    }

    @SaCheckPermission("DATASET:EDIT")
    @PostMapping("/dataset/{datasetId}/document/{documentId}/paragraph/{paragraphId}/problem")
    public R<Boolean> createProblemsByParagraphId(@PathVariable String datasetId,@PathVariable String documentId, @PathVariable String paragraphId,  @RequestBody ProblemDTO dto) {
        return R.status(problemService.createProblemsByParagraphId(datasetId,documentId,paragraphId, dto));
    }

    @SaCheckPermission("DATASET:EDIT")
    @PutMapping("/dataset/{id}/problem/{problemId}")
    public R<Boolean> updateProblemByDatasetId(@PathVariable String id, @PathVariable String problemId, @RequestBody ProblemEntity problem) {
        problem.setId(problemId);
        return R.status(datasetService.updateProblemById(problem));
    }

    @SaCheckPermission("DATASET:DELETE")
    @DeleteMapping("/dataset/{id}/problem/{problemId}")
    public R<Boolean> deleteProblemByDatasetId(@PathVariable("id") String id, @PathVariable("problemId") String problemId) {
        return R.status(datasetService.deleteProblemById(problemId));
    }

    @SaCheckPermission("DATASET:DELETE")
    @DeleteMapping("/dataset/{id}/problem/_batch")
    public R<Boolean> deleteBatchProblemByDatasetId(@PathVariable("id") String id, @RequestBody List<String> problemIds) {
        return R.status(datasetService.deleteProblemByIds(problemIds));
    }

    @SaCheckPermission("DATASET:READ")
    @GetMapping("/dataset/{id}/problem/{page}/{size}")
    public R<IPage<ProblemVO>> getProblemsByDatasetId(@PathVariable String id, @PathVariable("page") int page, @PathVariable("size") int size, String content) {
        return R.data(datasetService.getProblemsByDatasetId(id, page, size, content));
    }

    @SaCheckPermission("DATASET:READ")
    @GetMapping("/dataset/{id}/problem/{problemId}/paragraph")
    public R<List<ParagraphEntity>> getParagraphByProblemId(@PathVariable String id, @PathVariable("problemId") String problemId) {
        return R.data(datasetService.getParagraphByProblemId(problemId));
    }




}
