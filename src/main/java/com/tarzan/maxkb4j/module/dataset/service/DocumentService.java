package com.tarzan.maxkb4j.module.dataset.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.listener.PageReadListener;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.tarzan.maxkb4j.core.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.dataset.dto.*;
import com.tarzan.maxkb4j.module.dataset.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.enums.DocType;
import com.tarzan.maxkb4j.module.dataset.excel.DatasetExcel;
import com.tarzan.maxkb4j.module.dataset.mapper.DocumentMapper;
import com.tarzan.maxkb4j.module.dataset.vo.DocumentVO;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphSimpleVO;
import com.tarzan.maxkb4j.module.dataset.vo.TextSegmentVO;
import com.tarzan.maxkb4j.module.model.info.vo.KeyAndValueVO;
import com.tarzan.maxkb4j.util.ExcelUtil;
import com.tarzan.maxkb4j.util.JsoupUtil;
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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
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
public class DocumentService extends ServiceImpl<DocumentMapper, DocumentEntity>{

    private final ParagraphService paragraphService;
    private final ProblemService problemService;
    private final ProblemParagraphService problemParagraphService;
    private final DocumentParseService documentParseService;


    public void updateStatusMetaById(String id){
        baseMapper.updateStatusMetaById(id);
    }

    public void updateStatusMetaByIds(List<String> ids){
        baseMapper.updateStatusMetaByIds(ids);
    }

    //type 1向量化 2 生成问题 3同步
    public void updateStatusById(String id, int type,int status) {
        baseMapper.updateStatusById(id,type,status,type-1,type+1);
    }

    public void updateStatusByIds(List<String> ids, int type,int status) {
        baseMapper.updateStatusByIds(ids,type,status,type-1,type+1);
    }


    public boolean updateCharLengthById(String id) {
       return baseMapper.updateCharLengthById(id);
    }

    public List<DocumentEntity> listDocByDatasetId(String id) {
        return this.lambdaQuery().eq(DocumentEntity::getDatasetId, id).list();
    }

    @Transactional
    public boolean migrateDoc(String sourceId, String targetId, List<String> docIds) {
        if (!CollectionUtils.isEmpty(docIds)) {
            paragraphService.migrateDoc(sourceId,targetId,docIds);
            return this.lambdaUpdate().set(DocumentEntity::getDatasetId, targetId).eq(DocumentEntity::getDatasetId, sourceId).update();
        }
        return false;
    }

    @Transactional
    public boolean batchHitHandling(String datasetId, DatasetBatchHitHandlingDTO dto) {
        List<String> ids = dto.getIdList();
        if (!CollectionUtils.isEmpty(ids)) {
            List<DocumentEntity> documentEntities = new ArrayList<>();
            ids.forEach(id -> {
                DocumentEntity entity = new DocumentEntity();
                entity.setId(id);
                entity.setDatasetId(datasetId);
                entity.setHitHandlingMethod(dto.getHitHandlingMethod());
                entity.setDirectlyReturnSimilarity(dto.getDirectlyReturnSimilarity());
                documentEntities.add(entity);
            });
            return this.updateBatchById(documentEntities);
        }
        return false;
    }

    @Transactional
    public void importQa(String datasetId, MultipartFile[] file) throws IOException {
        for (MultipartFile uploadFile : file) {
            String fileName = uploadFile.getOriginalFilename();
            if (fileName != null && fileName.toLowerCase().endsWith(".zip")) {
                try (InputStream fis = uploadFile.getInputStream();
                     ZipArchiveInputStream zipIn = new ZipArchiveInputStream(fis)) {
                    ArchiveEntry entry;
                    while ((entry = zipIn.getNextEntry()) != null) {
                        // 假设ZIP包内只有一个文件或主要处理第一个找到的Excel文件
                        if (!entry.isDirectory() && (entry.getName().toLowerCase().endsWith(".xls") || entry.getName().endsWith(".xlsx") || entry.getName().endsWith(".csv"))) {
                            processExcelFile(datasetId, IOUtils.toByteArray(zipIn));
                            break; // 如果只处理一个文件，则在此处跳出循环
                        }
                    }
                }
            }
            if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
                processCsvFile(datasetId, uploadFile);
            } else {
                processExcelFile(datasetId, uploadFile.getBytes());
            }
        }
    }

    @Transactional
    protected void processCsvFile(String datasetId, MultipartFile file) throws IOException {
        List<ParagraphEntity> paragraphs = new ArrayList<>();
        DocumentEntity doc = createDocument(datasetId, file.getOriginalFilename(), DocType.BASE.getType());
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReader(br)) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                if (values.length > 1) {
                    String title = values[0];
                    String content = values[1];
                    ParagraphEntity paragraph = getParagraphEntity(datasetId, doc.getId(), title, content);
                    paragraphs.add(paragraph);
                    doc.setCharLength(doc.getCharLength() + paragraph.getContent().length());
                }
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
        this.save(doc);
        if (!CollectionUtils.isEmpty(paragraphs)) {
            paragraphService.saveBatch(paragraphs);
        }
    }

    @Transactional
    protected void processExcelFile(String datasetId, byte[] fileBytes) throws IOException {
        try (InputStream fis = new ByteArrayInputStream(fileBytes)) {
            Workbook workbook = WorkbookFactory.create(fis);
            int numberOfSheets = workbook.getNumberOfSheets();
            List<ProblemEntity> allProblems = problemService.lambdaQuery().eq(ProblemEntity::getDatasetId, datasetId).list();
            List<ProblemEntity> problemEntities = new ArrayList<>();
            List<ProblemParagraphEntity> problemParagraphs = new ArrayList<>();
            List<ParagraphEntity> paragraphs = new ArrayList<>();
            List<DocumentEntity> docs = new ArrayList<>();
            for (int i = 0; i < numberOfSheets; i++) {
                String sheetName = workbook.getSheetName(i);
                DocumentEntity doc = createDocument(datasetId, sheetName,DocType.BASE.getType());
                // 对于每一个Sheet进行数据读取
                EasyExcel.read(new ByteArrayInputStream(fileBytes))
                        .sheet(sheetName) // 使用Sheet编号读取
                        .head(DatasetExcel.class)
                        .registerReadListener(new PageReadListener<DatasetExcel>(dataList -> {
                            for (DatasetExcel data : dataList) {
                                log.info("在Sheet {} 中读取到一条数据{}", sheetName, JSON.toJSONString(data));
                                ParagraphEntity paragraph = getParagraphEntity(datasetId, doc.getId(), data.getTitle(), data.getContent());
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
                                            problemEntity.setDatasetId(datasetId);
                                            problemEntity.setContent(problem);
                                            problemEntities.add(problemEntity);
                                            allProblems.add(problemEntity);
                                        } else {
                                            problemId = existingProblem.getId();
                                        }
                                        if (!isExistProblemParagraph(paragraph.getId(), problemId, problemParagraphs)) {
                                            ProblemParagraphEntity problemParagraph = new ProblemParagraphEntity();
                                            problemParagraph.setDatasetId(datasetId);
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
    public void importTable(String datasetId, MultipartFile[] file) throws IOException {
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
            DocumentEntity doc = createDocument(datasetId, uploadFile.getOriginalFilename(),DocType.BASE.getType());
            if (!CollectionUtils.isEmpty(list)) {
                for (String text : list) {
                    doc.setCharLength(doc.getCharLength() + text.length());
                    ParagraphEntity paragraph = getParagraphEntity(datasetId, doc.getId(), "", text);
                    paragraphs.add(paragraph);
                }
                this.save(doc);
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
                String docText = documentParseService.extractText(inputStream);
                List<TextSegment> textSegments = getTextSegments(Document.document(docText), patterns, limit, withFilter);
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

/*    public List<TextSegmentVO> split1(MultipartFile[] files, String[] patterns, Integer limit, Boolean withFilter) {
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
    }*/

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
    public boolean createBatchDoc(String datasetId, List<DocumentNameDTO> docs) {
        if (!CollectionUtils.isEmpty(docs)) {
            List<DocumentEntity> documentEntities = new ArrayList<>();
            List<ParagraphEntity> paragraphEntities = new ArrayList<>();
            docs.parallelStream().forEach(e -> {
                DocumentEntity doc = createDocument(datasetId, e.getName(),DocType.BASE.getType());
                AtomicInteger docCharLength = new AtomicInteger();
                if (!CollectionUtils.isEmpty(e.getParagraphs())) {
                    e.getParagraphs().forEach(p ->{
                        paragraphEntities.add(createParagraph(datasetId, doc.getId(), p));
                        docCharLength.addAndGet(p.getContent().length());
                    });
                }
                doc.setCharLength(docCharLength.get());
                documentEntities.add(doc);
            });
            if (!CollectionUtils.isEmpty(paragraphEntities)) {
                paragraphService.saveBatch(paragraphEntities);
            }
            return this.saveBatch(documentEntities);
        }
        return false;
    }

    public DocumentEntity createDocument(String datasetId, String name,Integer type) {
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setId(IdWorker.get32UUID());
        documentEntity.setDatasetId(datasetId);
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
    public ParagraphEntity createParagraph(String datasetId, String docId, ParagraphSimpleDTO paragraph) {
        return getParagraphEntity(datasetId, docId, paragraph.getTitle(), paragraph.getContent());
    }

    private ParagraphEntity getParagraphEntity(String datasetId, String docId, String title, String content) {
        ParagraphEntity paragraph = new ParagraphEntity();
        paragraph.setId(IdWorker.get32UUID());
        paragraph.setTitle(title == null ? "" : title);
        paragraph.setContent(content == null ? "" : content);
        paragraph.setDatasetId(datasetId);
        paragraph.setStatus("nn0");
        paragraph.setHitNum(0);
        paragraph.setIsActive(true);
        // paragraph.setStatusMeta(paragraph.defaultStatusMeta());
        paragraph.setDocumentId(docId);
        return paragraph;
    }

    @Transactional
    public boolean deleteBatchDocByDocIds(List<String> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return false;
        }
        paragraphService.deleteByDocIds(docIds);
        return this.lambdaUpdate().in(DocumentEntity::getId, docIds).remove();
    }

    @Async
    public void embedByDocIds(EmbeddingModel embeddingModel,List<String> docIds) {
        if (!CollectionUtils.isEmpty(docIds)) {
            docIds.forEach(docId -> {
                paragraphService.updateStatusByDocId(docId, 1, 0);
                this.updateStatusById(docId,1,0);
                //目的是为了显示进度计数
                this.updateStatusMetaById(docId);
            });
        }
    }

/*    public void createRelatedProblemByDocId(EmbeddingModel embeddingModel,String docId) {
        log.info("开始--->文档索引:{}", docId);
        List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).list();
        this.updateStatusById(docId,1,1);
        paragraphs.forEach(paragraph -> {
            paragraphService.paragraphIndex(paragraph,embeddingModel);
            this.updateStatusMetaById(docId);
        });
        this.updateStatusById(docId,1,2);
        log.info("结束--->文档索引:{}", docId);
    }*/


    public void createIndexByDocId(EmbeddingModel embeddingModel,String docId) {
        log.info("开始--->文档索引:{}", docId);
        List<ParagraphEntity> paragraphs = paragraphService.lambdaQuery().eq(ParagraphEntity::getDocumentId, docId).list();
        this.updateStatusById(docId,1,1);
        paragraphs.forEach(paragraph -> {
            paragraphService.paragraphIndex(paragraph,embeddingModel);
            this.updateStatusMetaById(docId);
        });
        this.updateStatusById(docId,1,2);
        log.info("结束--->文档索引:{}", docId);
    }

    public boolean cancelTask(String docId, DocumentEntity doc) {
        DocumentEntity entity=baseMapper.selectById(docId);
        entity.setId(docId);
        String status=entity.getStatus();
        if(doc.getType()==1){
            entity.setStatus(status.replace(status.substring(2), "3"));
        }else if(doc.getType()==2){
            entity.setStatus(status.replace(status.substring(1,2), "3"));
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

    public IPage<DocumentVO> getDocByDatasetId(String datasetId, int page, int size, QueryDTO query) {
        Page<DocumentVO> docPage = new Page<>(page, size);
        return baseMapper.selectDocPage(docPage, datasetId,query);
    }

    @Transactional
    public boolean createParagraph(String datasetId, String docId, ParagraphDTO paragraph) {
        paragraph.setDatasetId(datasetId);
        paragraph.setDocumentId(docId);
        boolean flag;
        paragraph.setStatus("nn2");
        paragraph.setHitNum(0);
        paragraph.setIsActive(true);
        flag = paragraphService.save(paragraph);
        this.updateCharLengthById(docId);
        List<ProblemEntity> problems = paragraph.getProblemList();
        if (!CollectionUtils.isEmpty(problems)) {
            List<String> problemContents = problems.stream().map(ProblemEntity::getContent).toList();
            problems = problemService.lambdaQuery().in(ProblemEntity::getContent, problemContents).list();
            List<String> problemIds = problems.stream().map(ProblemEntity::getId).toList();
            List<ProblemParagraphEntity> problemParagraphMappingEntities = new ArrayList<>();
            problemIds.forEach(problemId -> {
                ProblemParagraphEntity entity = new ProblemParagraphEntity();
                entity.setDatasetId(paragraph.getDatasetId());
                entity.setProblemId(problemId);
                entity.setParagraphId(paragraph.getId());
                entity.setDocumentId(paragraph.getDocumentId());
                problemParagraphMappingEntities.add(entity);
            });
            flag = problemParagraphService.saveBatch(problemParagraphMappingEntities);
        }
        return flag;
    }

    public IPage<ParagraphEntity> pageParagraphByDocId(String docId, int page, int size, String title, String content) {
        Page<ParagraphEntity> paragraphPage = new Page<>(page, size);
        LambdaQueryWrapper<ParagraphEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ParagraphEntity::getDocumentId, docId);
        if (StringUtils.isNotBlank(title)) {
            wrapper.like(ParagraphEntity::getTitle, title);
        }
        if (StringUtils.isNotBlank(content)) {
            wrapper.like(ParagraphEntity::getContent, content);
        }
        return paragraphService.page(paragraphPage, wrapper);
    }

    @Transactional
    public boolean updateParagraphByParagraphId(String docId,String paragraphId, ParagraphEntity paragraph) {
        paragraph.setId(paragraphId);
        paragraphService.updateParagraphById(paragraph);
        return this.updateCharLengthById(docId);
    }

    @Transactional
    public boolean deleteParagraphByParagraphId(String docId, String paragraphId) {
        return deleteBatchParagraphByParagraphIds(docId,List.of(paragraphId));
    }

    @Transactional
    public boolean deleteBatchParagraphByParagraphIds(String docId,List<String> paragraphIds) {
        boolean flag=paragraphService.deleteBatchParagraphByIds(paragraphIds);
        this.updateCharLengthById(docId);
        return flag;
    }

    public List<ProblemEntity> getProblemsByParagraphId(String paragraphId) {
        List<ProblemParagraphEntity> list = problemParagraphService.lambdaQuery()
                .select(ProblemParagraphEntity::getProblemId).eq(ProblemParagraphEntity::getParagraphId, paragraphId).list();
        if (!CollectionUtils.isEmpty(list)) {
            List<String> problemIds = list.stream().map(ProblemParagraphEntity::getProblemId).toList();
            return problemService.lambdaQuery().in(ProblemEntity::getId, problemIds).list();
        }
        return Collections.emptyList();
    }


    @Transactional
    public Boolean paragraphMigrate(String sourceDatasetId, String sourceDocId, String targetDatasetId, String targetDocId, List<String> paragraphIds) {
        List<ProblemParagraphEntity> list = problemParagraphService.lambdaQuery()
                .select(ProblemParagraphEntity::getProblemId)
                .in(ProblemParagraphEntity::getParagraphId, paragraphIds)
                .list();
        if (!CollectionUtils.isEmpty(list)) {
            problemService.lambdaUpdate()
                    .in(ProblemEntity::getId, list.stream().map(ProblemParagraphEntity::getProblemId).toList())
                    .set(ProblemEntity::getDatasetId, targetDatasetId).update();
        }
        problemParagraphService.lambdaUpdate()
                .in(ProblemParagraphEntity::getParagraphId, paragraphIds)
                .set(ProblemParagraphEntity::getDatasetId, targetDatasetId)
                .set(ProblemParagraphEntity::getDocumentId, targetDocId)
                .update();
        paragraphService.lambdaUpdate()
                .in(ParagraphEntity::getId, paragraphIds)
                .set(ParagraphEntity::getDatasetId, targetDatasetId)
                .set(ParagraphEntity::getDocumentId, targetDocId)
                .update();
        this.updateCharLengthById(sourceDocId);
        return this.updateCharLengthById(targetDocId);
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
    public void web(String datasetId, WebUrlDTO params) {
        webDoc(datasetId, params.getSourceUrlList(), params.getSelector());
    }

    @Async
    @Transactional
    public void webDataset(String datasetId,String baseUrl,String selector) {
        if(StringUtils.isBlank(selector)){
            selector="body";
        }
        String finalSelector = selector;
        baseUrl=baseUrl.endsWith("/")?baseUrl.substring(0,baseUrl.length()-1):baseUrl;
        org.jsoup.nodes.Document  html=JsoupUtil.getDocument(baseUrl);
        Elements elements=html.select(finalSelector);
        List<String> sourceUrlList=new ArrayList<>();
        sourceUrlList.add(baseUrl);
        Elements links= elements.select("a");
        for (Element link : links) {
            String href=link.attr("href");
            String url=baseUrl+href;
            String[]  catalogs=href.split("/");
            if (!sourceUrlList.contains(url)&&href.startsWith("/")&&catalogs.length==2){
                if(!href.contains("?")&&!href.contains("#")){
                    sourceUrlList.add(baseUrl+href);
                }
            }
        }
        webDoc(datasetId, sourceUrlList,finalSelector);
    }


    @Transactional
    public void webDoc(String datasetId,List<String> sourceUrlList,String selector) {
        if(StringUtils.isBlank(selector)){
            selector="body";
        }
        List<DocumentEntity> docs=new ArrayList<>();
        List<ParagraphEntity> paragraphs=new ArrayList<>();
        String finalSelector = selector;
        sourceUrlList.forEach(url -> {
            org.jsoup.nodes.Document  html=JsoupUtil.getDocument(url);
            Elements elements=html.select(finalSelector);
            Document document=Document.document(elements.text());
            DocumentEntity doc = createDocument(datasetId, JsoupUtil.getTitle(html),DocType.WEB.getType());
            JSONObject meta=new JSONObject();
            meta.put("source_url",url);
            meta.put("selector", finalSelector);
            doc.setMeta(meta);
            List<TextSegment> textSegments= defaultSplitter.split(document);
            for (TextSegment textSegment : textSegments) {
                ParagraphEntity paragraph = getParagraphEntity(datasetId, doc.getId(), "", textSegment.text());
                paragraphs.add(paragraph);
                doc.setCharLength(doc.getCharLength()+paragraph.getContent().length());
            }
            docs.add(doc);
        });
        this.saveBatch(docs);
        paragraphService.saveBatch(paragraphs);
    }

    @Transactional
    public void sync(String datasetId, String docId) {
        DocumentEntity doc = this.getById(docId);
        deleteBatchDocByDocIds(List.of(docId));
        webDoc(datasetId, List.of(doc.getMeta().getString("source_url")),doc.getMeta().getString("selector"));
    }
}
