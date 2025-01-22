package com.tarzan.maxkb4j.module.dataset.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.common.dto.DeleteDTO;
import com.tarzan.maxkb4j.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.dataset.dto.*;
import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.service.DatasetService;
import com.tarzan.maxkb4j.module.dataset.vo.*;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.tool.api.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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


    @GetMapping("api/dataset")
    public R<List<DatasetEntity>> listDatasets(){
        UUID userId=UUID.fromString(StpUtil.getLoginIdAsString());
        return R.success(datasetService.listByUserId(userId));
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
        return R.success(datasetService.deleteDatasetById(id));
    }

    @GetMapping("api/dataset/{page}/{size}")
    public R<IPage<DatasetVO>> userDatasets(@PathVariable("page")int page, @PathVariable("size")int size, QueryDTO query){
        Page<DatasetVO> datasetPage = new Page<>(page, size);
        return R.success(datasetService.selectDatasetPage(datasetPage,query));
    }

    @GetMapping("api/dataset/{id}/export")
    public void export(@PathVariable("id") UUID id, HttpServletResponse response) throws IOException {
        datasetService.exportExcelByDatasetId(id,response);
    }



    @GetMapping("api/dataset/{id}/export_zip")
    public void exportZip(@PathVariable("id") UUID id, HttpServletResponse response) throws IOException {
        datasetService.exportExcelZipByDatasetId(id,response);
    }

    @DeleteMapping("api/dataset/{id}/document/{docId}")
    public  R<Boolean>  deleteDoc(@PathVariable("id") UUID id, @PathVariable("docId") UUID docId) throws IOException {
        return R.success(datasetService.deleteDoc(docId));
    }

    @GetMapping("api/dataset/{id}/document/{docId}/export")
    public void export(@PathVariable("id") UUID id,@PathVariable("docId") UUID docId, HttpServletResponse response) throws IOException {
        datasetService.exportExcelByDocId(docId,response);
    }

    @GetMapping("api/dataset/{id}/document/{docId}/export_zip")
    public void exportZip(@PathVariable("id") UUID id,@PathVariable("docId") UUID docId, HttpServletResponse response) throws IOException {
        datasetService.exportExcelZipByDocId(docId,response);
    }

    @PostMapping("api/dataset/{id}/document/qa")
    public void importQa(@PathVariable("id") UUID id, MultipartFile[] file) throws IOException {
        datasetService.importQa(id,file);
    }

    @PostMapping("api/dataset/{id}/document/table")
    public void importTable(@PathVariable("id") UUID id, MultipartFile[] file) throws IOException {
        datasetService.importTable(id,file);
    }

    @GetMapping("api/dataset/document/table_template/export")
    public void tableTemplateExport(String type,HttpServletResponse response) throws Exception {
         datasetService.tableTemplateExport(type,response);
    }

    @GetMapping("api/dataset/document/template/export")
    public void templateExport(String type,HttpServletResponse response) throws Exception {
        datasetService.templateExport(type,response);
    }

    @PostMapping("api/dataset/document/split")
    public R<List<TextSegmentVO>>  split(MultipartFile[] file) throws IOException {
        return R.success(datasetService.split(file));
    }

    @GetMapping("api/dataset/{id}/application")
    public R<List<ApplicationEntity>> getApplicationByDatasetId(@PathVariable UUID id){
        return R.success(datasetService.getApplicationByDatasetId(id));
    }

    @GetMapping("api/dataset/{id}/model")
    public R<List<ModelEntity>> getModels(@PathVariable UUID id){
        return R.success(datasetService.getModels(id));
    }


    @GetMapping("api/dataset/{id}/document")
    public R<List<DocumentEntity>> listDocByDatasetId(@PathVariable UUID id){
        return R.success(datasetService.listDocByDatasetId(id));
    }

    @PutMapping("api/dataset/{id}/document/batch_generate_related")
    public R<Boolean> batchGenerateRelated(@PathVariable UUID id,@RequestBody GenerateProblemDTO dto){
        return R.success(datasetService.batchGenerateRelated(id,dto));
    }

    @PutMapping("api/dataset/{id}/document/{docId}/paragraph/batch_generate_related")
    public R<Boolean> paragraphBatchGenerateRelated(@PathVariable UUID id,@PathVariable UUID docId,@RequestBody GenerateProblemDTO dto){
        return R.success(datasetService.paragraphBatchGenerateRelated(id,docId,dto));
    }

    @PutMapping("api/dataset/{sourceId}/document/migrate/{targetId}")
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
    public R<Boolean> deleteBatchDocByDocIds(@PathVariable("id") UUID id, @RequestBody DeleteDTO dto){
        return R.success(datasetService.deleteBatchDocByDocIds(dto.getIdList()));
    }

    @GetMapping("api/dataset/{id}/document/{docId}")
    public R<DocumentEntity> getDocByDocId(@PathVariable UUID id, @PathVariable("docId") UUID docId){
        return R.success(datasetService.getDocByDocId(docId));
    }

    @PutMapping("api/dataset/{id}/document/{docId}/refresh")
    public R<Boolean> refresh(@PathVariable UUID id, @PathVariable("docId") UUID docId){
        return R.success(datasetService.refresh(id,docId));
    }

    @PutMapping("api/dataset/{id}/document/{docId}/cancel_task")
    public R<Boolean> cancelTask(@PathVariable UUID id, @PathVariable("docId") UUID docId,@RequestBody JSONObject json){
        return R.success(datasetService.cancelTask(docId,1));
    }

    @PutMapping("api/dataset/{id}/document/{docId}")
    public R<DocumentEntity> updateDocByDocId(@PathVariable UUID id, @PathVariable("docId") UUID docId,@RequestBody DocumentEntity documentEntity){
        return R.success(datasetService.updateDocByDocId(docId,documentEntity));
    }

    @DeleteMapping("api/dataset/{id}/document/{docId}")
    public R<Boolean> deleteDocByDocId(@PathVariable UUID id, @PathVariable("docId") UUID docId){
        return R.success(datasetService.deleteDocByDocId(docId));
    }

    @GetMapping("api/dataset/{id}/document/{page}/{size}")
    public R<IPage<DocumentVO>> pageDocByDatasetId(@PathVariable UUID id, @PathVariable("page")int page, @PathVariable("size")int size, QueryDTO query){
        return R.success(datasetService.getDocByDatasetId(id,page,size,query));
    }

    @PostMapping("api/dataset/{id}/document/{docId}/paragraph")
    public R<Boolean> createParagraph(@PathVariable UUID id, @PathVariable("docId") UUID docId,@RequestBody ParagraphDTO paragraph){
        return R.success(datasetService.createParagraph(id,docId,paragraph));
    }

    @GetMapping("api/dataset/{id}/document/{docId}/paragraph/{page}/{size}")
    public R<IPage<ParagraphEntity>> getParagraphByProblemId(@PathVariable UUID id, @PathVariable("docId") UUID docId,@PathVariable("page")int page, @PathVariable("size")int size,String title,String content){
        return R.success(datasetService.pageParagraphByDocId(docId,page,size,title,content));
    }

    @PutMapping("api/dataset/{id}/document/{docId}/paragraph/{paragraphId}")
    public R<Boolean> updateParagraphByParagraphId(@PathVariable UUID id, @PathVariable("docId") UUID docId,@PathVariable("paragraphId")UUID paragraphId,@RequestBody ParagraphEntity paragraph){
        paragraph.setId(paragraphId);
        return R.success(datasetService.updateParagraphByParagraphId(paragraph));
    }

    @DeleteMapping("api/dataset/{id}/document/{docId}/paragraph/{paragraphId}")
    public R<Boolean> deleteParagraphByParagraphId(@PathVariable UUID id, @PathVariable("docId") UUID docId,@PathVariable("paragraphId")UUID paragraphId){
        return R.success(datasetService.deleteParagraphByParagraphId(paragraphId));
    }

    @DeleteMapping("api/dataset/{id}/document/{docId}/paragraph/_batch")
    public R<Boolean> deleteBatchParagraphByParagraphId(@PathVariable UUID id, @PathVariable("docId") UUID docId, @RequestBody DeleteDTO dto){
        return R.success(datasetService.deleteBatchParagraphByParagraphIds(dto.getIdList()));
    }

    @GetMapping("api/dataset/{id}/document/{docId}/paragraph/{paragraphId}/problem")
    public R<List<ProblemEntity>> getProblemsByParagraphId(@PathVariable UUID id, @PathVariable("docId") UUID docId,@PathVariable("paragraphId")UUID paragraphId){
        return R.success(datasetService.getProblemsByParagraphId(paragraphId));
    }

    @PutMapping("api/dataset/{id}/document/{docId}/paragraph/{paragraphId}/problem/{problemId}/association")
    public R<Boolean> association(@PathVariable UUID id, @PathVariable("docId") UUID docId,@PathVariable("paragraphId")UUID paragraphId, @PathVariable("problemId")UUID problemId){
        return R.success(datasetService.association(id,docId,paragraphId,problemId));
    }

    @PutMapping("api/dataset/{id}/document/{docId}/paragraph/{paragraphId}/problem/{problemId}/un_association")
    public R<Boolean> unAssociation(@PathVariable UUID id, @PathVariable("docId") UUID docId,@PathVariable("paragraphId")UUID paragraphId, @PathVariable("problemId")UUID problemId){
        return R.success(datasetService.unAssociation(id,docId,paragraphId,problemId));
    }


    @PostMapping("api/dataset/{id}/problem")
    public R<Boolean> createProblemsByDatasetId(@PathVariable UUID id,@RequestBody List<String> problems){
        return R.success(datasetService.createProblemsByDatasetId(id,problems));
    }

    @PutMapping("api/dataset/{id}/problem/{problemId}")
    public R<Boolean> updateProblemByDatasetId(@PathVariable UUID id,@PathVariable UUID problemId,@RequestBody ProblemEntity problem){
        problem.setId(problemId);
        return R.success(datasetService.updateProblemById(problem));
    }

    @DeleteMapping("api/dataset/{id}/problem/{problemId}")
    public R<Boolean> deleteProblemByDatasetId(@PathVariable("id") UUID id, @PathVariable("problemId") UUID problemId){
        return R.success(datasetService.deleteProblemByDatasetId(problemId));
    }

    @DeleteMapping("api/dataset/{id}/problem/_batch")
    public R<Boolean> deleteBatchProblemByDatasetId(@PathVariable("id") UUID id, @RequestBody List<UUID> problemIds){
        return R.success(datasetService.deleteProblemByDatasetIds(problemIds));
    }


    @GetMapping("api/dataset/{id}/problem/{page}/{size}")
    public R<IPage<ProblemVO>> getProblemsByDatasetId(@PathVariable UUID id, @PathVariable("page")int page, @PathVariable("size")int size,String content){
        return R.success(datasetService.getProblemsByDatasetId(id,page,size,content));
    }

    @GetMapping("api/dataset/{id}/problem/{problemId}/paragraph")
    public R<List<ParagraphEntity>> getParagraphByProblemId(@PathVariable UUID id,@PathVariable("problemId") UUID problemId){
        return R.success(datasetService.getParagraphByProblemId(problemId));
    }

    @PutMapping("api/dataset/{sourceDatasetId}/document/{sourceDocId}/paragraph/migrate/dataset/{targetDatasetId}/document/{targetDocId}")
    public R<Boolean> paragraphMigrate(@PathVariable UUID sourceDatasetId,@PathVariable UUID sourceDocId,@PathVariable UUID targetDatasetId,@PathVariable UUID targetDocId,@RequestBody List<UUID> paragraphIds){
        return R.success(datasetService.paragraphMigrate(sourceDatasetId,sourceDocId,targetDatasetId,targetDocId,paragraphIds));
    }


}
