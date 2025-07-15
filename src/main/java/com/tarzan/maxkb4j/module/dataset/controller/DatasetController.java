package com.tarzan.maxkb4j.module.dataset.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.common.dto.Query;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.dataset.domain.dto.DatasetDTO;
import com.tarzan.maxkb4j.module.dataset.domain.dto.DataSearchDTO;
import com.tarzan.maxkb4j.module.dataset.domain.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.service.DatasetService;
import com.tarzan.maxkb4j.module.dataset.service.EmbedTextService;
import com.tarzan.maxkb4j.module.dataset.service.RetrieveService;
import com.tarzan.maxkb4j.module.dataset.domain.vo.DatasetVO;
import com.tarzan.maxkb4j.module.dataset.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.core.api.R;
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
@RequestMapping(AppConst.BASE_PATH)
@AllArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;
    private final RetrieveService retrieveService;
    private final EmbedTextService embedTextService;


    @SaCheckPermission("DATASET:READ")
    @GetMapping("/dataset")
    public R<List<DatasetEntity>> listDatasets() {
        return R.success(datasetService.listByUserId(StpUtil.getLoginIdAsString()));
    }

    @SaCheckPermission("DATASET:CREATE")
    @PostMapping("/dataset")
    public R<DatasetEntity> createDataset(@RequestBody DatasetEntity dataset) {
        return R.success(datasetService.createDataset(dataset));
    }

    @SaCheckPermission("DATASET:CREATE")
    @PostMapping("/dataset/web")
    public R<DatasetEntity> createDatasetWeb(@RequestBody DatasetDTO dataset) {
        return R.success(datasetService.createWebDataset(dataset));
    }

    @SaCheckPermission("DATASET:READ")
    @GetMapping("/dataset/{id}")
    public R<DatasetVO> getDatasetById(@PathVariable("id") String id) {
        return R.success(datasetService.getByDatasetId(id));
    }

    @SaCheckPermission("DATASET:READ")
    @GetMapping("/dataset/{id}/hit_test")
    public R<List<ParagraphVO>> hitTest(@PathVariable("id") String id, DataSearchDTO dto) {
        return R.success(retrieveService.paragraphSearch(List.of(id), dto));
    }

    @SaCheckPermission("DATASET:EDIT")
    @PutMapping("/dataset/{id}/re_embedding")
    public R<Boolean> reEmbedding(@PathVariable("id") String id) {
        return R.success(embedTextService.reEmbedding(id));
    }

    @SaCheckPermission("DATASET:EDIT")
    @PutMapping("/dataset/{id}")
    public R<Boolean> updateDatasetById(@PathVariable("id") String id, @RequestBody DatasetEntity datasetEntity) {
        datasetEntity.setId(id);
        return R.success(datasetService.updateById(datasetEntity));
    }

    @SaCheckPermission("DATASET:DELETE")
    @DeleteMapping("/dataset/{id}")
    public R<Boolean> deleteDatasetById(@PathVariable("id") String id) {
        return R.success(datasetService.deleteDatasetById(id));
    }

    @SaCheckPermission("DATASET:READ")
    @GetMapping("/dataset/{page}/{size}")
    public R<IPage<DatasetVO>> userDatasets(@PathVariable("page") int page, @PathVariable("size") int size, Query query) {
        Page<DatasetVO> datasetPage = new Page<>(page, size);
        return R.success(datasetService.selectDatasetPage(datasetPage, query));
    }

    @SaCheckPermission("DATASET:READ")
    @GetMapping("/dataset/{id}/export")
    public void export(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        datasetService.exportExcelByDatasetId(id, response);
    }

    @SaCheckPermission("DATASET:READ")
    @GetMapping("/dataset/{id}/export_zip")
    public void exportZip(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        datasetService.exportExcelZipByDatasetId(id, response);
    }
    @SaCheckPermission("DATASET:READ")
    @GetMapping("/dataset/{id}/application")
    public R<List<ApplicationEntity>> getApplicationByDatasetId(@PathVariable String id) {
        return R.success(datasetService.getApplicationByDatasetId(id));
    }

    @SaCheckPermission("DATASET:READ")
    @GetMapping("/dataset/{id}/model")
    public R<List<ModelEntity>> getModels(@PathVariable String id) {
        return R.success(datasetService.getModels(id));
    }



}
