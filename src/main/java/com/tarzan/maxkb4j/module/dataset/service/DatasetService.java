package com.tarzan.maxkb4j.module.dataset.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.application.entity.ApplicationDatasetMappingEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationDatasetMappingMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.dataset.dto.DatasetBatchHitHandlingDTO;
import com.tarzan.maxkb4j.module.dataset.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.entity.*;
import com.tarzan.maxkb4j.module.dataset.excel.DatasetExcel;
import com.tarzan.maxkb4j.module.dataset.mapper.DatasetMapper;
import com.tarzan.maxkb4j.module.dataset.vo.*;
import com.tarzan.maxkb4j.module.embedding.entity.EmbeddingEntity;
import com.tarzan.maxkb4j.module.embedding.service.EmbeddingService;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.BeanUtil;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
public class DatasetService extends ServiceImpl<DatasetMapper, DatasetEntity> {

    @Autowired
    private DocumentService documentService;
    @Autowired
    private ProblemService problemService;
    @Autowired
    private ApplicationMapper applicationMapper;
    @Autowired
    private ApplicationDatasetMappingMapper applicationDatasetMappingMapper;
    @Autowired
    private ParagraphService paragraphService;
    @Autowired
    private ProblemParagraphService problemParagraphMappingService;
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private ModelService modelService;


    public IPage<DatasetVO> selectDatasetPage(Page<DatasetVO> datasetPage, QueryDTO query) {
        if (Objects.isNull(query.getSelectUserId())) {
            query.setSelectUserId(StpUtil.getLoginIdAsString());
        }
        return baseMapper.selectDatasetPage(datasetPage, query, "USE");
    }

    public List<DatasetEntity> getUserId(String userId) {
        return this.list(Wrappers.<DatasetEntity>lambdaQuery().eq(DatasetEntity::getUserId, userId));
    }

    public IPage<ProblemVO> getProblemsByDatasetId(String id, int page, int size, String content) {
        Page<ProblemEntity> problemPage = new Page<>(page, size);
        return problemService.getProblemsByDatasetId(problemPage, id, content);
    }

    public List<ApplicationEntity> getApplicationByDatasetId(String id) {
        //TODO 缓存处理
        return applicationMapper.selectList(null);
    }

    public DatasetVO getByDatasetId(String id) {
        DatasetEntity entity = baseMapper.selectById(id);
        if (Objects.isNull(entity)) {
            return null;
        }
        DatasetVO vo = BeanUtil.copy(entity, DatasetVO.class);
        List<ApplicationDatasetMappingEntity> apps = applicationDatasetMappingMapper.selectList(Wrappers.lambdaQuery(ApplicationDatasetMappingEntity.class)
                .select(ApplicationDatasetMappingEntity::getApplicationId)
                .eq(ApplicationDatasetMappingEntity::getDatasetId, id));
        List<String> appIds = apps.stream().map(ApplicationDatasetMappingEntity::getApplicationId).toList();
        vo.setApplicationidList(appIds);
        return vo;
    }

    public boolean createProblemsByDatasetId(String id, List<String> problems) {
        return problemService.createProblemsByDatasetId(id, problems);
    }

    public boolean updateProblemById(ProblemEntity problem) {
        return problemService.updateById(problem);
    }

    @Transactional
    public boolean deleteProblemByDatasetId(String problemId) {
        problemParagraphMappingService.lambdaUpdate().eq(ProblemParagraphEntity::getProblemId, problemId).remove();
        return problemService.removeById(problemId);
    }

    @Transactional
    public boolean deleteProblemByDatasetIds(List<String> problemIds) {
        if (CollectionUtils.isEmpty(problemIds)) {
            return false;
        }
        problemParagraphMappingService.lambdaUpdate().in(ProblemParagraphEntity::getProblemId, problemIds).remove();
        embeddingService.lambdaUpdate().in(EmbeddingEntity::getSourceId, problemIds.stream().map(String::toString).toList()).remove();
        return problemService.lambdaUpdate().in(ProblemEntity::getId, problemIds).remove();
    }

    @Transactional
    public boolean refresh(String datasetId, String docId) {
        return documentService.embedByDocIds(getDatasetEmbeddingModel(datasetId),datasetId, List.of(docId));
    }

    @Transactional
    public boolean batchRefresh(String datasetId, DatasetBatchHitHandlingDTO dto) {
        return documentService.embedByDocIds(getDatasetEmbeddingModel(datasetId),datasetId, dto.getIdList());
    }

    public EmbeddingModel getDatasetEmbeddingModel(String datasetId){
        DatasetEntity dataset=baseMapper.selectById(datasetId);
        return modelService.getModelById(dataset.getEmbeddingModeId());
    }


    public List<ParagraphEntity> getParagraphByProblemId(String problemId) {
        List<ProblemParagraphEntity> list = problemParagraphMappingService.lambdaQuery()
                .select(ProblemParagraphEntity::getParagraphId).eq(ProblemParagraphEntity::getProblemId, problemId).list();
        if (!CollectionUtils.isEmpty(list)) {
            List<String> paragraphIds = list.stream().map(ProblemParagraphEntity::getParagraphId).toList();
            return paragraphService.lambdaQuery().in(ParagraphEntity::getId, paragraphIds).list();
        }
        return Collections.emptyList();
    }

    public List<ParagraphVO> hitTest(List<String> ids, HitTestDTO dto) {
        return embeddingService.paragraphSearch(ids, dto);
    }

    public List<ParagraphVO> hitTest(String id, HitTestDTO dto) {
        return embeddingService.paragraphSearch(List.of(id), dto);
    }

    public boolean reEmbedding(String datasetId) {
        return embeddingService.embedByDatasetId(datasetId);
    }

    public List<ModelEntity> getModels(String id) {
        return modelService.models("LLM");
    }

    public boolean batchGenerateRelated(String datasetId, GenerateProblemDTO dto) {
        if (CollectionUtils.isEmpty(dto.getDocument_id_list())) {
            return false;
        }
        paragraphService.updateStatusByDocIds(dto.getDocument_id_list(), 2, 0);
        documentService.updateStatusMetaByIds(dto.getDocument_id_list());
        documentService.updateStatusByIds(dto.getDocument_id_list(), 2, 0);
        DatasetEntity dataset = this.getById(datasetId);
        BaseChatModel chatModel = modelService.getModelById(dto.getModel_id());
        EmbeddingModel embeddingModel=modelService.getModelById(dataset.getEmbeddingModeId());
        dto.getDocument_id_list().parallelStream().forEach(docId -> {
            List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).list();
            List<ProblemEntity> docProblems=new ArrayList<>();
            List<ProblemEntity> dbProblemEntities = problemService.lambdaQuery().eq(ProblemEntity::getDatasetId, datasetId).list();
            documentService.updateStatusById(docId,2,1);
            paragraphs.parallelStream().forEach(paragraph -> {
                problemService.generateRelated(chatModel,embeddingModel,datasetId, docId, paragraph,dbProblemEntities,docProblems, dto);
                paragraphService.updateStatusById(paragraph.getId(),2,2);
                documentService.updateStatusMetaById(paragraph.getDocumentId());
            });
            documentService.updateStatusById(docId,2,2);
        });
        return true;
    }

    @Transactional
    public Boolean deleteDatasetById(String id) {
        problemParagraphMappingService.lambdaUpdate().eq(ProblemParagraphEntity::getDatasetId, id).remove();
        problemService.lambdaUpdate().eq(ProblemEntity::getDatasetId, id).remove();
        paragraphService.lambdaUpdate().eq(ParagraphEntity::getDatasetId, id).remove();
        documentService.lambdaUpdate().eq(DocumentEntity::getDatasetId, id).remove();
        applicationDatasetMappingMapper.delete(Wrappers.<ApplicationDatasetMappingEntity>lambdaQuery().eq(ApplicationDatasetMappingEntity::getDatasetId, id));
        embeddingService.lambdaUpdate().eq(EmbeddingEntity::getDatasetId, id).remove();
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
        DatasetEntity dataset = this.getById(id);
        List<DocumentEntity> docs = documentService.lambdaQuery().eq(DocumentEntity::getDatasetId, id).list();
        exportExcelZipByDocs(docs, dataset.getName(), response);
    }

    public void exportExcelByDatasetId(String id, HttpServletResponse response) throws IOException {
        DatasetEntity dataset = this.getById(id);
        List<DocumentEntity> docs = documentService.lambdaQuery().eq(DocumentEntity::getDatasetId, id).list();
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
            List<ProblemEntity> problemEntities = problemParagraphMappingService.getProblemsByParagraphId(paragraph.getId());
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


    public Boolean paragraphBatchGenerateRelated(String datasetId, String docId, GenerateProblemDTO dto) {
        paragraphService.updateStatusByDocIds(List.of(docId), 2, 0);
        documentService.updateStatusMetaByIds(List.of(docId));
        documentService.updateStatusByIds(List.of(docId), 2, 0);
        DatasetEntity dataset = this.getById(datasetId);
        BaseChatModel chatModel = modelService.getModelById(dto.getModel_id());
        EmbeddingModel embeddingModel=modelService.getModelById(dataset.getEmbeddingModeId());
        List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).list();
        List<ProblemEntity> docProblems=new ArrayList<>();
        List<ProblemEntity> dbProblemEntities = problemService.lambdaQuery().eq(ProblemEntity::getDatasetId, datasetId).list();
        documentService.updateStatusById(docId,2,1);
        paragraphs.parallelStream().forEach(paragraph -> {
            problemService.generateRelated(chatModel,embeddingModel,datasetId, docId, paragraph,dbProblemEntities,docProblems, dto);
            documentService.updateStatusMetaById(paragraph.getDocumentId());
        });
        documentService.updateStatusById(docId,2,2);
        return true;
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




    public List<DatasetEntity> listByUserId(String userId) {
        return this.lambdaQuery().eq(DatasetEntity::getUserId, userId).list();
    }


}
