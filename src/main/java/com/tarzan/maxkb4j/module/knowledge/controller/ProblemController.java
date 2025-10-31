package com.tarzan.maxkb4j.module.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.ProblemDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ProblemVO;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeService;
import com.tarzan.maxkb4j.module.knowledge.service.ProblemService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2025-03-18 16:00:15
 */
@RestController
@RequestMapping(AppConst.ADMIN_API+"/workspace/default")
@AllArgsConstructor
public class ProblemController {

    private final KnowledgeService datasetService;
    private final ProblemService problemService;

  
    @PostMapping("/knowledge/{id}/problem")
    public R<Boolean> createProblemsByDatasetId(@PathVariable String id, @RequestBody List<String> problems) {
        return R.status(problemService.createProblemsByDatasetId(id, problems));
    }

  
    @PostMapping("/knowledge/{knowledgeId}/document/{documentId}/paragraph/{paragraphId}/problem")
    public R<Boolean> createProblemsByParagraphId(@PathVariable String knowledgeId,@PathVariable String documentId, @PathVariable String paragraphId,  @RequestBody ProblemDTO dto) {
        return R.status(problemService.createProblemsByParagraphId(knowledgeId,documentId,paragraphId, dto));
    }

   
    @PutMapping("/knowledge/{id}/problem/{problemId}")
    public R<Boolean> updateProblemByDatasetId(@PathVariable String id, @PathVariable String problemId, @RequestBody ProblemEntity problem) {
        problem.setId(problemId);
        return R.status(problemService.updateById(problem));
    }

  
    @DeleteMapping("/knowledge/{id}/problem/{problemId}")
    public R<Boolean> deleteProblemByDatasetId(@PathVariable("id") String id, @PathVariable("problemId") String problemId) {
        return R.status(problemService.deleteProblemByIds(id,List.of(problemId)));
    }

  
    @PutMapping("/knowledge/{id}/problem/batch_delete")
    public R<Boolean> deleteBatchProblemByDatasetId(@PathVariable("id") String id, @RequestBody List<String> problemIds) {
        return R.status(problemService.deleteProblemByIds(id,problemIds));
    }

   
    @GetMapping("/knowledge/{id}/problem/{page}/{size}")
    public R<IPage<ProblemVO>> getProblemsByDatasetId(@PathVariable String id, @PathVariable("page") int page, @PathVariable("size") int size, String content) {
        return R.data(problemService.pageByDatasetId(id, page, size, content));
    }

  
    @GetMapping("/knowledge/{id}/problem/{problemId}/paragraph")
    public R<List<ParagraphEntity>> getParagraphByProblemId(@PathVariable String id, @PathVariable("problemId") String problemId) {
        return R.data(datasetService.getParagraphByProblemId(problemId));
    }




}
