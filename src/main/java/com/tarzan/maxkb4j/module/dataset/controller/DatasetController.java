package com.tarzan.maxkb4j.module.dataset.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.common.dto.DeleteDTO;
import com.tarzan.maxkb4j.module.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.dataset.dto.DatasetBatchHitHandlingDTO;
import com.tarzan.maxkb4j.module.dataset.dto.DocumentNameDTO;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.dto.ParagraphDTO;
import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.service.DatasetService;
import com.tarzan.maxkb4j.module.dataset.vo.*;
import com.tarzan.maxkb4j.tool.api.R;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@RestController
@AllArgsConstructor
public class DatasetController{

    @Autowired
    private DatasetService datasetService;

    // 使用正则表达式定义UUID格式
    private static final String UUID_REGEX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";


    @GetMapping("api/dataset")
    public R<List<DatasetEntity>> listDatasets(){
        return R.success(datasetService.list());
    }


    @PostMapping("api/dataset")
    public R<DatasetEntity> createDataset(@RequestBody DatasetEntity dataset){
        dataset.setMeta(new JSONObject());
        String userId= StpUtil.getLoginIdAsString();
        dataset.setUserId(UUID.fromString(userId));
        datasetService.save(dataset);
        return R.success(dataset);
    }

    @GetMapping("api/dataset/{id}")
    public R<DatasetVO> getDatasetById(@PathVariable("id") UUID id){
        return R.success(datasetService.getByDatasetId(id));
    }

    @GetMapping("api/dataset/{id}/hit_test")
    public R<List<ParagraphVO>> hitTest(@PathVariable("id") UUID id, HitTestDTO dto){
        return R.success(datasetService.hitTest(id,dto));
    }

    @PutMapping("api/dataset/{id}/re_embedding")
    public R<Boolean> reEmbedding(@PathVariable("id") UUID id){
        return R.success(datasetService.reEmbedding(id));
    }

    @PutMapping("api/dataset/{id}")
    public R<Boolean> updateDatasetById(@PathVariable("id") UUID id,@RequestBody DatasetEntity datasetEntity){
        datasetEntity.setId(id);
        return R.success(datasetService.updateById(datasetEntity));
    }

    @DeleteMapping("api/dataset/{id}")
    public R<Boolean> deleteDatasetById(@PathVariable("id") UUID id){
        return R.success(datasetService.removeById(id));
    }

    @GetMapping("api/dataset/{page}/{size}")
    public R<IPage<DatasetVO>> userDatasets(@PathVariable("page")int page, @PathVariable("size")int size, QueryDTO query){
        Page<DatasetVO> datasetPage = new Page<>(page, size);
        return R.success(datasetService.selectDatasetPage(datasetPage,query));
    }

    @GetMapping("api/dataset/{id}/application")
    public R<List<ApplicationEntity>> getApplicationByDatasetId(@PathVariable String id){
        return R.success(datasetService.getApplicationByDatasetId(id));
    }


    @GetMapping("api/dataset/{id}/document")
    public R<List<DocumentEntity>> listDocByDatasetId(@PathVariable UUID id){
        return R.success(datasetService.listDocByDatasetId(id));
    }

    @PutMapping("api/dataset/{sourceId:" + UUID_REGEX + "}/document/migrate/{targetId:" + UUID_REGEX + "}")
    public R<Boolean> migrateDoc(@PathVariable("sourceId") UUID sourceId,@PathVariable("targetId") UUID targetId,@RequestBody List<UUID> docIds){
        return R.success(datasetService.migrateDoc(sourceId,targetId,docIds));
    }

    @PutMapping("api/dataset/{id}/document/batch_hit_handling")
    public R<Boolean> batchHitHandling(@PathVariable UUID id, @RequestBody DatasetBatchHitHandlingDTO dto){
        return R.success(datasetService.batchHitHandling(id,dto));
    }

    @PutMapping("api/dataset/{id}/document/batch_refresh")
    public R<Boolean> batchRefresh(@PathVariable UUID id, @RequestBody DatasetBatchHitHandlingDTO dto){
        return R.success(datasetService.batchRefresh(id,dto));
    }

    @PostMapping("api/dataset/{id}/document/_bach")
    public R<Boolean> createBatchDoc(@PathVariable("id") UUID id, @RequestBody List<DocumentNameDTO> docs){
        return R.success(datasetService.createBatchDoc(id,docs));
    }

    @DeleteMapping("api/dataset/{id}/document/_bach")
    public R<Boolean> deleteBatchDocByDocIds(@PathVariable("id") String id, @RequestBody DeleteDTO dto){
        return R.success(datasetService.deleteBatchDocByDocIds(dto.getIdList()));
    }

    @GetMapping("api/dataset/{id}/document/{documentId}")
    public R<DocumentEntity> getDocByDocId(@PathVariable String id, @PathVariable("documentId") String documentId){
        return R.success(datasetService.getDocByDocId(UUID.fromString(documentId)));
    }

    @PutMapping("api/dataset/{id}/document/{documentId}")
    public R<DocumentEntity> updateDocByDocId(@PathVariable UUID id, @PathVariable("documentId") UUID docId,@RequestBody DocumentEntity documentEntity){
        return R.success(datasetService.updateDocByDocId(docId,documentEntity));
    }

    @DeleteMapping("api/dataset/{id}/document/{documentId}")
    public R<Boolean> deleteDocByDocId(@PathVariable UUID id, @PathVariable("documentId") UUID docId){
        return R.success(datasetService.deleteDocByDocId(docId));
    }

    @GetMapping("api/dataset/{id}/document/{page}/{size}")
    public R<IPage<DocumentVO>> pageDocByDatasetId(@PathVariable UUID id, @PathVariable("page")int page, @PathVariable("size")int size, QueryDTO query){
        return R.success(datasetService.getDocByDatasetId(id,page,size,query));
    }

    @PostMapping("api/dataset/{id}/document/{documentId}/paragraph")
    public R<Boolean> createParagraph(@PathVariable String id, @PathVariable("documentId") String documentId,@RequestBody ParagraphDTO paragraph){
        paragraph.setDatasetId(UUID.fromString(id));
        paragraph.setDocumentId(UUID.fromString(documentId));
        return R.success(datasetService.createParagraph(paragraph));
    }

    @GetMapping("api/dataset/{id}/document/{documentId}/paragraph/{page}/{size}")
    public R<IPage<ParagraphEntity>> getParagraphByProblemId(@PathVariable String id, @PathVariable("documentId") String documentId,@PathVariable("page")int page, @PathVariable("size")int size){
        return R.success(datasetService.pageParagraphByDocId(UUID.fromString(documentId),page,size));
    }

    @PutMapping("api/dataset/{id}/document/{documentId}/paragraph/{paragraphId}")
    public R<Boolean> updateParagraphByParagraphId(@PathVariable String id, @PathVariable("documentId") String documentId,@PathVariable("paragraphId")String paragraphId,@RequestBody ParagraphEntity paragraph){
        paragraph.setId(UUID.fromString(paragraphId));
        return R.success(datasetService.updateParagraphByParagraphId(paragraph));
    }

    @DeleteMapping("api/dataset/{id}/document/{documentId}/paragraph/{paragraphId}")
    public R<Boolean> deleteParagraphByParagraphId(@PathVariable String id, @PathVariable("documentId") String documentId,@PathVariable("paragraphId")String paragraphId){
        return R.success(datasetService.deleteParagraphByParagraphId(UUID.fromString(paragraphId)));
    }

    @DeleteMapping("api/dataset/{id}/document/{documentId}/paragraph/_batch")
    public R<Boolean> deleteBatchParagraphByParagraphId(@PathVariable String id, @PathVariable("documentId") String documentId, @RequestBody DeleteDTO dto){
        System.out.println("deleteBatchParagraphByParagraphId");
        return R.success(datasetService.deleteBatchParagraphByParagraphIds(dto.getIdList()));
    }

    @GetMapping("api/dataset/{id}/document/{documentId}/paragraph/{paragraphId}/problem")
    public R<List<ProblemEntity>> getProblemsByParagraphId(@PathVariable String id, @PathVariable("documentId") String documentId,@PathVariable("paragraphId")String paragraphId){
        return R.success(datasetService.getProblemsByParagraphId(UUID.fromString(paragraphId)));
    }

    @PutMapping("api/dataset/{id}/document/{documentId}/paragraph/{paragraphId}/problem/{problemId}/association")
    public R<Boolean> association(@PathVariable String id, @PathVariable("documentId") String documentId,@PathVariable("paragraphId")String paragraphId, @PathVariable("problemId")String problemId){
        return R.success(datasetService.association(UUID.fromString(id),UUID.fromString(documentId),UUID.fromString(paragraphId),UUID.fromString(problemId)));
    }

    @PutMapping("api/dataset/{id}/document/{documentId}/paragraph/{paragraphId}/problem/{problemId}/un_association")
    public R<Boolean> unAssociation(@PathVariable String id, @PathVariable("documentId") String documentId,@PathVariable("paragraphId")String paragraphId, @PathVariable("problemId")String problemId){
        return R.success(datasetService.unAssociation(UUID.fromString(id),UUID.fromString(documentId),UUID.fromString(paragraphId),UUID.fromString(problemId)));
    }


    @PostMapping("api/dataset/{id}/problem")
    public R<Boolean> createProblemsByDatasetId(@PathVariable String id,@RequestBody List<String> problems){
        return R.success(datasetService.createProblemsByDatasetId(UUID.fromString(id),problems));
    }

    @PutMapping("api/dataset/{id}/problem/{problemId}")
    public R<Boolean> updateProblemByDatasetId(@PathVariable String id,@PathVariable String problemId,@RequestBody ProblemEntity problem){
        problem.setId(UUID.fromString(problemId));
        return R.success(datasetService.updateProblemById(problem));
    }

    @DeleteMapping("api/dataset/{id}/problem/{problemId}")
    public R<Boolean> deleteProblemByDatasetId(@PathVariable("id") String id, @PathVariable("problemId") String problemId){
        return R.success(datasetService.deleteProblemByDatasetId(UUID.fromString(problemId)));
    }

    @DeleteMapping("api/dataset/{id}/problem/_batch")
    public R<Boolean> deleteBatchProblemByDatasetId(@PathVariable("id") String id, @RequestBody List<String> problemIds){
        return R.success(datasetService.deleteProblemByDatasetIds(problemIds));
    }


    @GetMapping("api/dataset/{id}/problem/{page}/{size}")
    public R<IPage<ProblemVO>> getProblemsByDatasetId(@PathVariable String id, @PathVariable("page")int page, @PathVariable("size")int size){
        return R.success(datasetService.getProblemsByDatasetId(UUID.fromString(id),page,size));
    }

    @GetMapping("api/dataset/{id}/problem/{problemId}/paragraph")
    public R<List<ParagraphEntity>> getParagraphByProblemId(@PathVariable String id,@PathVariable("problemId") String problemId){
        return R.success(datasetService.getParagraphByProblemId(UUID.fromString(problemId)));
    }


}
