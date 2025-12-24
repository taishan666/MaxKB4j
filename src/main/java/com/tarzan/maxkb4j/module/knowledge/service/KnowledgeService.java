package com.tarzan.maxkb4j.module.knowledge.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.form.BaseField;
import com.tarzan.maxkb4j.common.form.LocalFileUpload;
import com.tarzan.maxkb4j.common.form.TextInputField;
import com.tarzan.maxkb4j.common.util.BeanUtil;
import com.tarzan.maxkb4j.common.util.DateTimeUtil;
import com.tarzan.maxkb4j.common.util.StpKit;
import com.tarzan.maxkb4j.core.event.GenerateProblemEvent;
import com.tarzan.maxkb4j.core.workflow.builder.NodeBuilder;
import com.tarzan.maxkb4j.core.workflow.handler.KnowledgeWorkflowHandler;
import com.tarzan.maxkb4j.core.workflow.logic.LogicFlow;
import com.tarzan.maxkb4j.core.workflow.model.KnowledgeWorkflow;
import com.tarzan.maxkb4j.core.workflow.node.INode;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationKnowledgeMappingEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationKnowledgeMappingMapper;
import com.tarzan.maxkb4j.module.chat.dto.KnowledgeParams;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.KnowledgeDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.KnowledgeQuery;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.*;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.KnowledgeVO;
import com.tarzan.maxkb4j.module.knowledge.excel.DatasetExcel;
import com.tarzan.maxkb4j.module.knowledge.mapper.KnowledgeMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.ParagraphMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.ProblemMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.ProblemParagraphMapper;
import com.tarzan.maxkb4j.module.system.permission.constant.AuthTargetType;
import com.tarzan.maxkb4j.module.system.permission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService extends ServiceImpl<KnowledgeMapper, KnowledgeEntity> {

    private final ProblemMapper problemMapper;
    private final ApplicationKnowledgeMappingMapper applicationDatasetMappingMapper;
    private final ParagraphMapper paragraphMapper;
    private final ProblemParagraphMapper problemParagraphMapper;
    private final DocumentService documentService;
    private final UserService userService;
    private final UserResourcePermissionService userResourcePermissionService;
    private final ApplicationEventPublisher eventPublisher;
    private final DataIndexService dataIndexService;
    private final KnowledgeActionService knowledgeActionService;
    private final KnowledgeWorkflowHandler knowledgeWorkflowHandler;
    private final KnowledgeVersionService knowledgeVersionService;


    public IPage<KnowledgeVO> selectKnowledgePage(Page<KnowledgeVO> knowledgePage, KnowledgeQuery query) {
        String loginId = StpKit.ADMIN.getLoginIdAsString();
        List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.KNOWLEDGE, loginId);
        UserEntity user = userService.getById(loginId);
        query.setIsAdmin(user.getRole().contains("ADMIN"));
        query.setTargetIds(targetIds);
        IPage<KnowledgeVO> page = baseMapper.selectKnowledgePage(knowledgePage, query);
        Map<String, String> nicknameMap = userService.getNicknameMap();
        page.getRecords().forEach(vo -> vo.setNickname(nicknameMap.get(vo.getUserId())));
        return page;
    }


    public KnowledgeVO getKnowledgeById(String id) {
        KnowledgeEntity entity = baseMapper.selectById(id);
        if (Objects.isNull(entity)) {
            return null;
        }
        KnowledgeVO vo = BeanUtil.copy(entity, KnowledgeVO.class);
        List<ApplicationKnowledgeMappingEntity> apps = applicationDatasetMappingMapper.selectList(Wrappers.lambdaQuery(ApplicationKnowledgeMappingEntity.class)
                .select(ApplicationKnowledgeMappingEntity::getApplicationId)
                .eq(ApplicationKnowledgeMappingEntity::getKnowledgeId, id));
        List<String> appIds = apps.stream().map(ApplicationKnowledgeMappingEntity::getApplicationId).toList();
        vo.setApplicationIdList(appIds);
        return vo;
    }


    public List<ParagraphEntity> getParagraphByProblemId(String problemId) {
        List<ProblemParagraphEntity> list = problemParagraphMapper.selectList(Wrappers.<ProblemParagraphEntity>lambdaQuery()
                .select(ProblemParagraphEntity::getParagraphId).eq(ProblemParagraphEntity::getProblemId, problemId));
        if (!CollectionUtils.isEmpty(list)) {
            List<String> paragraphIds = list.stream().map(ProblemParagraphEntity::getParagraphId).toList();
            return paragraphMapper.selectByIds(paragraphIds);
        }
        return Collections.emptyList();
    }


    @Transactional
    public Boolean deleteKnowledgeId(String id) {
        problemParagraphMapper.delete(Wrappers.<ProblemParagraphEntity>lambdaQuery().eq(ProblemParagraphEntity::getKnowledgeId, id));
        problemMapper.delete(Wrappers.<ProblemEntity>lambdaQuery().eq(ProblemEntity::getKnowledgeId, id));
        paragraphMapper.delete(Wrappers.<ParagraphEntity>lambdaQuery().eq(ParagraphEntity::getKnowledgeId, id));
        documentService.remove(Wrappers.<DocumentEntity>lambdaQuery().eq(DocumentEntity::getKnowledgeId, id));
        applicationDatasetMappingMapper.delete(Wrappers.<ApplicationKnowledgeMappingEntity>lambdaQuery().eq(ApplicationKnowledgeMappingEntity::getKnowledgeId, id));
        userResourcePermissionService.remove(AuthTargetType.APPLICATION, id);
        dataIndexService.removeByDatasetId(id);
        return this.removeById(id);
    }


    public void exportExcelZipByDocs(List<DocumentEntity> docs, String exportName, HttpServletResponse response) throws IOException {
        response.setContentType("application/zip");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode(exportName, StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".zip");
        // 创建字节输出流和ZIP输出流
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
        // 使用ByteArrayOutputStream作为临时存储Excel文件
        ByteArrayOutputStream excelOutputStream = new ByteArrayOutputStream();
        ExcelWriter excelWriter = EasyExcel.write(excelOutputStream, DatasetExcel.class).build();
        for (DocumentEntity doc : docs) {
            List<DatasetExcel> list = getDatasetExcelByDoc(doc);
            // 使用同一个写入器添加新的 sheet 页
            WriteSheet writeSheet = EasyExcel.writerSheet(doc.getName()).build();
            excelWriter.write(list, writeSheet);
        }
        // 完成写入操作
        excelWriter.finish();
        // 将生成的Excel添加到ZIP中
        ZipEntry zipEntry = new ZipEntry(exportName + ".xlsx");
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(excelOutputStream.toByteArray()); // 将字节输出流转为字节数组写入
        zipOutputStream.closeEntry();
        // 关闭Excel相关的资源
        excelOutputStream.close();
        // 完成ZIP文件的写入
        zipOutputStream.finish();
        zipOutputStream.close();
        // 将所有数据写入最终输出流
        OutputStream outputStream = response.getOutputStream();
        byteArrayOutputStream.writeTo(outputStream);
        outputStream.flush();
        byteArrayOutputStream.close(); // 关闭字节输出流
    }

    public void exportExcelZip(String id, HttpServletResponse response) throws IOException {
        KnowledgeEntity dataset = this.getById(id);
        List<DocumentEntity> docs = documentService.list(Wrappers.<DocumentEntity>lambdaQuery().eq(DocumentEntity::getKnowledgeId, id));
        exportExcelZipByDocs(docs, dataset.getName(), response);
    }

    public void exportExcel(String id, HttpServletResponse response) throws IOException {
        KnowledgeEntity dataset = this.getById(id);
        List<DocumentEntity> docs = documentService.list(Wrappers.<DocumentEntity>lambdaQuery().eq(DocumentEntity::getKnowledgeId, id));
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode(dataset.getName(), StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        OutputStream outputStream = response.getOutputStream();
        // 创建 EasyExcel 写入器
        ExcelWriter excelWriter = EasyExcel.write(outputStream, DatasetExcel.class).build();
        for (DocumentEntity doc : docs) {
            List<DatasetExcel> list = getDatasetExcelByDoc(doc);
            // 使用同一个写入器添加新的 sheet 页
            WriteSheet writeSheet = EasyExcel.writerSheet(doc.getName()).build();
            excelWriter.write(list, writeSheet);
        }
        // 完成写入操作
        excelWriter.finish();
    }


    private List<DatasetExcel> getDatasetExcelByDoc(DocumentEntity doc) {
        List<DatasetExcel> list = new ArrayList<>();
        List<ParagraphEntity> paragraphs = paragraphMapper.selectList(Wrappers.<ParagraphEntity>lambdaQuery().eq(ParagraphEntity::getDocumentId, doc.getId()));
        for (ParagraphEntity paragraph : paragraphs) {
            DatasetExcel excel = new DatasetExcel();
            excel.setTitle(paragraph.getTitle());
            excel.setContent(paragraph.getContent());
            List<ProblemEntity> problemEntities = problemParagraphMapper.getProblemsByParagraphId(paragraph.getId());
            StringBuilder sb = new StringBuilder();
            if (!CollectionUtils.isEmpty(problemEntities)) {
                List<String> problems = problemEntities.stream().map(ProblemEntity::getContent).toList();
                String result = String.join("\n", problems);
                sb.append(result);
            }
            excel.setProblems(sb.toString());
            list.add(excel);
        }
        return list;
    }


    public List<KnowledgeEntity> list(String userId, String folderId) {
        return this.lambdaQuery().eq(KnowledgeEntity::getUserId, userId).eq(KnowledgeEntity::getFolderId, folderId).list();
    }

    @Transactional
    public KnowledgeEntity createDatasetBase(KnowledgeEntity knowledge) {
        knowledge.setMeta(new JSONObject());
        knowledge.setUserId(StpKit.ADMIN.getLoginIdAsString());
        //knowledge.setType(0);
        this.save(knowledge);
        userResourcePermissionService.ownerSave(AuthTargetType.KNOWLEDGE, knowledge.getId(), knowledge.getUserId());
        return knowledge;
    }

    @Transactional
    public KnowledgeEntity createDatasetWeb(KnowledgeDTO knowledge) {
        knowledge.setUserId(StpKit.ADMIN.getLoginIdAsString());
        JSONObject meta = new JSONObject();
        meta.put("source_url", knowledge.getSourceUrl());
        meta.put("selector", knowledge.getSelector());
        knowledge.setMeta(meta);
        knowledge.setType(1);
        this.save(knowledge);
        documentService.webDataset(knowledge.getId(), knowledge.getSourceUrl(), knowledge.getSelector());
        userResourcePermissionService.ownerSave(AuthTargetType.KNOWLEDGE, knowledge.getId(), knowledge.getUserId());
        return knowledge;
    }


    public boolean embeddingKnowledge(String knowledgeId) {
        List<DocumentEntity> documents = documentService.lambdaQuery().select(DocumentEntity::getId).eq(DocumentEntity::getKnowledgeId, knowledgeId).list();
        documentService.embedByDocIds(knowledgeId, documents.stream().map(DocumentEntity::getId).toList(), List.of("0", "1", "2", "3", "n"));
        return true;
    }

    public List<KnowledgeEntity> listKnowledge() {
        List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.KNOWLEDGE, StpKit.ADMIN.getLoginIdAsString());
        return this.lambdaQuery().in(KnowledgeEntity::getId, targetIds).list();
    }

    public Boolean generateRelated(String knowledgeId, GenerateProblemDTO dto) {
        eventPublisher.publishEvent(new GenerateProblemEvent(this, knowledgeId, dto.getDocumentIdList(), dto.getModelId(), dto.getPrompt(), dto.getStateList()));
        return true;
    }

    public KnowledgeEntity updateDatasetWorkflow(String id, KnowledgeEntity dataset) {
        dataset.setId(id);
        return this.updateById(dataset) ? dataset : null;
    }

    public List<BaseField> datasourceFormList(String id, String nodeType, JSONObject node) {
        if ("data-source-web-node".equals(nodeType)) {
            BaseField field1 = new TextInputField("Web 根地址", "source_url", "请输入 Web 根地址", true);
            BaseField field2 = new TextInputField("选择器", "selector", "默认为 body，可输入 .classname/#idname/tagname", false);
            return List.of(field1, field2);
        } else {
            BaseField localFileUpload = new LocalFileUpload(50, 100, List.of("TXT", "DOCX", "PDF", "HTML", "XLS", "XLSX", "CSV"));
            return List.of(localFileUpload);
        }
    }

    public KnowledgeActionEntity uploadDocument(String id, KnowledgeParams params,boolean debug) {
        KnowledgeEntity knowledge = baseMapper.selectById(id);
        KnowledgeActionEntity knowledgeAction = new KnowledgeActionEntity();
        knowledgeAction.setKnowledgeId(id);
        knowledgeAction.setState("STARTED");
        knowledgeAction.setDetails(new JSONObject());
        knowledgeAction.setRunTime(0F);
        JSONObject meta = new JSONObject();
        meta.put("userId", StpKit.ADMIN.getLoginIdAsString());
        meta.put("username", StpKit.ADMIN.getExtra("username"));
        knowledgeAction.setMeta(meta);
        knowledgeActionService.save(knowledgeAction);
        LogicFlow logicFlow = LogicFlow.newInstance(knowledge.getWorkFlow());
        List<INode> nodes = logicFlow.getNodes().stream().map(NodeBuilder::getNode).filter(Objects::nonNull).toList();
        params.setActionId(knowledgeAction.getId());
        params.setKnowledgeId(id);
        KnowledgeWorkflow workflow = new KnowledgeWorkflow(
                params,
                nodes,
                logicFlow.getEdges());
        CompletableFuture.runAsync(() -> knowledgeWorkflowHandler.execute(workflow));
        return knowledgeAction;
    }

    public KnowledgeActionEntity action(String id, String actionId) {
        return knowledgeActionService.getById(actionId);
    }

    public IPage<KnowledgeActionEntity> actionPage(String id, int current, int size, String username, String state) {
        Page<KnowledgeActionEntity> actionPage = new Page<>(current, size);
        LambdaQueryWrapper<KnowledgeActionEntity> query = Wrappers.lambdaQuery();
        //todo
        if (!StringUtils.isEmpty(username)){
            query.eq(KnowledgeActionEntity::getMeta, username);
        }
        if (!StringUtils.isEmpty(state)){
            query.eq(KnowledgeActionEntity::getState, state);
        }
        query.eq(KnowledgeActionEntity::getKnowledgeId, id);
        query.orderByDesc(KnowledgeActionEntity::getCreateTime);
        return  knowledgeActionService.pageList(actionPage,username, state);
    }

    @Transactional
    public Boolean publish(String id) {
        KnowledgeEntity knowledge = new KnowledgeEntity();
        knowledge.setId(id);
        knowledge.setIsPublish(true);
        this.updateById(knowledge);
        knowledge= this.getById(id);
        KnowledgeVersionEntity knowledgeVersion = new KnowledgeVersionEntity();
        knowledgeVersion.setKnowledgeId(id);
        knowledgeVersion.setName(DateTimeUtil.now());
        knowledgeVersion.setWorkFlow(knowledge.getWorkFlow());
        knowledgeVersion.setPublishUserId(StpKit.ADMIN.getLoginIdAsString());
        knowledgeVersion.setPublishUserName((String) StpKit.ADMIN.getExtra("username"));
        return knowledgeVersionService.save(knowledgeVersion);
    }

    public List<KnowledgeVersionEntity> knowledgeVersion(String id) {
        return knowledgeVersionService.lambdaQuery().eq(KnowledgeVersionEntity::getKnowledgeId, id).list();
    }

    public Boolean knowledgeVersion(String versionId,KnowledgeVersionEntity  knowledgeVersion) {
        knowledgeVersion.setId(versionId);
        return knowledgeVersionService.updateById(knowledgeVersion);
    }
}
