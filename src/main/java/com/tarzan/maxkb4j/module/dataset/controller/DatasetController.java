package com.tarzan.maxkb4j.module.dataset.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.core.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.dataset.dto.DatasetDTO;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.service.DatasetService;
import com.tarzan.maxkb4j.module.dataset.service.EmbedTextService;
import com.tarzan.maxkb4j.module.dataset.service.RetrieveService;
import com.tarzan.maxkb4j.module.dataset.vo.DatasetVO;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
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
@AllArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;
    private final RetrieveService retrieveService;
    private final EmbedTextService embedTextService;


    @GetMapping("api/dataset")
    public R<List<DatasetEntity>> listDatasets() {
        return R.success(datasetService.listByUserId(StpUtil.getLoginIdAsString()));
    }

    @PostMapping("api/dataset")
    public R<DatasetEntity> createDataset(@RequestBody DatasetEntity dataset) {
        return R.success(datasetService.createDataset(dataset));
    }

    @PostMapping("api/dataset/web")
    public R<DatasetEntity> createDatasetWeb(@RequestBody DatasetDTO dataset) {
        return R.success(datasetService.createWebDataset(dataset));
    }

    @GetMapping("api/dataset/{id}")
    public R<DatasetVO> getDatasetById(@PathVariable("id") String id) {
        return R.success(datasetService.getByDatasetId(id));
    }

    @GetMapping("api/dataset/{id}/hit_test")
    public R<List<ParagraphVO>> hitTest(@PathVariable("id") String id, HitTestDTO dto) {
        return R.success(retrieveService.paragraphSearch(List.of(id), dto));
    }

    @PutMapping("api/dataset/{id}/re_embedding")
    public R<Boolean> reEmbedding(@PathVariable("id") String id) {
        return R.success(embedTextService.reEmbedding(id));
    }

    @PutMapping("api/dataset/{id}")
    public R<Boolean> updateDatasetById(@PathVariable("id") String id, @RequestBody DatasetEntity datasetEntity) {
        datasetEntity.setId(id);
        return R.success(datasetService.updateById(datasetEntity));
    }

    @DeleteMapping("api/dataset/{id}")
    public R<Boolean> deleteDatasetById(@PathVariable("id") String id) {
        return R.success(datasetService.deleteDatasetById(id));
    }

    @GetMapping("api/dataset/{page}/{size}")
    public R<IPage<DatasetVO>> userDatasets(@PathVariable("page") int page, @PathVariable("size") int size, QueryDTO query) {
        Page<DatasetVO> datasetPage = new Page<>(page, size);
        return R.success(datasetService.selectDatasetPage(datasetPage, query));
    }

    @GetMapping("api/dataset/{id}/export")
    public void export(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        datasetService.exportExcelByDatasetId(id, response);
    }


    @GetMapping("api/dataset/{id}/export_zip")
    public void exportZip(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        datasetService.exportExcelZipByDatasetId(id, response);
    }

    @GetMapping("api/dataset/{id}/application")
    public R<List<ApplicationEntity>> getApplicationByDatasetId(@PathVariable String id) {
        return R.success(datasetService.getApplicationByDatasetId(id));
    }

    @GetMapping("api/dataset/{id}/model")
    public R<List<ModelEntity>> getModels(@PathVariable String id) {
        return R.success(datasetService.getModels(id));
    }



}
