package com.maxkb4j.knowledge.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.domain.form.BaseField;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.knowledge.consts.KnowledgeType;
import com.maxkb4j.knowledge.dto.DataSearchDTO;
import com.maxkb4j.knowledge.dto.GenerateProblemDTO;
import com.maxkb4j.knowledge.dto.WebKnowledgeDTO;
import com.maxkb4j.knowledge.dto.KnowledgeQuery;
import com.maxkb4j.knowledge.entity.KnowledgeActionEntity;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.entity.KnowledgeVersionEntity;
import com.maxkb4j.knowledge.service.KnowledgeService;
import com.maxkb4j.knowledge.service.RetrieveService;
import com.maxkb4j.knowledge.vo.KnowledgeListVO;
import com.maxkb4j.knowledge.vo.KnowledgeVO;
import com.maxkb4j.knowledge.vo.ParagraphVO;
import com.maxkb4j.workflow.model.KnowledgeParams;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@RestController
@RequestMapping(AppConst.ADMIN_WORKSPACE_API)
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final RetrieveService retrieveService;


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_READ)
    @GetMapping("/knowledge")
    public R<List<KnowledgeListVO>> listKnowledge(String folderId) {
        return R.data(knowledgeService.listKnowledge());
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_CREATE)
    @PostMapping("/knowledge/base")
    public R<KnowledgeEntity> createKnowledgeBase(@RequestBody KnowledgeEntity knowledge) {
        knowledge.setType(KnowledgeType.BASE);
        return R.data(knowledgeService.createKnowledge(knowledge));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_CREATE)
    @PostMapping("/knowledge/web")
    public R<KnowledgeEntity> createKnowledgeWeb(@RequestBody WebKnowledgeDTO knowledge) {
        knowledge.setType(KnowledgeType.WEB);
        return R.data(knowledgeService.createKnowledgeWeb(knowledge));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_CREATE)
    @PostMapping("/knowledge/workflow")
    public R<KnowledgeEntity> createKnowledgeWorkflow(@RequestBody KnowledgeEntity knowledge) {
        knowledge.setType(KnowledgeType.WORKFLOW);
        return R.data(knowledgeService.createKnowledge(knowledge));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_WORKFLOW_EDIT)
    @PutMapping("/knowledge/{id}/workflow")
    public R<KnowledgeEntity> updateDatasetWorkflow(@PathVariable String id,@RequestBody KnowledgeEntity knowledge) {
        return R.data(knowledgeService.updateDatasetWorkflow(id,knowledge));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_READ)
    @GetMapping("/knowledge/{id}")
    public R<KnowledgeVO> getKnowledgeById(@PathVariable("id") String id) {
        return R.data(knowledgeService.getKnowledgeById(id));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_HIT_TEST_READ)
    @PutMapping("/knowledge/{id}/hit_test")
    public R<List<ParagraphVO>> hitTest(@PathVariable("id") String id, @RequestBody DataSearchDTO dto) {
        return R.data(retrieveService.paragraphSearch(List.of(id), dto));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_EDIT)
    @PutMapping("/knowledge/{id}")
    public R<KnowledgeEntity> updatedKnowledge(@PathVariable("id") String id, @RequestBody KnowledgeEntity datasetEntity) {
        datasetEntity.setId(id);
        knowledgeService.updateById(datasetEntity);
        knowledgeService.saveResourceMappings(datasetEntity);
        return R.data(datasetEntity);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_VECTOR)
    @PutMapping("/knowledge/{id}/embedding")
    public R<Boolean> embeddingKnowledge(@PathVariable("id") String id) {
        return R.status(knowledgeService.embeddingKnowledge(id));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_PROBLEM_RELATE)
    @PutMapping("/knowledge/{id}/generate_related")
    public R<Boolean> generateRelated(@PathVariable String id, @RequestBody GenerateProblemDTO dto) {
        return R.status(knowledgeService.generateRelated(id, dto));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DELETE)
    @DeleteMapping("/knowledge/{id}")
    public R<Boolean> deleteKnowledgeId(@PathVariable("id") String id) {
        return R.status(knowledgeService.deleteById(id));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_BATCH_DELETE)
    @DeleteMapping("/knowledge/batchDelete")
    public R<Boolean> delMulKnowledge(@RequestParam("idList") List<String> idList) {
        return R.status(knowledgeService.delMulApplication(idList));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_READ)
    @GetMapping("/knowledge/{current}/{size}")
    public R<IPage<KnowledgeVO>> knowledgePage(@PathVariable("current") int current, @PathVariable("size") int size, KnowledgeQuery query) {
        Page<KnowledgeVO> knowledgePage = new Page<>(current, size);
        return R.data(knowledgeService.selectKnowledgePage(knowledgePage, query));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_EXPORT)
    @GetMapping("/knowledge/{id}/export")
    public void export(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        knowledgeService.exportExcel(id, response);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_EXPORT)
    @GetMapping("/knowledge/{id}/export_zip")
    public void exportZip(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        knowledgeService.exportExcelZip(id, response);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_EXPORT)
    @GetMapping("/knowledge/{id}/export_knowledge")
    public void exportKnowledge(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        knowledgeService.exportKnowledge(id, response);
    }

    @PostMapping("/knowledge/import_knowledge")
    public R<KnowledgeEntity> importKnowledge(@RequestParam("file") MultipartFile file) throws IOException {
        return R.data(knowledgeService.importKnowledgeZip(file));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PostMapping("/knowledge/{id}/datasource/local/{nodeType}/form_list")
    public R<List<BaseField>> datasourceFormList(@PathVariable("id") String id, @PathVariable("nodeType")String nodeType, @RequestBody JSONObject params) {
      return R.data(knowledgeService.datasourceFormList(nodeType,params));
    }

    @PostMapping("/knowledge/{id}/debug")
    public R<KnowledgeActionEntity> debug(@PathVariable("id") String id, @RequestBody KnowledgeParams params) {
        return R.data(knowledgeService.uploadDocument(id,params, true));
    }

    @PutMapping("/knowledge/{id}/publish")
    public R<Boolean> publish(@PathVariable("id") String id) {
        return R.status(knowledgeService.publish(id));
    }
    @GetMapping("/knowledge/{id}/knowledge_version")
    public R<List<KnowledgeVersionEntity>> knowledgeVersion(@PathVariable("id") String id) {
        return R.data(knowledgeService.knowledgeVersion(id));
    }

    @PutMapping("/knowledge/{id}/knowledge_version/{versionId}")
    public R<Boolean> knowledgeVersion(@PathVariable("id") String id,@PathVariable("versionId") String versionId,@RequestBody KnowledgeVersionEntity knowledgeVersionEntity) {
        return R.status(knowledgeService.knowledgeVersion(versionId,knowledgeVersionEntity));
    }

    @GetMapping("/knowledge/{id}/action/{current}/{size}")
    public R<IPage<KnowledgeActionEntity>> actionPage(@PathVariable("id") String id,@PathVariable("current") int current, @PathVariable("size") int size, String username, String state) {
        return R.data(knowledgeService.actionPage(id,current,size,username,state));
    }

    @PostMapping("/knowledge/{id}/upload_document")
    public R<KnowledgeActionEntity> uploadDocument(@PathVariable("id") String id,@RequestBody  KnowledgeParams params) {
        return R.data(knowledgeService.uploadDocument(id,params, false));
    }

    @GetMapping("/knowledge/{id}/action/{actionId}")
    public R<KnowledgeActionEntity> action(@PathVariable("id") String id, @PathVariable("actionId") String actionId) {
        return R.data(knowledgeService.action(actionId));
    }


}
