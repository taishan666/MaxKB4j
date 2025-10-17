package com.tarzan.maxkb4j.module.knowledge.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DataSearchDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.KnowledgeDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.KnowledgeQuery;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.KnowledgeVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeService;
import com.tarzan.maxkb4j.module.knowledge.service.RetrieveService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@RestController
@RequestMapping(AppConst.ADMIN_API + "/workspace/default")
@AllArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final RetrieveService retrieveService;


    @GetMapping("/knowledge")
    public R<List<KnowledgeEntity>> listDatasets() {
        return R.success(knowledgeService.listByUserId(StpUtil.getLoginIdAsString()));
    }

    @PostMapping("/knowledge/base")
    public R<KnowledgeEntity> createDatasetBase(@RequestBody KnowledgeEntity dataset) {
        return R.success(knowledgeService.createDatasetBase(dataset));
    }

    @PostMapping("/knowledge/web")
    public R<KnowledgeEntity> createDatasetWeb(@RequestBody KnowledgeDTO dataset) {
        return R.success(knowledgeService.createDatasetWeb(dataset));
    }

    
    @GetMapping("/knowledge/{id}")
    public R<KnowledgeVO> getKnowledgeById(@PathVariable("id") String id) {
        return R.success(knowledgeService.getKnowledgeById(id));
    }

    
    @PutMapping("/knowledge/{id}/hit_test")
    public R<List<ParagraphVO>> hitTest(@PathVariable("id") String id, @RequestBody DataSearchDTO dto) {
        return R.success(retrieveService.paragraphSearch(List.of(id), dto));
    }

    
/*
    @PutMapping("/knowledge/{id}/re_embedding")
    public R<Boolean> reEmbedding(@PathVariable("id") String id) {
        return R.success(knowledgeService.reEmbedding(id));
    }
*/

    
    @PutMapping("/knowledge/{id}")
    public R<Boolean> updatedKnowledge(@PathVariable("id") String id, @RequestBody KnowledgeEntity datasetEntity) {
        datasetEntity.setId(id);
        return R.success(knowledgeService.updateById(datasetEntity));
    }

    @PutMapping("/knowledge/{id}/embedding")
    public R<Boolean> embeddingKnowledge(@PathVariable("id") String id) {
        return R.success(knowledgeService.embeddingKnowledge(id));
    }

    
    @DeleteMapping("/knowledge/{id}")
    public R<Boolean> deleteKnowledgeId(@PathVariable("id") String id) {
        return R.success(knowledgeService.deleteKnowledgeId(id));
    }

    
    @GetMapping("/knowledge/{current}/{size}")
    public R<IPage<KnowledgeVO>> knowledgePage(@PathVariable("current") int current, @PathVariable("size") int size, KnowledgeQuery query) {
        Page<KnowledgeVO> knowledgePage = new Page<>(current, size);
        return R.success(knowledgeService.selectKnowledgePage(knowledgePage, query));
    }

    
    @GetMapping("/knowledge/{id}/export")
    public void export(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        knowledgeService.exportExcel(id, response);
    }

    
    @GetMapping("/knowledge/{id}/export_zip")
    public void exportZip(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        knowledgeService.exportExcelZip(id, response);
    }


}
