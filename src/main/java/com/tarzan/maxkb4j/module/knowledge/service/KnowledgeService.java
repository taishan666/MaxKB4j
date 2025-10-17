package com.tarzan.maxkb4j.module.knowledge.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.util.BeanUtil;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationKnowledgeMappingEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationKnowledgeMappingMapper;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.KnowledgeDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.KnowledgeQuery;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.*;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.KnowledgeVO;
import com.tarzan.maxkb4j.module.knowledge.excel.DatasetExcel;
import com.tarzan.maxkb4j.module.knowledge.mapper.DocumentMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.KnowledgeMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.ProblemParagraphMapper;
import com.tarzan.maxkb4j.module.system.permission.constant.AuthTargetType;
import com.tarzan.maxkb4j.module.system.permission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author tarzan
 * @date 2024-12-25 16:00:15
 */
@Slf4j
@Service
@AllArgsConstructor
public class KnowledgeService extends ServiceImpl<KnowledgeMapper, KnowledgeEntity> {

    private final DocumentMapper documentMapper;
    private final ProblemService problemService;
    private final ApplicationKnowledgeMappingMapper applicationDatasetMappingMapper;
    private final ParagraphService paragraphService;
    private final ProblemParagraphMapper problemParagraphMapper;
    private final DataIndexService dataIndexService;
    private final DocumentService documentService;
    private final UserService userService;
    private final UserResourcePermissionService userResourcePermissionService;


    public IPage<KnowledgeVO> selectKnowledgePage(Page<KnowledgeVO> knowledgePage, KnowledgeQuery query) {
        String loginId = StpUtil.getLoginIdAsString();
        List<String> targetIds =userResourcePermissionService.getTargetIds("KNOWLEDGE",loginId);
        UserEntity user =userService.getById(loginId);
        query.setIsAdmin(user.getRole().contains("ADMIN"));
        query.setTargetIds(targetIds);
        IPage<KnowledgeVO>  page= baseMapper.selectKnowledgePage(knowledgePage, query);
        Map<String, String> nicknameMap=userService.getNicknameMap();
        page.getRecords().forEach(vo-> vo.setNickname(nicknameMap.get(vo.getUserId())));
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
            return paragraphService.lambdaQuery().in(ParagraphEntity::getId, paragraphIds).list();
        }
        return Collections.emptyList();
    }



    @Transactional
    public Boolean deleteKnowledgeId(String id) {
        problemParagraphMapper.delete(Wrappers.<ProblemParagraphEntity>lambdaQuery().eq(ProblemParagraphEntity::getKnowledgeId, id));
        problemService.lambdaUpdate().eq(ProblemEntity::getKnowledgeId, id).remove();
        paragraphService.lambdaUpdate().eq(ParagraphEntity::getKnowledgeId, id).remove();
        documentMapper.delete(Wrappers.<DocumentEntity>lambdaQuery().eq(DocumentEntity::getKnowledgeId, id));
        applicationDatasetMappingMapper.delete(Wrappers.<ApplicationKnowledgeMappingEntity>lambdaQuery().eq(ApplicationKnowledgeMappingEntity::getKnowledgeId, id));
        dataIndexService.removeByDatasetId(id);
        userResourcePermissionService.remove(AuthTargetType.APPLICATION, id);
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
        List<DocumentEntity> docs = documentMapper.selectList(Wrappers.<DocumentEntity>lambdaQuery().eq(DocumentEntity::getKnowledgeId, id));
        exportExcelZipByDocs(docs, dataset.getName(), response);
    }

    public void exportExcel(String id, HttpServletResponse response) throws IOException {
        KnowledgeEntity dataset = this.getById(id);
        List<DocumentEntity> docs = documentMapper.selectList(Wrappers.<DocumentEntity>lambdaQuery().eq(DocumentEntity::getKnowledgeId, id));
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
        List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, doc.getId()).list();
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



    public List<KnowledgeEntity> listByUserId(String userId) {
        return this.lambdaQuery().eq(KnowledgeEntity::getUserId, userId).list();
    }

    @Transactional
    public KnowledgeEntity createDatasetBase(KnowledgeEntity knowledge) {
        knowledge.setMeta(new JSONObject());
        knowledge.setUserId(StpUtil.getLoginIdAsString());
        knowledge.setType(0);
        this.save(knowledge);
        userResourcePermissionService.ownerSave(AuthTargetType.KNOWLEDGE, knowledge.getId(), knowledge.getUserId());
        return knowledge;
    }

    @Transactional
    public KnowledgeEntity createDatasetWeb(KnowledgeDTO knowledge) {
        knowledge.setUserId(StpUtil.getLoginIdAsString());
        JSONObject meta = new JSONObject();
        meta.put("source_url",knowledge.getSourceUrl());
        meta.put("selector",knowledge.getSelector());
        knowledge.setMeta(meta);
        knowledge.setType(1);
        this.save(knowledge);
        documentService.webDataset(knowledge.getId(),knowledge.getSourceUrl(),knowledge.getSelector());
        userResourcePermissionService.ownerSave(AuthTargetType.KNOWLEDGE, knowledge.getId(), knowledge.getUserId());
        return knowledge;
    }

/*    public boolean reEmbedding(String knowledgeId) {
        List<DocumentEntity> documents=documentService.lambdaQuery().select(DocumentEntity::getId).eq(DocumentEntity::getKnowledgeId, knowledgeId).list();
        documentService.embedByDocIds(documents.stream().map(DocumentEntity::getId).toList());
        return true;
    }*/

    public boolean embeddingKnowledge(String knowledgeId) {
        List<DocumentEntity> documents=documentService.lambdaQuery().select(DocumentEntity::getId).eq(DocumentEntity::getKnowledgeId, knowledgeId).list();
        documentService.embedByDocIds(documents.stream().map(DocumentEntity::getId).toList());
        return true;
    }
}
