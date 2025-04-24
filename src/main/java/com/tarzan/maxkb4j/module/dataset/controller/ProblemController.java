package com.tarzan.maxkb4j.module.dataset.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.service.DatasetService;
import com.tarzan.maxkb4j.module.dataset.vo.ProblemVO;
import com.tarzan.maxkb4j.core.api.R;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2025-03-18 16:00:15
 */
@RestController
@AllArgsConstructor
public class ProblemController {

    private final DatasetService datasetService;

    @SaCheckPermission("DATASET:EDIT")
    @PostMapping("api/dataset/{id}/problem")
    public R<Boolean> createProblemsByDatasetId(@PathVariable String id, @RequestBody List<String> problems) {
        return R.status(datasetService.createProblemsByDatasetId(id, problems));
    }

    @SaCheckPermission("DATASET:EDIT")
    @PutMapping("api/dataset/{id}/problem/{problemId}")
    public R<Boolean> updateProblemByDatasetId(@PathVariable String id, @PathVariable String problemId, @RequestBody ProblemEntity problem) {
        problem.setId(problemId);
        return R.status(datasetService.updateProblemById(problem));
    }

    @SaCheckPermission("DATASET:DELETE")
    @DeleteMapping("api/dataset/{id}/problem/{problemId}")
    public R<Boolean> deleteProblemByDatasetId(@PathVariable("id") String id, @PathVariable("problemId") String problemId) {
        return R.status(datasetService.deleteProblemById(problemId));
    }

    @SaCheckPermission("DATASET:DELETE")
    @DeleteMapping("api/dataset/{id}/problem/_batch")
    public R<Boolean> deleteBatchProblemByDatasetId(@PathVariable("id") String id, @RequestBody List<String> problemIds) {
        return R.status(datasetService.deleteProblemByIds(problemIds));
    }

    @SaCheckPermission("DATASET:READ")
    @GetMapping("api/dataset/{id}/problem/{page}/{size}")
    public R<IPage<ProblemVO>> getProblemsByDatasetId(@PathVariable String id, @PathVariable("page") int page, @PathVariable("size") int size, String content) {
        return R.data(datasetService.getProblemsByDatasetId(id, page, size, content));
    }

    @SaCheckPermission("DATASET:READ")
    @GetMapping("api/dataset/{id}/problem/{problemId}/paragraph")
    public R<List<ParagraphEntity>> getParagraphByProblemId(@PathVariable String id, @PathVariable("problemId") String problemId) {
        return R.data(datasetService.getParagraphByProblemId(problemId));
    }




}
