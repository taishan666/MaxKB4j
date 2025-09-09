package com.tarzan.maxkb4j.module.knowledge.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DataSearchDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.KnowledgeDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.KnowledgeQuery;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.KnowledgeVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeService;
import com.tarzan.maxkb4j.module.knowledge.service.RetrieveService;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
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
@RequestMapping(AppConst.ADMIN_PATH + "/workspace/default")
@AllArgsConstructor
public class KnowledgeController {

    private final KnowledgeService datasetService;
    private final RetrieveService retrieveService;


    //  @SaCheckPermission("DATASET:READ")
    @GetMapping("/knowledge")
    public R<List<KnowledgeEntity>> listDatasets() {
        return R.success(datasetService.listByUserId(StpUtil.getLoginIdAsString()));
    }

    //  @SaCheckPermission("DATASET:CREATE")
    @PostMapping("/knowledge/base")
    public R<KnowledgeEntity> createDatasetBase(@RequestBody KnowledgeEntity dataset) {
        return R.success(datasetService.createDatasetBase(dataset));
    }

    // @SaCheckPermission("DATASET:CREATE")
    @PostMapping("/knowledge/web")
    public R<KnowledgeEntity> createDatasetWeb(@RequestBody KnowledgeDTO dataset) {
        return R.success(datasetService.createDatasetWeb(dataset));
    }

    //  @SaCheckPermission("DATASET:READ")
    @GetMapping("/knowledge/{id}")
    public R<KnowledgeVO> getDatasetById(@PathVariable("id") String id) {
        return R.success(datasetService.getByDatasetId(id));
    }

    // @SaCheckPermission("DATASET:READ")
    @PutMapping("/knowledge/{id}/hit_test")
    public R<List<ParagraphVO>> hitTest(@PathVariable("id") String id, @RequestBody DataSearchDTO dto) {
        return R.success(retrieveService.paragraphSearch(List.of(id), dto));
    }

    //  @SaCheckPermission("DATASET:EDIT")
    @PutMapping("/knowledge/{id}/re_embedding")
    public R<Boolean> reEmbedding(@PathVariable("id") String id) {
        return R.success(datasetService.reEmbedding(id));
    }

    // @SaCheckPermission("DATASET:EDIT")
    @PutMapping("/knowledge/{id}")
    public R<Boolean> updateDatasetById(@PathVariable("id") String id, @RequestBody KnowledgeEntity datasetEntity) {
        datasetEntity.setId(id);
        return R.success(datasetService.updateById(datasetEntity));
    }

    //  @SaCheckPermission("DATASET:DELETE")
    @DeleteMapping("/knowledge/{id}")
    public R<Boolean> deleteDatasetById(@PathVariable("id") String id) {
        return R.success(datasetService.deleteDatasetById(id));
    }

    // @SaCheckPermission("DATASET:READ")
    @GetMapping("/knowledge/{current}/{size}")
    public R<IPage<KnowledgeVO>> knowledgePage(@PathVariable("current") int current, @PathVariable("size") int size, KnowledgeQuery query) {
        Page<KnowledgeVO> knowledgePage = new Page<>(current, size);
        return R.success(datasetService.selectKnowledgePage(knowledgePage, query));
    }

    // @SaCheckPermission("DATASET:READ")
    @GetMapping("/knowledge/{id}/export")
    public void export(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        datasetService.exportExcelByDatasetId(id, response);
    }

    //  @SaCheckPermission("DATASET:READ")
    @GetMapping("/knowledge/{id}/export_zip")
    public void exportZip(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        datasetService.exportExcelZipByDatasetId(id, response);
    }

    // @SaCheckPermission("DATASET:READ")
    @GetMapping("/knowledge/{id}/application")
    public R<List<ApplicationEntity>> getApplicationByDatasetId(@PathVariable String id) {
        return R.success(datasetService.getApplicationByDatasetId(id));
    }

    // @SaCheckPermission("DATASET:READ")
    @GetMapping("/knowledge/{id}/model")
    public R<List<ModelEntity>> getModels(@PathVariable String id) {
        return R.success(datasetService.getModels(id));
    }


}
