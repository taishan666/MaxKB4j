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
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationKnowledgeMappingEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationKnowledgeMappingMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.KnowledgeDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.KnowledgeQuery;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.*;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.KnowledgeVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphSimpleVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.TextSegmentVO;
import com.tarzan.maxkb4j.module.knowledge.excel.DatasetExcel;
import com.tarzan.maxkb4j.module.knowledge.mapper.DocumentMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.KnowledgeMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.ProblemParagraphMapper;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.system.permission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.util.BeanUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
    private final ApplicationMapper applicationMapper;
    private final ApplicationKnowledgeMappingMapper applicationDatasetMappingMapper;
    private final ParagraphService paragraphService;
    private final ProblemParagraphMapper problemParagraphMapper;
    private final DataIndexService dataIndexService;
    private final ModelService modelService;
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
        page.getRecords().forEach(vo->vo.setNickname(nicknameMap.get(vo.getUserId())));
        return page;
    }

    public List<KnowledgeEntity> getByUserId(String userId) {
        return this.list(Wrappers.<KnowledgeEntity>lambdaQuery().eq(KnowledgeEntity::getUserId, userId));
    }

    public List<ApplicationEntity> getApplicationByDatasetId(String id) {
        //TODO 缓存处理
        return applicationMapper.selectList(null);
    }

    public KnowledgeVO getByDatasetId(String id) {
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



    public List<ModelEntity> getModels(String id) {
        return modelService.models("LLM");
    }

    @Transactional
    public Boolean deleteDatasetById(String id) {
        problemParagraphMapper.delete(Wrappers.<ProblemParagraphEntity>lambdaQuery().eq(ProblemParagraphEntity::getDatasetId, id));
        problemService.lambdaUpdate().eq(ProblemEntity::getDatasetId, id).remove();
        paragraphService.lambdaUpdate().eq(ParagraphEntity::getDatasetId, id).remove();
        documentMapper.delete(Wrappers.<DocumentEntity>lambdaQuery().eq(DocumentEntity::getDatasetId, id));
        applicationDatasetMappingMapper.delete(Wrappers.<ApplicationKnowledgeMappingEntity>lambdaQuery().eq(ApplicationKnowledgeMappingEntity::getKnowledgeId, id));
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

    public void exportExcelZipByDatasetId(String id, HttpServletResponse response) throws IOException {
        KnowledgeEntity dataset = this.getById(id);
        List<DocumentEntity> docs = documentMapper.selectList(Wrappers.<DocumentEntity>lambdaQuery().eq(DocumentEntity::getDatasetId, id));
        exportExcelZipByDocs(docs, dataset.getName(), response);
    }

    public void exportExcelByDatasetId(String id, HttpServletResponse response) throws IOException {
        KnowledgeEntity dataset = this.getById(id);
        List<DocumentEntity> docs = documentMapper.selectList(Wrappers.<DocumentEntity>lambdaQuery().eq(DocumentEntity::getDatasetId, id));
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


    private final DocumentParser parser = new ApacheTikaDocumentParser();
    private final DocumentSplitter defaultSplitter = new DocumentBySentenceSplitter(512, 20);

    public List<TextSegmentVO> split(MultipartFile[] files, String[] patterns, Integer limit, Boolean withFilter) {
        List<TextSegmentVO> list = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue; // 或抛出异常根据业务需求
            }
            TextSegmentVO textSegmentVO = new TextSegmentVO();
            textSegmentVO.setName(file.getOriginalFilename());
            try (InputStream inputStream = file.getInputStream()) {
                Document document = parser.parse(inputStream);
                List<TextSegment> textSegments = getTextSegments(document, patterns, limit, withFilter);
                List<ParagraphSimpleVO> content = textSegments.stream()
                        .map(segment -> new ParagraphSimpleVO(segment.text()))
                        .collect(Collectors.toList());

                textSegmentVO.setContent(content);
            } catch (IOException e) {
                // 添加日志记录
                throw new RuntimeException("File processing failed: " + file.getOriginalFilename(), e);
            }
            list.add(textSegmentVO);
        }
        return list;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    private List<TextSegment> getTextSegments(Document document, String[] patterns, Integer limit, Boolean withFilter) {
        if (patterns != null) {
            List<TextSegment> textSegments = recursive(document, patterns, limit);
            if (withFilter) {
                textSegments = textSegments.stream()
                        .filter(e -> StringUtils.isNotBlank(e.text()))
                        .filter(distinctByKey(TextSegment::text))
                        .collect(Collectors.toList());
            }
            return textSegments;
        } else {
            return defaultSplitter.split(document);
        }
    }

    public List<TextSegment> recursive(Document document, String[] patterns, Integer limit) {
        List<TextSegment> textSegments = new ArrayList<>();
        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            if (i == 0) {
                DocumentSplitter splitter = new DocumentByRegexSplitter(pattern, "", 1, 0, new DocumentByCharacterSplitter(limit, 0));
                textSegments = recursive(splitter.split(document), pattern);
            } else {
                textSegments = recursive(textSegments, pattern);
            }
        }
        return textSegments;
    }

    public List<TextSegment> recursive(List<TextSegment> segments, String pattern) {
        List<TextSegment> result = new ArrayList<>();
        for (TextSegment segment : segments) {
            String text = segment.text();
            if (StringUtils.isNotBlank(text)) {
                String[] split = text.split(pattern);
                for (String s : split) {
                    result.add(TextSegment.textSegment(s));
                }
            }
        }
        return result;
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
        userResourcePermissionService.save("KNOWLEDGE", knowledge.getId(), StpUtil.getLoginIdAsString(), "default");
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
        userResourcePermissionService.save("KNOWLEDGE", knowledge.getId(), StpUtil.getLoginIdAsString(), "default");
        return knowledge;
    }

    public boolean reEmbedding(String datasetId) {
        List<DocumentEntity> documents=documentService.lambdaQuery().select(DocumentEntity::getId).eq(DocumentEntity::getDatasetId, datasetId).list();
        documentService.embedByDocIds(documents.stream().map(DocumentEntity::getId).toList());
        return true;
    }
}
