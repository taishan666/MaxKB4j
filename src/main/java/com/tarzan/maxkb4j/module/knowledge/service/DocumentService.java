package com.tarzan.maxkb4j.module.knowledge.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.util.ExcelUtil;
import com.tarzan.maxkb4j.common.util.IoUtil;
import com.tarzan.maxkb4j.common.util.JsoupUtil;
import com.tarzan.maxkb4j.core.event.DocumentIndexEvent;
import com.tarzan.maxkb4j.core.event.GenerateProblemEvent;
import com.tarzan.maxkb4j.listener.DataListener;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DatasetBatchHitHandlingDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DocQuery;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.DocumentSimple;
import com.tarzan.maxkb4j.module.knowledge.domain.dto.GenerateProblemDTO;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.DocumentEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ProblemParagraphEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.DocumentVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.FileStreamVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphSimpleVO;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.TextSegmentVO;
import com.tarzan.maxkb4j.module.knowledge.enums.DocType;
import com.tarzan.maxkb4j.module.knowledge.excel.DatasetExcel;
import com.tarzan.maxkb4j.module.knowledge.mapper.DocumentMapper;
import com.tarzan.maxkb4j.module.model.info.vo.KeyAndValueVO;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author tarzan
 * @date 2024-12-25 17:00:26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService extends ServiceImpl<DocumentMapper, DocumentEntity> {

    private final ParagraphService paragraphService;
    private final ProblemService problemService;
    private final ProblemParagraphService problemParagraphService;
    private final DocumentParseService documentParseService;
    private final DocumentSpiltService documentSpiltService;
    private final MongoFileService mongoFileService;
    private final ApplicationEventPublisher eventPublisher;


    public void updateStatusMetaById(String id) {
        baseMapper.updateStatusMetaByIds(List.of(id));
    }


    //type 1向量化 2 生成问题 3同步
    public void updateStatusById(String id, int type, int status) {
        baseMapper.updateStatusByIds(List.of(id), type, status);
    }

    public void updateStatusByIds(List<String> ids, int type, int status) {
        baseMapper.updateStatusByIds(ids, type, status);
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
                            processQaFile(knowledgeId, zipIn, fileName);
                            break; // 如果只处理一个文件，则在此处跳出循环
                        }
                    }
                }
            }
            processQaFile(knowledgeId, uploadFile.getInputStream(), fileName);
        }
    }

    @Transactional
    public void importTable(String knowledgeId, MultipartFile[] file) throws IOException {
        List<String> docIds = new ArrayList<>();
        for (MultipartFile uploadFile : file) {
            List<String> list = documentParseService.extractTable(uploadFile.getInputStream());
            List<ParagraphEntity> paragraphs = new ArrayList<>();
            DocumentEntity doc = createDocument(knowledgeId, uploadFile.getOriginalFilename(), DocType.BASE.getType());
            if (!CollectionUtils.isEmpty(list)) {
                for (String text : list) {
                    doc.setCharLength(doc.getCharLength() + text.length());
                    ParagraphEntity paragraph = paragraphService.createParagraph(knowledgeId, doc.getId(), "", text, null);
                    paragraphs.add(paragraph);
                }
                doc.setMeta(upload(uploadFile));
                this.save(doc);
                paragraphService.saveBatch(paragraphs);
            }
            docIds.add(doc.getId());
        }
        eventPublisher.publishEvent(new DocumentIndexEvent(this, knowledgeId, docIds, List.of("0")));
    }


    private JSONObject upload(MultipartFile file) throws IOException {
        String fileId = mongoFileService.storeFile(file);
        return new JSONObject(Map.of("allow_download", true, "sourceFileId", fileId));
    }

    @Transactional
    protected void processQaFile(String knowledgeId, InputStream fis, String fileName) {
        List<ProblemEntity> knowledgeProblems = problemService.lambdaQuery().eq(ProblemEntity::getKnowledgeId, knowledgeId).list();
        List<ProblemEntity> problemEntities = new ArrayList<>();
        List<ProblemParagraphEntity> problemParagraphs = new ArrayList<>();
        List<ParagraphEntity> paragraphs = new ArrayList<>();
        List<DocumentEntity> docs = new ArrayList<>();
        DataListener<DatasetExcel> dataListener = new DataListener<>();
        try (ExcelReader excelReader = EasyExcel.read(fis, DatasetExcel.class, dataListener).build()) {
            List<ReadSheet> sheets = excelReader.excelExecutor().sheetList();
            for (ReadSheet sheet : sheets) {
                String sheetName = sheet.getSheetName() == null ? fileName : sheet.getSheetName();
                DocumentEntity doc = createDocument(knowledgeId, sheetName, DocType.BASE.getType());
                docs.add(doc);
                System.out.println("正在读取 Sheet: " + sheet.getSheetName());
                // 使用已构建的 excelReader 读取当前 sheet
                excelReader.read(sheet);
                List<DatasetExcel> dataList = dataListener.getDataList();
                for (DatasetExcel data : dataList) {
                    log.info("在Sheet {} 中读取到一条数据{}", sheet.getSheetName(), JSON.toJSONString(data));
                    ParagraphEntity paragraph = paragraphService.createParagraph(knowledgeId, doc.getId(), data.getTitle(), data.getContent(), null);
                    paragraphs.add(paragraph);
                    doc.setCharLength(doc.getCharLength() + paragraph.getContent().length());
                    if (StringUtils.isNotBlank(data.getProblems())) {
                        String[] problems = data.getProblems().split("\n");
                        for (String problem : problems) {
                            String problemId = IdWorker.get32UUID();
                            ProblemEntity existingProblem = problemService.findProblem(problem, knowledgeProblems);
                            if (existingProblem == null) {
                                ProblemEntity problemEntity = ProblemEntity.createDefault();
                                problemEntity.setId(problemId);
                                problemEntity.setKnowledgeId(knowledgeId);
                                problemEntity.setContent(problem);
                                problemEntities.add(problemEntity);
                                knowledgeProblems.add(problemEntity);
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
                dataListener.clear();
            }
        } catch (Exception e) {
            throw new RuntimeException("读取 Excel 失败", e);
        }
        this.saveBatch(docs);
        paragraphService.saveBatch(paragraphs);
        problemService.saveBatch(problemEntities);
        problemParagraphService.saveBatch(problemParagraphs);
        eventPublisher.publishEvent(new DocumentIndexEvent(this, knowledgeId, docs.stream().map(DocumentEntity::getId).toList(), List.of("0")));
    }


    private boolean isExistProblemParagraph(String paragraphId, String problemId, List<ProblemParagraphEntity> problemParagraphs) {
        if (CollectionUtils.isEmpty(problemParagraphs)) {
            return false;
        }
        return problemParagraphs.stream().anyMatch(e -> problemId.equals(e.getProblemId()) && paragraphId.equals(e.getParagraphId()));
    }


    public void exportExcelByDocId(String docId, HttpServletResponse response) {
        DocumentEntity doc = this.getById(docId);
        List<DatasetExcel> list = getDatasetExcelByDoc(doc);
        ExcelUtil.export(response, doc.getName(), doc.getName(), list, DatasetExcel.class);

    }

    public void exportExcelZipByDocId(String docId, HttpServletResponse response) throws IOException {
        DocumentEntity doc = this.getById(docId);
        exportExcelZipByDocs(List.of(doc), doc.getName(), response);
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
    public boolean batchCreateDoc(String knowledgeId, List<DocumentSimple> docs) {
        List<String> docIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(docs)) {
            List<DocumentEntity> documentEntities = new ArrayList<>();
            List<ParagraphEntity> paragraphEntities = new ArrayList<>();
            docs.parallelStream().forEach(e -> {
                DocumentEntity doc = createDocument(knowledgeId, e.getName(), DocType.BASE.getType());
                AtomicInteger docCharLength = new AtomicInteger();
                if (!CollectionUtils.isEmpty(e.getParagraphs())) {
                    e.getParagraphs().forEach(p -> {
                        paragraphEntities.add(paragraphService.createParagraph(knowledgeId, doc.getId(), p.getTitle(), p.getContent(), null));
                        docCharLength.addAndGet(p.getContent().length());
                    });
                }
                doc.setCharLength(docCharLength.get());
                String sourceFileId = e.getSourceFileId() == null ? "" : e.getSourceFileId();
                doc.setMeta(new JSONObject(Map.of("allow_download", true, "sourceFileId", sourceFileId)));
                documentEntities.add(doc);
            });
            if (!CollectionUtils.isEmpty(paragraphEntities)) {
                paragraphService.saveBatch(paragraphEntities);
            }
            this.saveBatch(documentEntities);
            documentEntities.forEach(doc -> docIds.add(doc.getId()));
            eventPublisher.publishEvent(new DocumentIndexEvent(this, knowledgeId, docIds, List.of("0")));
        }
        return true;
    }

    public DocumentEntity createDocument(String knowledgeId, String name, Integer type) {
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setId(IdWorker.get32UUID());
        documentEntity.setKnowledgeId(knowledgeId);
        documentEntity.setName(name);
        documentEntity.setMeta(new JSONObject());
        documentEntity.setCharLength(0);
        documentEntity.setStatus("nn0");
        documentEntity.setIsActive(true);
        documentEntity.setType(type);
        documentEntity.setHitHandlingMethod("optimization");
        documentEntity.setDirectlyReturnSimilarity(0.9);
        return documentEntity;
    }


    @Transactional
    public boolean deleteBatchDocByDocIds(String knowledgeId, List<String> docIds) {
        if (CollectionUtils.isEmpty(docIds)) {
            return false;
        }
        paragraphService.deleteByDocIds(knowledgeId, docIds);
        return this.lambdaUpdate().in(DocumentEntity::getId, docIds).remove();
    }

    @Transactional
    public boolean embedByDocIds(String knowledgeId, List<String> docIds, List<String> stateList) {
        eventPublisher.publishEvent(new DocumentIndexEvent(this, knowledgeId, docIds, stateList));
        return true;
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

    public DocumentEntity updateAndGetById(String docId, DocumentEntity documentEntity) {
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

    public IPage<DocumentVO> getDocByKnowledgeId(String knowledgeId, int current, int size, DocQuery query) {
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
                            //todo
                            fileStreams.add(new FileStreamVO(entryName, inputStream, ""));
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("解压ZIP文件失败", e);
                }
            } else {
                fileStreams.add(new FileStreamVO(file.getOriginalFilename(), file.getInputStream(), file.getContentType()));
            }
        }
        for (FileStreamVO fileStream : fileStreams) {
            TextSegmentVO textSegmentVO = new TextSegmentVO();
            textSegmentVO.setName(fileStream.getName());
            String docText = documentParseService.extractText(fileStream.getInputStream());
            List<TextSegment> textSegments = Collections.emptyList();
            if (StringUtils.isNotBlank(docText)) {
                textSegments = documentSpiltService.split(docText, patterns, limit, withFilter);
            }
            List<ParagraphSimpleVO> content = textSegments.stream()
                    .map(segment -> new ParagraphSimpleVO(segment.text()))
                    .collect(Collectors.toList());
            textSegmentVO.setContent(content);
            String fileId = mongoFileService.storeFile(fileStream.getInputStream(), fileStream.getName(), fileStream.getContentType());
            textSegmentVO.setSourceFileId(fileId);
            list.add(textSegmentVO);
        }
        return list;
    }


    /**
     * 判断是否为 ZIP 文件（通过文件头 MAGIC NUMBER）
     */
    private boolean isZipFile(MultipartFile file) {
        return Objects.requireNonNull(file.getOriginalFilename()).endsWith(".zip");
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
            Document html = JsoupUtil.getDocument(url);
            Elements elements = html.select(finalSelector);
            DocumentEntity doc = createDocument(knowledgeId, JsoupUtil.getTitle(html), DocType.WEB.getType());
            JSONObject meta = new JSONObject();
            meta.put("source_url", url);
            meta.put("selector", finalSelector);
            doc.setMeta(meta);
            List<TextSegment> textSegments = documentSpiltService.defaultSplit(elements.text());
            for (TextSegment textSegment : textSegments) {
                ParagraphEntity paragraph = paragraphService.createParagraph(knowledgeId, doc.getId(), "", textSegment.text(), null);
                paragraphs.add(paragraph);
                doc.setCharLength(doc.getCharLength() + paragraph.getContent().length());
            }
            docs.add(doc);
        });
        this.saveBatch(docs);
        paragraphService.saveBatch(paragraphs);
        eventPublisher.publishEvent(new DocumentIndexEvent(this, knowledgeId, docs.stream().map(DocumentEntity::getId).toList(), List.of("0")));
    }

    @Transactional
    public void sync(String knowledgeId, String docId) {
        DocumentEntity doc = this.getById(docId);
        deleteBatchDocByDocIds(knowledgeId, List.of(docId));
        webDoc(knowledgeId, List.of(doc.getMeta().getString("source_url")), doc.getMeta().getString("selector"));
    }

    public boolean batchGenerateRelated(String knowledgeId, GenerateProblemDTO dto) {
        eventPublisher.publishEvent(new GenerateProblemEvent(this, knowledgeId, dto.getDocumentIdList(), dto.getModelId(), dto.getPrompt(), dto.getStateList()));
        return true;
    }

    public boolean downloadSourceFile(String docId, HttpServletResponse response) throws IOException {
        DocumentEntity doc = this.getById(docId);
        JSONObject meta = doc.getMeta();
        if (meta.get("sourceFileId") != null) {
            String fileId = doc.getMeta().getString("sourceFileId");
            InputStream inputStream = mongoFileService.getStream(fileId);
            IoUtil.copy(inputStream, response.getOutputStream());
            return true;
        }
        return false;
    }
}
