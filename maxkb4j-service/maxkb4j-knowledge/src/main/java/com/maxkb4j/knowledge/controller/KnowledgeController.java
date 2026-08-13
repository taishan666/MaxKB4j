package com.maxkb4j.knowledge.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.model.form.BaseField;
import com.maxkb4j.common.enums.PermissionEnum;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.knowledge.consts.KnowledgeType;
import com.maxkb4j.knowledge.dto.DataSearchDTO;
import com.maxkb4j.knowledge.dto.GenerateProblemDTO;
import com.maxkb4j.knowledge.dto.KnowledgeQuery;
import com.maxkb4j.knowledge.dto.KnowledgeSaveDTO;
import com.maxkb4j.knowledge.dto.KnowledgeVersionUpdateDTO;
import com.maxkb4j.knowledge.dto.WebKnowledgeDTO;
import com.maxkb4j.knowledge.entity.KnowledgeActionEntity;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.entity.KnowledgeVersionEntity;
import com.maxkb4j.knowledge.handler.KnowledgeImportHandler;
import com.maxkb4j.knowledge.retriever.ParagraphRetriever;
import com.maxkb4j.knowledge.service.IKnowledgeInternalService;
import com.maxkb4j.knowledge.service.KnowledgeExportService;
import com.maxkb4j.knowledge.service.KnowledgePublishService;
import com.maxkb4j.knowledge.service.KnowledgeWorkflowService;
import com.maxkb4j.knowledge.vo.*;
import com.maxkb4j.workflow.model.KnowledgeParams;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

    private final IKnowledgeInternalService knowledgeService;
    private final KnowledgeExportService knowledgeExportService;
    private final KnowledgeWorkflowService knowledgeWorkflowService;
    private final KnowledgePublishService knowledgePublishService;
    private final ParagraphRetriever retrieveService;
    private final KnowledgeImportHandler knowledgeImportHandler;


    @SaCheckPerm(PermissionEnum.KNOWLEDGE_READ)
    @GetMapping("/knowledge")
    public R<List<KnowledgeListVO>> listKnowledge(KnowledgeQuery query) {
        return R.data(knowledgeService.listKnowledge(query));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_CREATE)
    @PostMapping("/knowledge/base")
    public R<KnowledgeVO> createKnowledgeBase(@Valid @RequestBody KnowledgeSaveDTO dto) {
        KnowledgeEntity knowledge = BeanUtil.copy(dto, KnowledgeEntity.class);
        knowledge.setType(KnowledgeType.BASE);
        KnowledgeEntity ke = knowledgeService.createKnowledge(knowledge);
        return R.data(ke == null ? null : BeanUtil.copy(ke, KnowledgeVO.class));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_CREATE)
    @PostMapping("/knowledge/web")
    public R<KnowledgeVO> createKnowledgeWeb(@RequestBody WebKnowledgeDTO knowledge) {
        knowledge.setType(KnowledgeType.WEB);
        KnowledgeEntity ke = knowledgeService.createKnowledgeWeb(knowledge);
        return R.data(ke == null ? null : BeanUtil.copy(ke, KnowledgeVO.class));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_CREATE)
    @PostMapping("/knowledge/workflow")
    public R<KnowledgeVO> createKnowledgeWorkflow(@Valid @RequestBody KnowledgeSaveDTO dto) {
        KnowledgeEntity knowledge = BeanUtil.copy(dto, KnowledgeEntity.class);
        knowledge.setType(KnowledgeType.WORKFLOW);
        KnowledgeEntity ke = knowledgeService.createKnowledge(knowledge);
        return R.data(ke == null ? null : BeanUtil.copy(ke, KnowledgeVO.class));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_WORKFLOW_EDIT)
    @PutMapping("/knowledge/{id}/workflow")
    public R<KnowledgeVO> updateDatasetWorkflow(@PathVariable String id,@RequestBody KnowledgeSaveDTO dto) {
        KnowledgeEntity knowledge = BeanUtil.copy(dto, KnowledgeEntity.class);
        KnowledgeEntity ke = knowledgeWorkflowService.updateDatasetWorkflow(id,knowledge);
        return R.data(ke == null ? null : BeanUtil.copy(ke, KnowledgeVO.class));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_READ)
    @GetMapping("/knowledge/{id}")
    public R<KnowledgeVO> getKnowledgeById(@PathVariable("id") String id) {
        return R.data(knowledgeService.getKnowledgeById(id));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_HIT_TEST_READ)
    @PutMapping("/knowledge/{id}/hit_test")
    public R<List<ParagraphRagVO>> hitTest(@PathVariable("id") String id, @Valid @RequestBody DataSearchDTO dto) {
        return R.data(retrieveService.paragraphSearch(List.of(id), dto));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_EDIT)
    @PutMapping("/knowledge/{id}")
    public R<KnowledgeVO> updatedKnowledge(@PathVariable("id") String id, @RequestBody KnowledgeSaveDTO dto) {
        KnowledgeEntity knowledge = BeanUtil.copy(dto, KnowledgeEntity.class);
        knowledgeService.updateKnowledge(id, knowledge);
        return R.data(BeanUtil.copy(knowledge, KnowledgeVO.class));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_VECTOR)
    @PutMapping("/knowledge/{id}/embedding")
    public R<Boolean> embeddingKnowledge(@PathVariable("id") String id) {
        return R.status(knowledgeWorkflowService.embeddingKnowledge(id));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_PROBLEM_RELATE)
    @PutMapping("/knowledge/{id}/generate_related")
    public R<Boolean> generateRelated(@PathVariable String id, @Valid @RequestBody GenerateProblemDTO dto) {
        return R.status(knowledgeWorkflowService.generateRelated(id, dto));
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
        return R.data(knowledgeService.pageList(knowledgePage, query));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_EXPORT)
    @GetMapping("/knowledge/{id}/export")
    public void export(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        knowledgeExportService.exportExcel(id, response);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_EXPORT)
    @GetMapping("/knowledge/{id}/export_zip")
    public void exportZip(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        knowledgeExportService.exportExcelZip(id, response);
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_EXPORT)
    @GetMapping("/knowledge/{id}/export_knowledge")
    public void exportKnowledge(@PathVariable("id") String id, HttpServletResponse response) throws IOException {
        knowledgeExportService.exportKnowledge(id, response);
    }

    @PostMapping("/knowledge/import_knowledge")
    public R<KnowledgeVO> importKnowledge(@RequestParam("file") MultipartFile file) throws IOException {
        KnowledgeEntity ke = knowledgeImportHandler.importKnowledgeFromZip(file);
        return R.data(ke == null ? null : BeanUtil.copy(ke, KnowledgeVO.class));
    }

    @SaCheckPerm(PermissionEnum.KNOWLEDGE_DOCUMENT_CREATE)
    @PostMapping("/knowledge/{id}/datasource/local/{nodeType}/form_list")
    public R<List<BaseField>> datasourceFormList(@PathVariable("id") String id, @PathVariable("nodeType")String nodeType, @RequestBody JSONObject params) {
      return R.data(knowledgeWorkflowService.datasourceFormList(nodeType,params));
    }

    @PostMapping("/knowledge/{id}/debug")
    public R<KnowledgeActionVO> debug(@PathVariable("id") String id, @RequestBody KnowledgeParams params) {
        KnowledgeActionEntity ae = knowledgeWorkflowService.uploadDocument(id,params, true);
        return R.data(ae == null ? null : BeanUtil.copy(ae, KnowledgeActionVO.class));
    }

    @PutMapping("/knowledge/{id}/publish")
    public R<Boolean> publish(@PathVariable("id") String id) {
        return R.status(knowledgePublishService.publish(id));
    }
    @GetMapping("/knowledge/{id}/knowledge_version")
    public R<List<KnowledgeVersionVO>> knowledgeVersion(@PathVariable("id") String id) {
        return R.data(BeanUtil.copyList(knowledgePublishService.knowledgeVersion(id), KnowledgeVersionVO.class));
    }

    @PutMapping("/knowledge/{id}/knowledge_version/{versionId}")
    public R<Boolean> knowledgeVersion(@PathVariable("id") String id,@PathVariable("versionId") String versionId,@RequestBody KnowledgeVersionUpdateDTO dto) {
        KnowledgeVersionEntity knowledgeVersionEntity = new KnowledgeVersionEntity();
        knowledgeVersionEntity.setName(dto.getName());
        return R.status(knowledgePublishService.knowledgeVersion(versionId,knowledgeVersionEntity));
    }

    @GetMapping("/knowledge/{id}/action/{current}/{size}")
    public R<IPage<KnowledgeActionVO>> actionPage(@PathVariable("id") String id,@PathVariable("current") int current, @PathVariable("size") int size, String username, String state) {
        return R.data(BeanUtil.copyPage(knowledgePublishService.actionPage(id,current,size,username,state), KnowledgeActionVO.class));
    }

    @PostMapping("/knowledge/{id}/upload_document")
    public R<KnowledgeActionVO> uploadDocument(@PathVariable("id") String id,@RequestBody  KnowledgeParams params) {
        KnowledgeActionEntity ae = knowledgeWorkflowService.uploadDocument(id,params, false);
        return R.data(ae == null ? null : BeanUtil.copy(ae, KnowledgeActionVO.class));
    }

    @GetMapping("/knowledge/{id}/action/{actionId}")
    public R<KnowledgeActionVO> action(@PathVariable("id") String id, @PathVariable("actionId") String actionId) {
        KnowledgeActionEntity ae = knowledgePublishService.action(actionId);
        return R.data(ae == null ? null : BeanUtil.copy(ae, KnowledgeActionVO.class));
    }


}
