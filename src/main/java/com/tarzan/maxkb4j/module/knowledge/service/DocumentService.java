package com.tarzan.maxkb4j.module.knowledge.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.tarzan.maxkb4j.common.base.dto.Query;
import com.tarzan.maxkb4j.common.util.ExcelUtil;
import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.common.util.JsoupUtil;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DatasetBatchHitHandlingDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DocumentNameDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.WebUrlDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.*;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.DocumentVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.FileStreamVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphSimpleVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.TextSegmentVO;
import com.tarzan.maxkb4j.module.knowledge.enums.DocType;
import com.tarzan.maxkb4j.module.knowledge.excel.DatasetExcel;
import com.tarzan.maxkb4j.module.knowledge.mapper.DocumentMapper;
import com.tarzan.maxkb4j.module.knowledge.mapper.KnowledgeMapper;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.info.vo.KeyAndValueVO;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseChatModel;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Slf4j
@Service
@AllArgsConstructor
public class DocumentService extends ServiceImpl<DocumentMapper, DocumentEntity> {

    private final ParagraphService paragraphService;
    private final ProblemService problemService;
    private final ProblemParagraphService problemParagraphService;
    private final DocumentParseService documentParseService;
    private final KnowledgeMapper datasetMapper;
    private final ModelService modelService;
    private final MongoFileService mongoFileService;


    public void updateStatusMetaById(String id) {
        baseMapper.updateStatusMetaByIds(List.of(id));
    }

    public void updateStatusMetaByIds(List<String> ids) {
        baseMapper.updateStatusMetaByIds(ids);
    }

    //type 1向量化 2 生成问题 3同步
    public void updateStatusById(String id, int type, int status) {
        baseMapper.updateStatusByIds(List.of(id), type, status);
    }

    public void updateStatusByIds(List<String> ids, int type, int status) {
        baseMapper.updateStatusByIds(ids, type, status);
    }

    public boolean updateCharLengthById(String id) {
        return baseMapper.updateCharLengthById(id);
    }

    public List<DocumentEntity> listDocByKnowledgeId(String id) {
        return this.lambdaQuery().eq(DocumentEntity::getKnowledgeId, id).list();
    }

    @Transactional
    public boolean migrateDoc(String sourceKnowledgeId, String targetKnowledgeId, List<String> docIds) {
        if (!CollectionUtils.isEmpty(docIds)) {
            paragraphService.migrateDoc(sourceKnowledgeId, targetKnowledgeId, docIds);
            return this.lambdaUpdate().set(DocumentEntity::getKnowledgeId, targetKnowledgeId).in(DocumentEntity::getId, docIds).update();
        }
        return false;
    }

    @Transactional
    public boolean batchHitHandling(String knowledgeId, DatasetBatchHitHandlingDTO dto) {
        List<String> ids = dto.getIdList();
        if (!CollectionUtils.isEmpty(ids)) {
            List<DocumentEntity> documentEntities = new ArrayList<>();
            ids.forEach(id -> {
                DocumentEntity entity = new DocumentEntity();
                entity.setId(id);
                entity.setKnowledgeId(knowledgeId);
                entity.setHitHandlingMethod(dto.getHitHandlingMethod());
                entity.setDirectlyReturnSimilarity(dto.getDirectlyReturnSimilarity());
                documentEntities.add(entity);
            });
            return this.updateBatchById(documentEntities);
        }
        return false;
    }

    @Transactional
    public void importQa(String knowledgeId, MultipartFile[] file) throws IOException {
        for (MultipartFile uploadFile : file) {
            String fileName = uploadFile.getOriginalFilename();
            if (fileName != null && fileName.toLowerCase().endsWith(".zip")) {
                try (InputStream fis = uploadFile.getInputStream();
                     ZipArchiveInputStream zipIn = new ZipArchiveInputStream(fis)) {
                    ArchiveEntry entry;
                    while ((entry = zipIn.getNextEntry()) != null) {
                        // 假设ZIP包内只有一个文件或主要处理第一个找到的Excel文件
                        if (!entry.isDirectory() && (entry.getName().toLowerCase().endsWith(".xls") || entry.getName().endsWith(".xlsx") || entry.getName().endsWith(".csv"))) {
                            processExcelFile(knowledgeId, IOUtils.toByteArray(zipIn));
                            break; // 如果只处理一个文件，则在此处跳出循环
                        }
                    }
                }
            }
            if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
                processCsvFile(knowledgeId, uploadFile);
            } else {
                processExcelFile(knowledgeId, uploadFile.getBytes());
            }
        }
    }

    @Transactional
    protected void processCsvFile(String knowledgeId, MultipartFile file) throws IOException {
        List<ParagraphEntity> paragraphs = new ArrayList<>();
        DocumentEntity doc = createDocument(knowledgeId, file.getOriginalFilename(), DocType.BASE.getType());
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReader(br)) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                if (values.length > 1) {
                    String title = values[0];
                    String content = values[1];
                    ParagraphEntity paragraph = paragraphService.getParagraphEntity(knowledgeId, doc.getId(), title, content);
                    paragraphs.add(paragraph);
                    doc.setCharLength(doc.getCharLength() + paragraph.getContent().length());
                }
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
        this.save(doc,file);
        if (!CollectionUtils.isEmpty(paragraphs)) {
            paragraphService.saveBatch(paragraphs);
        }
    }

    private void save(DocumentEntity doc,MultipartFile file) throws IOException {
        String fileId =mongoFileService.storeFile(file);
        System.out.println(fileId);
        doc.setMeta(new JSONObject(Map.of("allow_download",true,"source_file_id",fileId)));
        this.save(doc);
    }

    @Transactional
    protected void processExcelFile(String knowledgeId, byte[] fileBytes) throws IOException {
        try (InputStream fis = new ByteArrayInputStream(fileBytes)) {
            Workbook workbook = WorkbookFactory.create(fis);
            int numberOfSheets = workbook.getNumberOfSheets();
            List<ProblemEntity> allProblems = problemService.lambdaQuery().eq(ProblemEntity::getKnowledgeId, knowledgeId).list();
            List<ProblemEntity> problemEntities = new ArrayList<>();
            List<ProblemParagraphEntity> problemParagraphs = new ArrayList<>();
            List<ParagraphEntity> paragraphs = new ArrayList<>();
            List<DocumentEntity> docs = new ArrayList<>();
            for (int i = 0; i < numberOfSheets; i++) {
                String sheetName = workbook.getSheetName(i);
                DocumentEntity doc = createDocument(knowledgeId, sheetName, DocType.BASE.getType());
                // 对于每一个Sheet进行数据读取
                EasyExcel.read(new ByteArrayInputStream(fileBytes))
                        .sheet(sheetName) // 使用Sheet编号读取
                        .head(DatasetExcel.class)
                        .registerReadListener(new PageReadListener<DatasetExcel>(dataList -> {
                            for (DatasetExcel data : dataList) {
                                log.info("在Sheet {} 中读取到一条数据{}", sheetName, JSON.toJSONString(data));
                                ParagraphEntity paragraph = paragraphService.getParagraphEntity(knowledgeId, doc.getId(), data.getTitle(), data.getContent());
                                paragraphs.add(paragraph);
                                doc.setCharLength(doc.getCharLength() + paragraph.getContent().length());
                                if (StringUtils.isNotBlank(data.getProblems())) {
                                    String[] problems = data.getProblems().split("\n");
                                    for (String problem : problems) {
                                        String problemId = IdWorker.get32UUID();
                                        ProblemEntity existingProblem = problemService.findProblem(problem, allProblems);
                                        if (existingProblem == null) {
                                            ProblemEntity problemEntity = ProblemEntity.createDefault();
                                            problemEntity.setId(problemId);
                                            problemEntity.setKnowledgeId(knowledgeId);
                                            problemEntity.setContent(problem);
                                            problemEntities.add(problemEntity);
                                            allProblems.add(problemEntity);
                                        } else {
                                            problemId = existingProblem.getId();
                                        }
                                        if (!isExistProblemParagraph(paragraph.getId(), problemId, problemParagraphs)) {
                                            ProblemParagraphEntity problemParagraph = new ProblemParagraphEntity();
                                            problemParagraph.setKnowledgeId(knowledgeId);
                                            problemParagraph.setParagraphId(paragraph.getId());
                                            problemParagraph.setDocumentId(doc.getId());
                                            problemParagraph.setProblemId(problemId);
                                            problemParagraphs.add(problemParagraph);
                                        }
                                    }
                                }
                            }
                        })).doRead();
                docs.add(doc);
            }
            this.saveBatch(docs);
            paragraphService.saveBatch(paragraphs);
            problemService.saveBatch(problemEntities);
            problemParagraphService.saveBatch(problemParagraphs);
        }
    }


    @Transactional
    public void importTable(String knowledgeId, MultipartFile[] file) throws IOException {
        for (MultipartFile uploadFile : file) {
            System.out.println(uploadFile.getOriginalFilename());
            List<String> list = new ArrayList<>();
            EasyExcel.read(uploadFile.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
                Map<Integer, String> headMap = new LinkedHashMap<>();
                // 表头信息会在此方法中获取
                @Override
                public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                    this.headMap = headMap;
                }

                // 每一行数据都会调用此方法
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    StringBuilder sb = new StringBuilder();
                    for (Integer i : data.keySet()) {
                        String value = data.get(i) == null ? "" : data.get(i);
                        sb.append(headMap.get(i)).append(":").append(value).append(";");
                    }
                    sb.deleteCharAt(sb.length() - 1);
                    list.add(sb.toString());
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                    log.info("所有数据解析完成！");
                }
            }).sheet().doRead();
            List<ParagraphEntity> paragraphs = new ArrayList<>();
            DocumentEntity doc = createDocument(knowledgeId, uploadFile.getOriginalFilename(), DocType.BASE.getType());
            if (!CollectionUtils.isEmpty(list)) {
                for (String text : list) {
                    doc.setCharLength(doc.getCharLength() + text.length());
                    ParagraphEntity paragraph = paragraphService.getParagraphEntity(knowledgeId, doc.getId(), "", text);
                    paragraphs.add(paragraph);
                }
                this.save(doc,uploadFile);
                paragraphService.saveBatch(paragraphs);
            }
        }
    }


    private boolean isExistProblemParagraph(String paragraphId, String problemId, List<ProblemParagraphEntity> problemParagraphs) {
        if (CollectionUtils.isEmpty(problemParagraphs)) {
            return false;
        }
        return problemParagraphs.stream().anyMatch(e -> problemId.equals(e.getProblemId()) && paragraphId.equals(e.getParagraphId()));
    }


    public void exportExcelByDocId(String docId, HttpServletResponse response) throws IOException {
        DocumentEntity doc = this.getById(docId);
        exportExcelZipByDocs(List.of(doc), doc.getName(), response);
    }

    public void exportExcelZipByDocId(String docId, HttpServletResponse response) {
        DocumentEntity doc = this.getById(docId);
        List<DatasetExcel> list = getDatasetExcelByDoc(doc);
        ExcelUtil.export(response, doc.getName(), doc.getName(), list, DatasetExcel.class);
    }

    public void exportTemplate(String type, HttpServletResponse response, String csvPath, String excelPath, String csvFileName, String excelFileName) throws Exception {
        // 设置字符编码
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = "";
        String contentType = "";
        InputStream inputStream = null;
        ClassLoader classLoader = getClass().getClassLoader();
        if ("csv".equals(type)) {
            contentType = "text/csv";
            fileName = URLEncoder.encode(csvFileName, StandardCharsets.UTF_8);
            inputStream = classLoader.getResourceAsStream(csvPath);
        } else if ("excel".equals(type)) {
            contentType = "application/vnd.ms-excel"; // 更准确的Excel MIME类型
            fileName = URLEncoder.encode(excelFileName, StandardCharsets.UTF_8);
            inputStream = classLoader.getResourceAsStream(excelPath);
        }

        if (inputStream != null) {
            try (OutputStream outputStream = response.getOutputStream()) {
                // 设置响应内容类型和头部信息
                response.setContentType(contentType);
                response.setHeader("Content-disposition", "attachment;filename=" + fileName);

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            } finally {
                // 确保输入流被关闭，即使发生异常
                inputStream.close();
            }
        } else {
            throw new Exception("无法找到指定类型的模板文件");
        }
    }

    public void tableTemplateExport(String type, HttpServletResponse response) throws Exception {
        exportTemplate(type, response, "templates/MaxKB4J表格模板.csv", "templates/MaxKB4J表格模板.xlsx", "csv_template.csv", "excel_template.xlsx");
    }

    public void templateExport(String type, HttpServletResponse response) throws Exception {
        exportTemplate(type, response, "templates/csv_template.csv", "templates/excel_template.xlsx", "csv_template.csv", "excel_template.xlsx");
    }


    private final DocumentSplitter defaultSplitter = new DocumentBySentenceSplitter(512, 20);

    public List<TextSegmentVO> split(MultipartFile[] files, String[] patterns, Integer limit, Boolean withFilter) throws IOException {
        List<TextSegmentVO> list = new ArrayList<>();
        List<FileStreamVO> fileStreams = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue; // 或抛出异常根据业务需求
            }
            // 判断是否是zip文件
            if (isZipFile(file)) {
                //todo 未处理zip下的zip文件
                try (ZipArchiveInputStream zis = new ZipArchiveInputStream(file.getInputStream())) {
                    ZipArchiveEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            String entryName = entry.getName();
                            byte[] bytes = zis.readAllBytes();
                            InputStream inputStream = new ByteArrayInputStream(bytes);
                            fileStreams.add(new FileStreamVO(entryName, inputStream));
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("解压ZIP文件失败", e);
                }
            } else {
                fileStreams.add(new FileStreamVO(file.getOriginalFilename(), file.getInputStream()));
            }
        }
        for (FileStreamVO fileStream : fileStreams) {
            TextSegmentVO textSegmentVO = new TextSegmentVO();
            textSegmentVO.setName(fileStream.getName());
            String docText = documentParseService.extractText(fileStream.getInputStream());
            List<TextSegment> textSegments = Collections.emptyList();
            if (StringUtil.isNotBlank(docText)) {
                textSegments = getTextSegments(Document.document(docText), patterns, limit, withFilter);
            }
            List<ParagraphSimpleVO> content = textSegments.stream()
                    .map(segment -> new ParagraphSimpleVO(segment.text()))
                    .collect(Collectors.toList());
            textSegmentVO.setContent(content);
            list.add(textSegmentVO);
        }
        return list;
    }


    /**
     * 判断是否为 ZIP 文件（通过文件头 MAGIC NUMBER）
     */
    private boolean isZipFile(MultipartFile file) throws IOException {
        return Objects.requireNonNull(file.getOriginalFilename()).endsWith(".zip");
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

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
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


    private List<DatasetExcel> getDatasetExcelByDoc(DocumentEntity doc) {
        List<DatasetExcel> list = new ArrayList<>();
        List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, doc.getId()).list();
        for (ParagraphEntity paragraph : paragraphs) {
            DatasetExcel excel = new DatasetExcel();
            excel.setTitle(paragraph.getTitle());
            excel.setContent(paragraph.getContent());
            List<ProblemEntity> problemEntities = problemParagraphService.getProblemsByParagraphId(paragraph.getId());
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

    @Transactional
    public boolean createBatchDoc(String knowledgeId, List<DocumentNameDTO> docs) {
        if (!CollectionUtils.isEmpty(docs)) {
            List<DocumentEntity> documentEntities = new ArrayList<>();
            List<ParagraphEntity> paragraphEntities = new ArrayList<>();
            docs.parallelStream().forEach(e -> {
                DocumentEntity doc = createDocument(knowledgeId, e.getName(), DocType.BASE.getType());
                AtomicInteger docCharLength = new AtomicInteger();
                if (!CollectionUtils.isEmpty(e.getParagraphs())) {
                    e.getParagraphs().forEach(p -> {
                        paragraphEntities.add(paragraphService.createParagraph(knowledgeId, doc.getId(), p));
                        docCharLength.addAndGet(p.getContent().length());
                    });
                }
                doc.setCharLength(docCharLength.get());
                documentEntities.add(doc);
            });
            if (!CollectionUtils.isEmpty(paragraphEntities)) {
                paragraphService.saveBatch(paragraphEntities);
            }
            this.saveBatch(documentEntities);
            documentEntities.forEach(doc -> {
               // paragraphService.updateStatusByDocId(doc.getId(), 1, 0);
                this.updateStatusById(doc.getId(), 1, 0);
                //目的是为了显示进度计数
                this.updateStatusMetaById(doc.getId());
            });
        }
        return false;
    }

    public DocumentEntity createDocument(String knowledgeId, String name, Integer type) {
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setId(IdWorker.get32UUID());
        documentEntity.setKnowledgeId(knowledgeId);
        documentEntity.setName(name);
        documentEntity.setMeta(new JSONObject());
        documentEntity.setCharLength(0);
        documentEntity.setStatus("nn0");
        //documentEntity.setStatusMeta(documentEntity.defaultStatusMeta());
        documentEntity.setIsActive(true);
        documentEntity.setType(type);
        documentEntity.setHitHandlingMethod("optimization");
        documentEntity.setDirectlyReturnSimilarity(0.9);
        return documentEntity;
    }


    @Transactional
    public boolean deleteBatchDocByDocIds(List<String> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return false;
        }
        paragraphService.deleteByDocIds(docIds);
        return this.lambdaUpdate().in(DocumentEntity::getId, docIds).remove();
    }

    @Transactional
    public boolean embedByDocIds(List<String> docIds) {
        if (!CollectionUtils.isEmpty(docIds)) {
            docIds.parallelStream().forEach(docId -> {
                paragraphService.updateStatusByDocId(docId, 1, 0);
                this.updateStatusById(docId, 1, 0);
                //目的是为了显示进度计数
                this.updateStatusMetaById(docId);
            });
        }
        return true;
    }


    public void createIndex(EmbeddingModel embeddingModel, String docId) {
        log.info("开始--->文档索引:{}", docId);
        List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).list();
        this.updateStatusById(docId, 1, 1);
        paragraphs.forEach(paragraph -> {
            paragraphService.createIndex(paragraph, embeddingModel);
            this.updateStatusMetaById(docId);
        });
        this.updateStatusById(docId, 1, 2);
        log.info("结束--->文档索引:{}", docId);
    }

    public boolean cancelTask(String docId, DocumentEntity doc) {
        DocumentEntity entity = baseMapper.selectById(docId);
        entity.setId(docId);
        String status = entity.getStatus();
        if (doc.getType() == 1) {
            entity.setStatus(status.replace(status.substring(2), "3"));
        } else if (doc.getType() == 2) {
            entity.setStatus(status.replace(status.substring(1, 2), "3"));
        }
        return this.updateById(entity);
    }

    public DocumentEntity updateDocByDocId(String docId, DocumentEntity documentEntity) {
        documentEntity.setId(docId);
        this.updateById(documentEntity);
        return this.getById(docId);
    }

    @Transactional
    public boolean deleteDoc(String docId) {
        List<ProblemParagraphEntity> list = problemParagraphService.lambdaQuery().select(ProblemParagraphEntity::getProblemId).eq(ProblemParagraphEntity::getDocumentId, docId).list();
        problemParagraphService.lambdaUpdate().eq(ProblemParagraphEntity::getDocumentId, docId).remove();
        paragraphService.lambdaUpdate().eq(ParagraphEntity::getDocumentId, docId).remove();
        if (!CollectionUtils.isEmpty(list)) {
            problemService.removeBatchByIds(list.stream().map(ProblemParagraphEntity::getProblemId).toList());
        }
        return this.removeById(docId);
    }

    public IPage<DocumentVO> getDocByKnowledgeId(String knowledgeId, int current, int size, Query query) {
        Page<DocumentVO> docPage = new Page<>(current, size);
        baseMapper.selectDocPage(docPage, knowledgeId, query);
        return docPage;
    }


    public List<KeyAndValueVO> splitPattern() {
        List<KeyAndValueVO> resultList = new ArrayList<>();
        resultList.add(new KeyAndValueVO("#", "(?<=^)# .*|(?<=\\n)# .*"));
        resultList.add(new KeyAndValueVO("##", "(?<=\\n)(?<!#)## (?!#).*|(?<=^)(?<!#)## (?!#).*"));
        resultList.add(new KeyAndValueVO("##", "(?<=\\n)(?<!#)## (?!#).*|(?<=^)(?<!#)## (?!#).*"));
        resultList.add(new KeyAndValueVO("###", "(?<=\\n)(?<!#)### (?!#).*|(?<=^)(?<!#)### (?!#).*"));
        resultList.add(new KeyAndValueVO("####", "(?<=\\n)(?<!#)#### (?!#).*|(?<=^)(?<!#)#### (?!#).*"));
        resultList.add(new KeyAndValueVO("#####", "(?<=\\n)(?<!#)##### (?!#).*|(?<=^)(?<!#)##### (?!#).*"));
        resultList.add(new KeyAndValueVO("######", "(?<=\\n)(?<!#)###### (?!#).*|(?<=^)(?<!#)###### (?!#).*"));
        resultList.add(new KeyAndValueVO("-", "(?<! )- .*"));
        resultList.add(new KeyAndValueVO("space", "(?<! ) (?! )"));
        resultList.add(new KeyAndValueVO("semicolon", "(?<!；)；(?!；)"));
        resultList.add(new KeyAndValueVO("comma", "(?<!，)，(?!，)"));
        resultList.add(new KeyAndValueVO("period", "(?<!。)。(?!。)"));
        resultList.add(new KeyAndValueVO("enter", "(?<!\\n)\\n(?!\\n)"));
        resultList.add(new KeyAndValueVO("blank line", "(?<!\\n)\\n\\n(?!\\n)"));
        return resultList;
    }

    @Transactional
    public void web(String knowledgeId, WebUrlDTO params) {
        webDoc(knowledgeId, params.getSourceUrlList(), params.getSelector());
    }

    @Async
    @Transactional
    public void webDataset(String knowledgeId, String baseUrl, String selector) {
        if (StringUtils.isBlank(selector)) {
            selector = "body";
        }
        String finalSelector = selector;
        baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        org.jsoup.nodes.Document html = JsoupUtil.getDocument(baseUrl);
        Elements elements = html.select(finalSelector);
        List<String> sourceUrlList = new ArrayList<>();
        sourceUrlList.add(baseUrl);
        Elements links = elements.select("a");
        for (Element link : links) {
            String href = link.attr("href");
            String url = baseUrl + href;
            String[] catalogs = href.split("/");
            if (!sourceUrlList.contains(url) && href.startsWith("/") && catalogs.length == 2) {
                if (!href.contains("?") && !href.contains("#")) {
                    sourceUrlList.add(baseUrl + href);
                }
            }
        }
        webDoc(knowledgeId, sourceUrlList, finalSelector);
    }


    @Transactional
    public void webDoc(String knowledgeId, List<String> sourceUrlList, String selector) {
        if (StringUtils.isBlank(selector)) {
            selector = "body";
        }
        List<DocumentEntity> docs = new ArrayList<>();
        List<ParagraphEntity> paragraphs = new ArrayList<>();
        String finalSelector = selector;
        sourceUrlList.forEach(url -> {
            org.jsoup.nodes.Document html = JsoupUtil.getDocument(url);
            Elements elements = html.select(finalSelector);
            Document document = Document.document(elements.text());
            DocumentEntity doc = createDocument(knowledgeId, JsoupUtil.getTitle(html), DocType.WEB.getType());
            JSONObject meta = new JSONObject();
            meta.put("source_url", url);
            meta.put("selector", finalSelector);
            doc.setMeta(meta);
            List<TextSegment> textSegments = defaultSplitter.split(document);
            for (TextSegment textSegment : textSegments) {
                ParagraphEntity paragraph = paragraphService.getParagraphEntity(knowledgeId, doc.getId(), "", textSegment.text());
                paragraphs.add(paragraph);
                doc.setCharLength(doc.getCharLength() + paragraph.getContent().length());
            }
            docs.add(doc);
        });
        this.saveBatch(docs);
        paragraphService.saveBatch(paragraphs);
    }

    @Transactional
    public void sync(String knowledgeId, String docId) {
        DocumentEntity doc = this.getById(docId);
        deleteBatchDocByDocIds(List.of(docId));
        webDoc(knowledgeId, List.of(doc.getMeta().getString("source_url")), doc.getMeta().getString("selector"));
    }

    public boolean batchGenerateRelated(String knowledgeId, GenerateProblemDTO dto) {
        if (CollectionUtils.isEmpty(dto.getDocumentIdList())) {
            return false;
        }
        paragraphService.updateStatusByDocIds(dto.getDocumentIdList(), 2, 0);
        baseMapper.updateStatusByIds(dto.getDocumentIdList(), 2, 0);
        baseMapper.updateStatusMetaByIds(dto.getDocumentIdList());
        KnowledgeEntity dataset = datasetMapper.selectById(knowledgeId);
        BaseChatModel chatModel = modelService.getModelById(dto.getModelId());
        EmbeddingModel embeddingModel = modelService.getModelById(dataset.getEmbeddingModelId());
        dto.getDocumentIdList().parallelStream().forEach(docId -> {
            List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).list();
            List<ProblemEntity> allProblems = problemService.lambdaQuery().eq(ProblemEntity::getKnowledgeId, knowledgeId).list();
            baseMapper.updateStatusByIds(List.of(docId), 2, 1);
            paragraphs.forEach(paragraph -> {
                problemService.generateRelated(chatModel, embeddingModel, knowledgeId, docId, paragraph, allProblems, dto);
                paragraphService.updateStatusById(paragraph.getId(), 2, 2);
                baseMapper.updateStatusMetaByIds(List.of(paragraph.getDocumentId()));
            });
            baseMapper.updateStatusByIds(List.of(docId), 2, 2);
        });
        return true;
    }

    public boolean downloadSourceFile(String docId, HttpServletResponse response) throws IOException {
        DocumentEntity doc =this.getById(docId);
        JSONObject meta = doc.getMeta();
        if (meta.get("source_file_id")!= null) {
            String fileId = doc.getMeta().getString("source_file_id");
            InputStream inputStream = mongoFileService.getStream(fileId);
            IoUtil.copy(inputStream, response.getOutputStream());
            return true;
        }
        return false;
    }
}
