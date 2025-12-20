package com.tarzan.maxkb4j.module.knowledge.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tarzan.maxkb4j.common.aop.SaCheckPerm;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.form.BaseField;
import com.tarzan.maxkb4j.module.chat.dto.KnowledgeParams;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DataSearchDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.KnowledgeDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.KnowledgeQuery;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeActionEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeVersionEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.KnowledgeVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeService;
import com.tarzan.maxkb4j.module.knowledge.service.RetrieveService;
import com.tarzan.maxkb4j.module.system.user.enums.PermissionEnum;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@RestController
@RequestMapping(AppConst.ADMIN_API + "/workspace/default")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final RetrieveService retrieveService;


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_READ)
    @GetMapping("/knowledge")
    public R<List<KnowledgeEntity>> listKnowledge(String folderId) {
        return R.success(knowledgeService.listKnowledge());
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_CREATE)
    @PostMapping("/knowledge/base")
    public R<KnowledgeEntity> createDatasetBase(@RequestBody KnowledgeEntity dataset) {
        dataset.setType(0);
        return R.success(knowledgeService.createDatasetBase(dataset));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_CREATE)
    @PostMapping("/knowledge/workflow")
    public R<KnowledgeEntity> createDatasetWorkflow(@RequestBody KnowledgeEntity dataset) {
        dataset.setType(2);
        return R.success(knowledgeService.createDatasetBase(dataset));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_WORKFLOW_EDIT)
    @PutMapping("/knowledge/{id}/workflow")
    public R<KnowledgeEntity> updateDatasetWorkflow(@PathVariable String id,@RequestBody KnowledgeEntity dataset) {
        return R.success(knowledgeService.updateDatasetWorkflow(id,dataset));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_CREATE)
    @PostMapping("/knowledge/web")
    public R<KnowledgeEntity> createDatasetWeb(@RequestBody KnowledgeDTO dataset) {
        return R.success(knowledgeService.createDatasetWeb(dataset));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_READ)
    @GetMapping("/knowledge/{id}")
    public R<KnowledgeVO> getKnowledgeById(@PathVariable("id") String id) {
        return R.success(knowledgeService.getKnowledgeById(id));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_HIT_TEST_READ)
    @PutMapping("/knowledge/{id}/hit_test")
    public R<List<ParagraphVO>> hitTest(@PathVariable("id") String id, @RequestBody DataSearchDTO dto) {
        return R.success(retrieveService.paragraphSearch(List.of(id), dto));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_EDIT)
    @PutMapping("/knowledge/{id}")
    public R<KnowledgeEntity> updatedKnowledge(@PathVariable("id") String id, @RequestBody KnowledgeEntity datasetEntity) {
        datasetEntity.setId(id);
        knowledgeService.updateById(datasetEntity);
        return R.success(datasetEntity);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_VECTOR)
    @PutMapping("/knowledge/{id}/embedding")
    public R<Boolean> embeddingKnowledge(@PathVariable("id") String id) {
        return R.success(knowledgeService.embeddingKnowledge(id));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_PROBLEM_RELATE)
    @PutMapping("/knowledge/{id}/generate_related")
    public R<Boolean> generateRelated(@PathVariable String id, @RequestBody GenerateProblemDTO dto) {
        return R.success(knowledgeService.generateRelated(id, dto));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DELETE)
    @DeleteMapping("/knowledge/{id}")
    public R<Boolean> deleteKnowledgeId(@PathVariable("id") String id) {
        return R.success(knowledgeService.deleteKnowledgeId(id));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_READ)
    @GetMapping("/knowledge/{current}/{size}")
    public R<IPage<KnowledgeVO>> knowledgePage(@PathVariable("current") int current, @PathVariable("size") int size, KnowledgeQuery query) {
        Page<KnowledgeVO> knowledgePage = new Page<>(current, size);
        return R.success(knowledgeService.selectKnowledgePage(knowledgePage, query));
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

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PostMapping("/knowledge/{id}/datasource/local/{nodeType}/form_list")
    public R<List<BaseField>> datasourceFormList(@PathVariable("id") String id, @PathVariable("nodeType")String nodeType, JSONObject node) {
      return R.success(knowledgeService.datasourceFormList(id,nodeType,node));
    }

    @PostMapping("/knowledge/{id}/debug")
    public R<KnowledgeActionEntity> debug(@PathVariable("id") String id,@RequestBody KnowledgeParams params) {
        return R.success(knowledgeService.debug(id,params));
    }

    @PutMapping("/knowledge/{id}/publish")
    public R<Boolean> publish(@PathVariable("id") String id) {
        return R.success(knowledgeService.publish(id));
    }
    @GetMapping("/knowledge/{id}/knowledge_version")
    public R<List<KnowledgeVersionEntity>> knowledgeVersion(@PathVariable("id") String id) {
        return R.success(knowledgeService.knowledgeVersion(id));
    }

    @PutMapping("/knowledge/{id}/knowledge_version/{versionId}")
    public R<Boolean> knowledgeVersion(@PathVariable("id") String
                                                   id,@PathVariable("versionId") String versionId,@RequestBody KnowledgeVersionEntity knowledgeVersionEntity) {
        return R.success(knowledgeService.knowledgeVersion(versionId,knowledgeVersionEntity));
    }

    @GetMapping("/knowledge/{id}/action/{current}/{size}")
    public R<IPage<KnowledgeActionEntity>> actionPage(@PathVariable("id") String id,@PathVariable("current") int current, @PathVariable("size") int size, String username, String state) {
        return R.success(knowledgeService.actionPage(id,current,size,username,state));
    }

    @PostMapping("/knowledge/{id}/upload_document")
    public R<KnowledgeActionEntity> uploadDocument(@PathVariable("id") String id,@RequestBody  KnowledgeParams params) {
        return R.success(knowledgeService.debug(id,params));
    }

    @GetMapping("/knowledge/{id}/action/{actionId}")
    public R<KnowledgeActionEntity> action(@PathVariable("id") String id, @PathVariable("actionId") String actionId) {
        return R.success(knowledgeService.action(id,actionId));
    }






}
